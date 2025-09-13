package org.tillerino.jagger.processor;

import static javax.tools.Diagnostic.Kind.ERROR;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.annotations.JsonTemplate;
import org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates;
import org.tillerino.jagger.processor.apis.*;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PrototypeKind;

@SupportedAnnotationTypes({
    "org.tillerino.jagger.annotations.JsonOutput",
    "org.tillerino.jagger.annotations.JsonInput",
    "org.tillerino.jagger.annotations.JsonConfig",
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class JaggerProcessor extends AbstractProcessor {

    AnnotationProcessorUtils utils;

    Set<String> generatedClasses = new LinkedHashSet<>();

    private void setupUtils(ProcessingEnvironment processingEnv) {
        if (utils == null) {
            // AFAICT, the typeElement is only used for type resolution, so the first processed type should do fine
            utils = new AnnotationProcessorUtils(processingEnv);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        collectElements(roundEnv);
        generateCode();
        return true;
    }

    private void collectElements(RoundEnvironment roundEnv) {
        collectJsonConfig(roundEnv);
        collectJsonOutput(roundEnv);
        collectJsonInput(roundEnv);
        collectJsonTemplates(roundEnv); // collect last so that custom methods are appear first in implementations
    }

    private void collectJsonConfig(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonConfig.class).forEach(element -> {
            if (!(element instanceof TypeElement type)) {
                return;
            }
            setupUtils(processingEnv);
            Exceptions.runWithContext(
                    () -> {
                        JaggerBlueprint blueprint = utils.blueprint(type);
                        for (ExecutableElement exec :
                                ElementFilter.methodsIn(utils.elements.getAllMembers((TypeElement) element))) {
                            if (!exec.getEnclosingElement().equals(element)) {
                                InstantiatedMethod instantiated = utils.generics.instantiateMethod(
                                        exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
                                PrototypeKind.of(instantiated, utils).ifPresent(kind -> {
                                    JaggerPrototype method =
                                            JaggerPrototype.of(blueprint, instantiated, kind, utils, true);
                                    // should actually check if super method is not being generated and THIS is being
                                    // generated
                                    if (method.config()
                                            .resolveProperty(ConfigProperty.IMPLEMENT)
                                            .value()
                                            .shouldImplement()) {
                                        blueprint.prototypes.add(method);
                                    }
                                });
                            }
                        }
                    },
                    "config",
                    element);
        });
    }

    private void collectJsonOutput(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonOutput.class).forEach(element -> {
            ExecutableElement exec = (ExecutableElement) element;
            TypeElement type = (TypeElement) exec.getEnclosingElement();
            setupUtils(processingEnv);
            JaggerBlueprint blueprint = utils.blueprint(type);
            InstantiatedMethod instantiated =
                    utils.generics.instantiateMethod(exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
            PrototypeKind.of(instantiated, utils)
                    .ifPresentOrElse(
                            kind -> {
                                JaggerPrototype method = JaggerPrototype.of(blueprint, instantiated, kind, utils, true);
                                blueprint.prototypes.add(method);
                            },
                            () -> {
                                logError("Signature unknown. Please see @JsonOutput for hints.", exec);
                            });
        });
    }

    private void collectJsonInput(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonInput.class).forEach(element -> {
            ExecutableElement exec = (ExecutableElement) element;
            TypeElement type = (TypeElement) exec.getEnclosingElement();
            setupUtils(processingEnv);
            JaggerBlueprint blueprint = utils.blueprint(type);
            InstantiatedMethod instantiated =
                    utils.generics.instantiateMethod(exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
            PrototypeKind.of(instantiated, utils)
                    .ifPresentOrElse(
                            kind -> {
                                JaggerPrototype method = JaggerPrototype.of(blueprint, instantiated, kind, utils, true);
                                blueprint.prototypes.add(method);
                            },
                            () -> {
                                logError("Signature unknown. Please see @JsonInput for hints.", exec);
                            });
        });
    }

    private void collectJsonTemplates(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonTemplate.class).forEach(element -> {
            if (!(element instanceof TypeElement type)) {
                return;
            }
            setupUtils(processingEnv);
            JaggerBlueprint blueprint = utils.blueprint(type);
            blueprint.prototypes.addAll(utils.templates.instantiateTemplatedPrototypesFromSingleAnnotation(blueprint));
        });
        roundEnv.getElementsAnnotatedWith(JsonTemplates.class).forEach(element -> {
            if (!(element instanceof TypeElement type)) {
                return;
            }
            setupUtils(processingEnv);
            JaggerBlueprint blueprint = utils.blueprint(type);
            blueprint.prototypes.addAll(
                    utils.templates.instantiateTemplatedPrototypesFromMultipleAnnotations(blueprint));
        });
    }

    private void generateCode() {
        for (JaggerBlueprint blueprint : utils.blueprints.values()) {
            if (!generatedClasses.add(blueprint.generatedClassName())) {
                continue;
            }
            if (blueprint.prototypes.stream().anyMatch(method -> method.config()
                    .resolveProperty(ConfigProperty.IMPLEMENT)
                    .value()
                    .shouldImplement())) {
                try {
                    setupUtils(processingEnv);
                    generateCode(blueprint);
                } catch (Exception e) {
                    if (e instanceof ContextedRuntimeException) {
                        System.out.println(((ContextedRuntimeException) e).getContextEntries());
                    }
                    e.printStackTrace();
                    String msg = e.getMessage() != null
                            ? e.getMessage()
                            : e.getClass().getName();
                    processingEnv.getMessager().printMessage(ERROR, msg);
                }
            }
        }
    }

    private void generateCode(JaggerBlueprint blueprint) throws IOException {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(blueprint.className.nameInCompilationUnit() + "Impl")
                .addModifiers(Modifier.PUBLIC);
        if (blueprint.typeElement.getKind() == ElementKind.INTERFACE) {
            classBuilder.addSuperinterface(blueprint.typeElement.asType());
        } else {
            classBuilder.superclass(blueprint.typeElement.asType());
            copyConstructors(blueprint.typeElement, classBuilder);
        }
        List<MethodSpec> methods = new ArrayList<>();
        GeneratedClass generatedClass = new GeneratedClass(classBuilder, utils, blueprint);
        for (JaggerPrototype method : blueprint.prototypes) {
            if (!method.methodElement().getModifiers().contains(Modifier.ABSTRACT)
                    || !method.config()
                            .resolveProperty(ConfigProperty.IMPLEMENT)
                            .value()
                            .shouldImplement()) {
                // method is implemented by user and can be used by us
                continue;
            }
            Exceptions.runWithContext(
                    () -> {
                        MethodSpec e = generateMethod(method, generatedClass);
                        if (e != null) {
                            methods.add(e);
                        }
                    },
                    "specification",
                    method);
        }
        generatedClass.buildFields(classBuilder);
        for (MethodSpec method : methods) {
            classBuilder.addMethod(method);
        }
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(blueprint.generatedClassName());
        try (Writer writer = sourceFile.openWriter()) {
            JavaFile.Builder builder = JavaFile.builder(blueprint.className.packageName(), classBuilder.build());
            generatedClass.fileBuilderMods.forEach(mod -> mod.accept(builder));
            JavaFile file = builder.build();
            file.writeTo(writer);
        }
        generatedClass.verificationForBlueprint.finish();
    }

    private void copyConstructors(TypeElement type, Builder classBuilder) {
        List<ExecutableElement> constructors = type.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(e -> (ExecutableElement) e)
                .toList();
        for (ExecutableElement superConstructor : constructors) {
            MethodSpec.Builder builder = MethodSpec.constructorBuilder();

            if (superConstructor.getParameters().isEmpty() && constructors.size() == 1) {
                // only default constructor, so not necessary to generate anything
                return;
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
    }

    private MethodSpec generateMethod(JaggerPrototype method, GeneratedClass generatedClass) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.name());
        if (method.overrides()) {
            methodBuilder.addAnnotation(Override.class);
        }
        methodBuilder
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariables(method.methodElement().getTypeParameters().stream()
                        .map(TypeParameterElement::getSimpleName)
                        .map(name -> TypeVariableName.get(name.toString()))
                        .toList())
                .returns(ClassName.get(method.instantiatedReturnType()));
        method.instantiatedParameters()
                .forEach(param -> methodBuilder.addParameter(ClassName.get(param.type()), param.name()));
        method.methodElement().getThrownTypes().forEach(type -> methodBuilder.addException(ClassName.get(type)));
        Supplier<CodeBlock.Builder> codeGenerator =
                switch (method.kind().direction()) {
                    case INPUT -> determineInputCodeGenerator(method, generatedClass);
                    case OUTPUT -> determineOutputCodeGenerator(method, generatedClass);
                };
        methodBuilder.addCode(codeGenerator.get().build());
        return methodBuilder.build();
    }

    private Supplier<CodeBlock.Builder> determineOutputCodeGenerator(
            JaggerPrototype method, GeneratedClass generatedClass) {
        return switch (method.kind().jsonType().toString()) {
            case PrototypeKind.JACKSON_JSON_GENERATOR -> new JacksonJsonGeneratorWriterGenerator(
                    utils, method, generatedClass)::build;
            case PrototypeKind.GSON_JSON_WRITER -> new GsonJsonWriterWriterGenerator(utils, method, generatedClass)
                    ::build;
            case PrototypeKind.FASTJSON_2_JSONWRITER -> new Fastjson2WriterGenerator(utils, method, generatedClass)
                    ::build;
            case PrototypeKind.JAKARTA_JSON_GENERATOR -> new JakartaJsonGeneratorGenerator(
                    utils, method, generatedClass)::build;
            case PrototypeKind.NANOJSON_JSON_WRITER -> new NanojsonWriterGenerator(utils, method, generatedClass)
                    ::build;
            case PrototypeKind.JAGGER_WRITER -> new JaggerWriterGenerator(utils, method, generatedClass)::build;
            default -> throw new ContextedRuntimeException(
                    "Unknown output type: " + method.kind().jsonType());
        };
    }

    private Supplier<CodeBlock.Builder> determineInputCodeGenerator(
            JaggerPrototype method, GeneratedClass generatedClass) {
        return switch (method.kind().jsonType().toString()) {
            case PrototypeKind.JACKSON_JSON_PARSER -> new JacksonJsonParserReaderGenerator(
                    utils, method, generatedClass)::build;
            case PrototypeKind.GSON_JSON_READER -> new GsonJsonReaderReaderGenerator(utils, method, generatedClass)
                    ::build;
            case PrototypeKind.FASTJSON_2_JSONREADER -> new Fastjson2ReaderGenerator(utils, method, generatedClass)
                    ::build;
            case PrototypeKind.JAKARTA_JSON_PARSER -> new JakartaJsonParserGenerator(utils, method, generatedClass)
                    ::build;
            case PrototypeKind.JAGGER_READER -> new JaggerReaderGenerator(utils, method, generatedClass)::build;
            default -> throw new ContextedRuntimeException(
                    "Unknown input type: " + method.kind().jsonType());
        };
    }

    private void logError(String msg, Element element) {
        processingEnv.getMessager().printMessage(ERROR, msg != null ? msg : "(null)", element);
    }
}
