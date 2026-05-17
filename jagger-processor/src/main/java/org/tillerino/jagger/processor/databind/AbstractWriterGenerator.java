package org.tillerino.jagger.processor.databind;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.databind.AbstractReaderGenerator.Branch;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Return;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.features.Enums;
import org.tillerino.jagger.processor.features.IgnoreProperties;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.features.Polymorphism;
import org.tillerino.jagger.processor.features.References.Setup;
import org.tillerino.jagger.processor.features.Verification.ProtoAndProps;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.Snippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet.StaticMethodInvocation;
import org.tillerino.jagger.processor.util.Snippet.TypedSnippet;

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

    protected AbstractWriterGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext, generatorContext.prototype().parameters().get(0).type());
        this.rhs = new RHS(
                // TODO does this clash with the flexible detection?
                prototype.method().parameters().get(0), true);
        this.lhs = new LHS.Return();
    }

    public CodeBlock.Builder build() {
        // delegate to any of the used blueprints
        Optional<Delegatee> delegate = ctx.delegation.findDelegatee(
                ((TemplatablePrototypeKind) prototype.kind()).withTypesPrefix(List.of(type)),
                prototype,
                !(lhs instanceof Return),
                stackDepth() > 1,
                config,
                generatedClass);
        if (delegate.isPresent()) {
            invokeDelegate(delegate.get());
            return code;
        }

        detectSelfReferencingType();
        if (type.getKind().isPrimitive()) {
            PrimitiveType pt = (PrimitiveType) type;
            if (List.of(TypeKind.FLOAT, TypeKind.DOUBLE).contains(pt.getKind())
                    && features().onlySupportsFiniteNumbers()) {
                TypeMirror boxedType = ctx.types.boxedClass(pt).asType();
                beginControlFlow("if ($T.isFinite($C))", boxedType, rhs);
                writePrimitive(type);
                nextControlFlow("else");
                RHS asString = new RHS(
                        new StaticMethodInvocation(ctx.commonTypes.string, boxedType, "toString", List.of(rhs)), false);
                nest(ctx.commonTypes.string, lhs, null, asString, false, config.propagateTo(PropagationKind.SUBSTITUTE))
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
            if (rhs.isVariable()) {
                beginControlFlow("if ($C != null)", rhs);
            } else {
                RHS nest = new RHS(createVariable(property.canonicalName()).withType(rhs.type()), true);
                addStatement("$T $C = $C", type, nest, rhs);
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
        Optional<Setup> referenceSetup = ctx.references.resolveSetup(config, prototype, type, contextParameter());
        if (referenceSetup.isPresent()) {
            Setup setup = referenceSetup.get();
            TypeMirror idType = setup.finalIdType(type, ctx);
            RHS idVar = new RHS(createVariable("id").withType(idType), false);
            addStatement("$T $C = $C", idType, idVar, setup.previouslyWritten(rhs));
            beginControlFlow("if ($C != null)", idVar);
            nest(idType, lhs, new Property("id", "id", null), idVar, true, config.propagateTo(PropagationKind.PROPERTY))
                    .build();
            nextControlFlow("else");
        }

        Optional<PerfectSnippet> converter = ctx.converters
                .findOutputConverter(rhs, prototype, config, generatedClass)
                .or(() -> ctx.converters.findJsonValueMethod(rhs));
        if (converter.isPresent()) {
            TypedSnippet converted = converter.get();
            RHS newValue = new RHS(createVariable("converted").withType(converted.type()), true);
            addStatement(Snippet.of("$T $C = $C", converted.type(), newValue, converted));
            nest(converted.type(), lhs, null, newValue, true, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
            return;
        }

        if (ctx.commonTypes.isBoxed(type)) {
            nest(ctx.types.unboxedType(type), lhs, null, rhs, false, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
        } else if (ctx.commonTypes.isString(type) || ctx.commonTypes.isArrayOf(type, TypeKind.CHAR)) {
            writeString(ctx.commonTypes.isString(type) ? StringKind.STRING : StringKind.CHAR_ARRAY);
        } else if (ctx.commonTypes.isEnum(type)) {
            writeEnum();
        } else if (ctx.commonTypes.isArrayOf(type, TypeKind.BYTE)) {
            writeBinary(BinaryKind.BYTE_ARRAY);
        } else if (ctx.commonTypes.isIterableOrArray(type)) {
            writeIterable();
        } else if (ctx.commonTypes.isErasureAssignableTo(type, Map.class)) {
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
                : ctx.generics
                        .recordTypeBindingsFor((DeclaredType) type, ctx.commonTypes.elem(Iterable.class))
                        .values()
                        .iterator()
                        .next();

        RHS elemVar = new RHS(createVariable("item").withType(componentType), true);
        SELF nested = nest(
                componentType,
                new LHS.Array(),
                Property.ITEM,
                elemVar,
                true,
                config.propagateTo(PropagationKind.PROPERTY));
        startArray();
        ScopedVar firstMarker = writeCommaMarkerIfNecessary();
        beginControlFlow("for ($T $C : $C)", nested.type, elemVar, rhs);
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
        TypeMirror[] typeBindings = ctx.generics
                .recordTypeBindingsFor((DeclaredType) type, ctx.commonTypes.elem(Map.class))
                .values()
                .toArray(TypeMirror[]::new);
        TypeMirror keyType = typeBindings[0];
        TypeMirror valueType = typeBindings[1];

        ScopedVar entry = createVariable("entry");

        RHS value = new RHS(PerfectSnippet.unsafe(entry).invokeMethod(valueType, "getValue"), true);
        LHS.Field key = new LHS.Field("$L.getKey()", new Object[] {entry.name()});
        SELF valueNested =
                nest(valueType, key, Property.VALUE, value, true, config.propagateTo(PropagationKind.PROPERTY));

        startObject();
        beginControlFlow("for ($T<$T, $T> $C : $C.entrySet())", Map.Entry.class, keyType, valueType, entry, rhs);
        Exceptions.runWithContext(valueNested::build, "value", valueType);
        endControlFlow();
        endObject();
    }

    protected void writeObjectAsMap() {
        if (!(type instanceof DeclaredType dt)) {
            throw new ContextedRuntimeException("I don't know what to do with this type: " + type);
        }
        TypeElement typeElement = (TypeElement) dt.asElement();
        Polymorphism.of(typeElement, ctx).ifPresentOrElse(this::writePolymorphicObject, () -> {
            startObject();
            code.add("\n");
            writeObjectPropertiesAsFields();
            endObject();
        });
    }

    private void writePolymorphicObject(Polymorphism polymorphism) {
        Branch branch = Branch.IF;
        for (Polymorphism.Child child : polymorphism.children()) {
            branch.controlFlow(this, "$C instanceof $T", rhs, child.type());
            branch = Branch.ELSE_IF;
            RHS casted = new RHS(createVariable(propertyName() + "Cast").withType(child.type()), false);
            addStatement("$T $C = ($T) $C", child.type(), casted, child.type(), rhs);

            AnyConfig childConfig = config.propagateTo(
                    PropagationKind.SUBSTITUTE /* this is fine with the configuration options that we
                 currently have */);
            TemplatablePrototypeKind target =
                    ((TemplatablePrototypeKind) prototype.kind()).withTypesPrefix(List.of(child.type()));
            ctx.delegation
                    .findDelegatee(target, prototype, false, true, config, generatedClass)
                    .ifPresentOrElse(
                            delegatee -> {
                                InstantiatedVariable callerContext = contextParameter()
                                        .orElseThrow(() -> new ContextedRuntimeException(
                                                "Prototype method must have a context parameter"));
                                if (!delegatee.method().hasParameterAssignableFrom(callerContext.type(), ctx)) {
                                    throw new ContextedRuntimeException(
                                            "Delegate method must have a context parameter");
                                }
                                addStatement(
                                        "$C.setPendingDiscriminator($S, $S)",
                                        callerContext,
                                        polymorphism.discriminator(),
                                        child.name());
                                Exceptions.runWithContext(
                                        () -> nest(child.type(), lhs, Property.INSTANCE, casted, true, childConfig)
                                                .invokeDelegate(delegatee),
                                        "instance",
                                        child.type());
                            },
                            () -> {
                                startObject();
                                code.add("\n");
                                nest(
                                                ctx.commonTypes.string,
                                                new LHS.Field("$S", new Object[] {polymorphism.discriminator()}),
                                                new Property("discriminator", polymorphism.discriminator(), null),
                                                new RHS(ctx.commonTypes.stringLiteral(child.name()), false),
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
            InstantiatedVariable context = contextParameter().orElseThrow();
            beginControlFlow("if ($C.isDiscriminatorPending())", context);
            nest(
                            ctx.commonTypes.string,
                            new LHS.Field("$L.pendingDiscriminatorProperty", new Object[] {context.name()}),
                            Property.DISCRIMINATOR,
                            new RHS(context.readField(ctx.commonTypes.string, "pendingDiscriminatorValue"), false),
                            false,
                            config.propagateTo(PropagationKind.PROPERTY))
                    .build();
            addStatement("$C.pendingDiscriminatorProperty = null", context);
            endControlFlow();
        }

        Optional<Setup> referencesSetup = ctx.references.resolveSetup(config, prototype, type, contextParameter());
        referencesSetup.ifPresent(setup -> setup.generateId(rhs)
                .ifPresent(id -> nest(
                                setup.idType(),
                                new LHS.Field("$S", new Object[] {setup.property()}),
                                new Property("id", "id", null),
                                new RHS(id, false),
                                true,
                                config.propagateTo(PropagationKind.PROPERTY))
                        .build()));

        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addWriter(prototype, type);
        Set<String> ignoredProperties =
                config.resolveProperty(IgnoreProperties.IGNORED_PROPERTIES).value();

        ctx.properties.outputProperties(type, this.config).forEach(property -> {
            if (IgnoreProperty.isIgnoredForJson(property.config())
                    || ignoredProperties.contains(property.externalName())) {
                return;
            }
            verificationForDto.addProperty(
                    property.externalName(), property.accessor().type(), property.config());

            LHS lhs = new LHS.Field("$S", new Object[] {property.externalName()});
            RHS accessorCall = new RHS(property.accessor().readSnippet(rhs), true);
            SELF nested = nest(
                    property.accessor().type(),
                    lhs,
                    new Property(property.canonicalName(), property.externalName(), property.config()),
                    accessorCall,
                    true,
                    property.config().propagateTo(PropagationKind.PROPERTY));
            Exceptions.runWithContext(nested::build, "property", property.canonicalName());
            referencesSetup.flatMap(s -> s.rememberId(rhs, accessorCall)).ifPresent(this::addStatement);
            code.add("\n");
        });
    }

    private void writeEnum() {
        RHS representation = new RHS(createVariable(propertyName()).withType(ctx.commonTypes.string), false);
        Snippet reprSnippet = Enums.serializationSnippet(ctx, generatedClass, type, rhs);
        addStatement("$T $C = $C", ctx.commonTypes.string, representation, reprSnippet);
        nest(ctx.commonTypes.string, lhs, null, representation, false, config.propagateTo(PropagationKind.SUBSTITUTE))
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

    protected abstract void invokeDelegate(Delegatee delegatee);

    protected abstract SELF nest(
            TypeMirror type, LHS lhs, Property property, RHS rhs, boolean stackRelevantType, AnyConfig config);

    Snippet base64Encode(Snippet snippet) {
        return Snippet.of("$T.getEncoder().encodeToString($C)", Base64.class, snippet);
    }

    Snippet charArrayToString(Snippet snippet) {
        return Snippet.of("new $T($C)", String.class, snippet);
    }

    protected sealed interface LHS {

        record Return() implements LHS {}

        record Array() implements LHS {}

        record Field(String format, Object[] args) implements LHS, Snippet {
            @Override
            public Flattened flatten() {
                return new Flattened(format, args);
            }
        }
    }

    record RHS(PerfectSnippet snippet, boolean nullable) implements PerfectSnippet {
        public Flattened flatten() {
            return snippet.flatten();
        }

        @Override
        public TypeMirror type() {
            return snippet.type();
        }

        @Override
        public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
            return new RHS(snippet.replaceVar(name, replacement), nullable);
        }

        @Override
        public boolean isVariable() {
            return snippet.isVariable();
        }
    }

    record Features(boolean onlySupportsFiniteNumbers) {}
}
