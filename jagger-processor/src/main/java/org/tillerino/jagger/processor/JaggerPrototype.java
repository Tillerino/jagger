package org.tillerino.jagger.processor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import org.apache.commons.lang3.NotImplementedException;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeKind;

/**
 * Accessor object for a method which is annotated with {@link org.tillerino.jagger.annotations.JsonInput} or
 * {@link org.tillerino.jagger.annotations.JsonOutput}.
 */
public record JaggerPrototype(
        JaggerBlueprint blueprint,
        String name,
        ExecutableElement methodElement,
        PrototypeKind kind,
        AnnotationProcessorUtils utils,
        TypeMirror instantiatedReturnType,
        List<InstantiatedVariable> instantiatedParameters,
        AnyConfig config,
        boolean overrides) {

    public static JaggerPrototype of(
            JaggerBlueprint blueprint,
            InstantiatedMethod instantiated,
            PrototypeKind kind,
            AnnotationProcessorUtils utils,
            boolean overrides) {
        AnyConfig config = AnyConfig.create(instantiated.element(), ConfigProperty.LocationKind.PROTOTYPE, utils)
                .merge(blueprint.config);

        return new JaggerPrototype(
                blueprint,
                instantiated.name(),
                instantiated.element(),
                kind,
                utils,
                instantiated.returnType(),
                instantiated.parameters(),
                config,
                overrides);
    }

    /** Checks if reads/writes the given type and matches the signature of a reference method. */
    public InstantiatedMethod matches(JaggerPrototype caller, TypeMirror callerType, boolean allowExact) {
        if (kind.direction() != caller.kind().direction()
                || !utils.types.isSameType(kind().jsonType(), caller.kind().jsonType())) {
            return null;
        }

        LinkedHashSet<TypeVar> localTypeVars = methodElement.getTypeParameters().stream()
                .map(TypeParameterElement::asType)
                .map(TypeVariable.class::cast)
                .map(TypeVar::of)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashMap<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();

        if (isSameTypeWithBindings(kind().javaType(), callerType, localTypeVars, typeBindings)) {
            if (!allowExact && typeBindings.isEmpty()) {
                return null;
            }
            return utils.generics.applyTypeBindings(this.asInstantiatedMethod(), typeBindings);
        }
        return null;
    }

    private static boolean isSameTypeWithBindings(
            TypeMirror calleeType,
            TypeMirror callerType,
            LinkedHashSet<TypeVar> calleeTypeVars,
            LinkedHashMap<TypeVar, TypeMirror> calleeBindings) {
        if (calleeType instanceof TypeVariable t) {
            TypeVar typeVar = TypeVar.of(t);
            if (calleeBindings.containsKey(typeVar)) {
                return calleeBindings.get(typeVar).equals(callerType);
            }
            if (calleeTypeVars.contains(typeVar)) {
                if (callerType.getKind().isPrimitive()) {
                    return false;
                }
                calleeBindings.put(typeVar, callerType);
                return true;
            }
            return false;
        }
        if (calleeType instanceof DeclaredType t) {
            if (!(callerType instanceof DeclaredType tt) || !t.asElement().equals(tt.asElement())) {
                return false;
            }
            if (t.getTypeArguments().isEmpty() || tt.getTypeArguments().isEmpty()) {
                // if either is raw
                return true;
            }
            for (int i = 0; i < t.getTypeArguments().size(); i++) {
                TypeMirror type = t.getTypeArguments().get(i);
                TypeMirror targetTypeArg = tt.getTypeArguments().get(i);
                if (!isSameTypeWithBindings(type, targetTypeArg, calleeTypeVars, calleeBindings)) {
                    return false;
                }
            }
            return true;
        }
        if (calleeType instanceof PrimitiveType p && callerType instanceof PrimitiveType pt) {
            return p.getKind().equals(pt.getKind());
        }
        if (calleeType instanceof ArrayType a && callerType instanceof ArrayType at) {
            return isSameTypeWithBindings(a.getComponentType(), at.getComponentType(), calleeTypeVars, calleeBindings);
        }
        if (calleeType.getKind() == TypeKind.WILDCARD && callerType.getKind() == TypeKind.WILDCARD) {
            return true;
        }
        return false;
    }

    public Optional<VariableElement> contextParameter() {
        for (VariableElement parameter : methodElement.getParameters()) {
            if (utils.types.isAssignable(
                    parameter.asType(),
                    switch (kind.direction()) {
                        case INPUT -> utils.commonTypes.deserializationContext;
                        case OUTPUT -> utils.commonTypes.serializationContext;
                    })) {
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
        return new InstantiatedMethod(name, instantiatedReturnType, instantiatedParameters, methodElement, config);
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
