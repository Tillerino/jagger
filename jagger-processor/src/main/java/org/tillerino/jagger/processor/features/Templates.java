package org.tillerino.jagger.processor.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PrototypeKind;
import org.tillerino.jagger.processor.util.PrototypeKind.TemplatablePrototypeKind;

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
        List<TypeMirror> types = findTypes(templateAnnotation);
        List<JaggerPrototype> instantiatedPrototypes = new ArrayList<>();
        for (TypeMirror type : types) {
            for (Template template : templates) {
                TemplatablePrototypeKind prototypeKind = template.kind.withInternalType(type);
                InstantiatedMethod instantiatedMethod = ctx.generics
                        .applyTypeBindings(template.method, Map.of(template.typeVar, type))
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
                    if (!(t.internalType() instanceof TypeVariable v)) {
                        throw new ContextedRuntimeException("Template prototype must serialize a type variable")
                                .addContextValue("prototype", template)
                                .addContextValue("serialized", t.internalType());
                    }
                    return new Template(template, t, TypeVar.of(v));
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<TypeMirror> findTypes(AnnotationMirrorWrapper templateAnnotation) {
        return templateAnnotation.method("types", false).orElseThrow(Exceptions::unexpected).asArray().stream()
                .map(AnnotationValueWrapper::asTypeMirror)
                .toList();
    }

    record Template(InstantiatedMethod method, TemplatablePrototypeKind kind, TypeVar typeVar) {}
}
