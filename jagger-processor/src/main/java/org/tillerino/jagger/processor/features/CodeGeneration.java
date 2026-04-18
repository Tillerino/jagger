package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.config.ConfigProperty.createConfigProperty;

import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.*;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.FullyQualifiedName.FullyQualifiedClassName;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public record CodeGeneration(AnnotationProcessorUtils utils) {

    public static ConfigProperty<JsonConfig.ImplementationMode> IMPLEMENT = createConfigProperty(
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE),
            List.of(AnnotationConfigPropertyRetriever.jsonConfigPropertyRetriever(
                    "implement", JsonConfig.ImplementationMode.class)),
            JsonConfig.ImplementationMode.DEFAULT,
            MergeFunction.notDefault(JsonConfig.ImplementationMode.DEFAULT),
            List.of());

    public static ConfigProperty<Set<TypeElement>> ON_GENERATED_CLASS = createConfigProperty(
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            List.of(new AnnotationConfigPropertyRetriever<>(
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
            List.of(new AnnotationConfigPropertyRetriever<>(
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
            List.of(new AnnotationConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig",
                    (ann, utils) ->
                            ann.method("addGeneratedAnnotationToClass", false).map(AnnotationValueWrapper::asBoolean))),
            true,
            MergeFunction.notDefault(true),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Boolean> ADD_GENERATED_ANNOTATION_TO_METHODS = createConfigProperty(
            List.of(ConfigProperty.LocationKind.BLUEPRINT, ConfigProperty.LocationKind.PROTOTYPE),
            List.of(new AnnotationConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig",
                    (ann, utils) -> ann.method("addGeneratedAnnotationToMethods", false)
                            .map(AnnotationValueWrapper::asBoolean))),
            false,
            MergeFunction.notDefault(false),
            ConfigProperty.PropagationKind.all());

    public static boolean isAbstractAndShouldImplement(ExecutableElement method, AnyConfig config) {
        return method.getModifiers().contains(Modifier.ABSTRACT) && shouldImplement(config);
    }

    public static boolean shouldImplement(AnyConfig config) {
        return config.resolveProperty(IMPLEMENT).value().shouldImplement();
    }

    public Builder getClassBuilder(FullyQualifiedClassName className, TypeElement typeElement, AnyConfig config) {
        Builder classBuilder = TypeSpec.classBuilder(className.nameInCompilationUnit() + "Impl")
                .addModifiers(Modifier.PUBLIC);
        addClassAnnotations(config, classBuilder);
        addSuper(typeElement, classBuilder);
        addRequiredConstructors(typeElement, classBuilder, config);
        return classBuilder;
    }

    public void addClassAnnotations(AnyConfig config, Builder classBuilder) {
        boolean addGenerated = config.resolveProperty(CodeGeneration.ADD_GENERATED_ANNOTATION_TO_CLASS)
                .value();
        if (addGenerated) {
            classBuilder.addAnnotation(AnnotationSpec.builder(
                            ClassName.get(utils.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
                    .build());
        }
        for (TypeElement annotation :
                config.resolveProperty(CodeGeneration.ON_GENERATED_CLASS).value()) {
            classBuilder.addAnnotation(
                    AnnotationSpec.builder(ClassName.get(annotation)).build());
        }
    }

    public void addSuper(TypeElement impl, Builder classBuilder) {
        if (impl.getKind() == ElementKind.INTERFACE) {
            classBuilder.addSuperinterface(impl.asType());
        } else {
            classBuilder.superclass(impl.asType());
        }
    }

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
            MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

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
            MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

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

    public MethodSpec.Builder getMethodBuilder(InstantiatedMethod method, boolean overrides, AnyConfig config) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.name());
        methodBuilder
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariables(method.element().getTypeParameters().stream()
                        .map(TypeParameterElement::getSimpleName)
                        .map(name -> TypeVariableName.get(name.toString()))
                        .toList())
                .returns(ClassName.get(method.returnType()));
        method.parameters().forEach(param -> methodBuilder.addParameter(ClassName.get(param.type()), param.name()));
        method.element().getThrownTypes().forEach(type -> methodBuilder.addException(ClassName.get(type)));
        if (overrides) {
            methodBuilder.addAnnotation(Override.class);
        }
        boolean addGenerated = config.resolveProperty(CodeGeneration.ADD_GENERATED_ANNOTATION_TO_METHODS)
                .value();
        if (addGenerated) {
            methodBuilder.addAnnotation(AnnotationSpec.builder(
                            ClassName.get(utils.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
                    .build());
        }

        return methodBuilder;
    }
}
