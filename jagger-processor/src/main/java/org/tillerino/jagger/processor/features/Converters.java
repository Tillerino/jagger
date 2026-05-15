package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.Snippet;
import org.tillerino.jagger.processor.util.Snippet.TypedSnippet;

public record Converters(JaggerContext ctx) {
    public Optional<InstantiatedMethod> findInputConverter(
            JaggerBlueprint blueprint, TypeMirror targetType, AnyConfig config) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        return declaredMethodsFromSelfAndUsed(blueprint, config)
                .flatMap(method -> {
                    typeBindings.clear();
                    if (isInputConverter(method.element())
                            && ctx.generics.typeBindingsSatisfyingEquality(
                                    targetType, method.returnType(), typeBindings, method.freeTypeVars())) {
                        return Stream.of(ctx.generics.applyTypeBindings(method, typeBindings));
                    }
                    return Stream.empty();
                })
                .findFirst();
    }

    public Optional<TypedSnippet> findOutputConverter(
            TypedSnippet toConvert, JaggerPrototype prototype, AnyConfig config, GeneratedClass generatedClass) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        return declaredMethodsFromSelfAndUsed(prototype.blueprint(), config)
                .flatMap(method -> {
                    typeBindings.clear();
                    if (isOutputConverter(method.element())
                            && ctx.generics.typeBindingsSatisfyingEquality(
                                    toConvert.type(),
                                    method.parameters().get(0).type(),
                                    typeBindings,
                                    method.freeTypeVars())) {
                        InstantiatedMethod instantiatedMethod = ctx.generics.applyTypeBindings(method, typeBindings);
                        return Stream.of(TypedSnippet.of(
                                instantiatedMethod.returnType(),
                                "$C($C$C)",
                                method.callSymbol(ctx),
                                toConvert,
                                Snippet.joinPrependingCommaToEach(
                                        ctx.delegation.findArguments(prototype, method, 1, generatedClass))));
                    }
                    return Stream.empty();
                })
                .findFirst();
    }

    public boolean isInputConverter(ExecutableElement methodElement) {
        return methodElement.getModifiers().contains(Modifier.STATIC)
                && ctx.annotations
                        .findAnnotation(methodElement, "org.tillerino.jagger.annotations.JsonInputConverter")
                        .isPresent()
                && !methodElement.getParameters().isEmpty();
    }

    public boolean isOutputConverter(ExecutableElement methodElement) {
        return methodElement.getModifiers().contains(Modifier.STATIC)
                && ctx.annotations
                        .findAnnotation(methodElement, "org.tillerino.jagger.annotations.JsonOutputConverter")
                        .isPresent()
                && !methodElement.getParameters().isEmpty();
    }

    static Stream<InstantiatedMethod> declaredMethodsFromSelfAndUsed(JaggerBlueprint blueprint, AnyConfig config) {
        return Stream.concat(
                blueprint.declaredMethods.stream(),
                config.reversedUses().stream().flatMap(use -> use.declaredMethods.stream()));
    }

    public Optional<TypedSnippet> findJsonValueMethod(TypedSnippet toConvert) {
        Optional<InstantiatedMethod> result = findJsonValueMethod(toConvert.type(), __ -> true);
        return result.map(m -> TypedSnippet.of(m.returnType(), Snippet.of("$C.$L()", toConvert, m.name())));
    }

    public Optional<InstantiatedMethod> findJsonValueMethod(
            TypeMirror dtoType, Predicate<TypeMirror> returnTypeFilter) {
        return Polymorphism.typeHierarchyBfs(dtoType, ctx, type -> {
            Map<TypeVar, TypeMirror> typeBindings = ctx.generics.recordTypeBindings(type);
            for (ExecutableElement method :
                    ElementFilter.methodsIn(type.asElement().getEnclosedElements())) {
                if (ctx.annotations
                                .findAnnotation(method, "com.fasterxml.jackson.annotation.JsonValue")
                                .isPresent()
                        && method.getParameters().isEmpty()
                        && method.getReturnType().getKind() != TypeKind.VOID
                        && returnTypeFilter.test(method.getReturnType())) {
                    return Optional.of(ctx.generics.instantiateMethod(method, typeBindings, LocationKind.BLUEPRINT));
                }
            }
            return Optional.empty();
        });
    }
}
