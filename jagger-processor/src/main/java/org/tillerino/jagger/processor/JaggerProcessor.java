package org.tillerino.jagger.processor;

import static javax.tools.Diagnostic.Kind.ERROR;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonTemplate;
import org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.CodeGeneration;
import org.tillerino.jagger.processor.util.FullyQualifiedName.FullyQualifiedClassName;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class JaggerProcessor extends AbstractProcessor {

    JaggerContext ctx;

    Set<String> generatedClasses = new LinkedHashSet<>();

    private void setupUtils(ProcessingEnvironment processingEnv) {
        if (ctx == null) {
            ctx = new JaggerContext(processingEnv);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ServiceLoader.load(JaggerPlugin.class, JaggerProcessor.class.getClassLoader()).stream()
                .flatMap(plugin -> plugin.get().getSupportedAnnotationTypes().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        setupUtils(processingEnv);
        collectJsonConfig(roundEnv);
        collectMethodGenerators(roundEnv);
        collectJsonTemplates(roundEnv); // collect last so that custom methods are appear first in implementations
        generateCode();
        return true;
    }

    private void collectJsonConfig(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonConfig.class).forEach(element -> {
            if (!(element instanceof TypeElement type)) {
                return;
            }
            try {
                JaggerBlueprint blueprint = ctx.blueprint(type);
                for (ExecutableElement exec : ElementFilter.methodsIn(ctx.elements.getAllMembers(type))) {
                    if (!exec.getEnclosingElement().equals(element)) {
                        InstantiatedMethod instantiated =
                                ctx.generics.instantiateMethod(exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
                        ctx.detectPrototype(instantiated).ifPresent(kind -> {
                            JaggerPrototype prototype =
                                    JaggerPrototype.of(blueprint, instantiated, kind, ctx, true, new Trigger(element));
                            // should actually check if super method is not being generated and THIS is being
                            // generated
                            if (CodeGeneration.shouldImplement(prototype)) {
                                blueprint.prototypes.add(prototype);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                logError(e, element);
            }
        });
    }

    private void collectMethodGenerators(RoundEnvironment roundEnv) {
        Set<TypeElement> types = ctx.detectors.stream()
                .flatMap(detector -> detector.supportedAnnotationTypes().stream()
                        .filter(Objects::nonNull)
                        .flatMap(t -> roundEnv.getElementsAnnotatedWith(t).stream()))
                .filter(elem -> elem.getKind() == ElementKind.METHOD)
                .map(el -> (TypeElement) el.getEnclosingElement())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (TypeElement type : types) {
            // This is the point of this way of processing the annotations: the order of the methods in generated
            // classes is supposed to be identical to the blueprint.
            for (ExecutableElement elem : ElementFilter.methodsIn(type.getEnclosedElements())) {
                runDetectorsOnMethods(elem);
            }
        }
    }

    private void runDetectorsOnMethods(ExecutableElement elem) {
        for (PrototypeDetector detector : ctx.detectors) {
            for (TypeElement supportedAnnotationType : detector.supportedAnnotationTypes()) {
                if (supportedAnnotationType == null) {
                    continue;
                }
                if (ctx.annotations
                        .findAnnotation(elem, supportedAnnotationType)
                        .isPresent()) {
                    try {
                        addPrototype(
                                elem, supportedAnnotationType.getSimpleName().toString());
                    } catch (Exception e) {
                        logError(e, elem);
                    }
                }
            }
        }
    }

    private void addPrototype(ExecutableElement exec, String nick) {
        TypeElement type = (TypeElement) exec.getEnclosingElement();
        JaggerBlueprint blueprint = ctx.blueprint(type);
        InstantiatedMethod instantiated =
                ctx.generics.instantiateMethod(exec, blueprint.typeBindings, LocationKind.PROTOTYPE);
        ctx.detectPrototype(instantiated)
                .ifPresentOrElse(
                        kind -> {
                            JaggerPrototype method =
                                    JaggerPrototype.of(blueprint, instantiated, kind, ctx, true, new Trigger(exec));
                            blueprint.prototypes.add(method);
                        },
                        () -> logError("Signature unknown. Please see @" + nick + " for hints.", exec));
    }

    private void collectJsonTemplates(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonTemplate.class).forEach(element -> {
            try {
                if (!(element instanceof TypeElement type)) {
                    return;
                }
                JaggerBlueprint blueprint = ctx.blueprint(type);
                blueprint.prototypes.addAll(
                        ctx.templates.instantiateTemplatedPrototypesFromSingleAnnotation(blueprint));
            } catch (Exception e) {
                logError(e, element);
            }
        });
        roundEnv.getElementsAnnotatedWith(JsonTemplates.class).forEach(element -> {
            try {
                if (!(element instanceof TypeElement type)) {
                    return;
                }
                JaggerBlueprint blueprint = ctx.blueprint(type);
                blueprint.prototypes.addAll(
                        ctx.templates.instantiateTemplatedPrototypesFromMultipleAnnotations(blueprint));
            } catch (Exception e) {
                logError(e, element);
            }
        });
    }

    private void generateCode() {
        for (JaggerBlueprint blueprint : ctx.blueprints.values()) {
            if (!generatedClasses.add(blueprint.generatedClassName())) {
                continue;
            }
            if (CodeGeneration.shouldImplement(blueprint.config) && !blueprint.prototypes.isEmpty()) {
                try {
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
        Builder classBuilder = ctx.codeGeneration.getClassBuilder(className, typeElement, config);
        List<MethodSpec> methods = new ArrayList<>();
        GeneratedClass generatedClass = new GeneratedClass(classBuilder, ctx, blueprint);
        for (JaggerPrototype prototype : blueprint.prototypes) {
            try {
                if (!CodeGeneration.shouldImplement(prototype)) {
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
        MethodSpec.Builder methodBuilder =
                ctx.codeGeneration.getMethodBuilder(method.asInstantiatedMethod(), method.overrides(), method.config());
        methodBuilder.addCode(method.kind()
                .generateCode(new CodeGeneratorContext(ctx, method, generatedClass))
                .build());
        return methodBuilder.build();
    }

    private void logError(String msg, Element element) {
        processingEnv.getMessager().printMessage(ERROR, msg != null ? msg : "(null)", element);
    }

    private void logError(Exception e, Element element) {
        String msg = e != null ? e.getMessage() : null;
        if (JaggerContext.isJaggerDebug()) {
            e.printStackTrace();
        }
        logError(msg, element);
    }

    public record Trigger(Element element) {}
}
