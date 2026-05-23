package org.tillerino.jagger.processor.util;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.AbstractTypeVisitor8;
import javax.lang.model.util.Types;
import org.apache.commons.lang3.exception.ContextedRuntimeException;

public class RebuildingTypeVisitor extends AbstractTypeVisitor8<TypeMirror, Types> {

    @Override
    public TypeMirror visitIntersection(IntersectionType t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitPrimitive(PrimitiveType t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitNull(NullType t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitArray(ArrayType t, Types types) {
        return types.getArrayType(t.getComponentType().accept(this, types));
    }

    @Override
    public TypeMirror visitDeclared(DeclaredType t, Types types) {
        try {
            TypeMirror[] newTypeArguments = t.getTypeArguments().stream()
                    .map(arg -> arg.accept(this, types))
                    .toArray(TypeMirror[]::new);

            for (TypeMirror tm : newTypeArguments) {
                if (tm.getKind().isPrimitive()) {
                    throw new ContextedRuntimeException("Cannot use primitive as type parameter")
                            .addContextValue("primitive", tm)
                            .addContextValue("parameterized type", t);
                }
            }

            return types.getDeclaredType((TypeElement) t.asElement(), newTypeArguments);
        } catch (IllegalArgumentException e) {
            throw new ContextedRuntimeException(e);
        }
    }

    @Override
    public TypeMirror visitError(ErrorType t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitTypeVariable(TypeVariable t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitWildcard(WildcardType t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitExecutable(ExecutableType t, Types types) {
        return null;
    }

    @Override
    public TypeMirror visitNoType(NoType t, Types types) {
        return t;
    }

    @Override
    public TypeMirror visitUnion(UnionType t, Types types) {
        return t;
    }
}
