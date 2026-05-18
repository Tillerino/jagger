package org.tillerino.jagger.processor.databind;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AbstractCodeGenerator;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.databind.DatabindPrototypeDetector.DatabindPrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Polymorphism;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public abstract class AbstractCodeGeneratorStack<SELF extends AbstractCodeGeneratorStack<SELF>>
        extends AbstractCodeGenerator<SELF> {
    protected final TypeMirror type;

    @Nullable
    protected final SELF parent;

    protected final boolean stackRelevantType;

    /** potential variable name */
    protected final String pvn;

    protected final boolean canBePolyChild;

    protected final AnyConfig config;

    protected final DatabindPrototypeKind kind;

    // for creating the root generator
    protected AbstractCodeGeneratorStack(CodeGeneratorContext generatorContext, TypeMirror type) {
        super(generatorContext); // add method parameters to variables scope
        this.type = type;

        this.parent = null;
        this.stackRelevantType = true;
        this.pvn = "root";
        this.canBePolyChild =
                contextParameter().isPresent() && stackDepth() == 1 && Polymorphism.isSomeChild(type, ctx);
        this.config = type instanceof DeclaredType dt && dt.asElement() != null
                ? AnyConfig.create(dt.asElement(), ConfigProperty.LocationKind.DTO, ctx)
                        .merge(prototype.config())
                : prototype.config();
        this.kind = (DatabindPrototypeKind) prototype.kind();
    }

    // for nesting generators
    protected AbstractCodeGeneratorStack(@Nonnull SELF parent, TypeMirror type, String pvn, AnyConfig config) {
        super(parent);
        this.type = type;

        this.parent = parent;
        this.stackRelevantType = !parent.ctx.types.isSameType(parent.type, type);
        this.pvn = Objects.requireNonNull(pvn);
        this.canBePolyChild =
                contextParameter().isPresent() && stackDepth() == 1 && Polymorphism.isSomeChild(type, ctx);
        this.config = config;
        this.kind = parent.kind;
    }

    protected void detectSelfReferencingType() {
        if (stackRelevantType && parent != null && parent.stackContainsType(type)) {
            throw new ContextedRuntimeException(
                            "Self-referencing type detected. Define a separate method for this type.")
                    .addContextValue("type", type);
        }
    }

    boolean stackContainsType(TypeMirror type) {
        if ((stackRelevantType || parent == null) && ctx.types.isSameType(this.type, type)) {
            return true;
        }
        if (parent != null) {
            return parent.stackContainsType(type);
        }
        return false;
    }

    int stackDepth() {
        return parent != null ? 1 + parent.stackDepth() : 1;
    }

    protected Optional<InstantiatedVariable> contextParameter() {
        for (InstantiatedVariable parameter : prototype.parameters()) {
            TypeMirror targetContextType = ((DatabindPrototypeKind) prototype.kind()).contextType();
            if (ctx.commonTypes.isAssignable(parameter.type(), targetContextType)) {
                return Optional.of(parameter);
            }
        }
        return Optional.empty();
    }

    void beginControlFlow(Branch branch, String s, Object... args) {
        switch (branch) {
            case IF -> beginControlFlow("if (" + s + ")", args);
            case ELSE_IF -> nextControlFlow("else if (" + s + ")", args);
        }
        ;
    }

    void beginControlFlow(Branch branch, Code condition) {
        switch (branch) {
            case IF -> beginControlFlow("if ($C)", condition);
            case ELSE_IF -> nextControlFlow("else if ($C)", condition);
        }
        ;
    }

    NullaryControlFlowScope lastBranch(Branch branch) {
        return switch (branch) {
            case IF -> new NullaryControlFlowScope(() -> {});
            case ELSE_IF -> nextControlFlow("else");
        };
    }

    protected enum StringKind {
        STRING,
        CHAR_ARRAY
    }

    enum BinaryKind {
        BYTE_ARRAY
    }

    protected enum Branch {
        IF,
        ELSE_IF,
        ;
    }

    public class NestedProperty {
        public final String canonicalName;
        public final String serializedName;
        public final AnyConfig config;
        public final SELF generator;

        NestedProperty(String canonicalName, String serializedName, AnyConfig config, SELF generator) {
            this.canonicalName = canonicalName;
            this.serializedName = serializedName;
            this.config = config;
            this.generator = generator;
        }
    }
}
