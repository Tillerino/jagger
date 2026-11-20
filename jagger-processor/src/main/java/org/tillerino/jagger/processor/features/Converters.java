package org.tillerino.jagger.processor.features;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.MethodInScope;

public class Converters {
    protected final JaggerContext ctx;

    public Converters(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public Optional<InputConverter> findInputConverter(
            JaggerPrototype prototype, TypeMirror targetType, AnyConfig config, GeneratedClass generatedClass) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (MethodInScope methodInScope : listMethodsInScope(prototype, config)) {
            InstantiatedMethod method = methodInScope.method();
            if (!isInputConverter(method.element())) {
                continue;
            }
            typeBindings.clear();
            if (ctx.generics.typeBindingsSatisfyingEquality(
                    targetType, method.returnType(), typeBindings, method.freeTypeVars(), true)) {
                MethodInScope boundMethod = methodInScope.applyTypeBindings(typeBindings);
                return Optional.of(new InputConverter(
                        toConvert -> boundMethod.call(prototype, List.of(toConvert), generatedClass),
                        boundMethod.method().parameters().get(0).type()));
            }
        }
        return Optional.empty();
    }

    public Optional<OutputConverter> findOutputConverter(
            TypeMirror toConvertType, JaggerPrototype prototype, AnyConfig config, GeneratedClass generatedClass) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (MethodInScope methodInScope : listMethodsInScope(prototype, config)) {
            InstantiatedMethod method = methodInScope.method();
            if (!isOutputConverter(method.element())) {
                continue;
            }
            typeBindings.clear();
            if (ctx.generics.typeBindingsSatisfyingEquality(
                    toConvertType, method.parameters().get(0).type(), typeBindings, method.freeTypeVars(), true)) {
                MethodInScope boundMethod = methodInScope.applyTypeBindings(typeBindings);
                return Optional.of(new OutputConverter(
                        toConvert -> boundMethod.call(prototype, List.of(toConvert), generatedClass),
                        boundMethod.method().returnType()));
            }
        }
        return Optional.empty();
    }

    /**
     * Lists methods available through prototype parameters, plus methods declared by the prototype's blueprint and its
     * used blueprints. Parameter methods take precedence.
     */
    List<MethodInScope> listMethodsInScope(JaggerPrototype prototype, AnyConfig config) {
        List<MethodInScope> methods = new ArrayList<>();
        ctx.delegation
                .methodsInParameters(prototype)
                .forEach(parameterMethod -> methods.add(
                        new MethodInScope.FromParameter(parameterMethod.parameter(), parameterMethod.method())));
        Stream<MethodInScope> own = prototype.blueprint().declaredMethods.stream()
                .map(method -> new MethodInScope.FromBlueprint(prototype.blueprint(), method));
        Stream<MethodInScope> used = config.reversedUses().stream()
                .flatMap(use ->
                        use.declaredMethods.stream().map(method -> new MethodInScope.FromBlueprint(use, method)));
        methods.addAll(Stream.concat(own, used).toList());
        return methods;
    }

    public record OutputConverter(Function<Expr, Expr> converter, TypeMirror convertedType) {}

    public record InputConverter(Function<Expr, Expr> converter, TypeMirror typeToConvert) {}

    public boolean isInputConverter(ExecutableElement methodElement) {
        return ctx.annotations
                        .findAnnotation(methodElement, "org.tillerino.jagger.annotations.JsonInputConverter")
                        .isPresent()
                && !methodElement.getParameters().isEmpty();
    }

    public boolean isOutputConverter(ExecutableElement methodElement) {
        return ctx.annotations
                        .findAnnotation(methodElement, "org.tillerino.jagger.annotations.JsonOutputConverter")
                        .isPresent()
                && !methodElement.getParameters().isEmpty();
    }

    public Optional<InstantiatedMethod> findJsonValueMethod(
            TypeMirror dtoType, Predicate<TypeMirror> returnTypeFilter) {
        return Polymorphism.typeHierarchyBfs(dtoType, ctx, type -> {
            for (InstantiatedMethod method : ctx.generics.instantiateMethods(type, LocationKind.BLUEPRINT)) {
                if (method.findAnnotation("com.fasterxml.jackson.annotation.JsonValue")
                                .isPresent()
                        && method.parameters().isEmpty()
                        && method.returnType().getKind() != TypeKind.VOID
                        && returnTypeFilter.test(method.returnType())) {
                    return Optional.of(method);
                }
            }
            return Optional.empty();
        });
    }
}
