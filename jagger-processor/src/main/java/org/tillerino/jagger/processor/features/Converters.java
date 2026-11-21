package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.util.Expr.e;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.helpers.CharacterHelper;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Creators.Creator;
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
        for (MethodInScope methodInScope : ctx.delegation.listMethodsInScope(prototype, config)) {
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
        return ctx.creators
                .findJsonCreatorMethod(targetType)
                .filter(Creator.Converter.class::isInstance)
                .map(creator -> {
                    InstantiatedMethod method = ((Creator.Converter) creator).method();
                    return new InputConverter(
                            toConvert ->
                                    method.callStaticFindingArguments(prototype, List.of(toConvert), generatedClass),
                            method.parameters().get(0).type());
                });
    }

    public Optional<OutputConverter> findOutputConverter(
            TypeMirror toConvertType, JaggerPrototype prototype, AnyConfig config, GeneratedClass generatedClass) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (MethodInScope methodInScope : ctx.delegation.listMethodsInScope(prototype, config)) {
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
        return findJsonValueMethod(toConvertType, __ -> true)
                .map(method -> new OutputConverter(method::call, method.returnType()));
    }

    /** Boxed types are converted as {@code x -> Objects.toString(x, null)}, enums are serialized via {@link Enums}. */
    public Optional<Expr> instantiateToStringConverter(TypeMirror targetType, GeneratedClass generatedClass) {
        if (!ctx.generics.isFunctionalInterface(targetType)) {
            return Optional.empty();
        }
        List<InstantiatedMethod> methods = ctx.generics.instantiateMethods(targetType, LocationKind.PROTOTYPE);
        if (methods.size() != 1) {
            return Optional.empty();
        }
        InstantiatedMethod method = methods.get(0);
        if (!isOutputConverter(method.element())
                || method.parameters().size() != 1
                || !ctx.commonTypes.isString(method.returnType())) {
            return Optional.empty();
        }
        TypeMirror parameterType = method.parameters().get(0).type();
        String parameterName = method.parameters().get(0).name();
        if (ctx.commonTypes.isBoxed(parameterType)) {
            return Optional.of(
                    e(targetType, "$L -> $T.toString($L, null)", parameterName, Objects.class, parameterName));
        }
        if (ctx.commonTypes.isEnum(parameterType)) {
            Expr enumValue = e(parameterType, "$L", parameterName);
            return Optional.of(e(
                    targetType,
                    "$L -> $C",
                    parameterName,
                    Enums.serializationCode(generatedClass, parameterType, enumValue)));
        }
        return Optional.empty();
    }

    /**
     * Boxed types are converted as {@code x -> Integer.valueOf(x)}, enums are deserialized via the generated lookup map
     * ({@link Enums}).
     */
    public Optional<Expr> instantiateFromStringConverter(TypeMirror targetType, GeneratedClass generatedClass) {
        if (!ctx.generics.isFunctionalInterface(targetType)) {
            return Optional.empty();
        }
        List<InstantiatedMethod> methods = ctx.generics.instantiateMethods(targetType, LocationKind.PROTOTYPE);
        if (methods.size() != 1) {
            return Optional.empty();
        }
        InstantiatedMethod method = methods.get(0);
        if (!isInputConverter(method.element())
                || method.parameters().size() != 1
                || !ctx.commonTypes.isString(method.parameters().get(0).type())) {
            return Optional.empty();
        }
        TypeMirror convertedType = method.returnType();
        String parameterName = method.parameters().get(0).name();
        if (ctx.commonTypes.isBoxed(convertedType)) {
            if (ctx.types.isSameType(convertedType, ctx.commonTypes.boxedChar)) {
                return Optional.of(
                        e(targetType, "$L -> $T.toCharacter($L)", parameterName, CharacterHelper.class, parameterName));
            }
            return Optional.of(e(targetType, "$L -> $T.valueOf($L)", parameterName, convertedType, parameterName));
        }
        if (ctx.commonTypes.isEnum(convertedType)) {
            Expr key = e(ctx.commonTypes.string, "$L", parameterName);
            return Optional.of(e(
                    targetType,
                    "$L -> $C",
                    parameterName,
                    Enums.deserializationCode(generatedClass, convertedType, key)));
        }
        return Optional.empty();
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
