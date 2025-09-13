package org.tillerino.jagger.processor.apis;

import jakarta.annotation.Nonnull;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class GsonJsonWriterWriterGenerator extends AbstractWriterGenerator<GsonJsonWriterWriterGenerator> {
    private final VariableElement writerVariable;

    public GsonJsonWriterWriterGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(utils, prototype, generatedClass);
        this.writerVariable = prototype.methodElement().getParameters().get(1);
    }

    public GsonJsonWriterWriterGenerator(
            TypeMirror type,
            @Nonnull GsonJsonWriterWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            Property property,
            boolean stackRelevantType,
            AnyConfig config) {
        super(parent, type, property, rhs, lhs, stackRelevantType, config);
        this.writerVariable = parent.writerVariable;
    }

    @Override
    protected void writeNull() {
        addFieldNameIfNeeded();
        addStatement("$L.nullValue()", writerVariable.getSimpleName());
    }

    @Override
    protected void writeString(StringKind stringKind) {
        addFieldNameIfNeeded();
        switch (stringKind) {
            case STRING -> addStatement(
                    "$L.value(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), rhs.args()));
            case CHAR_ARRAY -> addStatement(
                    "$L.value(new String(" + rhs.format() + "))", flatten(writerVariable.getSimpleName(), rhs.args()));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfNeeded();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(Snippet.of("$L.value($C)", writerVariable, base64Encode(rhs)));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        addFieldNameIfNeeded();
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.CHAR) {
            addStatement(
                    "$L.value(String.valueOf(" + rhs.format() + "))",
                    flatten(writerVariable.getSimpleName(), rhs.args()));
        } else if (kind == TypeKind.FLOAT || kind == TypeKind.DOUBLE) {
            beginControlFlow(
                    "if ($T.isFinite(" + rhs.format() + "))",
                    flatten(kind == TypeKind.FLOAT ? Float.class : Double.class, rhs.args()));
            addStatement("$L.value(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), rhs.args()));
            nextControlFlow("else");
            addStatement(
                    "$L.value(String.valueOf(" + rhs.format() + "))",
                    flatten(writerVariable.getSimpleName(), rhs.args()));
            endControlFlow();
        } else {
            addStatement("$L.value(" + rhs.format() + ")", flatten(writerVariable.getSimpleName(), rhs.args()));
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfNeeded();
        addStatement("$L.beginArray()", writerVariable.getSimpleName());
    }

    @Override
    protected void endArray() {
        addStatement("$L.endArray()", writerVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        addFieldNameIfNeeded();
        addStatement("$L.beginObject()", writerVariable.getSimpleName());
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
    protected GsonJsonWriterWriterGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new GsonJsonWriterWriterGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
    }

    private void addFieldNameIfNeeded() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.name(" + f.format() + ")", flatten(writerVariable.getSimpleName(), f.args()));
        }
    }
}
