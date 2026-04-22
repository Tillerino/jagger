package org.tillerino.jagger.processor.util;

import static java.util.Arrays.asList;

import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public interface PrototypeKind {
    Enum<?> direction();

    TypeMirror externalType();

    TypeMirror internalType();

    CodeBlock.Builder generateCode(CodeGeneratorContext context);

    List<InstantiatedMethod.InstantiatedVariable> otherParameters();

    String defaultMethodName();

    PrototypeKind withInternalType(TypeMirror newType);

    default Optional<TypeMirror> contextType() {
        return Optional.empty();
    }

    default boolean matches(PrototypeKind other, JaggerContext ctx) {
        return direction() == other.direction()
                && ctx.types.isSameType(externalType(), other.externalType())
                && ctx.types.isSameType(internalType(), other.internalType());
    }

    static Optional<PrototypeKind> detect(
            List<TypeMirror> externalTypes, InstantiatedMethod m, JaggerContext ctx, PrototypeKindInstantiator c) {
        for (TypeMirror externalType : externalTypes) {
            if (externalType == null) {
                continue;
            }
            externalType = ctx.types.erasure(externalType);
            if (variablesContain(m.parameters(), externalType, ctx)) {
                InstantiatedVariable externalParameter = variableOfType(m.parameters(), externalType, ctx);
                List<InstantiatedVariable> otherParameters = variablesExcept(m.parameters(), externalType, ctx);
                return Optional.of(c.instantiate(externalType, externalParameter, otherParameters));
            }
        }
        return Optional.empty();
    }

    static String simpleTypeName(TypeMirror t) {
        if (t.getKind().isPrimitive()) {
            return "Primitive" + StringUtils.capitalize(t.toString());
        }

        if (t instanceof ArrayType a) {
            return "ArrayOf" + simpleTypeName(a.getComponentType());
        }

        if (!(t instanceof DeclaredType d)) {
            throw new ContextedRuntimeException("Only primitives or declared types expected").addContextValue("t", t);
        }

        return d.asElement().getSimpleName().toString();
    }

    static boolean variablesContain(
            List<InstantiatedVariable> variables, TypeMirror fullyQualified, JaggerContext ctx) {
        for (InstantiatedVariable variable : variables) {
            if (ctx.types.isAssignable(variable.type(), fullyQualified)) {
                return true;
            }
        }
        return false;
    }

    static InstantiatedVariable variableOfType(
            List<InstantiatedVariable> variables, TypeMirror fullyQualified, JaggerContext ctx) {
        for (InstantiatedVariable variable : variables) {
            if (ctx.types.isAssignable(variable.type(), fullyQualified)) {
                return variable;
            }
        }
        throw Exceptions.unexpected();
    }

    static List<InstantiatedVariable> variablesExcept(
            List<InstantiatedVariable> variables, TypeMirror excludeFullyQualified, JaggerContext ctx) {
        return variables.stream()
                .filter(v -> !ctx.types.isAssignable(v.type(), excludeFullyQualified))
                .toList();
    }

    static List<TypeMirror> nullableTypeList(TypeMirror... nullableTypes) {
        return asList(nullableTypes);
    }

    interface PrototypeKindInstantiator {
        PrototypeKind instantiate(
                TypeMirror externalBaseKind,
                InstantiatedVariable externalParameter,
                List<InstantiatedVariable> otherParameters);
    }

    record CodeGeneratorContext(JaggerContext ctx, JaggerPrototype prototype, GeneratedClass generatedClass) {}
}
