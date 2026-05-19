package org.tillerino.jagger.processor.databind;

import java.util.List;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Member;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class Fastjson2WriterGenerator extends AbstractWriterGenerator<Fastjson2WriterGenerator> {
    private final InstantiatedVariable writerVariable;

    public Fastjson2WriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.writerVariable = prototype.method().parameterOfType(kind.types().get(1));
    }

    public Fastjson2WriterGenerator(
            TypeMirror type,
            Fastjson2WriterGenerator parent,
            LHS lhs,
            RHS rhs,
            String potentialVariableName,
            AnyConfig config) {
        super(parent, type, potentialVariableName, rhs, lhs, config);
        this.writerVariable = parent.writerVariable;
    }

    @Override
    protected void writeNullable() {
        if (writeNatively()) {
            return;
        }
        super.writeNullable();
    }

    @Override
    protected void writeNull() {
        addFieldNameIfNeeded();
        addStatement("$C.writeNull()", writerVariable);
    }

    @Override
    protected void writeString(StringKind stringKind) {
        addFieldNameIfNeeded();
        switch (stringKind) {
            case STRING -> addStatement("$C.writeString($C)", writerVariable, rhs);
            case CHAR_ARRAY -> addStatement("$C.writeString(new String($C))", writerVariable, rhs);
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfNeeded();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement("$C.writeBase64($C)", writerVariable, rhs);
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        addFieldNameIfNeeded();
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.CHAR) {
            addStatement("$C.writeString(String.valueOf($C))", writerVariable, rhs);
        } else if (kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            String write = kind == TypeKind.FLOAT ? "writeFloat" : "writeDouble";
            String cast = kind == TypeKind.FLOAT ? "(float)" : "(double)";
            beginControlFlow("if ($T.isFinite($C))", kind == TypeKind.FLOAT ? Float.class : Double.class, rhs);
            addStatement("$C.$L($L $C)", writerVariable, write, cast, rhs);
            nextControlFlow("else");
            addStatement("$C.writeString(String.valueOf($C))", writerVariable, rhs);
            endControlFlow();
        } else {
            String write =
                    switch (kind) {
                        case BOOLEAN -> "writeBool";
                        case BYTE -> "writeInt8";
                        case SHORT -> "writeInt16";
                        case INT -> "writeInt32";
                        case LONG -> "writeInt64";
                        default -> throw new ContextedRuntimeException("Unexpected type: " + kind);
                    };
            addStatement("$C.$L($C)", writerVariable, write, rhs);
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfNeeded();
        addStatement("$C.startArray()", writerVariable);
    }

    @Override
    protected void endArray() {
        addStatement("$C.endArray()", writerVariable);
    }

    @Override
    protected void startObject() {
        addFieldNameIfNeeded();
        addStatement("$C.startObject()", writerVariable);
    }

    @Override
    boolean needsToWriteComma() {
        return true;
    }

    @Override
    protected void writeComma() {
        addStatement("$C.writeComma()", writerVariable);
    }

    @Override
    protected void endObject() {
        addStatement("$C.endObject()", writerVariable);
    }

    @Override
    protected void callDelegate(Delegatee delegatee) {
        addFieldNameIfNeeded();
        addStatement(delegatee.call(prototype, List.of(rhs), generatedClass));
    }

    @Override
    protected Fastjson2WriterGenerator nest(
            TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config) {
        return new Fastjson2WriterGenerator(type, this, lhs, rhs, potentialVariableName, config);
    }

    private boolean writeNatively() {
        if (type instanceof ArrayType at && writeArrayNatively(at.getComponentType())) {
            return true;
        }
        return false;
    }

    private boolean writeArrayNatively(TypeMirror componentType) {
        if (writePrimitiveArrayNatively(componentType.getKind())) {
            return true;
        }
        return false;
    }

    private boolean writePrimitiveArrayNatively(TypeKind kind) {
        String t =
                switch (kind) {
                    case BOOLEAN -> "Bool";
                    case SHORT -> "Int16";
                    case INT -> "Int32";
                    case LONG -> "Int64";
                    // for floating point, writes null for non-finite, so we cannot use those
                    default -> null;
                };
        if (t != null) {
            addFieldNameIfNeeded();
            addStatement("$C.write" + t + "($C)", writerVariable, rhs);
            return true;
        }
        return false;
    }

    private void addFieldNameIfNeeded() {
        if (lhs instanceof Member f) {
            addStatement("$C.writeName($C)", writerVariable, f);
            addStatement("$C.writeColon()", writerVariable);
        }
    }
}
