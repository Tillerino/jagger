package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.util.Code.c;
import static org.tillerino.jagger.processor.util.Expr.e;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.*;
import javax.lang.model.util.AbstractTypeVisitor14;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.RebuildingTypeVisitor;

public class Generics {
    protected final JaggerContext ctx;

    public Generics(JaggerContext ctx) {
        this.ctx = ctx;
    }

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
                TypeVariable typeVariable =
                        (TypeVariable) superType.getTypeParameters().get(i).asType();
                String name = typeVariable.asElement().getSimpleName().toString();
                bindings.put(new TypeVar(superType, name), d.getTypeArguments().get(i));
            }
            return bindings;
        }

        for (DeclaredType d2 : Polymorphism.directSupertypes(d, ctx)) {
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
                ctx.types);
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
        Set<TypeVar> methodTypeVars = new LinkedHashSet<>(instantiatedMethod.freeTypeVars());
        methodTypeVars.removeAll(typeBindings.keySet());
        return new InstantiatedMethod(
                instantiatedMethod.name(),
                newReturnType,
                newParameters,
                instantiatedMethod.element(),
                Collections.unmodifiableSet(methodTypeVars),
                instantiatedMethod.config(),
                ctx);
    }

    public List<InstantiatedMethod> instantiateMethods(TypeMirror tm, @Nullable LocationKind locationKind) {
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
            ExecutableElement methodElement,
            Map<TypeVar, TypeMirror> typeBindings,
            @Nullable LocationKind locationKind) {
        List<InstantiatedVariable> parameters = methodElement.getParameters().stream()
                .map(p -> new InstantiatedVariable(
                        p,
                        applyTypeBindings(p.asType(), typeBindings),
                        p.getSimpleName().toString(),
                        AnyConfig.create(p, LocationKind.PROPERTY, ctx)))
                .toList();
        Set<TypeVar> declaredTypeVars =
                methodElement.getTypeParameters().stream().map(TypeVar::of).collect(Collectors.toUnmodifiableSet());
        return new InstantiatedMethod(
                methodElement.getSimpleName().toString(),
                applyTypeBindings(methodElement.getReturnType(), typeBindings),
                parameters,
                methodElement,
                declaredTypeVars,
                AnyConfig.create(methodElement, locationKind, ctx),
                ctx);
    }

    /**
     * Records type variables such that the candidate type is equal to the actual type.
     *
     * @param typeBindings is modified by the (recursive) call
     */
    public boolean typeBindingsSatisfyingEquality(
            TypeMirror actualType,
            TypeMirror candidateType,
            Map<TypeVar, TypeMirror> typeBindings,
            Set<TypeVar> freeTypeVariables,
            boolean assignPrimitives) {
        if (ctx.types.isSameType(actualType, candidateType)) {
            return true;
        }
        if ((actualType instanceof DeclaredType actualDeclared)
                && (candidateType instanceof DeclaredType candidateDeclared)) {
            // compare raw type
            if (!ctx.types.isSameType(
                    actualDeclared.asElement().asType(),
                    candidateDeclared.asElement().asType())) {
                return false;
            }

            if (actualDeclared.getTypeArguments().isEmpty()
                    || candidateDeclared.getTypeArguments().isEmpty()) {
                // if either is raw
                return true;
            }

            for (int i = 0; i < actualDeclared.getTypeArguments().size(); i++) {
                if (!typeBindingsSatisfyingEquality(
                        actualDeclared.getTypeArguments().get(i),
                        candidateDeclared.getTypeArguments().get(i),
                        typeBindings,
                        freeTypeVariables,
                        false)) {
                    return false;
                }
            }
            return true;
        }
        if ((actualType instanceof ArrayType actualArray) && (candidateType instanceof ArrayType candidateArray)) {
            return typeBindingsSatisfyingEquality(
                    actualArray.getComponentType(),
                    candidateArray.getComponentType(),
                    typeBindings,
                    freeTypeVariables,
                    assignPrimitives);
        }
        if (candidateType instanceof TypeVariable candidateVar) {
            TypeVar candidate = TypeVar.of(candidateVar);
            if (typeBindings.containsKey(candidate)) {
                return ctx.types.isSameType(typeBindings.get(candidate), actualType);
            }
            if (!freeTypeVariables.contains(candidate)) {
                return false;
            }
            if (actualType.getKind().isPrimitive() && !assignPrimitives) {
                return false;
            }
            typeBindings.put(candidate, actualType);
            return true;
        }
        return false;
    }

    public Optional<Expr> getOrCreateLambda(
            GeneratedClass callingClass, TypeMirror targetType, List<InstantiatedVariable> availableValues, int depth) {
        // TODO availableValues is not being used, depth == 0. There was a plan here to instantiate recursively.
        if (depth > 10) {
            // this depth is pretty arbitrary, but surely larger than anything useful and it's just important that we
            // do not explode here.
            return Optional.empty();
        }
        return isFunctionalInterface(targetType)
                ? instantiateFunctionalInterface(callingClass, targetType)
                : Optional.empty();
    }

    boolean isFunctionalInterface(TypeMirror functionalInterface) {
        if (!(functionalInterface instanceof DeclaredType d)) {
            return false;
        }

        TypeElement typeElement = (TypeElement) d.asElement();
        if (!typeElement.getKind().isInterface()) {
            return false;
        }

        List<ExecutableElement> methods = ElementFilter.methodsIn(ctx.elements.getAllMembers(typeElement)).stream()
                .filter(method -> !method.getEnclosingElement().toString().equals("java.lang.Object"))
                .toList();

        return methods.size() == 1;
    }

    private Optional<Expr> instantiateFunctionalInterface(GeneratedClass callingClass, TypeMirror functionalInterface) {
        InstantiatedMethod targetMethod = ctx.generics
                .instantiateMethods(functionalInterface, LocationKind.PROTOTYPE)
                .get(0);

        JaggerBlueprint blueprint = callingClass.blueprint;
        List<JaggerPrototype> allAvailablePrototypes = Stream.concat(
                        blueprint.prototypes.stream(),
                        blueprint.config.reversedUses().stream().flatMap(b -> b.prototypes.stream()))
                .toList();

        for (JaggerPrototype method : allAvailablePrototypes) {
            Optional<Expr> methodReference = createMethodReference(
                            functionalInterface, targetMethod, callingClass, method)
                    .or(() -> createLambda(functionalInterface, targetMethod, callingClass, method));
            if (methodReference.isPresent()) {
                return methodReference;
            }
        }

        return Optional.empty();
    }

    private Optional<Expr> createMethodReference(
            TypeMirror functionalInterface,
            InstantiatedMethod singleMethod,
            GeneratedClass caller,
            JaggerPrototype callee) {
        if (!callee.method().hasSameSignature(singleMethod)) {
            return Optional.empty();
        }

        return Optional.of(e(
                functionalInterface,
                "$C::$L",
                caller.getOrCreateDelegateeField(caller.blueprint, callee.blueprint(), !callee.overrides()),
                callee.method().name()));
    }

    private Optional<Expr> createLambda(
            TypeMirror functionalInterface,
            InstantiatedMethod singleMethod,
            GeneratedClass caller,
            JaggerPrototype callee) {
        if (!callee.method().hasOnlyParametersFrom(singleMethod)) {
            return Optional.empty();
        }

        // TODO avoid local variables
        Code argsDecl = c("($C)", Code.join(singleMethod.parameters(), ", "));
        Code argsVals = Code.join(callee.method().findArguments(null, singleMethod.parameters(), caller), ", ");

        return Optional.of(e(
                functionalInterface,
                "$C -> $C.$L($C)",
                argsDecl,
                caller.getOrCreateDelegateeField(caller.blueprint, callee.blueprint(), !callee.overrides()),
                callee.method().name(),
                argsVals));
    }

    /** Finds a parameter of type {@code Class<T>} on the method. */
    public Optional<Code> findClassParameter(InstantiatedMethod method, TypeMirror t) {
        DeclaredType classOfT = ctx.types.getDeclaredType(ctx.commonTypes.classElement, t);
        for (InstantiatedVariable parameter : method.parameters()) {
            if (ctx.types.isSameType(parameter.type(), classOfT)) {
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

        public static TypeVar of(TypeParameterElement t) {
            return of((TypeVariable) t.asType());
        }
    }
}
