package org.tillerino.jagger.processor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.NotImplementedException;
import org.tillerino.jagger.processor.JaggerProcessor.Trigger;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

/**
 * Accessor object for a method which is annotated with {@link org.tillerino.jagger.annotations.JsonInput} or
 * {@link org.tillerino.jagger.annotations.JsonOutput}.
 */
public record JaggerPrototype(
        JaggerBlueprint blueprint,
        String name,
        ExecutableElement methodElement,
        PrototypeKind kind,
        JaggerContext ctx,
        TypeMirror instantiatedReturnType,
        List<InstantiatedVariable> instantiatedParameters,
        Set<TypeVar> freeTypeVars,
        AnyConfig config,
        boolean overrides,
        Trigger trigger) {

    public static JaggerPrototype of(
            JaggerBlueprint blueprint,
            InstantiatedMethod instantiated,
            PrototypeKind kind,
            JaggerContext ctx,
            boolean overrides,
            Trigger trigger) {
        AnyConfig config = AnyConfig.create(instantiated.element(), ConfigProperty.LocationKind.PROTOTYPE, ctx)
                .merge(blueprint.config);

        return new JaggerPrototype(
                blueprint,
                instantiated.name(),
                instantiated.element(),
                kind,
                ctx,
                instantiated.returnType(),
                instantiated.parameters(),
                instantiated.freeTypeVars(),
                config,
                overrides,
                trigger);
    }

    /** Checks if reads/writes the given type and matches the signature of a reference method. */
    public InstantiatedMethod matches(TemplatablePrototypeKind target, boolean allowExact) {
        if (!(kind instanceof TemplatablePrototypeKind t)) {
            return null;
        }
        LinkedHashMap<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();

        if (t.matches(target, ctx, typeBindings, freeTypeVars)) {
            if (!allowExact && typeBindings.isEmpty()) {
                return null;
            }
            return ctx.generics.applyTypeBindings(this.asInstantiatedMethod(), typeBindings);
        }
        return null;
    }

    public Optional<InstantiatedVariable> contextParameter() {
        for (InstantiatedVariable parameter : instantiatedParameters()) {
            Optional<TypeMirror> targetContextType = kind.contextType();
            if (targetContextType.isPresent()
                    && ctx.commonTypes.isAssignable(parameter.type(), targetContextType.get())) {
                return Optional.of(parameter);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return blueprint + "." + name
                + instantiatedParameters().stream()
                        .map(InstantiatedVariable::toString)
                        .collect(Collectors.joining(", ", "(", ")"));
    }

    public InstantiatedMethod asInstantiatedMethod() {
        return new InstantiatedMethod(
                name, instantiatedReturnType, instantiatedParameters, methodElement, freeTypeVars, config);
    }

    @Override
    public int hashCode() {
        throw new NotImplementedException("hashCode");
    }

    @Override
    public boolean equals(Object obj) {
        throw new NotImplementedException("equals");
    }
}
