package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;

import jakarta.annotation.Nonnull;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Member;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class GsonJsonWriterWriterGenerator extends AbstractWriterGenerator<GsonJsonWriterWriterGenerator> {
    private final InstantiatedVariable writerVariable;

    public GsonJsonWriterWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.writerVariable = prototype.parameters().get(1);
    }

    public GsonJsonWriterWriterGenerator(
            TypeMirror type,
            @Nonnull GsonJsonWriterWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            String potentialVariableName,
            AnyConfig config) {
        super(parent, type, potentialVariableName, rhs, lhs, config);
        this.writerVariable = parent.writerVariable;
    }

    @Override
    protected void writeNull() {
        addMemberNameIfNeeded();
        addStatement("$C.nullValue()", writerVariable);
    }

    @Override
    protected void writeString(StringKind stringKind) {
        addMemberNameIfNeeded();
        switch (stringKind) {
            case STRING -> addStatement("$C.value($C)", writerVariable, rhs);
            case CHAR_ARRAY -> addStatement("$C.value(new String($C))", writerVariable, rhs);
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addMemberNameIfNeeded();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(c("$C.value($C)", writerVariable, base64Encode(rhs)));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        addMemberNameIfNeeded();
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.CHAR) {
            addStatement("$C.value(String.valueOf($C))", writerVariable, rhs);
        } else if (kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            beginControlFlow("if ($T.isFinite($C))", kind == TypeKind.FLOAT ? Float.class : Double.class, rhs);
            addStatement("$C.value($C)", writerVariable, rhs);
            nextControlFlow("else");
            addStatement("$C.value(String.valueOf($C))", writerVariable, rhs);
            endControlFlow();
        } else {
            addStatement("$C.value($C)", writerVariable, rhs);
        }
    }

    @Override
    protected void startArray() {
        addMemberNameIfNeeded();
        addStatement("$C.beginArray()", writerVariable);
    }

    @Override
    protected void endArray() {
        addStatement("$C.endArray()", writerVariable);
    }

    @Override
    protected void startObject() {
        addMemberNameIfNeeded();
        addStatement("$C.beginObject()", writerVariable);
    }

    @Override
    protected void endObject() {
        addStatement("$C.endObject()", writerVariable);
    }

    @Override
    protected void callDelegate(Delegatee delegatee) {
        addMemberNameIfNeeded();
        addStatement(delegatee.call(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected GsonJsonWriterWriterGenerator nest(
            TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config) {
        return new GsonJsonWriterWriterGenerator(type, this, lhs, rhs, potentialVariableName, config);
    }

    private void addMemberNameIfNeeded() {
        if (lhs instanceof Member f) {
            addStatement("$C.name($C)", writerVariable, f);
        }
    }
}
