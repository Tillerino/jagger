package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;

import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Member;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JaggerWriterGenerator extends AbstractWriterGenerator<JaggerWriterGenerator> {
    private final InstantiatedVariable generatorVariable;

    public JaggerWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.generatorVariable = prototype.parameters().get(1);
    }

    protected JaggerWriterGenerator(
            TypeMirror type,
            JaggerWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            String potentialVariableName,
            AnyConfig config) {
        super(parent, type, potentialVariableName, rhs, lhs, config);
        this.generatorVariable = parent.generatorVariable;
    }

    @Override
    protected Features features() {
        return new Features(false);
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
        Code string = stringKind == StringKind.STRING ? rhs : charArrayToString(rhs);
        if (lhs instanceof Member f) {
            addStatement(c("$C.writeField($C, $C)", generatorVariable, f, string));
        } else {
            addStatement(c("$C.write($C)", generatorVariable, string));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        Code asString = base64Encode(rhs);
        if (lhs instanceof Member f) {
            if (binaryKind == BinaryKind.BYTE_ARRAY) {
                addStatement(c("$C.writeField($C, $C)", generatorVariable, f, asString));
                return;
            } else {
            }
        }
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(c("$C.write($C)", generatorVariable, asString));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        Code rhs_ = rhs;
        if (typeMirror.getKind() == TypeKind.CHAR) {
            rhs_ = c("String.valueOf($C)", rhs);
        }
        if (lhs instanceof Member f) {
            addStatement(c("$C.writeField($C, $C)", generatorVariable, f, rhs_));
        } else {
            addStatement(c("$C.write($C)", generatorVariable, rhs_));
        }
    }

    @Override
    protected void startArray() {
        if (lhs instanceof Member f) {
            addStatement("$C.startArrayField($C)", generatorVariable, f);
        } else {
            addStatement("$C.startArray()", generatorVariable);
        }
    }

    @Override
    protected void endArray() {
        addStatement("$C.endArray()", generatorVariable);
    }

    @Override
    protected void startObject() {
        if (lhs instanceof Member f) {
            addStatement("$C.startObjectField($C)", generatorVariable, f);
        } else {
            addStatement("$C.startObject()", generatorVariable);
        }
    }

    @Override
    protected void endObject() {
        addStatement("$C.endObject()", generatorVariable);
    }

    @Override
    protected void callDelegate(Delegatee delegatee) {
        if (lhs instanceof Member f) {
            addStatement(c("$C.writeFieldName($C)", generatorVariable, f));
        }

        addStatement(delegatee.call(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected JaggerWriterGenerator nest(
            TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config) {
        return new JaggerWriterGenerator(type, this, lhs, rhs, potentialVariableName, config);
    }
}
