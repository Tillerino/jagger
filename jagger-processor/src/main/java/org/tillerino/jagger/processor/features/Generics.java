package org.tillerino.jagger.processor.features;

import java.util.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.AbstractTypeVisitor14;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import org.tillerino.jagger.processor.*;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.RebuildingTypeVisitor;

public record Generics(AnnotationProcessorUtils utils) {
    public Map<TypeVar, TypeMirror> recordTypeBindings(DeclaredType d) {
        Map<TypeVar, TypeMirror> map = new LinkedHashMap<>();
        for (int i = 0; i < d.getTypeArguments().size(); i++) {
            TypeMirror type =
                    ((TypeElement) d.asElement()).getTypeParameters().get(i).asType();
            if (type instanceof TypeVariable tVar) {
                map.put(TypeVar.of(tVar), d.getTypeArguments().get(i));
            }
        }
        return map;
    }

    /** @return null if the super type is not a super type of d */
    public Map<TypeVar, TypeMirror> recordTypeBindingsFor(DeclaredType d, TypeElement superType) {
        if (d.asElement().equals(superType)) {
            Map<TypeVar, TypeMirror> bindings = new LinkedHashMap<>();
            for (int i = 0; i < superType.getTypeParameters().size(); i++) {
                bindings.put(
                        new TypeVar(
                                superType,
                                ((TypeVariable) superType
                                                .getTypeParameters()
                                                .get(i)
                                                .asType())
                                        .asElement()
                                        .getSimpleName()
                                        .toString()),
                        d.getTypeArguments().get(i));
            }
            return bindings;
        }

        for (DeclaredType d2 : Polymorphism.directSupertypes(d, utils)) {
            Map<TypeVar, TypeMirror> superTypeBindings = recordTypeBindingsFor(d2, superType);
            if (superTypeBindings != null) {
                return superTypeBindings;
            }
        }

        return null;
    }

    public TypeMirror applyTypeBindings(TypeMirror t, Map<TypeVar, TypeMirror> bindings) {
        return t.accept(
                new RebuildingTypeVisitor() {
                    @Override
                    public TypeMirror visitTypeVariable(TypeVariable t, Types types) {
                        return bindings.getOrDefault(TypeVar.of(t), t);
                    }
                },
                utils.types);
    }

    public InstantiatedVariable applyTypeBindings(InstantiatedVariable v, Map<TypeVar, TypeMirror> bindings) {
        return new InstantiatedVariable(v.elem(), applyTypeBindings(v.type(), bindings), v.name(), v.config());
    }

    public List<InstantiatedVariable> applyTypeBindingsToAll(
            List<InstantiatedVariable> v, Map<TypeVar, TypeMirror> bindings) {
        return v.stream().map(p -> applyTypeBindings(p, bindings)).toList();
    }

    public InstantiatedMethod applyTypeBindings(
            InstantiatedMethod instantiatedMethod, Map<TypeVar, TypeMirror> typeBindings) {
        List<InstantiatedVariable> newParameters =
                applyTypeBindingsToAll(instantiatedMethod.parameters(), typeBindings);
        TypeMirror newReturnType = applyTypeBindings(instantiatedMethod.returnType(), typeBindings);
        return new InstantiatedMethod(
                instantiatedMethod.name(),
                newReturnType,
                newParameters,
                instantiatedMethod.element(),
                instantiatedMethod.config());
    }

    public List<InstantiatedMethod> instantiateMethods(TypeMirror tm, LocationKind locationKind) {
        if (!(tm instanceof DeclaredType d)) {
            return List.of();
        }
        // TODO: do this eagerly, since it is probably not too cheap
        List<InstantiatedMethod> methods = new ArrayList<>();
        Map<TypeVar, TypeMirror> typeVariableMapping = recordTypeBindings(d);
        for (ExecutableElement method : ElementFilter.methodsIn(d.asElement().getEnclosedElements())) {
            methods.add(instantiateMethod(method, typeVariableMapping, locationKind));
        }
        return methods;
    }

    public InstantiatedMethod instantiateMethod(
            ExecutableElement methodElement, Map<TypeVar, TypeMirror> typeBindings, LocationKind locationKind) {
        List<InstantiatedVariable> parameters = methodElement.getParameters().stream()
                .map(p -> new InstantiatedVariable(
                        p,
                        applyTypeBindings(p.asType(), typeBindings),
                        p.getSimpleName().toString(),
                        AnyConfig.create(p, LocationKind.PROPERTY, utils)))
                .toList();
        return new InstantiatedMethod(
                methodElement.getSimpleName().toString(),
                applyTypeBindings(methodElement.getReturnType(), typeBindings),
                parameters,
                methodElement,
                AnyConfig.create(methodElement, locationKind, utils));
    }

