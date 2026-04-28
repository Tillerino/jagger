package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public record Creators(JaggerContext ctx) {

    public Optional<Creator> findJsonCreatorMethod(TypeMirror tm) {
        if (!(tm instanceof DeclaredType dt)) {
            return Optional.empty();
        }
        Map<TypeVar, TypeMirror> typeBindings = ctx.generics.recordTypeBindings(dt);
        for (ExecutableElement constructor :
                ElementFilter.constructorsIn(dt.asElement().getEnclosedElements())) {
            Optional<JsonCreatorMode> annotation = jsonCreatorType(constructor);
            if (annotation.isEmpty()) {
                continue;
            }
            return Optional.of(Creator.of(
                    annotation.get(), ctx.generics.instantiateMethod(constructor, typeBindings, LocationKind.CREATOR)));
        }
        for (ExecutableElement method : ElementFilter.methodsIn(dt.asElement().getEnclosedElements())) {
            Optional<JsonCreatorMode> annotation = jsonCreatorType(method);
            if (annotation.isEmpty()
                    || method.getReturnType().getKind() == TypeKind.VOID
                    || !method.getModifiers().contains(javax.lang.model.element.Modifier.STATIC)) {
                continue;
            }
            // Cannot instantiate with type bindings from class.
            // Need to infer from return type.
            InstantiatedMethod methodWithTypeTypeVars =
                    ctx.generics.instantiateMethod(method, typeBindings, LocationKind.CREATOR);
            Map<TypeVar, TypeMirror> methodTypeVars = new LinkedHashMap<>();
            if (!ctx.generics.typeBindingsSatisfyingEquality(
                    tm, methodWithTypeTypeVars.returnType(), methodTypeVars, methodWithTypeTypeVars.freeTypeVars())) {
                continue;
            }
            return Optional.of(Creator.of(
                    annotation.get(), ctx.generics.applyTypeBindings(methodWithTypeTypeVars, methodTypeVars)));
        }
        return Optional.empty();
    }

    private Optional<JsonCreatorMode> jsonCreatorType(ExecutableElement element) {
        return ctx.annotations
                .findAnnotation(element, "com.fasterxml.jackson.annotation.JsonCreator")
                .map(wrapper -> wrapper.method("mode", true)
                        .orElseThrow(Exceptions::unexpected)
                        .asEnum(JsonCreatorMode.class))
                .filter(mode -> mode != JsonCreatorMode.DISABLED);
    }

    public sealed interface Creator {
        static Creator of(JsonCreatorMode mode, InstantiatedMethod method) {
            return switch (mode) {
                case DEFAULT -> method.parameters().size() == 1 ? new Converter(method) : new Properties(method);
                case DELEGATING -> new Converter(method);
                case PROPERTIES -> new Properties(method);
                default -> throw new ContextedRuntimeException(String.valueOf(mode));
            };
        }

        record Converter(InstantiatedMethod method) implements Creator {}

        record Properties(InstantiatedMethod method) implements Creator {}
    }

    public enum JsonCreatorMode {
        DEFAULT,
        DELEGATING,
        PROPERTIES,
        DISABLED
    }
}
