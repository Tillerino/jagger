package org.tillerino.jagger.processor.features;

import com.google.auto.service.AutoService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerProcessor.Trigger;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.BlueprintConfigurator;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.CollectionUtil;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

@AutoService(JaggerPlugin.class)
public class Templates implements JaggerPlugin {
    public static final String JSON_TEMPLATE = "org.tillerino.jagger.annotations.JsonTemplate";
    public static final String JSON_TEMPLATES = "org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates";

    @Override
    public Collection<String> getSupportedAnnotationTypes() {
        return List.of(JSON_TEMPLATE, JSON_TEMPLATES);
    }

    @Override
    public void configure(JaggerContext ctx) {
        ctx.register(new SingleAnnotationDetector());
        ctx.register(new RepeatedAnnotationDetector());
    }

    private static void addTemplatesFromAnnotation(
            JaggerBlueprint blueprint, AnnotationMirrorWrapper templateAnnotation) {
        List<Template> templates = findTemplates(templateAnnotation);
        List<List<TypeMirror>> typeLists = findTypes(templateAnnotation);
        JaggerContext ctx = templateAnnotation.ctx();
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

                blueprint.prototypes.add(JaggerPrototype.of(
                        blueprint, instantiatedMethod, prototypeKind, ctx, false, new Trigger(blueprint.typeElement)));
            }
        }
    }

    private static List<Template> findTemplates(AnnotationMirrorWrapper templateAnnotation) {
        return templateAnnotation.method("templates", false).orElseThrow(Exceptions::unexpected).asArray().stream()
                .map(templateWrapper -> createTemplate(templateAnnotation, templateWrapper))
                .toList();
    }

    private static Template createTemplate(
            AnnotationMirrorWrapper templateAnnotation, AnnotationValueWrapper templateWrapper) {
        JaggerContext ctx = templateAnnotation.ctx();
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
            throw new ContextedRuntimeException("Prototype kind not templatable")
                    .addContextValue("prototype", template);
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
    }

    private static List<List<TypeMirror>> findTypes(AnnotationMirrorWrapper templateAnnotation) {
        Stream<List<TypeMirror>> plainTypes =
                templateAnnotation.method("types", true).orElseThrow(Exceptions::unexpected).asArray().stream()
                        .map(AnnotationValueWrapper::asTypeMirror)
                        .map(List::of);
        Stream<List<TypeMirror>> typeArrays =
                templateAnnotation.method("typeArrays", true).orElseThrow(Exceptions::unexpected).asArray().stream()
                        .map(
                                w -> w
                                        .asAnnotation()
                                        .method("value", false)
                                        .orElseThrow(Exceptions::unexpected)
                                        .asArray()
                                        .stream()
                                        .map(AnnotationValueWrapper::asTypeMirror)
                                        .toList());
        return Stream.concat(plainTypes, typeArrays).toList();
    }

    private record Template(InstantiatedMethod method, TemplatablePrototypeKind kind, List<TypeVar> typeVars) {}

    private static class SingleAnnotationDetector implements BlueprintConfigurator {
        @Override
        public List<String> supportedAnnotationTypes() {
            return List.of(JSON_TEMPLATE);
        }

        @Override
        public void configure(JaggerBlueprint blueprint, AnnotationMirrorWrapper annotation) {
            addTemplatesFromAnnotation(blueprint, annotation);
        }
    }

    private static class RepeatedAnnotationDetector implements BlueprintConfigurator {
        @Override
        public List<String> supportedAnnotationTypes() {
            return List.of(JSON_TEMPLATES);
        }

        @Override
        public void configure(JaggerBlueprint blueprint, AnnotationMirrorWrapper annotation) {
            List<AnnotationValueWrapper> annotations = annotation
                    .method("value", false)
                    .orElseThrow(Exceptions::unexpected)
                    .asArray();
            for (AnnotationValueWrapper templateAnnotation : annotations) {
                addTemplatesFromAnnotation(blueprint, templateAnnotation.asAnnotation());
            }
        }
    }
}
