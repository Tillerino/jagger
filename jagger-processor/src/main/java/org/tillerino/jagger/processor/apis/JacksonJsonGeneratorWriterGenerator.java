package org.tillerino.jagger.processor.apis;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class JacksonJsonGeneratorWriterGenerator extends AbstractWriterGenerator<JacksonJsonGeneratorWriterGenerator> {
    private final VariableElement generatorVariable;

    public JacksonJsonGeneratorWriterGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(utils, prototype, generatedClass);
        this.generatorVariable = prototype.methodElement().getParameters().get(1);
    }

    protected JacksonJsonGeneratorWriterGenerator(
            TypeMirror type,
            JacksonJsonGeneratorWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            Property property,
            boolean stackRelevantType,
            AnyConfig config) {
        super(parent, type, property, rhs, lhs, stackRelevantType, config);
        this.generatorVariable = parent.generatorVariable;
    }

    @Override
    protected void writeNull() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.writeNullField($C)", generatorVariable.getSimpleName(), f);
        } else {
            addStatement("$L.writeNull()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        if (lhs instanceof LHS.Field f) {
            if (stringKind == StringKind.STRING) {
                addStatement("$L.writeStringField($C, $C)", generatorVariable.getSimpleName(), f, rhs);
                return;
            } else {
                addStatement("$L.writeFieldName($C)", generatorVariable.getSimpleName(), f);
            }
        }
        switch (stringKind) {
            case STRING -> addStatement("$L.writeString($C)", generatorVariable.getSimpleName(), rhs);
            case CHAR_ARRAY -> addStatement(
                    "$L.writeString($C, 0, $C.length)", generatorVariable.getSimpleName(), rhs, rhs);
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfRequired();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement("$L.writeBinary($C)", generatorVariable.getSimpleName(), rhs);
        }
    }

    private boolean addFieldNameIfRequired() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.writeFieldName($C)", generatorVariable.getSimpleName(), f);
            return true;
        }
        return false;
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        if (lhs instanceof LHS.Field f) {
            if (typeMirror.getKind() == TypeKind.BOOLEAN) {
                addStatement("$L.writeBooleanField($C, $C)", generatorVariable.getSimpleName(), f, rhs);
            } else if (typeMirror.getKind() == TypeKind.CHAR) {
                addStatement("$L.writeStringField($C, String.valueOf($C))", generatorVariable.getSimpleName(), f, rhs);
            } else {
                addStatement("$L.writeNumberField($C, $C)", generatorVariable.getSimpleName(), f, rhs);
            }
        } else {
            if (typeMirror.getKind() == TypeKind.BOOLEAN) {
                addStatement("$L.writeBoolean($C)", generatorVariable.getSimpleName(), rhs);
            } else if (typeMirror.getKind() == TypeKind.CHAR) {
                addStatement("$L.writeString(String.valueOf($C))", generatorVariable.getSimpleName(), rhs);
            } else {
                addStatement("$L.writeNumber($C)", generatorVariable.getSimpleName(), rhs);
            }
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfRequired();
        addStatement("$L.writeStartArray()", generatorVariable.getSimpleName());
    }

    @Override
    protected void endArray() {
        addStatement("$L.writeEndArray()", generatorVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        addFieldNameIfRequired();
        addStatement("$L.writeStartObject()", generatorVariable.getSimpleName());
    }

    @Override
    protected void endObject() {
        addStatement("$L.writeEndObject()", generatorVariable.getSimpleName());
    }

    @Override
    protected void invokeDelegate(String instance, InstantiatedMethod callee) {
        addFieldNameIfRequired();
        addStatement(Snippet.of(
                "$L.$L($C$C)",
                instance,
                callee,
                rhs,
                Snippet.joinPrependingCommaToEach(
                        utils.delegation.findArguments(prototype, callee, 1, generatedClass))));
    }

    @Override
    protected JacksonJsonGeneratorWriterGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new JacksonJsonGeneratorWriterGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
    }
}
