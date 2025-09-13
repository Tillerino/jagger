package org.tillerino.jagger.processor.apis;

import static org.tillerino.jagger.processor.Snippet.joinPrependingCommaToEach;
import static org.tillerino.jagger.processor.Snippet.of;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public class JaggerWriterGenerator extends AbstractWriterGenerator<JaggerWriterGenerator> {
    private final VariableElement generatorVariable;

    public JaggerWriterGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(utils, prototype, generatedClass);
        this.generatorVariable = prototype.methodElement().getParameters().get(1);
    }

    protected JaggerWriterGenerator(
            TypeMirror type,
            JaggerWriterGenerator parent,
            LHS lhs,
            RHS rhs,
            Property property,
            boolean stackRelevantType,
            AnyConfig config) {
        super(parent, type, property, rhs, lhs, stackRelevantType, config);
        this.generatorVariable = parent.generatorVariable;
    }

    @Override
    protected Features features() {
        return new Features(false);
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
        Snippet string = stringKind == StringKind.STRING ? rhs : charArrayToString(rhs);
        if (lhs instanceof LHS.Field f) {
            addStatement(of("$L.writeField($C, $C)", generatorVariable.getSimpleName(), f, string));
        } else {
            addStatement(of("$L.write($C)", generatorVariable, string));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        Snippet asString = base64Encode(rhs);
        if (lhs instanceof LHS.Field f) {
            if (binaryKind == BinaryKind.BYTE_ARRAY) {
                addStatement(of("$L.writeField($C, $C)", generatorVariable, f, asString));
                return;
            } else {
            }
        }
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(of("$L.write($C)", generatorVariable, asString));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        Snippet rhs_ = rhs;
        if (typeMirror.getKind() == TypeKind.CHAR) {
            rhs_ = Snippet.of("String.valueOf($C)", rhs);
        }
        if (lhs instanceof LHS.Field f) {
            addStatement(of("$L.writeField($C, $C)", generatorVariable, f, rhs_));
        } else {
            addStatement(of("$L.write($C)", generatorVariable, rhs_));
        }
    }

    @Override
    protected void startArray() {
        if (lhs instanceof LHS.Field f) {
            addStatement(
                    "$L.startArrayField(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
        } else {
            addStatement("$L.startArray()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void endArray() {
        addStatement("$L.endArray()", generatorVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        if (lhs instanceof LHS.Field f) {
            addStatement(
                    "$L.startObjectField(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
        } else {
            addStatement("$L.startObject()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void endObject() {
        addStatement("$L.endObject()", generatorVariable.getSimpleName());
    }

    @Override
    protected void invokeDelegate(String instance, InstantiatedMethod callee) {
        if (lhs instanceof LHS.Field f) {
            addStatement(of("$L.writeFieldName($C)", generatorVariable, f));
        }
        addStatement(of(
                "$L.$L($C$C)",
                instance,
                callee,
                rhs,
                joinPrependingCommaToEach(utils.delegation.findArguments(prototype, callee, 1, generatedClass))));
    }

    @Override
    protected JaggerWriterGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new JaggerWriterGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
    }
}
