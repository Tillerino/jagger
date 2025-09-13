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
            addStatement("$L.writeNullField(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
        } else {
            addStatement("$L.writeNull()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        if (lhs instanceof LHS.Field f) {
            if (stringKind == StringKind.STRING) {
                addStatement(
                        "$L.writeStringField(" + f.format() + ", " + rhs.format() + ")",
                        flatten(generatorVariable.getSimpleName(), f.args(), rhs.args()));
                return;
            } else {
                addStatement(
                        "$L.writeFieldName(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
            }
        }
        switch (stringKind) {
            case STRING -> addStatement(
                    "$L.writeString(" + rhs.format() + ")", flatten(generatorVariable.getSimpleName(), rhs.args()));
            case CHAR_ARRAY -> addStatement(
                    "$L.writeString(" + rhs.format() + ", 0, " + rhs.format() + ".length)",
                    flatten(generatorVariable.getSimpleName(), rhs.args(), rhs.args()));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfRequired();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(
                    "$L.writeBinary(" + rhs.format() + ")", flatten(generatorVariable.getSimpleName(), rhs.args()));
        }
    }

    private boolean addFieldNameIfRequired() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.writeFieldName(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
            return true;
        }
        return false;
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        if (lhs instanceof LHS.Field f) {
            if (typeMirror.getKind() == TypeKind.BOOLEAN) {
                addStatement(
                        "$L.writeBooleanField(" + f.format() + ", " + rhs.format() + ")",
                        flatten(generatorVariable.getSimpleName(), f.args(), rhs.args()));
            } else if (typeMirror.getKind() == TypeKind.CHAR) {
                addStatement(
                        "$L.writeStringField(" + f.format() + ", String.valueOf(" + rhs.format() + "))",
                        flatten(generatorVariable.getSimpleName(), f.args(), rhs.args()));
            } else {
                addStatement(
                        "$L.writeNumberField(" + f.format() + ", " + rhs.format() + ")",
                        flatten(generatorVariable.getSimpleName(), f.args(), rhs.args()));
            }
        } else {
            if (typeMirror.getKind() == TypeKind.BOOLEAN) {
                addStatement(
                        "$L.writeBoolean(" + rhs.format() + ")",
                        flatten(generatorVariable.getSimpleName(), rhs.args()));
            } else if (typeMirror.getKind() == TypeKind.CHAR) {
                addStatement(
                        "$L.writeString(String.valueOf(" + rhs.format() + "))",
                        flatten(generatorVariable.getSimpleName(), rhs.args()));
            } else {
                addStatement(
                        "$L.writeNumber(" + rhs.format() + ")", flatten(generatorVariable.getSimpleName(), rhs.args()));
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
