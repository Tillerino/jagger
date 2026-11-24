package org.tillerino.jagger.processor.features;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PrototypeInScope;

public class DefaultValues {
    public static final String JSON_INPUT_DEFAULT_VALUE = "org.tillerino.jagger.annotations.JsonInputDefaultValue";

    protected final JaggerContext ctx;

    public DefaultValues(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public Optional<PrototypeInScope> findInputDefaultValue(
            JaggerPrototype prototype, TypeMirror targetType, AnyConfig config) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (PrototypeInScope prototypeInScope : prototype.listPrototypesInScope(config)) {
            InstantiatedMethod method = prototypeInScope.prototype().method();
            typeBindings.clear();
            if (isInputDefaultValue(method.element())
                    && ctx.generics.typeBindingsSatisfyingEquality(
                            targetType, method.returnType(), typeBindings, method.freeTypeVars(), true)) {
                return Optional.of(prototypeInScope.applyTypeBindings(typeBindings));
            }
        }
        return Optional.empty();
    }

    public Expr getDefaultValue(
            JaggerPrototype prototype,
            TypeMirror type,
            AnyConfig propertyConfig,
            GeneratedClass generatedClass,
            Set<String> avn) {
        return findInputDefaultValue(prototype, type, propertyConfig)
                .map(prototypeInScope -> prototypeInScope.call(prototype, List.of(), generatedClass, avn))
                .orElseGet(() -> ctx.commonTypes.getNullValueRaw(type));
    }

    public boolean isInputDefaultValue(ExecutableElement methodElement) {
        return ctx.annotations
                        .findAnnotation(methodElement, JSON_INPUT_DEFAULT_VALUE)
                        .isPresent()
                && methodElement.getParameters().isEmpty();
    }

    public record DefaultValuePrototypeKind(TypeMirror type) implements PrototypeKind {
        @Override
        public CodeBlock.Builder generateCode(CodeGeneratorContext context) {
            throw new UnsupportedOperationException("Default value prototypes are not generated");
        }

        @Override
        public boolean isGenerated() {
            return false;
        }
    }

    public static class DefaultValuePrototypeDetector implements PrototypeDetector {
        @Override
        public Collection<String> supportedAnnotationTypes() {
            return List.of(JSON_INPUT_DEFAULT_VALUE);
        }

        @Override
        public Optional<PrototypeKind> detect(InstantiatedMethod method, AnnotationMirrorWrapper annotation) {
            if (!method.parameters().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new DefaultValuePrototypeKind(method.returnType()));
        }
    }
}
