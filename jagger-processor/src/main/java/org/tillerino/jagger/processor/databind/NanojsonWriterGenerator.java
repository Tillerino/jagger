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
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class NanojsonWriterGenerator extends AbstractWriterGenerator<NanojsonWriterGenerator> {
    private final InstantiatedVariable generatorVariable;

    public NanojsonWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.generatorVariable = prototype.method().parameterOfType(kind.types().get(1));
    }

    protected NanojsonWriterGenerator(
            TypeMirror type,
            @Nonnull NanojsonWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            String potentialVariableName,
            AnyConfig config) {
        super(parent, type, potentialVariableName, rhs, lhs, config);
        this.generatorVariable = parent.generatorVariable;
    }

    @Override
    protected Features features() {
        return new Features(true);
    }

    @Override
    protected void writeNull() {
        if (lhs instanceof Member f) {
            addStatement("$C.nul($C)", generatorVariable, f);
        } else {
            addStatement("$C.nul()", generatorVariable);
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        Code string = stringKind == StringKind.STRING ? rhs : charArrayToString(rhs);
        if (lhs instanceof Member f) {
            addStatement(c("$C.value($C, $C)", generatorVariable, f, string));
        } else {
            addStatement(c("$C.value($C)", generatorVariable, string));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        Code asString = base64Encode(rhs);
        if (lhs instanceof Member f) {
            if (binaryKind == BinaryKind.BYTE_ARRAY) {
                addStatement(c("$C.value($C, $C)", generatorVariable, f, asString));
                return;
            } else {
            }
        }
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(c("$C.value($C)", generatorVariable, asString));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        Code rhs_ = rhs;
        if (typeMirror.getKind() == TypeKind.CHAR) {
            rhs_ = c("String.valueOf($C)", rhs);
        }
        if (lhs instanceof Member f) {
            addStatement(c("$C.value($C, $C)", generatorVariable, f, rhs_));
        } else {
            addStatement(c("$C.value($C)", generatorVariable, rhs_));
        }
    }

    @Override
    protected void startArray() {
        if (lhs instanceof Member f) {
            addStatement("$C.array($C)", generatorVariable, f);
        } else {
            addStatement("$C.array()", generatorVariable);
        }
    }

    @Override
    protected void endArray() {
        addStatement("$C.end()", generatorVariable);
    }

    @Override
    protected void startObject() {
        if (lhs instanceof Member f) {
            addStatement("$C.object($C)", generatorVariable, f);
        } else {
            addStatement("$C.object()", generatorVariable);
        }
    }

    @Override
    protected void endObject() {
        addStatement("$C.end()", generatorVariable);
    }

    @Override
    protected void callDelegate(Delegatee delegatee) {
        if (lhs instanceof Member f) {
            addStatement(c("$C.key($C)", generatorVariable, f));
        }

        addStatement(delegatee.call(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected NanojsonWriterGenerator nest(
            TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config) {
        return new NanojsonWriterGenerator(type, this, lhs, rhs, potentialVariableName, config);
    }
}
