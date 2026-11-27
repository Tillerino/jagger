package org.tillerino.jagger.processor;

import static javax.tools.Diagnostic.Kind.ERROR;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.io.IOException;
import java.util.*;
import java.util.ServiceLoader.Provider;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.ext.BlueprintConfigurator;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.CodeGeneration;
import org.tillerino.jagger.processor.features.Templates;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;

@AutoService(Processor.class)
public class JaggerProcessor extends AbstractProcessor {

    JaggerContext ctx;

    Set<String> generatedClasses = new LinkedHashSet<>();

    List<JaggerPlugin> plugins;

    private void setupContext(ProcessingEnvironment processingEnv) {
        if (ctx != null) {
            return;
        }

        ctx = new JaggerContext(processingEnv);

        plugins.forEach(plugin -> plugin.configure(ctx));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        plugins = ServiceLoader.load(JaggerPlugin.class, JaggerProcessor.class.getClassLoader()).stream()
                .map(Provider::get)
                .peek(plugin -> {
                    if (isJaggerDebug()) {
                        System.out.println("Detected JaggerPlugin: " + plugin);
                    }
                })
                .toList();

        return plugins.stream()
                .flatMap(plugin -> plugin.getSupportedAnnotationTypes().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        long time = System.currentTimeMillis();
        setupContext(processingEnv);
        findClassesCompiledByUs(roundEnv);
        collectPrototypelessBlueprints(roundEnv);
        collectPrototypeDetectors(roundEnv);
        // blueprint configurators run later so they can modify results of prototype detectors
        runBlueprintConfigurators(roundEnv);
        generateCode();
        if (isJaggerDebug()) {
            System.out.println("Jagger processor took " + (System.currentTimeMillis() - time) + "ms");
        }
        return true;
    }

    private void findClassesCompiledByUs(RoundEnvironment roundEnv) {
        Stream.of(
                        ctx.prototypeDetectors.keySet().stream(),
                        ctx.blueprintConfigurators.keySet().stream(),
                        Stream.of(JsonConfig.class.getCanonicalName()))
                .flatMap(Function.identity())
                .map(ctx.elements::getTypeElement)
                .<Element>flatMap(t -> roundEnv.getElementsAnnotatedWith(t).stream())
                .map(elem -> {
                    while (elem != null) {
                        if (elem instanceof TypeElement te) {
                            return te;
                        }
                        elem = elem.getEnclosingElement();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .forEach(ctx.classesCompiledByUs::add);
    }

    private void collectPrototypelessBlueprints(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(JsonConfig.class).forEach(element -> {
            if (!(element instanceof TypeElement type)) {
                return;
            }
            try {
                ctx.blueprint(type);
            } catch (Exception e) {
                logError(e, element);
            }
        });
    }

    private void collectPrototypeDetectors(RoundEnvironment roundEnv) {
        Set<TypeElement> types = ctx.prototypeDetectors.keySet().stream()
                .flatMap(t -> roundEnv.getElementsAnnotatedWith(ctx.elements.getTypeElement(t)).stream())
                .filter(elem -> elem.getKind() == ElementKind.METHOD)
                .map(el -> (TypeElement) el.getEnclosingElement())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (TypeElement type : types) {
            try {
                ctx.blueprint(type);
            } catch (Exception e) {
                logError(e, type);
            }
        }
    }

    private void runBlueprintConfigurators(RoundEnvironment roundEnv) {
        LinkedHashSet<? extends Element> allElements = ctx.blueprintConfigurators.keySet().stream()
                .flatMap(t -> roundEnv.getElementsAnnotatedWith(ctx.elements.getTypeElement(t)).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (TypeElement type : ElementFilter.typesIn(allElements)) {
            for (AnnotationMirror annotationMirror : type.getAnnotationMirrors()) {
                BlueprintConfigurator configurator = ctx.blueprintConfigurators.get(
                        annotationMirror.getAnnotationType().toString());
                if (configurator == null) {
                    continue;
                }
                configurator.configure(ctx.blueprint(type), new AnnotationMirrorWrapper(annotationMirror, ctx));
            }
        }
    }

    private void generateCode() {
        // blueprints are sorted so that the providers from breaking up circles remain in stable places. blueprints
        // with auto-templates are generated last, since other blueprints may add prototypes to them while generating.
        ArrayList<JaggerBlueprint> blueprintsSorted = new ArrayList<>(ctx.blueprints.values());
        blueprintsSorted.removeIf(b -> !ctx.classesCompiledByUs.contains(b.typeElement));
        blueprintsSorted.sort(Comparator.comparing((JaggerBlueprint b) -> Templates.hasAutoTemplates(b) ? 1 : 0)
                .thenComparing(b -> b.typeElement.getQualifiedName().toString()));

        List<GeneratedClass> toBeWritten = new ArrayList<>();
        for (JaggerBlueprint blueprint : blueprintsSorted) {
            if (!generatedClasses.add(blueprint.generatedClassName())) {
                continue;
            }
            if (CodeGeneration.shouldImplement(blueprint.config)
                    && blueprint.prototypes.stream().anyMatch(p -> p.kind().isGenerated())) {
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
        // we use an index here, since auto-templates can extend the list while we are generating
        for (int i = 0; i < blueprint.prototypes.size(); i++) {
            JaggerPrototype prototype = blueprint.prototypes.get(i);
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
        if (isJaggerDebug()) {
            e.printStackTrace();
        }
        logError(msg, element);
    }

    public static boolean isJaggerDebug() {
        return System.getenv("JAGGER_DEBUG") != null;
    }

    public record Trigger(Element element) {}
}
