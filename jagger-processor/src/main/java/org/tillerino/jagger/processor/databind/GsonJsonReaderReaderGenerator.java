package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.helpers.GsonJsonReaderHelper;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class GsonJsonReaderReaderGenerator extends AbstractReaderGenerator<GsonJsonReaderReaderGenerator> {
    private final InstantiatedVariable parserVariable;

    public GsonJsonReaderReaderGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        parserVariable = prototype.method().parameterOfType(kind.types().get(1));
    }

    public GsonJsonReaderReaderGenerator(
            String potentialVariableName,
            LHS lhs,
            @Nonnull GsonJsonReaderReaderGenerator parent,
            AnyConfig config,
            boolean exhaust) {
        super(parent, potentialVariableName, lhs, config, exhaust);
        this.parserVariable = parent.parserVariable;
    }

    @Override
    protected Code stringCaseCondition() {
        return c("$C.peek() == $T.STRING", parserVariable, jsonToken());
    }

    @Override
    protected Code numberCaseCondition() {
        return c("$C.peek() == $T.NUMBER", parserVariable, jsonToken());
    }

    @Override
    protected Code objectCaseCondition() {
        return c("$T.isBeginObject($C, true)", GsonJsonReaderHelper.class, parserVariable);
    }

    @Override
    protected Code arrayCaseCondition() {
        return c("$T.isBeginArray($C, true)", GsonJsonReaderHelper.class, parserVariable);
    }

    @Override
    protected Code booleanCaseCondition() {
        return c("$C.peek() == $T.BOOLEAN", parserVariable, jsonToken());
    }

    @Override
    protected Code memberCaseCondition() {
        return c("$C.peek() == $T.NAME", parserVariable, jsonToken());
    }

    @Override
    protected void initializeParser() {}

    @Override
    protected Code nullCaseCondition() {
        return c("$T.isNull($C, true)", GsonJsonReaderHelper.class, parserVariable);
    }

    private TypeElement jsonToken() {
        return ctx.elements.getTypeElement("com.google.gson.stream.JsonToken");
    }

    @Override
    protected void readPrimitive(TypeMirror type) {
        record R(String cast, String method) {}
        R readMethod =
                switch (type.getKind()) {
                    case BOOLEAN -> new R("", "nextBoolean");
                    case BYTE -> new R("(byte) ", "nextInt");
                    case SHORT -> new R("(short) ", "nextInt");
                    case INT -> new R("", "nextInt");
                    case LONG -> new R("", "nextLong");
                    case FLOAT -> new R("(float) ", "nextDouble");
                    case DOUBLE -> new R("", "nextDouble");
                    default ->
                        throw new ContextedRuntimeException(type.getKind().toString());
                };
        addStatement(lhs.assign("$L$C.$L()", readMethod.cast, parserVariable, readMethod.method));
    }

    @Override
    protected void readString(StringKind stringKind) {
        String conversion =
                switch (stringKind) {
                    case STRING -> "";
                    case CHAR_ARRAY -> ".toCharArray()";
                };
        addStatement(lhs.assign("$C.nextString()$L", parserVariable, conversion));
    }

    @Override
    protected void iterateOverFields() {
        beginControlFlow("while ($C.peek() != $T.END_OBJECT)", parserVariable, jsonToken());
    }

    @Override
    protected void skipValue() {
        addStatement("$C.skipValue()", parserVariable);
    }

    @Override
    protected void afterObject() {
        addStatement("$C.endObject()", parserVariable);
    }

    @Override
    protected void readMemberNameInIteration(String variableName) {
        addStatement("String $L = $C.nextName()", variableName, parserVariable);
    }

    @Override
    protected void readDiscriminator(String propertyName) {
        addStatement(
                lhs.assign("$T.readDiscriminator($S, $C)", GsonJsonReaderHelper.class, propertyName, parserVariable));
    }

    @Override
    protected void iterateOverElements() {
        beginControlFlow("while ($C.peek() != $T.END_ARRAY)", parserVariable, jsonToken());
    }

    @Override
    protected void afterArray() {
        addStatement("$C.endArray()", parserVariable);
    }

    @Override
    protected void throwUnexpected(String expected) {
        addStatement(
                "throw new $T($S + $C.peek() + $S + $C.getPath())",
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
    protected GsonJsonReaderReaderGenerator nest(
            String potentialVariableName, LHS lhs, AnyConfig config, boolean exhaust) {
        return new GsonJsonReaderReaderGenerator(potentialVariableName, lhs, this, config, exhaust);
    }
}
