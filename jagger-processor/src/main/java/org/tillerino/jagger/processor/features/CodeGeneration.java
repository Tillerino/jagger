package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.config.ConfigProperty.createConfigProperty;

import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class CodeGeneration {

    public static ConfigProperty<JsonConfig.ImplementationMode> IMPLEMENT = createConfigProperty(
            "IMPLEMENT",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE),
            JsonConfig.ImplementationMode.DEFAULT,
            MergeFunction.notDefault(),
            List.of());

    public static ConfigProperty<Set<TypeElement>> ON_GENERATED_CLASS = createConfigProperty(
            "ON_GENERATED_CLASS",
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            Set.of(),
            MergeFunction.mergeSets(),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Set<TypeElement>> ON_GENERATED_CONSTRUCTOR = createConfigProperty(
            "ON_GENERATED_CONSTRUCTOR",
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            Set.of(),
            MergeFunction.mergeSets(),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Boolean> ADD_GENERATED_ANNOTATION_TO_CLASS = createConfigProperty(
            "ADD_GENERATED_ANNOTATION_TO_CLASS",
            List.of(ConfigProperty.LocationKind.BLUEPRINT),
            true,
            MergeFunction.notDefault(),
            ConfigProperty.PropagationKind.none());

    public static ConfigProperty<Boolean> ADD_GENERATED_ANNOTATION_TO_METHODS = createConfigProperty(
            "ADD_GENERATED_ANNOTATION_TO_METHODS",
            List.of(ConfigProperty.LocationKind.BLUEPRINT, ConfigProperty.LocationKind.PROTOTYPE),
            false,
            MergeFunction.notDefault(),
            ConfigProperty.PropagationKind.all());

    public final JaggerContext ctx;
    public final ConfigProperty<TypeMirror> provider;

    public CodeGeneration(JaggerContext ctx) {
        this.ctx = ctx;
        provider = new ConfigProperty<>(
                "PROVIDER",
                List.of(LocationKind.BLUEPRINT),
                ctx.elements.getTypeElement("java.lang.Object").asType(),
                MergeFunction.notDefault(),
                List.of());
    }

    public static boolean shouldImplement(JaggerPrototype prototype) {
        return (prototype.element().getModifiers().contains(Modifier.ABSTRACT)
                        || prototype.kind().decorates(prototype.config()))
                && shouldImplement(prototype.config());
    }

    public static boolean shouldImplement(AnyConfig config) {
        return config.resolveProperty(IMPLEMENT).value().shouldImplement();
    }

    public void addClassAnnotations(AnyConfig config, Builder classBuilder) {
        boolean addGenerated = config.resolveProperty(CodeGeneration.ADD_GENERATED_ANNOTATION_TO_CLASS)
                .value();
        if (addGenerated) {
            classBuilder.addAnnotation(AnnotationSpec.builder(
                            ClassName.get(ctx.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
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

    public void addRequiredConstructors(GeneratedClass generatedClass) {
        TypeElement type = generatedClass.blueprint.typeElement;
        Builder classBuilder = generatedClass.typeBuilder;
        AnyConfig config = generatedClass.blueprint.config;
        Set<TypeElement> addAnnotations =
                config.resolveProperty(CodeGeneration.ON_GENERATED_CONSTRUCTOR).value();

        boolean addGeneratedAnnotation = config.resolveProperty(CodeGeneration.ADD_GENERATED_ANNOTATION_TO_METHODS)
                .value();

        List<ExecutableElement> constructors = type.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(e -> (ExecutableElement) e)
                .toList();

        List<FieldSpec> finalFields = generatedClass.uninitializedFields().toList();

        for (ExecutableElement superConstructor : constructors) {
            MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            if (superConstructor.getParameters().isEmpty()
                    && constructors.size() == 1
                    && addAnnotations.isEmpty()
                    && finalFields.isEmpty()) {
                // only default constructor, so not necessary to generate anything
                return;
            }

            addConstructorAnnotations(builder, addGeneratedAnnotation, addAnnotations);

            addSuperParametersAndCallSuper(superConstructor, builder);

            addAndInitializeFieldsInConstructor(finalFields, builder);

            classBuilder.addMethod(builder.build());
        }

        if (constructors.isEmpty() && (!addAnnotations.isEmpty() || !finalFields.isEmpty())) {
            MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            addConstructorAnnotations(builder, addGeneratedAnnotation, addAnnotations);
            addAndInitializeFieldsInConstructor(finalFields, builder);

            classBuilder.addMethod(builder.build());
        }
    }

    private void addConstructorAnnotations(
            MethodSpec.Builder builder, boolean addGeneratedAnnotation, Set<TypeElement> addAnnotations) {
        if (addGeneratedAnnotation) {
            builder.addAnnotation(AnnotationSpec.builder(
                            ClassName.get(ctx.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
                    .build());
        }

        for (TypeElement annotation : addAnnotations) {
            builder.addAnnotation(
                    AnnotationSpec.builder(ClassName.get(annotation)).build());
        }
    }

    private void addSuperParametersAndCallSuper(ExecutableElement superConstructor, MethodSpec.Builder builder) {
        for (VariableElement parameter : superConstructor.getParameters()) {
            builder.addParameter(
                    TypeName.get(parameter.asType()), parameter.getSimpleName().toString());
        }

        builder.addStatement(
                "super($L)",
                superConstructor.getParameters().stream()
                        .map(p -> p.getSimpleName().toString())
                        .collect(Collectors.joining(", ")));
    }

    private void addAndInitializeFieldsInConstructor(List<FieldSpec> finalFields, MethodSpec.Builder builder) {
        for (FieldSpec finalField : finalFields) {
            builder.addParameter(finalField.type, finalField.name);
            builder.addStatement("this.$L = $L", finalField.name, finalField.name);
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
                            ClassName.get(ctx.elements.getTypeElement("org.tillerino.jagger.annotations.Generated")))
                    .build());
        }

        return methodBuilder;
    }

    public TypeName providerType(TypeName type, AnyConfig config) {
        DeclaredType t = resolveProviderType(config);
        return ParameterizedTypeName.get(ClassName.get((TypeElement) t.asElement()), type);
    }

    private DeclaredType resolveProviderType(AnyConfig config) {
        TypeMirror value = config.resolveProperty(provider).value();
        if (ctx.types.isSameType(ctx.commonTypes.object, value)) {
            value = ctx.commonTypes.supplier;
        }
        if (!(value instanceof DeclaredType t)) {
            throw new ContextedRuntimeException("Type not allowed as provider: " + value);
        }
        return t;
    }

    public String callProvider(String literal, AnyConfig config) {
        DeclaredType declaredType = resolveProviderType(config);
        if (declaredType.getTypeArguments().size() != 1) {
            throw new ContextedRuntimeException("Provider type must have exactly one type parameter")
                    .addContextValue("type", declaredType);
        }
        TypeMirror typeArgument = declaredType.getTypeArguments().get(0);
        for (InstantiatedMethod instantiateMethod : ctx.generics.instantiateMethods(declaredType, null)) {
            if (instantiateMethod.parameters().isEmpty()
                    && ctx.types.isSameType(instantiateMethod.returnType(), typeArgument)) {
                return "%s.%s()".formatted(literal, instantiateMethod.name());
            }
        }
        throw new ContextedRuntimeException("Cannot find provider method on provider type")
                .addContextValue("type", declaredType);
    }
}
