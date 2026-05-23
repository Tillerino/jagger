package org.tillerino.jagger.processor.features;

import com.google.auto.service.AutoService;
import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerProcessor.Trigger;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.ext.BlueprintConfigurator;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind.MatchingOptions;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.CollectionUtil;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

@AutoService(JaggerPlugin.class)
public class Templates implements JaggerPlugin {
    static ConfigProperty<List<TemplateAnnotation>> TEMPLATES = new ConfigProperty<>(
            "TEMPLATES",
            List.of(LocationKind.BLUEPRINT),
            List.of(),
            MergeFunction.appendLists(),
            PropagationKind.none());

    public static final String JAGGER_TEMPLATE = "org.tillerino.jagger.annotations.JaggerTemplate";
    public static final String JAGGER_TEMPLATES = "org.tillerino.jagger.annotations.JaggerTemplate.JaggerTemplates";

    @Override
    public Collection<String> getSupportedAnnotationTypes() {
        return List.of(JAGGER_TEMPLATE, JAGGER_TEMPLATES);
    }

    @Override
    public void configure(JaggerContext ctx) {
        record TemplateConfigurator(String annotationType) implements BlueprintConfigurator {
            @Override
            public List<String> supportedAnnotationTypes() {
                return List.of(annotationType);
            }

            @Override
            public void configure(JaggerBlueprint blueprint, AnnotationMirrorWrapper annotation) {
                addTemplatesFromAnnotation(blueprint);
            }
        }
        ctx.register(new TemplateConfigurator(JAGGER_TEMPLATE));
        ctx.register(new TemplateConfigurator(JAGGER_TEMPLATES));

        ctx.configProperties.addConfigAnnotation(
                TEMPLATES,
                JAGGER_TEMPLATE,
                annotation -> Optional.of(List.of(TemplateAnnotation.fromAnnotation(annotation))));

        ctx.configProperties.addConfigAnnotation(TEMPLATES, JAGGER_TEMPLATES, annotation -> {
            List<TemplateAnnotation> annotations =
                    annotation.method("value", false).orElseThrow(Exceptions::unexpected).asArray().stream()
                            .map(AnnotationValueWrapper::asAnnotation)
                            .map(TemplateAnnotation::fromAnnotation)
                            .toList();
            return Optional.of(annotations);
        });
    }

    private static void addTemplatesFromAnnotation(JaggerBlueprint blueprint) {
        for (TemplateAnnotation templateAnnotation :
                blueprint.config.resolveProperty(TEMPLATES).value()) {
            for (List<TypeMirror> typeList : templateAnnotation.types()) {
                for (Template template : templateAnnotation.templates()) {
                    addPrototype(blueprint, template, typeList);
                }
            }
        }
    }

    private static void addPrototype(JaggerBlueprint blueprint, Template template, List<TypeMirror> typeList) {
        if (template.typeVars.size() != typeList.size()) {
            throw new ContextedRuntimeException("Mismatched number of type variables")
                    .addContextValue("template method name", template.method.name())
                    .addContextValue("type variables", template.typeVars)
                    .addContextValue("provided types", typeList);
        }
        TemplatablePrototypeKind prototypeKind = template.kind.withTypesPrefix(typeList);

        InstantiatedMethod instantiatedMethod = blueprint
                .ctx
                .generics
                .applyTypeBindings(template.method, CollectionUtil.mapLists(template.typeVars, typeList))
                .withName(prototypeKind.defaultMethodName());

        blueprint.prototypes.add(JaggerPrototype.of(
                blueprint,
                instantiatedMethod,
                prototypeKind,
                blueprint.ctx,
                false,
                new Trigger(blueprint.typeElement)));
    }

    public static Optional<Delegation.InstantiatedPrototype> delegateToAutoTemplate(
            JaggerContext ctx, TemplatablePrototypeKind target, JaggerPrototype caller) {
        for (TemplateAnnotation templateAnnotation :
                caller.config().resolveProperty(TEMPLATES).value()) {
            if (!templateAnnotation.auto()) {
                continue;
            }
            for (Template template : templateAnnotation.templates()) {
                LinkedHashMap<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
                Set<TypeVar> freeTypeVars = new LinkedHashSet<>(template.typeVars);
                if (template.kind.matches(target, new MatchingOptions(ctx, typeBindings, freeTypeVars, true))) {
                    List<TypeMirror> typeList = typeBindings.values().stream().toList();
                    TemplatablePrototypeKind protoKind = template.kind.withTypesPrefix(typeList);
                    InstantiatedMethod instantiated = ctx.generics
                            .applyTypeBindings(template.method, CollectionUtil.mapLists(template.typeVars, typeList))
                            .withName(protoKind.defaultMethodName());
                    JaggerBlueprint blueprint = caller.blueprint();
                    JaggerPrototype templatedPototype =
                            JaggerPrototype.of(blueprint, instantiated, protoKind, ctx, false, caller.trigger());
                    blueprint.prototypes.add(templatedPototype);
                    return Optional.of(
                            new Delegation.InstantiatedPrototype(blueprint, templatedPototype, instantiated));
                }
            }
        }
        return Optional.empty();
    }

    private record TemplateAnnotation(List<Template> templates, List<List<TypeMirror>> types, boolean auto) {
        private static TemplateAnnotation fromAnnotation(AnnotationMirrorWrapper templateAnnotation) {
            List<Template> templates = Template.fromAnnotation(templateAnnotation);
            List<List<TypeMirror>> types = findTypes(templateAnnotation);
            boolean auto = templateAnnotation.defaultMethod("auto").asBoolean();
            return new TemplateAnnotation(templates, types, auto);
        }

        private static List<List<TypeMirror>> findTypes(AnnotationMirrorWrapper templateAnnotation) {
            Stream<List<TypeMirror>> plainTypes = templateAnnotation.defaultMethod("types").asArray().stream()
                    .map(AnnotationValueWrapper::asTypeMirror)
                    .map(List::of);
            Stream<List<TypeMirror>> typeArrays = templateAnnotation.defaultMethod("typeArrays").asArray().stream()
                    .map(w -> w.asAnnotation().requiredMethod("value").asArray().stream()
                            .map(AnnotationValueWrapper::asTypeMirror)
                            .toList());
            return Stream.concat(plainTypes, typeArrays).toList();
        }
    }

    private record Template(InstantiatedMethod method, TemplatablePrototypeKind kind, List<TypeVar> typeVars) {
        private static List<Template> fromAnnotation(AnnotationMirrorWrapper templateAnnotation) {
            return templateAnnotation.requiredMethod("templates").asArray().stream()
                    .map(Template::createTemplate)
                    .toList();
        }

        private static Template createTemplate(AnnotationValueWrapper templateWrapper) {
            JaggerContext ctx = templateWrapper.ctx();
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
    }
}
