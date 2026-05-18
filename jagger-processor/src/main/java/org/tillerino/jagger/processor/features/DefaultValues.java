package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class DefaultValues {
    protected final JaggerContext ctx;

    public DefaultValues(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public Optional<InstantiatedMethod> findInputDefaultValue(
            JaggerBlueprint blueprint, TypeMirror targetType, AnyConfig config) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        return Converters.declaredMethodsFromSelfAndUsed(blueprint, config)
                .flatMap(method -> {
                    typeBindings.clear();
                    if (isInputDefaultValue(method.element())
                            && ctx.generics.typeBindingsSatisfyingEquality(
                                    targetType, method.returnType(), typeBindings, method.freeTypeVars())) {
                        return Stream.of(ctx.generics.applyTypeBindings(method, typeBindings));
                    }
                    return Stream.empty();
                })
                .findFirst();
    }

    public Expr getDefaultValue(JaggerPrototype prototype, TypeMirror type, AnyConfig propertyConfig) {
        return findInputDefaultValue(prototype.blueprint(), type, propertyConfig)
                .map(m -> m.callStatic(List.of()))
                .orElseGet(() -> ctx.commonTypes.getNullValueRaw(type));
    }

    public boolean isInputDefaultValue(ExecutableElement methodElement) {
        return methodElement.getModifiers().contains(Modifier.STATIC)
                && ctx.annotations
                        .findAnnotation(methodElement, "org.tillerino.jagger.annotations.JsonInputDefaultValue")
                        .isPresent()
                && methodElement.getParameters().isEmpty();
    }
}
