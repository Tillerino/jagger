package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;

import com.squareup.javapoet.ClassName;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.helpers.JacksonJsonParserReaderHelper;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JacksonJsonParserReaderGenerator extends AbstractReaderGenerator<JacksonJsonParserReaderGenerator> {
    private final InstantiatedVariable parserVariable;

    public JacksonJsonParserReaderGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        parserVariable = prototype.parameters().get(0);
    }

    public JacksonJsonParserReaderGenerator(
            String potentialVariableName,
            LHS lhs,
            @Nonnull JacksonJsonParserReaderGenerator parent,
            AnyConfig config,
            boolean exhaust) {
        super(parent, potentialVariableName, lhs, config, exhaust);
        this.parserVariable = parent.parserVariable;
    }

    @Override
    protected Code stringCaseCondition() {
        return c("$C.currentToken() == $L", parserVariable, token("VALUE_STRING"));
    }

    @Override
    protected Code numberCaseCondition() {
        return c("$C.currentToken().isNumeric()", parserVariable);
    }

    @Override
    protected Code objectCaseCondition() {
        importHelper();
        return c("nextIfCurrentTokenIs($C, $L)", parserVariable, token("START_OBJECT"));
    }

    @Override
    protected Code arrayCaseCondition() {
        importHelper();
        return c("nextIfCurrentTokenIs($C, $L)", parserVariable, token("START_ARRAY"));
    }

    @Override
    protected Code booleanCaseCondition() {
        return c("$C.currentToken().isBoolean()", parserVariable);
    }

    @Override
    protected Code memberCaseCondition() {
        return c("$C.currentToken() == $L", parserVariable, token("FIELD_NAME"));
    }

    @Override
    protected void initializeParser() {
        beginControlFlow("if (!$C.hasCurrentToken())", parserVariable);
        advance();
        endControlFlow();
    }

    @Override
    protected Code nullCaseCondition() {
        importHelper();
        return c("nextIfCurrentTokenIs($C, $L)", parserVariable, token("VALUE_NULL"));
    }

    @Override
    protected void readPrimitive(TypeMirror type) {
        String readMethod =
                switch (type.getKind()) {
                    case BOOLEAN -> "getBooleanValue";
                    case BYTE -> "getByteValue";
                    case SHORT -> "getShortValue";
                    case INT -> "getIntValue";
                    case LONG -> "getLongValue";
                    case FLOAT -> "getFloatValue";
                    case DOUBLE -> "getDoubleValue";
                    default ->
                        throw new ContextedRuntimeException(type.getKind().toString());
                };
        if (lhs instanceof LHS.Return) {
            TypedVariable tmp = createVariable(type, "tmp");
            addStatement("$T $C = $C.$L()", type, tmp, parserVariable, readMethod);
            advance();
            addStatement("return $C", tmp);
        } else {
            addStatement(lhs.assign("$C.$L()", parserVariable, readMethod));
            advance();
        }
    }

    @Override
    protected void readString(StringKind stringKind) {
        String conversion =
                switch (stringKind) {
                    case STRING -> "";
                    case CHAR_ARRAY -> ".toCharArray()";
                };
        if (lhs instanceof LHS.Return) {
            String tmp = createVariable(null, "tmp").name();
            addStatement(
                    "$T $L = $C.getText()$L",
                    stringKind == StringKind.STRING ? String.class : char[].class,
                    tmp,
                    parserVariable,
                    conversion);
            advance();
            addStatement("return $L", tmp);
        } else {
            addStatement(lhs.assign("$C.getText()$L", parserVariable, conversion));
            advance();
        }
    }

    @Override
    protected void iterateOverFields() {
        importHelper();
        // we immediately skip the END_OBJECT token once we encounter it
        beginControlFlow("while (!nextIfCurrentTokenIs($C, $L))", parserVariable, token("END_OBJECT"));
    }

    @Override
    protected void skipValue() {
        addStatement("$C.skipChildren()", parserVariable);
        advance();
    }

    @Override
    protected void afterObject() {}

    @Override
    protected void readMemberNameInIteration(String variableName) {
        addStatement("String $L = $C.currentName()", variableName, parserVariable);
        advance();
    }

    @Override
    protected void readDiscriminator(String propertyName) {
        importHelper();
        addStatement(lhs.assign("readDiscriminator($S, $C)", propertyName, parserVariable));
    }

    @Override
    protected void iterateOverElements() {
        importHelper();
        // we immediately skip the END_ARRAY token once we encounter it
        beginControlFlow("while (!nextIfCurrentTokenIs($C, $L))", parserVariable, token("END_ARRAY"));
    }

    @Override
    protected void afterArray() {
        // we skipped the END_ARRAY token in the head of the loop
    }

    @Override
    protected void throwUnexpected(String expected) {
        addStatement(
                "throw new $T($S + $C.currentToken() + $S + $C.getCurrentLocation())",
                IOException.class,
                "Expected " + expected + ", got ",
                parserVariable,
                " at ",
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
    protected JacksonJsonParserReaderGenerator nest(
            String potentialVariableName, LHS lhs, AnyConfig config, boolean exhaust) {
        return new JacksonJsonParserReaderGenerator(potentialVariableName, lhs, this, config, exhaust);
    }

    private Class<JacksonJsonParserReaderHelper> importHelper() {
        generatedClass.fileBuilderMods.add(
                builder -> builder.addStaticImport(JacksonJsonParserReaderHelper.class, "*"));
        return JacksonJsonParserReaderHelper.class;
    }

    private String token(String t) {
        generatedClass.fileBuilderMods.add(
                builder -> builder.addStaticImport(ClassName.get("com.fasterxml.jackson.core", "JsonToken"), "*"));
        return t;
    }

    private void advance() {
        addStatement("$C.nextToken()", parserVariable);
    }
}
