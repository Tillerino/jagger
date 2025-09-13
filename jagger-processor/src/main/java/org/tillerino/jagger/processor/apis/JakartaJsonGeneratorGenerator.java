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

public class JakartaJsonGeneratorGenerator extends AbstractWriterGenerator<JakartaJsonGeneratorGenerator> {
    private final VariableElement generatorVariable;

    public JakartaJsonGeneratorGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(utils, prototype, generatedClass);
        this.generatorVariable = prototype.methodElement().getParameters().get(1);
    }

    public JakartaJsonGeneratorGenerator(
            TypeMirror type,
            @Nonnull JakartaJsonGeneratorGenerator parent,
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
        return new Features(true);
    }

    @Override
    protected void writeNull() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.writeNull(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
        } else {
            addStatement("$L.writeNull()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        Snippet string = stringKind == StringKind.STRING ? rhs : charArrayToString(rhs);
        if (lhs instanceof LHS.Field f) {
            addStatement(Snippet.of("$L.write($C, $C)", generatorVariable, f, string));
        } else {
            addStatement(Snippet.of("$L.write($C)", generatorVariable, string));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        addFieldNameIfRequired();
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(Snippet.of("$L.write($C)", generatorVariable, base64Encode(rhs)));
        }
    }

    private boolean addFieldNameIfRequired() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.writeKey(" + f.format() + ")", flatten(generatorVariable.getSimpleName(), f.args()));
            return true;
        }
        return false;
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        Snippet value = typeMirror.getKind() == TypeKind.CHAR ? Snippet.of("String.valueOf($C)", rhs) : rhs;
        if (lhs instanceof LHS.Field f) {
            addStatement(Snippet.of("$L.write($C, $C)", generatorVariable, f, value));
        } else {
            addStatement(Snippet.of("$L.write($C)", generatorVariable, value));
        }
    }

    @Override
    protected void startArray() {
        addFieldNameIfRequired();
        addStatement("$L.writeStartArray()", generatorVariable.getSimpleName());
    }

    @Override
    protected void endArray() {
        addStatement("$L.writeEnd()", generatorVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        addFieldNameIfRequired();
        addStatement("$L.writeStartObject()", generatorVariable.getSimpleName());
    }

    @Override
    protected void endObject() {
        addStatement("$L.writeEnd()", generatorVariable.getSimpleName());
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
    protected JakartaJsonGeneratorGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new JakartaJsonGeneratorGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
    }
}
