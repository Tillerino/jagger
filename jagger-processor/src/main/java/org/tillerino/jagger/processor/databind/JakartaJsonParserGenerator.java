package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;

import com.squareup.javapoet.ClassName;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.helpers.JakartaJsonParserHelper;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JakartaJsonParserGenerator extends AbstractReaderGenerator<JakartaJsonParserGenerator> {
    private final InstantiatedVariable parserVariable;

    public JakartaJsonParserGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        parserVariable = prototype.parameters().get(0);
    }

    public JakartaJsonParserGenerator(
            String potentialVariableName,
            LHS lhs,
            @Nonnull JakartaJsonParserGenerator parent,
            AnyConfig config,
            boolean exhaust) {
        super(parent, potentialVariableName, lhs, config, exhaust);
        this.parserVariable = parent.parserVariable;
    }

    @Override
    protected Code stringCaseCondition() {
        return c("$C.currentEvent() == $L", parserVariable, token("VALUE_STRING"));
    }

    @Override
    protected Code numberCaseCondition() {
        return c("$C.currentEvent() == VALUE_NUMBER", parserVariable);
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
        return c("$C.currentEvent() == VALUE_TRUE || $C.currentEvent() == VALUE_FALSE", parserVariable, parserVariable);
    }

    @Override
    protected Code memberCaseCondition() {
        return c("$C.currentEvent() == $L", parserVariable, token("KEY_NAME"));
    }

    @Override
    protected void initializeParser() {
        beginControlFlow("if ($C.currentEvent() == null)", parserVariable);
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
        Code method =
                switch (type.getKind()) {
                    case BOOLEAN -> c("$C.currentEvent() == VALUE_TRUE", parserVariable);
                    case BYTE -> c("(byte) $C.getInt()", parserVariable);
                    case SHORT -> c("(short) $C.getInt()", parserVariable);
                    case INT -> c("$C.getInt()", parserVariable);
                    case LONG -> c("$C.getLong()", parserVariable);
                    case FLOAT -> c("(float) (($T) $C.getValue()).doubleValue()", jsonNumber(), parserVariable);
                    case DOUBLE -> c("(($T) $C.getValue()).doubleValue()", jsonNumber(), parserVariable);
                    default ->
                        throw new ContextedRuntimeException(type.getKind().toString());
                };
        if (lhs instanceof LHS.Return) {
            String tmp = createVariable(null, "tmp").name();
            addStatement(c("$T $L = $C", type, tmp, method));
            advance();
            addStatement("return $L", tmp);
        } else {
            addStatement(lhs.assign(method));
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
                    "$T $L = $C.getString()$L",
                    stringKind == StringKind.STRING ? String.class : char[].class,
                    tmp,
                    parserVariable,
                    conversion);
            advance();
            addStatement("return $L", tmp);
        } else {
            addStatement(lhs.assign("$C.getString()$L", parserVariable, conversion));
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
        importHelper();
        addStatement("skip($C)", parserVariable);
        advance();
    }

    @Override
    protected void afterObject() {}

    @Override
    protected void readMemberNameInIteration(String variableName) {
        addStatement("String $L = $C.getString()", variableName, parserVariable);
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
                "throw new $T($S + $C.currentEvent() + $S + $C.getLocation())",
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
    protected JakartaJsonParserGenerator nest(
            String potentialVariableName, LHS lhs, AnyConfig config, boolean exhaust) {
        return new JakartaJsonParserGenerator(potentialVariableName, lhs, this, config, exhaust);
    }

    private Class<JakartaJsonParserHelper> importHelper() {
        generatedClass.fileBuilderMods.add(builder -> builder.addStaticImport(JakartaJsonParserHelper.class, "*"));
        return JakartaJsonParserHelper.class;
    }

    private ClassName jsonNumber() {
        return ClassName.get("jakarta.json", "JsonNumber");
    }

    private String token(String t) {
        generatedClass.fileBuilderMods.add(
                builder -> builder.addStaticImport(ClassName.get("jakarta.json.stream.JsonParser", "Event"), "*"));
        return t;
    }

    private void advance() {
        addStatement("$C.next()", parserVariable);
    }
}