    /**
     * Records type variables such that the candidate type is equal to the actual type.
     *
     * @param typeBindings is modified by the (recursive) call
     */
    public boolean tybeBindingsSatisfyingEquality(
            TypeMirror actualType, TypeMirror candidateType, Map<TypeVar, TypeMirror> typeBindings) {
        if (utils.types.isSameType(actualType, candidateType)) {
            return true;
        }
        if ((actualType instanceof DeclaredType actualDeclared)
                && (candidateType instanceof DeclaredType candidateDeclared)) {
            // compare raw type
            if (!utils.types.isSameType(
                    actualDeclared.asElement().asType(),
                    candidateDeclared.asElement().asType())) {
                return false;
            }
            for (int i = 0; i < actualDeclared.getTypeArguments().size(); i++) {
                if (!tybeBindingsSatisfyingEquality(
                        actualDeclared.getTypeArguments().get(i),
                        candidateDeclared.getTypeArguments().get(i),
                        typeBindings)) {
                    return false;
                }
            }
            return true;
        }
        if ((actualType instanceof ArrayType actualArray) && (candidateType instanceof ArrayType candidateArray)) {
            return tybeBindingsSatisfyingEquality(
                    actualArray.getComponentType(), candidateArray.getComponentType(), typeBindings);
        }
        if (candidateType instanceof TypeVariable candidateVar) {
            TypeVar candidate = TypeVar.of(candidateVar);
            if (typeBindings.containsKey(candidate)) {
                return utils.types.isSameType(typeBindings.get(candidate), actualType);
            }
            typeBindings.put(candidate, actualType);
            return true;
        }
        return false;
    }

    public Optional<Snippet> getOrCreateLambda(
            GeneratedClass callingClass, TypeMirror targetType, List<InstantiatedVariable> availableValues, int depth) {
        if (depth > 10) {
            // this depth is pretty arbitrary, but surely larger than anything useful and it's just important that we
            // do not explode here.
            return Optional.empty();
        }
        return instantiateFunctionalInterface(targetType)
                .flatMap(functionalInterface -> createMethodReference(callingClass, functionalInterface));
    }

    Optional<InstantiatedMethod> instantiateFunctionalInterface(TypeMirror functionalInterface) {
        if (!(functionalInterface instanceof DeclaredType d)) {
            return Optional.empty();
        }
        TypeElement typeElement = (TypeElement) d.asElement();
        if (!typeElement.getKind().isInterface()) {
            return Optional.empty();
        }
        List<ExecutableElement> methods = ElementFilter.methodsIn(utils.elements.getAllMembers(typeElement)).stream()
                .filter(method -> !method.getEnclosingElement().toString().equals("java.lang.Object"))
                .toList();
        if (methods.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(
                utils.generics.instantiateMethods(d, LocationKind.PROTOTYPE).get(0));
    }

    private Optional<Snippet> createMethodReference(GeneratedClass callingClass, InstantiatedMethod targetMethod) {
        JaggerBlueprint blueprint = callingClass.blueprint;
        for (JaggerPrototype method : blueprint.prototypes) {
            if (method.asInstantiatedMethod().hasSameSignature(targetMethod, utils)) {
                return Optional.of(Snippet.of(
                        "$L::$L",
                        callingClass.getOrCreateDelegateeField(blueprint, blueprint, !method.overrides()),
                        method.name()));
            }
        }
        for (JaggerBlueprint use : blueprint.config.reversedUses()) {
            for (JaggerPrototype method : use.prototypes) {
                if (method.asInstantiatedMethod().hasSameSignature(targetMethod, utils)) {
                    return Optional.of(Snippet.of(
                            "$L::$L",
                            callingClass.getOrCreateDelegateeField(blueprint, use, !method.overrides()),
                            method.name()));
                }
            }
        }
        return Optional.empty();
    }

    /** Finds a parameter of type {@code Class<T>} on the method. */
    public Optional<Snippet> findClassParameter(InstantiatedMethod method, TypeMirror t) {
        DeclaredType classOfT = utils.types.getDeclaredType(utils.commonTypes.classElement, t);
        for (InstantiatedVariable parameter : method.parameters()) {
            if (utils.types.isSameType(parameter.type(), classOfT)) {
                return Optional.of(parameter);
            }
        }
        return Optional.empty();
    }

    /** Determines if there can be a {@code Class<T>} for a type T. */
    public static boolean canBeClass(TypeMirror t) {
        return t.accept(
                new AbstractTypeVisitor14<>() {

                    @Override
                    public Boolean visitPrimitive(PrimitiveType t, Boolean aBoolean) {
                        return true;
                    }

                    @Override
                    public Boolean visitNull(NullType t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitArray(ArrayType t, Boolean aBoolean) {
                        return t.getComponentType().accept(this, true);
                    }

                    @Override
                    public Boolean visitDeclared(DeclaredType t, Boolean aBoolean) {
                        return t.asElement() != null && t.getTypeArguments().isEmpty();
                    }

                    @Override
                    public Boolean visitError(ErrorType t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitTypeVariable(TypeVariable t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitWildcard(WildcardType t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitExecutable(ExecutableType t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitNoType(NoType t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitUnion(UnionType t, Boolean aBoolean) {
                        return false;
                    }

                    @Override
                    public Boolean visitIntersection(IntersectionType t, Boolean aBoolean) {
                        return false;
                    }
                },
                true);
    }

    /** Required since {@link TypeVariable} and its corresponding element do not implement hashCode and equals? */
    public record TypeVar(Element owner, String name) {
        public static TypeVar of(TypeVariable t) {
            return new TypeVar(
                    t.asElement().getEnclosingElement(),
                    t.asElement().getSimpleName().toString());
        }
    }
}
