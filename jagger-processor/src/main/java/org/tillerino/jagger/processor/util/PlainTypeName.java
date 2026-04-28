package org.tillerino.jagger.processor.util;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor14;
import org.apache.commons.lang3.StringUtils;

public class PlainTypeName extends SimpleTypeVisitor14<StringBuilder, StringBuilder> {
    public static String of(TypeMirror t) {
        return t.accept(new PlainTypeName(), new StringBuilder()).toString();
    }

    @Override
    public StringBuilder visitPrimitive(PrimitiveType t, StringBuilder stringBuilder) {
        return stringBuilder.append("Primitive").append(StringUtils.capitalize(t.toString()));
    }

    @Override
    public StringBuilder visitArray(ArrayType t, StringBuilder stringBuilder) {
        stringBuilder.append("ArrayOf");
        t.getComponentType().accept(this, stringBuilder);
        return stringBuilder;
    }

    @Override
    public StringBuilder visitDeclared(DeclaredType t, StringBuilder stringBuilder) {
        stringBuilder.append(t.asElement().getSimpleName());
        boolean first = true;
        for (TypeMirror typeArgument : t.getTypeArguments()) {
            stringBuilder.append(first ? "Of" : "And");
            typeArgument.accept(this, stringBuilder);
            first = false;
        }
        return stringBuilder;
    }
}
