package org.tillerino.jagger.processor.apis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.features.Polymorphism;

public abstract class AbstractCodeGeneratorStack<SELF extends AbstractCodeGeneratorStack<SELF>>
        extends AbstractCodeGenerator<SELF> {
    protected final AnnotationProcessorUtils utils;
    protected final GeneratedClass generatedClass;
    protected final JaggerPrototype prototype;
    protected final TypeMirror type;

    @Nullable
    protected final SELF parent;

    protected final boolean stackRelevantType;

    @Nullable
    protected final Property property;

    protected final boolean canBePolyChild;

    protected final AnyConfig config;

    // for creating the root generator
    protected AbstractCodeGeneratorStack(
            AnnotationProcessorUtils utils, GeneratedClass generatedClass, JaggerPrototype prototype, TypeMirror type) {
        super(prototype.asInstantiatedMethod()); // add method parameters to variables scope
        this.utils = utils;
        this.generatedClass = Objects.requireNonNull(generatedClass);
        this.prototype = prototype;
        this.type = type;

        this.parent = null;
        this.stackRelevantType = true;
        this.property = null;
        this.canBePolyChild =
                prototype.contextParameter().isPresent() && stackDepth() == 1 && Polymorphism.isSomeChild(type, utils);
        this.config = type instanceof DeclaredType dt && dt.asElement() != null
                ? AnyConfig.create(dt.asElement(), ConfigProperty.LocationKind.DTO, utils)
                        .merge(prototype.config())
                : prototype.config();
    }

    // for nesting generators
    protected AbstractCodeGeneratorStack(
            @Nonnull SELF parent,
            TypeMirror type,
            boolean stackRelevantType,
            @Nullable Property property,
            AnyConfig config) {
        super(parent);
        this.utils = parent.utils;
        this.generatedClass = Objects.requireNonNull(parent.generatedClass);
        this.prototype = parent.prototype;
        this.type = type;

        this.parent = parent;
        this.stackRelevantType = stackRelevantType;
        this.property = property;
        this.canBePolyChild =
                prototype.contextParameter().isPresent() && stackDepth() == 1 && Polymorphism.isSomeChild(type, utils);
        this.config = config;
    }

    protected void detectSelfReferencingType() {
        if (stackRelevantType && parent != null && parent.stackContainsType(type)) {
            throw new ContextedRuntimeException(
                            "Self-referencing type detected. Define a separate method for this type.")
                    .addContextValue("type", type);
        }
    }

    boolean stackContainsType(TypeMirror type) {
        if ((stackRelevantType || parent == null) && utils.types.isSameType(this.type, type)) {
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

    protected String propertyName() {
        return property != null ? property.serializedName : parent != null ? parent.propertyName() : "root";
    }

    protected enum StringKind {
        STRING,
        CHAR_ARRAY
    }

    enum BinaryKind {
        BYTE_ARRAY
    }

    protected record Property(String canonicalName, String serializedName, @Nullable AnyConfig config) {
        static Property ITEM = new Property("item", "item", null);
        static Property VALUE = new Property("value", "value", null);
        static Property DISCRIMINATOR = new Property("discriminator", "discriminator", null);
        static Property INSTANCE = new Property("instance", "instance", null);
    }
}
