package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.MethodInScope;

public class DefaultValues {
    protected final JaggerContext ctx;

    public DefaultValues(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public Optional<MethodInScope> findInputDefaultValue(
            JaggerPrototype prototype, TypeMirror targetType, AnyConfig config) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (MethodInScope methodInScope : ctx.converters.listMethodsInScope(prototype, config)) {
            InstantiatedMethod method = methodInScope.method();
            typeBindings.clear();
            if (isInputDefaultValue(method.element())
                    && ctx.generics.typeBindingsSatisfyingEquality(
                            targetType, method.returnType(), typeBindings, method.freeTypeVars(), true)) {
                return Optional.of(methodInScope.applyTypeBindings(typeBindings));
            }
        }
        return Optional.empty();
    }

    public Expr getDefaultValue(
            JaggerPrototype prototype, TypeMirror type, AnyConfig propertyConfig, GeneratedClass generatedClass) {
        return findInputDefaultValue(prototype, type, propertyConfig)
                .map(methodInScope -> methodInScope.call(prototype, List.of(), generatedClass))
                .orElseGet(() -> ctx.commonTypes.getNullValueRaw(type));
    }

    public boolean isInputDefaultValue(ExecutableElement methodElement) {
        return ctx.annotations
                        .findAnnotation(methodElement, "org.tillerino.jagger.annotations.JsonInputDefaultValue")
                        .isPresent()
                && methodElement.getParameters().isEmpty();
    }
}
