package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.api.JaggerReader.Advance.CONSUME;
import static org.tillerino.jagger.processor.util.Code.c;

import com.squareup.javapoet.ClassName;
import jakarta.annotation.Nonnull;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.api.JaggerReader;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JaggerReaderGenerator extends AbstractReaderGenerator<JaggerReaderGenerator> {
    private final InstantiatedVariable parserVariable;

    public JaggerReaderGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        parserVariable = prototype.parameters().get(0);
    }

    public JaggerReaderGenerator(
            String potentialVariableName,
            LHS lhs,
            @Nonnull JaggerReaderGenerator parent,
            AnyConfig config,
            boolean exhaust) {
        super(parent, potentialVariableName, lhs, config, exhaust);
        this.parserVariable = parent.parserVariable;
    }

    @Override
    protected Code stringCaseCondition() {
        return c("$C.isText()", parserVariable);
    }

    @Override
    protected Code numberCaseCondition() {
        return c("$C.isNumber()", parserVariable);
    }

    @Override
    protected Code objectCaseCondition() {
        return c("$C.isObjectStart($L)", parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected Code arrayCaseCondition() {
        return c("$C.isArrayStart($L)", parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected Code booleanCaseCondition() {
        return c("$C.isBoolean()", parserVariable);
    }

    @Override
    protected Code memberCaseCondition() {
        return c("$C.isFieldName()", parserVariable);
    }

    @Override
    protected void initializeParser() {
        // nothing to do, reader always starts with a token, yay!
    }

    @Override
    protected Code nullCaseCondition() {
        return c("$C.isNull($L)", parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected void readPrimitive(TypeMirror type) {
        String method =
                switch (type.getKind()) {
                    case BOOLEAN -> "getBoolean";
                    case BYTE -> "getByte";
                    case SHORT -> "getShort";
                    case INT -> "getInt";
                    case LONG -> "getLong";
                    case FLOAT -> "getFloat";
                    case DOUBLE -> "getDouble";
                    default ->
                        throw new ContextedRuntimeException(type.getKind().toString());
                };
        Code code = c("$C.$L($L)", parserVariable, method, importAdvance(CONSUME));
        addStatement(lhs.assign(code));
    }

    @Override
    protected void readString(StringKind stringKind) {
        String conversion =
                switch (stringKind) {
                    case STRING -> "";
                    case CHAR_ARRAY -> ".toCharArray()";
                };
        Code code = c("$C.getText($L)$L", parserVariable, importAdvance(CONSUME), conversion);
        addStatement(lhs.assign(code));
    }

    @Override
    protected void iterateOverFields() {
        beginControlFlow("while (!$C.isObjectEnd($L))", parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected void skipValue() {
        addStatement("$C.skipChildren($L)", parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected void afterObject() {}

    @Override
    protected void readMemberNameInIteration(String variableName) {
        addStatement("String $L = $C.getFieldName($L)", variableName, parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected void readDiscriminator(String propertyName) {
        addStatement(lhs.assign("$C.getDiscriminator($S, false)", parserVariable, propertyName));
    }

    @Override
    protected void iterateOverElements() {
        beginControlFlow("while (!$C.isArrayEnd($L))", parserVariable, importAdvance(CONSUME));
    }

    @Override
    protected void afterArray() {
        // we skipped the END_ARRAY token in the head of the loop
    }

    @Override
    protected void throwUnexpected(String expectedToken) {
        addStatement("throw $C.unexpectedToken($S)", parserVariable, expectedToken);
    }

    @Override
    protected void throwUnexpectedValue(Code message) {
        addStatement("throw $C.unexpectedValue($C)", parserVariable, message);
    }

    protected void throwUnrecognizedProperty(Code propertyName) {
        addStatement("throw $C.unrecognizedProperty($C)", parserVariable, propertyName);
    }

    @Override
    protected JaggerReaderGenerator nest(String potentialVariableName, LHS lhs, AnyConfig config, boolean exhaust) {
        return new JaggerReaderGenerator(potentialVariableName, lhs, this, config, exhaust);
    }

    private String importAdvance(JaggerReader.Advance advance) {
        generatedClass.fileBuilderMods.add(
                builder -> builder.addStaticImport(ClassName.get(JaggerReader.Advance.class), "*"));
        return advance.name();
    }
}
