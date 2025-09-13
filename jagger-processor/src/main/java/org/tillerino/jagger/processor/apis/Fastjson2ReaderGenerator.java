package org.tillerino.jagger.processor.apis;

import static org.tillerino.jagger.processor.Snippet.join;
import static org.tillerino.jagger.processor.Snippet.of;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.helpers.Fastjson2ReaderHelper;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class Fastjson2ReaderGenerator extends AbstractReaderGenerator<Fastjson2ReaderGenerator> {

    private final VariableElement parserVariable;

    public Fastjson2ReaderGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(utils, prototype, generatedClass);
        parserVariable = prototype.methodElement().getParameters().get(0);
    }

    public Fastjson2ReaderGenerator(
            TypeMirror type,
            Property property,
            LHS lhs,
            @Nonnull Fastjson2ReaderGenerator parent,
            boolean stackRelevantType,
            AnyConfig config) {
        super(parent, type, stackRelevantType, property, lhs, config);
        this.parserVariable = parent.parserVariable;
    }

    @Override
    protected void readNullable(Branch branch, boolean nullable, boolean lastCase) {
        if (type instanceof ArrayType at) {
            TypeMirror componentType = at.getComponentType();
            if (utils.commonTypes.isString(componentType)) {
                addStatement(lhs.assign("$L.readStringArray()", parserVariable.getSimpleName()));
                return;
            }
            if (utils.commonTypes.isArrayOf(type, TypeKind.INT)) {
                addStatement(lhs.assign("$L.readInt32ValueArray()", parserVariable.getSimpleName()));
                return;
            }
            if (utils.commonTypes.isArrayOf(type, TypeKind.LONG)) {
                addStatement(lhs.assign("$L.readInt64ValueArray()", parserVariable.getSimpleName()));
                return;
            }
        }
        super.readNullable(branch, nullable, lastCase);
    }

    @Override
    protected Snippet stringCaseCondition() {
        return Snippet.of("$L.isString()", parserVariable.getSimpleName());
    }

    @Override
    protected Snippet numberCaseCondition() {
        return Snippet.of("$L.isNumber()", parserVariable.getSimpleName());
    }

    @Override
    protected Snippet objectCaseCondition() {
        return Snippet.of("$L.nextIfObjectStart()", parserVariable.getSimpleName());
    }

    @Override
    protected Snippet arrayCaseCondition() {
        return Snippet.of("$L.nextIfArrayStart()", parserVariable.getSimpleName());
    }

    @Override
    protected Snippet booleanCaseCondition() {
        return Snippet.of(
                "$L.current() == 'f' || $L.current() == 't'",
                parserVariable.getSimpleName(),
                parserVariable.getSimpleName());
    }

    @Override
    protected Snippet fieldCaseCondition() {
        return Snippet.of("$L.isString()", parserVariable.getSimpleName());
    }

    @Override
    protected void initializeParser() {}

    @Override
    protected Snippet nullCaseCondition() {
        return Snippet.of("$L.nextIfNull()", parserVariable.getSimpleName());
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
                    default -> throw new ContextedRuntimeException(
                            type.getKind().toString());
                };
        addStatement(lhs.assign("$L.$L()", parserVariable.getSimpleName(), readMethod));
    }

    @Override
    protected void readString(StringKind stringKind) {
        String conversion =
                switch (stringKind) {
                    case STRING -> "";
                    case CHAR_ARRAY -> ".toCharArray()";
                };
        addStatement(lhs.assign("$L.readString()$L", parserVariable.getSimpleName(), conversion));
    }

    @Override
    protected void iterateOverFields() {
        beginControlFlow("while (!$L.nextIfObjectEnd())", parserVariable.getSimpleName());
    }

    @Override
    protected void skipValue() {
        addStatement("$L.skipValue()", parserVariable.getSimpleName());
    }

    @Override
    protected void afterObject() {}

    @Override
    protected void readFieldNameInIteration(String propertyName) {
        addStatement("String $L = $L.readFieldName()", propertyName, parserVariable.getSimpleName());
    }

    @Override
    protected void readDiscriminator(String propertyName) {
        addStatement(lhs.assign(
                "$T.readDiscriminator($S, $L)",
                Fastjson2ReaderHelper.class,
                propertyName,
                parserVariable.getSimpleName()));
    }

    @Override
    protected void iterateOverElements() {
        beginControlFlow("while (!$L.nextIfArrayEnd())", parserVariable.getSimpleName());
    }

    @Override
    protected void afterArray() {}

    @Override
    protected void throwUnexpected(String expected) {
        addStatement(
                "throw new $T($S + $L.current())",
                IOException.class,
                "Expected " + expected + ", got ",
                parserVariable.getSimpleName());
    }

    @Override
    protected void invokeDelegate(String instance, InstantiatedMethod callee) {
        addStatement(lhs.assign(of(
                "$L.$L($C)",
                instance,
                callee,
                join(utils.delegation.findArguments(prototype, callee, 0, generatedClass), ", "))));
    }

    @Override
    protected Fastjson2ReaderGenerator nest(
            TypeMirror type, @Nullable Property property, LHS lhs, boolean stackRelevantType, AnyConfig config) {
        return new Fastjson2ReaderGenerator(type, property, lhs, this, stackRelevantType, config);
    }
}
