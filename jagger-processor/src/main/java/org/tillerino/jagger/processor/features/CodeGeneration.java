package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.config.ConfigProperty.createConfigProperty;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.ConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public record CodeGeneration(AnnotationProcessorUtils utils) {
    public static ConfigProperty<Set<TypeElement>> ON_GENERATED_CLASS = createConfigProperty(
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            List.of(new ConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig", (ann, utils) -> ann.method("onGeneratedClass", false)
                            .map(AnnotationValueWrapper::asArray)
                            .map(arr -> arr.stream()
                                    .map(classValue -> utils.elements.getTypeElement(
                                            classValue.asTypeMirror().toString()))
                                    .collect(Collectors.toSet())))),
            Set.of(),
            MergeFunction.mergeSets(),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Set<TypeElement>> ON_GENERATED_CONSTRUCTOR = createConfigProperty(
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            List.of(new ConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig",
                    (ann, utils) -> ann.method("onGeneratedConstructors", false)
                            .map(AnnotationValueWrapper::asArray)
                            .map(arr -> arr.stream()
                                    .map(classValue -> utils.elements.getTypeElement(
                                            classValue.asTypeMirror().toString()))
                                    .collect(Collectors.toSet())))),
            Set.of(),
            MergeFunction.mergeSets(),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Boolean> ADD_GENERATED_ANNOTATION_TO_CLASS = createConfigProperty(
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            List.of(new ConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig",
                    (ann, utils) ->
                            ann.method("addGeneratedAnnotationToClass", false).map(AnnotationValueWrapper::asBoolean))),
            true,
            MergeFunction.notDefault(true),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Boolean> ADD_GENERATED_ANNOTATION_TO_METHODS = createConfigProperty(
            List.of(ConfigProperty.LocationKind.BLUEPRINT, ConfigProperty.LocationKind.PROTOTYPE),
            List.of(new ConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig",
                    (ann, utils) -> ann.method("addGeneratedAnnotationToMethods", false)
                            .map(AnnotationValueWrapper::asBoolean))),
            false,
            MergeFunction.notDefault(false),
            ConfigProperty.PropagationKind.all());

    public void addRequiredConstructors(TypeElement type, Builder classBuilder, AnyConfig config) {
        Set<TypeElement> addAnnotations =
                config.resolveProperty(CodeGeneration.ON_GENERATED_CONSTRUCTOR).value();

        boolean addGeneratedAnnotation = config.resolveProperty(CodeGeneration.ADD_GENERATED_ANNOTATION_TO_METHODS)
                .value();

        List<ExecutableElement> constructors = type.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(e -> (ExecutableElement) e)
                .toList();

        for (ExecutableElement superConstructor : constructors) {
            MethodSpec.Builder builder = MethodSpec.constructorBuilder();

            if (superConstructor.getParameters().isEmpty() && constructors.size() == 1 && addAnnotations.isEmpty()) {
                // only default constructor, so not necessary to generate anything
                return;
            }

            if (addGeneratedAnnotation) {
                builder.addAnnotation(AnnotationSpec.builder(ClassName.get(
                                utils.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
                        .build());
            }

            for (TypeElement annotation : addAnnotations) {
                builder.addAnnotation(
                        AnnotationSpec.builder(ClassName.get(annotation)).build());
            }

            for (VariableElement parameter : superConstructor.getParameters()) {
                builder.addParameter(
                        TypeName.get(parameter.asType()),
                        parameter.getSimpleName().toString());
            }

            builder.addStatement(
                    "super($L)",
                    superConstructor.getParameters().stream()
                            .map(p -> p.getSimpleName().toString())
                            .collect(Collectors.joining(", ")));

            classBuilder.addMethod(builder.build());
        }

        if (constructors.isEmpty() && !addAnnotations.isEmpty()) {
            MethodSpec.Builder builder = MethodSpec.constructorBuilder();

            if (addGeneratedAnnotation) {
                builder.addAnnotation(AnnotationSpec.builder(ClassName.get(
                                utils.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
                        .build());
            }

            for (TypeElement annotation : addAnnotations) {
                builder.addAnnotation(
                        AnnotationSpec.builder(ClassName.get(annotation)).build());
            }

            classBuilder.addMethod(builder.build());
        }
    }
}
