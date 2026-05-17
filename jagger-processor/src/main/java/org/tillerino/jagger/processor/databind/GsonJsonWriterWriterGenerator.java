package org.tillerino.jagger.processor.databind;

import jakarta.annotation.Nonnull;
import java.util.List;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.util.Snippet;

public class GsonJsonWriterWriterGenerator extends AbstractWriterGenerator<GsonJsonWriterWriterGenerator> {
    private final VariableElement writerVariable;

    public GsonJsonWriterWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.writerVariable = prototype.element().getParameters().get(1);
    }

    public GsonJsonWriterWriterGenerator(
            TypeMirror type,
            @Nonnull GsonJsonWriterWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            Property property,
            boolean stackRelevantType,
            AnyConfig config) {
        super(parent, type, property, rhs, lhs, stackRelevantType, config);
        this.writerVariable = parent.writerVariable;
    }

    @Override
    protected void writeNull() {
        addFieldNameIfNeeded();
        addStatement("$L.nullValue()", writerVariable.getSimpleName());
    }

    @Override
    protected void writeString(StringKind stringKind) {
        addFieldNameIfNeeded();
        switch (stringKind) {
            case STRING -> addStatement("$L.value($C)", writerVariable.getSimpleName(), rhs);
            case CHAR_ARRAY -> addStatement("$L.value(new String($C))", writerVariable.getSimpleName(), rhs);
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfNeeded();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(Snippet.of("$L.value($C)", writerVariable, base64Encode(rhs)));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        addFieldNameIfNeeded();
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.CHAR) {
            addStatement("$L.value(String.valueOf($C))", writerVariable.getSimpleName(), rhs);
        } else if (kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            beginControlFlow("if ($T.isFinite($C))", kind == TypeKind.FLOAT ? Float.class : Double.class, rhs);
            addStatement("$L.value($C)", writerVariable.getSimpleName(), rhs);
            nextControlFlow("else");
            addStatement("$L.value(String.valueOf($C))", writerVariable.getSimpleName(), rhs);
            endControlFlow();
        } else {
            addStatement("$L.value($C)", writerVariable.getSimpleName(), rhs);
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfNeeded();
        addStatement("$L.beginArray()", writerVariable.getSimpleName());
    }

    @Override
    protected void endArray() {
        addStatement("$L.endArray()", writerVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        addFieldNameIfNeeded();
        addStatement("$L.beginObject()", writerVariable.getSimpleName());
    }

    @Override
    protected void endObject() {
        addStatement("$L.endObject()", writerVariable.getSimpleName());
    }

    @Override
    protected void invokeDelegate(Delegatee delegatee) {
        addFieldNameIfNeeded();
        addStatement(delegatee.invoke(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected GsonJsonWriterWriterGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new GsonJsonWriterWriterGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
    }

    private void addFieldNameIfNeeded() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.name($C)", writerVariable.getSimpleName(), f);
        }
    }
}
