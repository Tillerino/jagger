package org.tillerino.jagger.processor;

import static javax.tools.Diagnostic.Kind.ERROR;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonTemplate;
import org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.CodeGeneration;
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
        // blueprints are sorted so that the providers from breaking up circles remain in stable places
        ArrayList<JaggerBlueprint> blueprintsSorted = new ArrayList<>(ctx.blueprints.values());
        blueprintsSorted.sort(
                Comparator.comparing(b -> b.typeElement.getQualifiedName().toString()));

        List<GeneratedClass> toBeWritten = new ArrayList<>();
        for (JaggerBlueprint blueprint : blueprintsSorted) {
            if (!generatedClasses.add(blueprint.generatedClassName())) {
                continue;
            }
            if (CodeGeneration.shouldImplement(blueprint.config) && !blueprint.prototypes.isEmpty()) {
                try {
                    toBeWritten.add(generateMethods(blueprint));
                } catch (Exception e) {
                    logError(e, blueprint.typeElement);
                }
            }
        }

        // Classes' constructors might need each other as arguments. This is tricky.
        // First, we break up cyclic dependencies with providers.
        Map<JaggerBlueprint, GeneratedClass> others =
                toBeWritten.stream().collect(Collectors.toMap(gc -> gc.blueprint, gc -> gc));

        for (GeneratedClass generatedClass : toBeWritten) {
            generatedClass.breakCircle(new LinkedHashSet<>(), generatedClass.blueprint, others);
        }

        // Then we propagate through the dependency graph if classes require args for the constructor:
        // Once one class requires args for a constructor, dependent classes cannot initialize that class in a field,
        // and need to get an instance passed to their constructor.
        for (boolean changed = true; changed; ) {
            changed = false;
            for (GeneratedClass generatedClass : toBeWritten) {
                changed |= generatedClass.propagateRequiresArgsForConstructor(others);
            }
        }

        for (GeneratedClass generatedClass : toBeWritten) {
            try {
                finishGenerationAndWriteCompilationUnit(generatedClass, others);
            } catch (IOException e) {
                logError(e, generatedClass.blueprint.typeElement);
            }
        }
    }

    private GeneratedClass generateMethods(JaggerBlueprint blueprint) {
        Builder classBuilder = TypeSpec.classBuilder(blueprint.className.nameInCompilationUnit() + "Impl")
                .addModifiers(Modifier.PUBLIC);
        ctx.codeGeneration.addClassAnnotations(blueprint.config, classBuilder);
        ctx.codeGeneration.addSuper(blueprint.typeElement, classBuilder);
        GeneratedClass generatedClass = new GeneratedClass(classBuilder, ctx, blueprint);
        for (JaggerPrototype prototype : blueprint.prototypes) {
            try {
                if (!CodeGeneration.shouldImplement(prototype)) {
                    // method is implemented by user and can be used by us
                    continue;
                }
                classBuilder.addMethod(generateMethod(prototype, generatedClass));
            } catch (Exception ex) {
                logError(ex, prototype.trigger().element());
            }
        }
        return generatedClass;
    }

    private void finishGenerationAndWriteCompilationUnit(
            GeneratedClass generatedClass, Map<JaggerBlueprint, GeneratedClass> others) throws IOException {
        generatedClass.buildFields(others);
        ctx.codeGeneration.addRequiredConstructors(generatedClass);
        generatedClass.writeFile(processingEnv.getFiler());
        generatedClass.verificationForBlueprint.finish();
    }

    private MethodSpec generateMethod(JaggerPrototype method, GeneratedClass generatedClass) {
        MethodSpec.Builder methodBuilder =
                ctx.codeGeneration.getMethodBuilder(method.method(), method.overrides(), method.config());
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
