package org.tillerino.jagger.processor.util;

import static org.tillerino.jagger.processor.util.Expr.e;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Code.AnyVariable;

/**
 * Need this to instantiate generics.
 *
 * @param freeTypeVars the type vars declared by the method itself, not the surrounding type
 */
public record InstantiatedMethod(
        String name,
        TypeMirror returnType,
        List<InstantiatedVariable> parameters,
        ExecutableElement element,
        Set<TypeVar> freeTypeVars,
        AnyConfig config,
        JaggerContext ctx)
        implements Named {

    public Expr callStatic(List<Expr> args) {
        TypeMirror tm = element.getEnclosingElement().asType();
        TypeMirror raw = ctx.types.erasure(tm);
        String diamond =
                (tm instanceof DeclaredType dt) && !dt.getTypeArguments().isEmpty() ? "<>" : "";
        return element.getKind() == ElementKind.CONSTRUCTOR
                ? e(returnType, "new $T$L($C)", raw, diamond, Code.join(args, ", "))
                : e(returnType, "$T.$L($C)", raw, name, Code.join(args, ", "));
    }

    public Expr callStaticFindingArguments(
            JaggerPrototype caller, List<Expr> additionalParameters, GeneratedClass generatedClass) {
        return callStatic(findArguments(caller, additionalParameters, generatedClass));
    }

    public Expr call(Expr instance) {
        return instance.call(returnType, name);
    }

    public Expr call(Expr instance, List<Expr> args) {
        return instance.call(returnType, name, args);
    }

    public Expr callFindingArguments(
            Expr instance, JaggerPrototype caller, List<Expr> additionalParameters, GeneratedClass generatedClass) {
        return call(instance, findArguments(caller, additionalParameters, generatedClass));
    }

    public List<Expr> findArguments(
            JaggerPrototype caller, List<Expr> additionalParameters, GeneratedClass generatedClass) {
        return ctx.delegation.findArguments(caller, this, additionalParameters, 0, generatedClass);
    }

    public boolean hasSameSignature(InstantiatedMethod other, JaggerContext ctx) {
        if (!ctx.types.isSameType(returnType, other.returnType)) {
            return false;
        }
        if (parameters.size() != other.parameters.size()) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (!ctx.types.isSameType(
                    parameters.get(i).type(), other.parameters.get(i).type())) {
                return false;
            }
        }
        return true;
    }

    public boolean parametersContain(TypeMirror fullyQualified) {
        for (InstantiatedVariable variable : parameters) {
            if (ctx.types.isAssignable(variable.type(), fullyQualified)) {
                return true;
            }
        }
        return false;
    }

    public InstantiatedVariable parameterOfType(TypeMirror fullyQualified) {
        for (InstantiatedVariable variable : parameters) {
            if (ctx.types.isAssignable(variable.type(), fullyQualified)) {
                return variable;
            }
        }
        throw Exceptions.unexpected();
    }

    public List<InstantiatedVariable> parametersExcept(TypeMirror excludeFullyQualified) {
        return parameters.stream()
                .filter(v -> !ctx.types.isAssignable(v.type(), excludeFullyQualified))
                .toList();
    }

    public boolean hasParameterAssignableFrom(TypeMirror t, JaggerContext ctx) {
        return parameters.stream().anyMatch(p -> ctx.commonTypes.isAssignable(t, p.type));
    }

    public Optional<AnnotationMirrorWrapper> findAnnotation(String annotationType) {
        return ctx.annotations.findAnnotation(element, annotationType);
    }

    @Override
    public String toString() {
        return String.format(
                "%s %s.%s(%s)",
                ShortName.of(returnType),
                element.getEnclosingElement().getSimpleName(),
                name,
                parameters.stream().map(InstantiatedVariable::toString).collect(Collectors.joining(", ")));
    }

    public InstantiatedMethod withName(String name) {
        return new InstantiatedMethod(name, returnType, parameters, element, freeTypeVars, config, ctx);
    }

    public record InstantiatedVariable(VariableElement elem, TypeMirror type, String name, AnyConfig config)
            implements AnyVariable {
        @Override
        public String toString() {
            return ShortName.of(type) + " " + name();
        }

        @Override
        public Flattened flatten() {
            return Flattened.of("$L", name);
        }

        @Override
        public Expr subst(Expr needle, Expr replacement) {
            return needle instanceof AnyVariable v && v.variableName().equals(name) ? replacement : this;
        }

        @Override
        public boolean isQuick() {
            return true;
        }

        @Override
        public String variableName() {
            return name;
        }
    }
}
