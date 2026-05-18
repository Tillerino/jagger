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

public class JakartaJsonGeneratorGenerator extends AbstractWriterGenerator<JakartaJsonGeneratorGenerator> {
    private final InstantiatedVariable generatorVariable;

    public JakartaJsonGeneratorGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.generatorVariable = prototype.parameters().get(1);
    }

    public JakartaJsonGeneratorGenerator(
            TypeMirror type,
            @Nonnull JakartaJsonGeneratorGenerator parent,
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
            addStatement("$C.writeNull($C)", generatorVariable, f);
        } else {
            addStatement("$C.writeNull()", generatorVariable);
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        Code string = stringKind == StringKind.STRING ? rhs : charArrayToString(rhs);
        if (lhs instanceof Member f) {
            addStatement(c("$C.write($C, $C)", generatorVariable, f, string));
        } else {
            addStatement(c("$C.write($C)", generatorVariable, string));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfRequired();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(c("$C.write($C)", generatorVariable, base64Encode(rhs)));
        }
    }

    private boolean addFieldNameIfRequired() {
        if (lhs instanceof Member f) {
            addStatement("$C.writeKey($C)", generatorVariable, f);
            return true;
        }
        return false;
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        Code value = typeMirror.getKind() == TypeKind.CHAR ? c("String.valueOf($C)", rhs) : rhs;
        if (lhs instanceof Member f) {
            addStatement(c("$C.write($C, $C)", generatorVariable, f, value));
        } else {
            addStatement(c("$C.write($C)", generatorVariable, value));
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfRequired();
        addStatement("$C.writeStartArray()", generatorVariable);
    }

    @Override
    protected void endArray() {
        addStatement("$C.writeEnd()", generatorVariable);
    }

    @Override
    protected void startObject() {
        addFieldNameIfRequired();
        addStatement("$C.writeStartObject()", generatorVariable);
    }

    @Override
    protected void endObject() {
        addStatement("$C.writeEnd()", generatorVariable);
    }

    @Override
    protected void callDelegate(Delegatee delegatee) {
        addFieldNameIfRequired();
        addStatement(delegatee.call(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected JakartaJsonGeneratorGenerator nest(
            TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config) {
        return new JakartaJsonGeneratorGenerator(type, this, lhs, rhs, potentialVariableName, config);
    }
}
