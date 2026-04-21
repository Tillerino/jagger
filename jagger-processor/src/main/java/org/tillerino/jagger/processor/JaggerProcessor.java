package org.tillerino.jagger.processor;

import static javax.tools.Diagnostic.Kind.ERROR;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.*;
import org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates;
import org.tillerino.jagger.processor.FullyQualifiedName.FullyQualifiedClassName;
import org.tillerino.jagger.processor.apis.*;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.CodeGeneration;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PrototypeKind;

@SupportedAnnotationTypes({
    "org.tillerino.jagger.annotations.JsonOutput",
    "org.tillerino.jagger.annotations.JsonInput",
    "org.tillerino.jagger.annotations.JsonConfig",
    "org.tillerino.jagger.annotations.JdbcSelect",
    "org.tillerino.jagger.annotations.JdbcInsert",
    "org.tillerino.jagger.annotations.JdbcUpdate",
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
        collect(
                roundEnv,
                new AnnotationAndConsumer(JsonOutput.class, element -> addPrototype(element, "@JsonOutput")),
                new AnnotationAndConsumer(JsonInput.class, element -> addPrototype(element, "@JsonInput")),
                new AnnotationAndConsumer(JdbcSelect.class, element -> addPrototype(element, "@JdbcSelect")),
                new AnnotationAndConsumer(JdbcInsert.class, element -> addPrototype(element, "@JdbcInsert")),
                new AnnotationAndConsumer(JdbcUpdate.class, element -> addPrototype(element, "@JdbcUpdate")));
        collectJsonTemplates(roundEnv); // collect last so that custom methods are appear first in implementations
    }

    private void collectJsonConfig(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonConfig.class).forEach(element -> {
            if (!(element instanceof TypeElement type)) {
                return;
            }
            setupUtils(processingEnv);
            try {
                JaggerBlueprint blueprint = utils.blueprint(type);
                for (ExecutableElement exec :
                        ElementFilter.methodsIn(utils.elements.getAllMembers((TypeElement) element))) {
                    if (!exec.getEnclosingElement().equals(element)) {
                        InstantiatedMethod instantiated =
                                utils.generics.instantiateMethod(exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
                        PrototypeKind.of(instantiated, utils).ifPresent(kind -> {
                            JaggerPrototype method = JaggerPrototype.of(
                                    blueprint, instantiated, kind, utils, true, new Trigger(element));
                            // should actually check if super method is not being generated and THIS is being
                            // generated
                            if (CodeGeneration.shouldImplement(method.config())) {
                                blueprint.prototypes.add(method);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logError(e, element);
            }
        });
    }

    private void collect(RoundEnvironment roundEnv, AnnotationAndConsumer... kinds) {
        Set<TypeElement> types = Stream.of(kinds)
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a.annotationClass()).stream())
                .map(el -> (TypeElement) el.getEnclosingElement())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (TypeElement type : types) {
            // This is the point of this way of processing the annotations: the order of the methods in generated
            // classes is supposed to be identical to the blueprint.
            for (Element elem : type.getEnclosedElements()) {
                for (AnnotationAndConsumer kind : kinds) {
                    Annotation annotation = elem.getAnnotation(kind.annotationClass());
                    if (annotation != null) {
                        try {
                            kind.action.accept(elem);
                        } catch (Exception e) {
                            logError(e, elem);
                        }
                    }
                }
            }
        }
    }

    private void addPrototype(Element element, String nick) {
        ExecutableElement exec = (ExecutableElement) element;
        TypeElement type = (TypeElement) exec.getEnclosingElement();
        setupUtils(processingEnv);
        JaggerBlueprint blueprint = utils.blueprint(type);
        InstantiatedMethod instantiated =
                utils.generics.instantiateMethod(exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
        PrototypeKind.of(instantiated, utils)
                .ifPresentOrElse(
                        kind -> {
                            JaggerPrototype method =
                                    JaggerPrototype.of(blueprint, instantiated, kind, utils, true, new Trigger(exec));
                            blueprint.prototypes.add(method);
                        },
                        () -> {
                            logError("Signature unknown. Please see " + nick + " for hints.", exec);
                        });
    }

    private void collectJsonTemplates(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonTemplate.class).forEach(element -> {
            try {
                if (!(element instanceof TypeElement type)) {
                    return;
                }
                setupUtils(processingEnv);
                JaggerBlueprint blueprint = utils.blueprint(type);
                blueprint.prototypes.addAll(
                        utils.templates.instantiateTemplatedPrototypesFromSingleAnnotation(blueprint));
            } catch (Exception e) {
                logError(e, element);
            }
        });
        roundEnv.getElementsAnnotatedWith(JsonTemplates.class).forEach(element -> {
            try {
                if (!(element instanceof TypeElement type)) {
                    return;
                }
                setupUtils(processingEnv);
                JaggerBlueprint blueprint = utils.blueprint(type);
                blueprint.prototypes.addAll(
                        utils.templates.instantiateTemplatedPrototypesFromMultipleAnnotations(blueprint));
            } catch (Exception e) {
                logError(e, element);
            }
        });
    }

    private void generateCode() {
        for (JaggerBlueprint blueprint : utils.blueprints.values()) {
            if (!generatedClasses.add(blueprint.generatedClassName())) {
                continue;
            }
            if (blueprint.prototypes.stream().anyMatch(method -> CodeGeneration.shouldImplement(method.config()))) {
                try {
                    setupUtils(processingEnv);
                    generateCode(blueprint);
                } catch (Exception e) {
                    logError(e, blueprint.typeElement);
                }
            }
        }
    }

    private void generateCode(JaggerBlueprint blueprint) throws IOException {
        AnyConfig config = blueprint.config;
        TypeElement typeElement = blueprint.typeElement;
        FullyQualifiedClassName className = blueprint.className;
        Builder classBuilder = utils.codeGeneration.getClassBuilder(className, typeElement, config);
        List<MethodSpec> methods = new ArrayList<>();
        GeneratedClass generatedClass = new GeneratedClass(classBuilder, utils, blueprint);
        for (JaggerPrototype prototype : blueprint.prototypes) {
            try {
                if (!CodeGeneration.isAbstractAndShouldImplement(prototype.methodElement(), prototype.config())) {
                    // method is implemented by user and can be used by us
                    continue;
                }
                methods.add(generateMethod(prototype, generatedClass));
            } catch (Exception ex) {
                logError(ex, prototype.trigger().element());
            }
        }
        generatedClass.buildFields(classBuilder);
        for (MethodSpec method : methods) {
            classBuilder.addMethod(method);
        }
        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(blueprint.generatedClassName());
        try (Writer writer = sourceFile.openWriter()) {
            JavaFile.Builder builder = JavaFile.builder(className.packageName(), classBuilder.build());
            generatedClass.fileBuilderMods.forEach(mod -> mod.accept(builder));
            JavaFile file = builder.build();
            file.writeTo(writer);
        }
        generatedClass.verificationForBlueprint.finish();
    }

    private MethodSpec generateMethod(JaggerPrototype method, GeneratedClass generatedClass) {
        MethodSpec.Builder methodBuilder = utils.codeGeneration.getMethodBuilder(
                method.asInstantiatedMethod(), method.overrides(), method.config());
        Supplier<CodeBlock.Builder> codeGenerator =
                switch (method.kind().direction()) {
                    case INPUT -> determineInputCodeGenerator(method, generatedClass);
                    case OUTPUT -> determineOutputCodeGenerator(method, generatedClass);
                    case JDBC_SELECT -> () -> new JdbcSelectGenerator(method, utils).build();
                    case JDBC_INSERT -> () -> new JdbcInsertGenerator(method, utils).build();
                    case JDBC_UPDATE -> () -> new JdbcUpdateGenerator(method, utils).build();
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

    private void logError(Exception e, Element element) {
        String msg = e != null ? e.getMessage() : null;
        if (System.getenv("JAGGER_DEBUG") != null) {
            e.printStackTrace();
        }
        logError(msg, element);
    }

    record AnnotationAndConsumer(Class<? extends Annotation> annotationClass, Consumer<Element> action) {}

    public record Trigger(Element element) {}
}
