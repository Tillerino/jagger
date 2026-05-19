package org.tillerino.jagger.processor.databind;

import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Member;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JacksonJsonGeneratorWriterGenerator extends AbstractWriterGenerator<JacksonJsonGeneratorWriterGenerator> {
    private final InstantiatedVariable generatorVariable;

    public JacksonJsonGeneratorWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.generatorVariable = prototype.method().parameterOfType(kind.types().get(1));
    }

    protected JacksonJsonGeneratorWriterGenerator(
            TypeMirror type,
            JacksonJsonGeneratorWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            String potentialVariableName,
            AnyConfig config) {
        super(parent, type, potentialVariableName, rhs, lhs, config);
        this.generatorVariable = parent.generatorVariable;
    }

    @Override
    protected void writeNull() {
        if (lhs instanceof Member f) {
            addStatement("$C.writeNullField($C)", generatorVariable, f);
        } else {
            addStatement("$C.writeNull()", generatorVariable);
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        if (lhs instanceof Member f) {
            if (stringKind == StringKind.STRING) {
                addStatement("$C.writeStringField($C, $C)", generatorVariable, f, rhs);
                return;
            } else {
                addStatement("$C.writeFieldName($C)", generatorVariable, f);
            }
        }
        switch (stringKind) {
            case STRING -> addStatement("$C.writeString($C)", generatorVariable, rhs);
            case CHAR_ARRAY -> addStatement("$C.writeString($C, 0, $C.length)", generatorVariable, rhs, rhs);
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfRequired();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement("$C.writeBinary($C)", generatorVariable, rhs);
        }
    }

    private boolean addFieldNameIfRequired() {
        if (lhs instanceof Member f) {
            addStatement("$C.writeFieldName($C)", generatorVariable, f);
            return true;
        }
        return false;
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        if (lhs instanceof Member f) {
            if (typeMirror.getKind() == TypeKind.BOOLEAN) {
                addStatement("$C.writeBooleanField($C, $C)", generatorVariable, f, rhs);
            } else if (typeMirror.getKind() == TypeKind.CHAR) {
                addStatement("$C.writeStringField($C, String.valueOf($C))", generatorVariable, f, rhs);
            } else {
                addStatement("$C.writeNumberField($C, $C)", generatorVariable, f, rhs);
            }
        } else {
            if (typeMirror.getKind() == TypeKind.BOOLEAN) {
                addStatement("$C.writeBoolean($C)", generatorVariable, rhs);
            } else if (typeMirror.getKind() == TypeKind.CHAR) {
                addStatement("$C.writeString(String.valueOf($C))", generatorVariable, rhs);
            } else {
                addStatement("$C.writeNumber($C)", generatorVariable, rhs);
            }
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfRequired();
        addStatement("$C.writeStartArray()", generatorVariable);
    }

    @Override
    protected void endArray() {
        addStatement("$C.writeEndArray()", generatorVariable);
    }

    @Override
    protected void startObject() {
        addFieldNameIfRequired();
        addStatement("$C.writeStartObject()", generatorVariable);
    }

    @Override
    protected void endObject() {
        addStatement("$C.writeEndObject()", generatorVariable);
    }

    @Override
    protected void callDelegate(Delegatee delegatee) {
        addFieldNameIfRequired();
        addStatement(delegatee.call(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected JacksonJsonGeneratorWriterGenerator nest(
            TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config) {
        return new JacksonJsonGeneratorWriterGenerator(type, this, lhs, rhs, potentialVariableName, config);
    }
}
