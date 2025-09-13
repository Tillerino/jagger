package org.tillerino.jagger.processor.apis;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class Fastjson2WriterGenerator extends AbstractWriterGenerator<Fastjson2WriterGenerator> {
    private final VariableElement writerVariable;

    public Fastjson2WriterGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(utils, prototype, generatedClass);
        this.writerVariable = prototype.methodElement().getParameters().get(1);
    }

    public Fastjson2WriterGenerator(
            TypeMirror type,
            Fastjson2WriterGenerator parent,
            LHS lhs,
            RHS rhs,
            Property property,
            boolean stackRelevantType,
            AnyConfig config) {
        super(parent, type, property, rhs, lhs, stackRelevantType, config);
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
        addStatement("$L.writeNull()", writerVariable.getSimpleName());
    }

    @Override
    protected void writeString(StringKind stringKind) {
        addFieldNameIfNeeded();
        switch (stringKind) {
            case STRING -> addStatement(
                    "$L.writeString(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), rhs.args()));
            case CHAR_ARRAY -> addStatement(
                    "$L.writeString(new String(" + rhs.format() + "))",
                    flatten(writerVariable.getSimpleName(), rhs.args()));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfNeeded();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(
                    "$L.writeBase64(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), rhs.args()));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        addFieldNameIfNeeded();
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.CHAR) {
            addStatement(
                    "$L.writeString(String.valueOf(" + rhs.format() + "))",
                    flatten(writerVariable.getSimpleName(), rhs.args()));
        } else if (kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            String write = kind == TypeKind.FLOAT ? "writeFloat" : "writeDouble";
            String cast = kind == TypeKind.FLOAT ? "(float)" : "(double)";
            beginControlFlow(
                    "if ($T.isFinite(" + rhs.format() + "))",
                    flatten(kind == TypeKind.FLOAT ? Float.class : Double.class, rhs.args()));
            addStatement(
                    "$L.$L($L " + rhs.format() + ")", flatten(writerVariable.getSimpleName(), write, cast, rhs.args()));
            nextControlFlow("else");
            addStatement(
                    "$L.writeString(String.valueOf(" + rhs.format() + "))",
                    flatten(writerVariable.getSimpleName(), rhs.args()));
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
            addStatement("$L.$L(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), write, rhs.args()));
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfNeeded();
        addStatement("$L.startArray()", writerVariable.getSimpleName());
    }

    @Override
    protected void endArray() {
        addStatement("$L.endArray()", writerVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        addFieldNameIfNeeded();
        addStatement("$L.startObject()", writerVariable.getSimpleName());
    }

    @Override
    boolean needsToWriteComma() {
        return true;
    }

    @Override
    protected void writeComma() {
        addStatement("$L.writeComma()", writerVariable.getSimpleName());
    }

    @Override
    protected void endObject() {
        addStatement("$L.endObject()", writerVariable.getSimpleName());
    }

    @Override
    protected void invokeDelegate(String instance, InstantiatedMethod callee) {
        addFieldNameIfNeeded();
        addStatement(Snippet.of(
                "$L.$L($C$C)",
                instance,
                callee,
                rhs,
                Snippet.joinPrependingCommaToEach(
                        utils.delegation.findArguments(prototype, callee, 1, generatedClass))));
    }

    @Override
    protected Fastjson2WriterGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new Fastjson2WriterGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
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
            addStatement(
                    "$L.write" + t + "(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), rhs.args()));
            return true;
        }
        return false;
    }

    private void addFieldNameIfNeeded() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.writeName(" + f.format() + ")", flatten(writerVariable.getSimpleName(), f.args()));
            addStatement("$L.writeColon()", writerVariable.getSimpleName());
        }
    }
}
