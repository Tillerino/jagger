package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Snippet.joinPrependingCommaToEach;
import static org.tillerino.jagger.processor.util.Snippet.of;

import jakarta.annotation.Nonnull;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.Snippet;

public class NanojsonWriterGenerator extends AbstractWriterGenerator<NanojsonWriterGenerator> {
    private final VariableElement generatorVariable;

    public NanojsonWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.generatorVariable = prototype.element().getParameters().get(1);
    }

    protected NanojsonWriterGenerator(
            TypeMirror type,
            @Nonnull NanojsonWriterGenerator parent,
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
            addStatement("$L.nul($C)", generatorVariable.getSimpleName(), f);
        } else {
            addStatement("$L.nul()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void writeString(StringKind stringKind) {
        Snippet string = stringKind == StringKind.STRING ? rhs : charArrayToString(rhs);
        if (lhs instanceof LHS.Field f) {
            addStatement(Snippet.of("$L.value($C, $C)", generatorVariable.getSimpleName(), f, string));
        } else {
            addStatement(Snippet.of("$L.value($C)", generatorVariable, string));
        }
    }

    @Override
    protected void writeBinary(BinaryKind binaryKind) {
        Snippet asString = base64Encode(rhs);
        if (lhs instanceof LHS.Field f) {
            if (binaryKind == BinaryKind.BYTE_ARRAY) {
                addStatement(Snippet.of("$L.value($C, $C)", generatorVariable, f, asString));
                return;
            } else {
            }
        }
        switch (binaryKind) {
            case BYTE_ARRAY -> addStatement(of("$L.value($C)", generatorVariable, asString));
        }
    }

    @Override
    public void writePrimitive(TypeMirror typeMirror) {
        Snippet rhs_ = rhs;
        if (typeMirror.getKind() == TypeKind.CHAR) {
            rhs_ = Snippet.of("String.valueOf($C)", rhs);
        }
        if (lhs instanceof LHS.Field f) {
            addStatement(of("$L.value($C, $C)", generatorVariable, f, rhs_));
        } else {
            addStatement(of("$L.value($C)", generatorVariable, rhs_));
        }
    }

    @Override
    protected void startArray() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.array($C)", generatorVariable.getSimpleName(), f);
        } else {
            addStatement("$L.array()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void endArray() {
        addStatement("$L.end()", generatorVariable.getSimpleName());
    }

    @Override
    protected void startObject() {
        if (lhs instanceof LHS.Field f) {
            addStatement("$L.object($C)", generatorVariable.getSimpleName(), f);
        } else {
            addStatement("$L.object()", generatorVariable.getSimpleName());
        }
    }

    @Override
    protected void endObject() {
        addStatement("$L.end()", generatorVariable.getSimpleName());
    }

    @Override
    protected void invokeDelegate(Snippet instance, InstantiatedMethod callee) {
        if (lhs instanceof LHS.Field f) {
            addStatement(of("$L.key($C)", generatorVariable, f));
        }
        addStatement(of(
                "$C.$L($C$C)",
                instance,
                callee,
                rhs,
                joinPrependingCommaToEach(ctx.delegation.findArguments(prototype, callee, 1, generatedClass))));
    }

    @Override
    protected NanojsonWriterGenerator nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config) {
        return new NanojsonWriterGenerator(type, this, lhs, rhs, property, stackRelevantType, config);
    }
}
