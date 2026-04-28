package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet.ConstructorCall;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet.StaticMethodInvocation;

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
        AnyConfig config)
        implements Named {
    public Snippet callSymbol(JaggerContext ctx) {
        TypeMirror tm = element.getEnclosingElement().asType();
        TypeMirror raw = ctx.types.erasure(tm);
        String diamond =
                (tm instanceof DeclaredType dt) && !dt.getTypeArguments().isEmpty() ? "<>" : "";
        return element.getKind() == ElementKind.CONSTRUCTOR
                ? Snippet.of("new $T$L", raw, diamond)
                : Snippet.of("$T.$L", raw, name);
    }

    public PerfectSnippet invoke(JaggerContext ctx, List<PerfectSnippet> args) {
        TypeMirror tm = element.getEnclosingElement().asType();
        TypeMirror raw = ctx.types.erasure(tm);
        String diamond =
                (tm instanceof DeclaredType dt) && !dt.getTypeArguments().isEmpty() ? "<>" : "";
        return element.getKind() == ElementKind.CONSTRUCTOR
                ? new ConstructorCall(returnType, raw, diamond, args)
                : new StaticMethodInvocation(returnType, raw, name, args);
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

    public boolean hasParameterAssignableFrom(TypeMirror t, JaggerContext ctx) {
        return parameters.stream().anyMatch(p -> ctx.commonTypes.isAssignable(t, p.type));
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
        return new InstantiatedMethod(name, returnType, parameters, element, freeTypeVars, config);
    }

    public record InstantiatedVariable(VariableElement elem, TypeMirror type, String name, AnyConfig config)
            implements PerfectSnippet {
        @Override
        public String toString() {
            return ShortName.of(type) + " " + name();
        }

        @Override
        public Flattened flatten() {
            return Flattened.of("$L", name);
        }

        @Override
        public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
            return this.name.equals(name) ? replacement : this;
        }
    }
}
