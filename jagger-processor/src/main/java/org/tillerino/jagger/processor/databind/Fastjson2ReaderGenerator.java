package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.helpers.Fastjson2ReaderHelper;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class Fastjson2ReaderGenerator extends AbstractReaderGenerator<Fastjson2ReaderGenerator> {

    private final InstantiatedVariable parserVariable;

    public Fastjson2ReaderGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        parserVariable = prototype.parameters().get(0);
    }

    public Fastjson2ReaderGenerator(
            String potentialVariableName,
            LHS lhs,
            @Nonnull Fastjson2ReaderGenerator parent,
            AnyConfig config,
            boolean exhaust) {
        super(parent, potentialVariableName, lhs, config, exhaust);
        this.parserVariable = parent.parserVariable;
    }

    @Override
    protected void readNullable(Branch branch, boolean nullable) {
        if (type instanceof ArrayType at) {
            TypeMirror componentType = at.getComponentType();
            if (ctx.commonTypes.isString(componentType)) {
                addStatement(lhs.assign("$C.readStringArray()", parserVariable));
                return;
            }
            if (ctx.commonTypes.isArrayOf(type, TypeKind.INT)) {
                addStatement(lhs.assign("$C.readInt32ValueArray()", parserVariable));
                return;
            }
            if (ctx.commonTypes.isArrayOf(type, TypeKind.LONG)) {
                addStatement(lhs.assign("$C.readInt64ValueArray()", parserVariable));
                return;
            }
        }
        super.readNullable(branch, nullable);
    }

    @Override
    protected Code stringCaseCondition() {
        return c("$C.isString()", parserVariable);
    }

    @Override
    protected Code numberCaseCondition() {
        return c("$C.isNumber()", parserVariable);
    }

    @Override
    protected Code objectCaseCondition() {
        return c("$C.nextIfObjectStart()", parserVariable);
    }

    @Override
    protected Code arrayCaseCondition() {
        return c("$C.nextIfArrayStart()", parserVariable);
    }

    @Override
    protected Code booleanCaseCondition() {
        return c("$C.current() == 'f' || $C.current() == 't'", parserVariable, parserVariable);
    }

    @Override
    protected Code memberCaseCondition() {
        return c("$C.isString()", parserVariable);
    }

    @Override
    protected void initializeParser() {}

    @Override
    protected Code nullCaseCondition() {
        return c("$C.nextIfNull()", parserVariable);
    }

    @Override
    protected void readPrimitive(TypeMirror type) {
        String readMethod =
                switch (type.getKind()) {
                    case BOOLEAN -> "readBoolValue";
                    case BYTE -> "readInt8Value";
                    case SHORT -> "readInt16Value";
                    case INT -> "readInt32Value";
                    case LONG -> "readInt64Value";
                    case FLOAT -> "readFloatValue";
                    case DOUBLE -> "readDoubleValue";
                    default ->
                        throw new ContextedRuntimeException(type.getKind().toString());
                };
        addStatement(lhs.assign("$C.$L()", parserVariable, readMethod));
    }

    @Override
    protected void readString(StringKind stringKind) {
        String conversion =
                switch (stringKind) {
                    case STRING -> "";
                    case CHAR_ARRAY -> ".toCharArray()";
                };
        addStatement(lhs.assign("$C.readString()$L", parserVariable, conversion));
    }

    @Override
    protected void iterateOverFields() {
        beginControlFlow("while (!$C.nextIfObjectEnd())", parserVariable);
    }

    @Override
    protected void skipValue() {
        addStatement("$C.skipValue()", parserVariable);
    }

    @Override
    protected void afterObject() {}

    @Override
    protected void readMemberNameInIteration(String variableName) {
        addStatement("String $L = $C.readFieldName()", variableName, parserVariable);
    }

    @Override
    protected void readDiscriminator(String propertyName) {
        addStatement(
                lhs.assign("$T.readDiscriminator($S, $C)", Fastjson2ReaderHelper.class, propertyName, parserVariable));
    }

    @Override
    protected void iterateOverElements() {
        beginControlFlow("while (!$C.nextIfArrayEnd())", parserVariable);
    }

    @Override
    protected void afterArray() {}

    @Override
    protected void throwUnexpected(String expected) {
        addStatement(
                "throw new $T($S + $C.current())",
                IOException.class,
                "Expected " + expected + ", got ",
                parserVariable);
    }

    @Override
    protected void throwUnexpectedValue(Code message) {
        addStatement("throw new $T($C)", IOException.class, message);
    }

    protected void throwUnrecognizedProperty(Code propertyName) {
        addStatement("throw new $T($S + $C + $S)", IOException.class, "Unrecognized field \"", propertyName, "\"");
    }

    @Override
    protected Fastjson2ReaderGenerator nest(String potentialVariableName, LHS lhs, AnyConfig config, boolean exhaust) {
        return new Fastjson2ReaderGenerator(potentialVariableName, lhs, this, config, exhaust);
    }
}
