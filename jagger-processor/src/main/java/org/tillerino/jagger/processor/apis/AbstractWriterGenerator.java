package org.tillerino.jagger.processor.apis;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.Snippet.TypedSnippet;
import org.tillerino.jagger.processor.apis.AbstractReaderGenerator.Branch;
import org.tillerino.jagger.processor.apis.AbstractWriterGenerator.RHS.AnySnippet;
import org.tillerino.jagger.processor.apis.AbstractWriterGenerator.RHS.Variable;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.features.IgnoreProperties;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.features.Polymorphism;
import org.tillerino.jagger.processor.features.PropertyName;
import org.tillerino.jagger.processor.features.References.Setup;
import org.tillerino.jagger.processor.features.Verification.ProtoAndProps;
import org.tillerino.jagger.processor.util.Accessor.ReadAccessor;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public abstract class AbstractWriterGenerator<SELF extends AbstractWriterGenerator<SELF>>
        extends AbstractCodeGeneratorStack<SELF> {
    protected final LHS lhs;

    protected final RHS rhs;

    protected AbstractWriterGenerator(
            SELF parent,
            TypeMirror type,
            Property property,
            RHS rhs,
            LHS lhs,
            boolean isStackRelevantType,
            AnyConfig config) {
        super(parent, type, isStackRelevantType, property, config);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    protected AbstractWriterGenerator(
            AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(
                utils,
                generatedClass,
                prototype,
                CodeBlock.builder(),
                null,
                prototype.instantiatedParameters().get(0).type(),
                true,
                null,
                null);
        this.rhs = new RHS.Variable(
                prototype.methodElement().getParameters().get(0).getSimpleName().toString(), true);
        this.lhs = new LHS.Return();
    }

    public CodeBlock.Builder build() {
        Optional<Delegatee> delegate = utils.delegation
                // delegate to any of the used blueprints
                .findDelegatee(type, prototype, !(lhs instanceof LHS.Return), stackDepth() > 1, config, generatedClass);
        if (delegate.isPresent()) {
            invokeDelegate(delegate.get().fieldOrParameter(), delegate.get().method());
            return code;
        }

        detectSelfReferencingType();
        if (type.getKind().isPrimitive()) {
            PrimitiveType pt = (PrimitiveType) type;
            if (List.of(TypeKind.FLOAT, TypeKind.DOUBLE).contains(pt.getKind())
                    && features().onlySupportsFiniteNumbers()) {
                TypeMirror boxedType = utils.types.boxedClass(pt).asType();
                beginControlFlow("if ($T.isFinite(" + rhs.format() + "))", flatten(boxedType, rhs.args()));
                writePrimitive(type);
                nextControlFlow("else");
                RHS asString = new RHS.AnySnippet(Snippet.of("$T.toString($C)", boxedType, rhs), false);
                nest(
                                utils.commonTypes.string,
                                lhs,
                                null,
                                asString,
                                false,
                                config.propagateTo(PropagationKind.SUBSTITUTE))
                        .build();
                endControlFlow();
            } else {
                writePrimitive(type);
            }
        } else {
            writeNullable();
        }
        return code;
    }

    /**
     * Writes non-primitive types. This is a good method to override if you want to add specializations for some types
     * that work with null values.
     */
    protected void writeNullable() {
        if (rhs.nullable()) {
            if (rhs instanceof RHS.Variable v) {
                beginControlFlow("if ($L != null)", v.name());
            } else {
                RHS.Variable nest = new RHS.Variable(
                        createVariable(property.canonicalName()).name(), true);
                addStatement("$T $L = " + rhs.format(), flatten(type, nest.name(), rhs.args()));
                nest(type, lhs, null, nest, false, config).writeNullable();
                return;
            }
        }

        writeNullCheckedObject();

        if (rhs.nullable()) {
            nextControlFlow("else");
            writeNull();
            endControlFlow();
        }
    }

    /**
     * Writes non-primitive types that are known to be non-null. This is a good method to override if you want to add
     * specializations for some types that require a dedicated null check.
     */
    protected void writeNullCheckedObject() {
        Optional<Setup> referenceSetup = utils.references.resolveSetup(config, prototype, type);
        if (referenceSetup.isPresent()) {
            Setup setup = referenceSetup.get();
            Variable idVar = new Variable(createVariable("id").name(), false);
            TypeMirror idType = setup.finalIdType(type, utils);
            addStatement("$T $C = $C", idType, idVar, setup.previouslyWritten(rhs));
            beginControlFlow("if ($C != null)", idVar);
            nest(idType, lhs, new Property("id", "id", null), idVar, true, config.propagateTo(PropagationKind.PROPERTY))
                    .build();
            nextControlFlow("else");
        }

        Optional<TypedSnippet> converter = utils.converters
                .findOutputConverter(TypedSnippet.of(type, rhs), prototype, config, generatedClass)
                .or(() -> utils.converters.findJsonValueMethod(TypedSnippet.of(type, rhs)));
        if (converter.isPresent()) {
            TypedSnippet converted = converter.get();
            RHS.Variable newValue = new RHS.Variable(createVariable("converted").name(), true);
            addStatement(Snippet.of("$T $L = $C", converted.type(), newValue.name, converted));
            nest(converted.type(), lhs, null, newValue, true, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
            return;
        }

        if (utils.isBoxed(type)) {
            nest(utils.types.unboxedType(type), lhs, null, rhs, false, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
        } else if (utils.commonTypes.isString(type) || utils.commonTypes.isArrayOf(type, TypeKind.CHAR)) {
            writeString(utils.commonTypes.isString(type) ? StringKind.STRING : StringKind.CHAR_ARRAY);
        } else if (utils.commonTypes.isEnum(type)) {
            writeEnum();
        } else if (utils.commonTypes.isArrayOf(type, TypeKind.BYTE)) {
            writeBinary(BinaryKind.BYTE_ARRAY);
        } else if (utils.commonTypes.isIterable(type)) {
            writeIterable();
        } else if (utils.commonTypes.isMap(type)) {
            writeMap();
        } else if (type.getKind() == TypeKind.TYPEVAR) {
            throw new ContextedRuntimeException("Missing serializer for type variable " + type);
        } else {
            writeObjectAsMap();
        }
        referenceSetup.ifPresent(__ -> endControlFlow());
    }

    protected void writeIterable() {
        TypeMirror componentType = type.getKind() == TypeKind.ARRAY
                ? ((ArrayType) type).getComponentType()
                : utils.generics
                        .recordTypeBindingsFor((DeclaredType) type, utils.commonTypes.elem(Iterable.class))
                        .values()
                        .iterator()
                        .next();

        RHS.Variable elemVar = new RHS.Variable(createVariable("item").name(), true);
        SELF nested = nest(
                componentType,
                new LHS.Array(),
                Property.ITEM,
                elemVar,
                true,
                config.propagateTo(PropagationKind.PROPERTY));
        startArray();
        ScopedVar firstMarker = writeCommaMarkerIfNecessary();
        beginControlFlow("for ($T $L : " + rhs.format() + ")", flatten(nested.type, elemVar.name(), rhs.args()));
        writeCommaIfNecessary(firstMarker);
        Exceptions.runWithContext(nested::build, "component", componentType);
        endControlFlow();
        endArray();
    }

    private ScopedVar writeCommaMarkerIfNecessary() {
        if (needsToWriteComma()) {
            ScopedVar variable = createVariable("first");
            addStatement("boolean $C = true", variable);
            return variable;
        }
        return null;
    }

    private void writeCommaIfNecessary(ScopedVar firstMarker) {
        if (firstMarker != null) {
            beginControlFlow("if (!$C)", firstMarker);
            writeComma();
            endControlFlow();
            addStatement("$C = false", firstMarker);
        }
    }

    private void writeMap() {
        TypeMirror[] typeBindings = utils.generics
                .recordTypeBindingsFor((DeclaredType) type, utils.commonTypes.elem(Map.class))
                .values()
                .toArray(TypeMirror[]::new);
        TypeMirror keyType = typeBindings[0];
        TypeMirror valueType = typeBindings[1];

        RHS.Variable entry = new RHS.Variable(createVariable("entry").name(), false);

        RHS.Accessor value = new RHS.Accessor(Snippet.of("$C.getValue()", entry), true);
        LHS.Field key = new LHS.Field("$L.getKey()", new Object[] {entry.name()});
        SELF valueNested =
                nest(valueType, key, Property.VALUE, value, true, config.propagateTo(PropagationKind.PROPERTY));

        startObject();
        beginControlFlow(
                "for ($T<$T, $T> $L : " + rhs.format() + ".entrySet())",
                flatten(Map.Entry.class, keyType, valueType, entry.name(), rhs.args()));
        Exceptions.runWithContext(valueNested::build, "value", valueType);
        endControlFlow();
        endObject();
    }

    protected void writeObjectAsMap() {
        if (!(type instanceof DeclaredType dt)) {
            throw new ContextedRuntimeException("Trying to write object which has no type element: " + type);
        }
        TypeElement typeElement = (TypeElement) dt.asElement();
        Polymorphism.of(typeElement, utils).ifPresentOrElse(this::writePolymorphicObject, () -> {
            startObject();
            code.add("\n");
            writeObjectPropertiesAsFields();
            endObject();
        });
    }

    private void writePolymorphicObject(Polymorphism polymorphism) {
        Branch branch = Branch.IF;
        for (Polymorphism.Child child : polymorphism.children()) {
            branch.controlFlow(this, rhs.format() + " instanceof $T", flatten(rhs.args(), child.type()));
            branch = Branch.ELSE_IF;
            RHS.Variable casted =
                    new RHS.Variable(createVariable(propertyName() + "Cast").name(), false);
            addStatement("$T $L = ($T) " + rhs.format(), flatten(child.type(), casted.name, child.type(), rhs.args()));

            AnyConfig childConfig = config.propagateTo(
                    PropagationKind.SUBSTITUTE /* this is fine with the configuration options that we
                 currently have */);
            utils.delegation
                    .findDelegatee(child.type(), prototype, false, true, config, generatedClass)
                    .ifPresentOrElse(
                            delegatee -> {
                                VariableElement callerContext = prototype
                                        .contextParameter()
                                        .orElseThrow(() -> new ContextedRuntimeException(
                                                "Prototype method must have a context parameter"));
                                if (!delegatee.method().hasParameterAssignableFrom(callerContext.asType(), utils)) {
                                    throw new ContextedRuntimeException(
                                            "Delegate method must have a context parameter");
                                }
                                if (!utils.types.isAssignable(callerContext.asType(), callerContext.asType())) {
                                    throw new ContextedRuntimeException("Context types must be compatible");
                                }
                                addStatement(
                                        "$L.setPendingDiscriminator($S, $S)",
                                        callerContext,
                                        polymorphism.discriminator(),
                                        child.name());
                                Exceptions.runWithContext(
                                        () -> nest(child.type(), lhs, Property.INSTANCE, casted, true, childConfig)
                                                .invokeDelegate(delegatee.fieldOrParameter(), delegatee.method()),
                                        "instance",
                                        child.type());
                            },
                            () -> {
                                startObject();
                                code.add("\n");
                                nest(
                                                utils.commonTypes.string,
                                                new LHS.Field("$S", new Object[] {polymorphism.discriminator()}),
                                                new Property("discriminator", polymorphism.discriminator(), null),
                                                new RHS.StringLiteral(child.name()),
                                                false,
                                                config.propagateTo(PropagationKind.PROPERTY))
                                        .build();
                                Exceptions.runWithContext(
                                        () -> nest(child.type(), lhs, Property.INSTANCE, casted, true, childConfig)
                                                .writeObjectPropertiesAsFields(),
                                        "instance",
                                        child.type());
                                endObject();
                            });
        }
        if (branch == Branch.IF) {
            throw new ContextedRuntimeException("Polymorphism must have at least one child type");
        }
        nextControlFlow("else");
        addStatement("throw new $T($S)", IllegalArgumentException.class, "Unknown type");
        endControlFlow();
    }

    void writeObjectPropertiesAsFields() {
        if (canBePolyChild) {
            VariableElement context = prototype.contextParameter().orElseThrow();
            beginControlFlow("if ($L.isDiscriminatorPending())", context);
            nest(
                            utils.commonTypes.string,
                            new LHS.Field("$L.pendingDiscriminatorProperty", new Object[] {context.getSimpleName()}),
                            Property.DISCRIMINATOR,
                            new RHS.Accessor(
                                    Snippet.of(
                                            "$L.pendingDiscriminatorValue",
                                            context.getSimpleName().toString()),
                                    false),
                            false,
                            config.propagateTo(PropagationKind.PROPERTY))
                    .build();
            addStatement("$L.pendingDiscriminatorProperty = null", context);
            endControlFlow();
        }

        Optional<Setup> referencesSetup = utils.references.resolveSetup(config, prototype, type);
        referencesSetup.ifPresent(setup -> setup.generateId(rhs).ifPresent(id -> nest(
                        setup.idType(),
                        new LHS.Field("$S", new Object[] {setup.property()}),
                        new Property("id", "id", null),
                        new AnySnippet(id, false),
                        true,
                        config.propagateTo(PropagationKind.PROPERTY))
                .build()));

        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addWriter(prototype, type);
        Set<String> ignoredProperties =
                config.resolveProperty(IgnoreProperties.IGNORED_PROPERTIES).value();

        outputProperties(type).forEach(property -> {
            if (property.config()
                            .resolveProperty(IgnoreProperty.IGNORE_PROPERTY)
                            .value()
                    || ignoredProperties.contains(property.externalName())) {
                return;
            }
            verificationForDto.addProperty(
                    property.externalName(), property.accessor().type(), property.config());

            LHS lhs = new LHS.Field("$S", new Object[] {property.externalName()});
            RHS.Accessor accessorCall = new RHS.Accessor(property.accessor().readSnippet(rhs), true);
            SELF nested = nest(
                    property.accessor().type(),
                    lhs,
                    new Property(property.canonicalName(), property.externalName(), property.config),
                    accessorCall,
                    true,
                    property.config().propagateTo(PropagationKind.PROPERTY));
            Exceptions.runWithContext(nested::build, "property", property.canonicalName());
            referencesSetup.flatMap(s -> s.rememberId(rhs, accessorCall)).ifPresent(this::addStatement);
            code.add("\n");
        });
    }

    private void writeEnum() {
        RHS.Variable enumValue =
                new RHS.Variable(createVariable(propertyName() + "String").name(), false);
        addStatement(
                "$T $L = " + rhs.format() + ".name()", flatten(utils.commonTypes.string, enumValue.name(), rhs.args()));
        nest(utils.commonTypes.string, lhs, null, enumValue, false, config.propagateTo(PropagationKind.SUBSTITUTE))
                .build();
    }

    protected Features features() {
        return new Features(false);
    }

    protected abstract void writeNull();

    protected abstract void writeString(StringKind stringKind);

    protected abstract void writeBinary(BinaryKind binaryKind);

    /** @param typeMirror if non-null, the type is a primitive wrapper and this is the primitive type */
    public abstract void writePrimitive(TypeMirror typeMirror);

    protected abstract void startArray();

    protected abstract void endArray();

    protected abstract void startObject();

    protected abstract void endObject();

    boolean needsToWriteComma() {
        return false;
    }

    protected void writeComma() {}

    protected abstract void invokeDelegate(String instance, InstantiatedMethod callee);

    protected abstract SELF nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config);

    Snippet base64Encode(Snippet snippet) {
        return Snippet.of("$T.getEncoder().encodeToString($C)", Base64.class, snippet);
    }

    Snippet charArrayToString(Snippet snippet) {
        return Snippet.of("new $T($C)", String.class, snippet);
    }

    protected List<OutputProperty> outputProperties(TypeMirror type) {
        return utils.properties.listReadAccessors(type).entrySet().stream()
                .map(entry -> {
                    String canonicalName = entry.getKey();
                    ReadAccessor accessor = entry.getValue();
                    AnyConfig propertyConfig = AnyConfig.fromAccessorConsideringField(
                                    accessor, accessor.name(), type, canonicalName, utils)
                            .merge(config);
                    String externalName = PropertyName.resolvePropertyName(propertyConfig, canonicalName);
                    return new OutputProperty(canonicalName, externalName, accessor, propertyConfig);
                })
                .toList();
    }

    protected record OutputProperty(
            String canonicalName, String externalName, ReadAccessor accessor, AnyConfig config) {}

    protected sealed interface LHS {

        record Return() implements LHS {}

        record Array() implements LHS {}

        record Field(String format, Object[] args) implements LHS, Snippet {}
    }

    sealed interface RHS extends Snippet {
        default String format() {
            if (this instanceof Variable) {
                return "$L";
            } else if (this instanceof Accessor a) {
                return a.accessor.format();
            } else if (this instanceof StringLiteral) {
                return "$S";
            } else if (this instanceof AnySnippet a) {
                return a.snippet().format();
            } else {
                throw new ContextedRuntimeException();
            }
        }

        default Object[] args() {
            if (this instanceof Variable v) {
                return new Object[] {v.name()};
            } else if (this instanceof Accessor a) {
                return a.accessor.args();
            } else if (this instanceof StringLiteral s) {
                return new Object[] {s.value()};
            } else if (this instanceof AnySnippet a) {
                return a.snippet().args();
            } else {
                throw new ContextedRuntimeException();
            }
        }

        boolean nullable();

        record Variable(String name, boolean nullable) implements RHS {}

        record Accessor(Snippet accessor, boolean nullable) implements RHS {}

        record StringLiteral(String value) implements RHS {
            @Override
            public boolean nullable() {
                return false;
            }
        }

        record AnySnippet(Snippet snippet, boolean nullable) implements RHS {}
    }

    record Features(boolean onlySupportsFiniteNumbers) {}
}
