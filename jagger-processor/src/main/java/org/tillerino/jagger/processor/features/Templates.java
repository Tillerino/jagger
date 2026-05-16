package org.tillerino.jagger.processor.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.JsonTemplate;
import org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerProcessor.Trigger;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.CollectionUtil;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public record Templates(JaggerContext ctx) {
    public List<JaggerPrototype> instantiateTemplatedPrototypesFromSingleAnnotation(JaggerBlueprint blueprint) {
        AnnotationMirrorWrapper templateAnnotation = ctx.annotations
                .findAnnotation(blueprint.typeElement, JsonTemplate.class.getCanonicalName())
                .orElseThrow(Exceptions::unexpected);
        return createTemplatesFromAnnotation(blueprint, templateAnnotation);
    }

    public List<JaggerPrototype> instantiateTemplatedPrototypesFromMultipleAnnotations(JaggerBlueprint blueprint) {
        List<JaggerPrototype> instantiatedPrototypes = new ArrayList<>();
        ctx.annotations
                .findAnnotation(blueprint.typeElement, JsonTemplates.class.getCanonicalName())
                .orElseThrow(Exceptions::unexpected)
                .method("value", false)
                .orElseThrow(Exceptions::unexpected)
                .asArray()
                .forEach(templateAnnotation -> instantiatedPrototypes.addAll(
                        createTemplatesFromAnnotation(blueprint, templateAnnotation.asAnnotation())));
        return instantiatedPrototypes;
    }

    private List<JaggerPrototype> createTemplatesFromAnnotation(
            JaggerBlueprint blueprint, AnnotationMirrorWrapper templateAnnotation) {
        List<Template> templates = findTemplates(templateAnnotation);
        List<List<TypeMirror>> typeLists = findTypes(templateAnnotation);
        List<JaggerPrototype> instantiatedPrototypes = new ArrayList<>();
        for (List<TypeMirror> typeList : typeLists) {
            for (Template template : templates) {
                if (template.typeVars.size() != typeList.size()) {
                    throw new ContextedRuntimeException("Mismatched number of type variables")
                            .addContextValue("template method name", template.method.name())
                            .addContextValue("type variables", template.typeVars)
                            .addContextValue("provided types", typeList);
                }
                TemplatablePrototypeKind prototypeKind = template.kind.withTypesPrefix(typeList);
                InstantiatedMethod instantiatedMethod = ctx.generics
                        .applyTypeBindings(template.method, CollectionUtil.mapLists(template.typeVars, typeList))
                        .withName(prototypeKind.defaultMethodName());

                instantiatedPrototypes.add(JaggerPrototype.of(
                        blueprint, instantiatedMethod, prototypeKind, ctx, false, new Trigger(blueprint.typeElement)));
            }
        }
        return instantiatedPrototypes;
    }

    private List<Template> findTemplates(AnnotationMirrorWrapper templateAnnotation) {
        return templateAnnotation.method("templates", false).orElseThrow(Exceptions::unexpected).asArray().stream()
                .map(templateWrapper -> {
                    TypeMirror templateType = templateWrapper.asTypeMirror();
                    List<InstantiatedMethod> templateMethods =
                            ctx.generics.instantiateMethods(templateType, LocationKind.PROTOTYPE);
                    if (templateMethods.size() != 1) {
                        throw new ContextedRuntimeException("Template is not a functional interface")
                                .addContextValue("template", templateType);
                    }
                    InstantiatedMethod template = templateMethods.get(0);
                    PrototypeKind prototypeKind = ctx.detectPrototype(template)
                            .orElseThrow(() -> new ContextedRuntimeException("Template prototype of unknown kind")
                                    .addContextValue("prototype", template));
                    if (!(prototypeKind instanceof TemplatablePrototypeKind t)) {
                        return null;
                    }
                    List<TypeVar> typeVars = t.types().stream()
                            .filter(type -> type instanceof TypeVariable)
                            .map(TypeVariable.class::cast)
                            .map(TypeVar::of)
                            .toList();
                    if (typeVars.isEmpty()) {
                        throw new ContextedRuntimeException("Template prototype must use at least one type variable")
                                .addContextValue("prototype", template);
                    }
                    return new Template(template, t, typeVars);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<List<TypeMirror>> findTypes(AnnotationMirrorWrapper templateAnnotation) {
        Stream<List<TypeMirror>> plainTypes =
                templateAnnotation.method("types", true).orElseThrow(Exceptions::unexpected).asArray().stream()
                        .map(AnnotationValueWrapper::asTypeMirror)
                        .map(List::of);
        Stream<List<TypeMirror>> typeArrays =
                templateAnnotation.method("typeArrays", true).orElseThrow(Exceptions::unexpected).asArray().stream()
                        .map(
                                w -> w
                                        .asAnnotation()
                                        .method("value", true)
                                        .orElseThrow(Exceptions::unexpected)
                                        .asArray()
                                        .stream()
                                        .map(AnnotationValueWrapper::asTypeMirror)
                                        .toList());
        return Stream.concat(plainTypes, typeArrays).toList();
    }

    record Template(InstantiatedMethod method, TemplatablePrototypeKind kind, List<TypeVar> typeVars) {}
}
