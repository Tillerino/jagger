package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;
import static org.tillerino.jagger.processor.util.Expr.e;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import java.util.function.Function;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import lombok.Getter;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.InstantiatedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Member;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Return;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.Binary;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.Boxed;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.Converted;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.Delegated;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.EnumValue;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.IterableValue;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.MapValue;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.ObjectAsMap;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.Primitive;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.WrittenType.Text;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.features.Converters.OutputConverter;
import org.tillerino.jagger.processor.features.Delegation;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.features.Enums;
import org.tillerino.jagger.processor.features.IgnoreProperties;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.features.Polymorphism;
import org.tillerino.jagger.processor.features.Properties.OutputProperty;
import org.tillerino.jagger.processor.features.References;
import org.tillerino.jagger.processor.features.References.Setup;
import org.tillerino.jagger.processor.features.Verification.ProtoAndProps;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.Expr.ExprWrapper;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public abstract class AbstractWriterGenerator<SELF extends AbstractWriterGenerator<SELF>>
        extends AbstractCodeGeneratorStack<SELF> {
    protected final LHS lhs;

    protected final RHS rhs;

    protected final WrittenType wt;

    protected AbstractWriterGenerator(
            SELF parent, TypeMirror type, String potentialVariableName, RHS rhs, LHS lhs, AnyConfig config) {
        super(parent, type, potentialVariableName, config);
        this.lhs = lhs;
        this.rhs = rhs;
        wt = detect();
    }

    protected AbstractWriterGenerator(CodeGeneratorContext generatorContext) {
        super(
                generatorContext,
                ((TemplatablePrototypeKind) generatorContext.prototype().kind())
                        .types()
                        .get(0));
        this.rhs = new RHS(prototype.method().parameterOfType(type), true);
        this.lhs = new LHS.Return();
        wt = detect();
    }

    private WrittenType detect() {
        // delegate to any of the used blueprints
        Optional<Delegatee> delegate = ctx.delegation.findDelegatee(
                kind.withTypesPrefix(List.of(type)),
                prototype,
                !(lhs instanceof LHS.Return),
                stackDepth() > 1,
                config,
                generatedClass);
        if (delegate.isPresent()) {
            return new Delegated(delegate.get());
        }
        detectSelfReferencingType();
        if (type.getKind().isPrimitive()) {
            return new Primitive((PrimitiveType) type);
        }
        Optional<OutputConverter> converter =
                ctx.converters.findOutputConverter(type, prototype, config, generatedClass);
        if (converter.isPresent()) {
            if (ctx.references.isConfigured(type)) {
                throw References.converterConflict(type);
            }
            OutputConverter c = converter.get();
            return new Converted(c.converter(), c.convertedType());
        }
        if (ctx.commonTypes.isBoxed(type)) {
            return new Boxed(ctx.types.unboxedType(type));
        }
        if (ctx.commonTypes.isString(type) || ctx.commonTypes.isArrayOf(type, TypeKind.CHAR)) {
            return new Text(ctx.commonTypes.isString(type) ? StringKind.STRING : StringKind.CHAR_ARRAY);
        }
        if (ctx.commonTypes.isEnum(type)) {
            return new EnumValue();
        }
        if (ctx.commonTypes.isArrayOf(type, TypeKind.BYTE)) {
            return new Binary(BinaryKind.BYTE_ARRAY);
        }
        if (ctx.commonTypes.isIterableOrArray(type)) {
            TypeMirror componentType = type.getKind() == TypeKind.ARRAY
                    ? ((ArrayType) type).getComponentType()
                    : ctx.generics
                            .recordTypeBindingsFor((DeclaredType) type, ctx.commonTypes.elem(Iterable.class))
                            .values()
                            .iterator()
                            .next();
            return new IterableValue(componentType);
        }
        if (ctx.commonTypes.isErasureAssignableTo(type, Map.class)) {
            TypeMirror[] typeBindings = ctx.generics
                    .recordTypeBindingsFor((DeclaredType) type, ctx.commonTypes.elem(Map.class))
                    .values()
                    .toArray(TypeMirror[]::new);
            return new MapValue(typeBindings[0], typeBindings[1]);
        }
        if (type.getKind() == TypeKind.TYPEVAR) {
            throw new ContextedRuntimeException("Missing serializer for type variable " + type);
        }
        if (!(type instanceof DeclaredType declaredType)) {
            throw new ContextedRuntimeException("I don't know what to do with this type: " + type);
        }
        return new ObjectAsMap(declaredType);
    }

    public CodeBlock.Builder build() {
        if (wt instanceof Delegated d) {
            callDelegate(d.delegatee());
        } else if (wt instanceof Primitive p) {
            writePotentiallyNonFinitePrimitive(p);
        } else {
            writeNullable(wt);
        }
        return code;
    }

    private void writePotentiallyNonFinitePrimitive(Primitive p) {
        if (List.of(TypeKind.FLOAT, TypeKind.DOUBLE).contains(p.pt.getKind())
                && features().onlySupportsFiniteNumbers()) {
            TypeMirror boxedType = ctx.types.boxedClass(p.pt).asType();
            beginControlFlow("if ($T.isFinite($C))", boxedType, rhs);
            writePrimitive(p.pt);
            nextControlFlow("else");
            RHS asString = new RHS(e(ctx.commonTypes.string, "$T.toString($C)", boxedType, rhs), false);
            nest(ctx.commonTypes.string, lhs, pvn, asString, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
            endControlFlow();
        } else {
            writePrimitive(p.pt);
        }
    }

    /**
     * Writes non-primitive types. This is a good method to override if you want to add specializations for some types
     * that work with null values.
     */
    protected void writeNullable(WrittenType wt) {
        if (rhs.isNullable()) {
            if (rhs.isQuick()) {
                beginControlFlow("if ($C != null)", rhs);
            } else {
                RHS nest = new RHS(createVariable(rhs.type(), pvn), true);
                addStatement("$T $C = $C", type, nest, rhs);
                nest(type, lhs, "nullChecked", nest, config).writeNullable(wt);
                return;
            }
        }

        writeNullCheckedObject(wt);

        if (rhs.isNullable()) {
            nextControlFlow("else");
            writeNull();
            endControlFlow();
        }
    }

    /**
     * Writes non-primitive types that are known to be non-null. This is a good method to override if you want to add
     * specializations for some types that require a dedicated null check.
     */
    protected void writeNullCheckedObject(WrittenType wt) {
        if (wt instanceof Converted converted) {
            Expr convertedValue = converted.converter().apply(rhs);
            RHS newValue = new RHS(createVariable(convertedValue.type(), "converted"), true);
            addStatement(c("$T $C = $C", convertedValue.type(), newValue, convertedValue));
            nest(convertedValue.type(), lhs, pvn, newValue, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
        } else if (wt instanceof Boxed boxed) {
            nest(boxed.unboxed(), lhs, pvn, rhs, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
        } else if (wt instanceof Text text) {
            writeString(text.kind());
        } else if (wt instanceof EnumValue) {
            writeEnum();
        } else if (wt instanceof Binary binary) {
            writeBinary(binary.kind());
        } else if (wt instanceof IterableValue iterable) {
            writeIterable(iterable.componentType());
        } else if (wt instanceof MapValue map) {
            writeMap(map.keyType(), map.valueType());
        } else if (wt instanceof ObjectAsMap object) {
            writeObjectAsMap(object.declaredType());
        } else {
            throw Exceptions.unexpected();
        }
    }

    protected void writeIterable(TypeMirror componentType) {
        RHS elemVar = new RHS(createVariable(componentType, "item"), true);
        SELF nested =
                nest(componentType, new LHS.Array(), "item", elemVar, config.propagateTo(PropagationKind.PROPERTY));
        startArray();
        TypedVariable firstMarker = writeCommaMarkerIfNecessary();
        beginControlFlow("for ($T $C : $C)", nested.type, elemVar, rhs);
        writeCommaIfNecessary(firstMarker);
        Exceptions.runWithContext("component", componentType, nested::build);
        endControlFlow();
        endArray();
    }

    private TypedVariable writeCommaMarkerIfNecessary() {
        if (needsToWriteComma()) {
            TypedVariable variable = createVariable(ctx.types.getPrimitiveType(TypeKind.BOOLEAN), "first");
            addStatement("boolean $C = true", variable);
            return variable;
        }
        return null;
    }

    private void writeCommaIfNecessary(TypedVariable firstMarker) {
        if (firstMarker != null) {
            beginControlFlow("if (!$C)", firstMarker);
            writeComma();
            endControlFlow();
            addStatement("$C = false", firstMarker);
        }
    }

    private void writeMap(TypeMirror keyType, TypeMirror valueType) {
        TypedVariable entry = createVariable(null, "entry");

        LHS keyLhs = Exceptions.getWithContext("key", keyType, () -> getMapKeyLhs(keyType, entry));
        RHS valueRhs = new RHS(entry.call(valueType, "getValue"), true);
        SELF valueNested = nest(valueType, keyLhs, "value", valueRhs, config.propagateTo(PropagationKind.PROPERTY));

        inGuardedObject(() -> {
            beginControlFlow("for ($T<$T, $T> $C : $C.entrySet())", Map.Entry.class, keyType, valueType, entry, rhs);
            Exceptions.runWithContext("value", valueType, valueNested::build);
            endControlFlow();
        });
    }

    private LHS getMapKeyLhs(TypeMirror keyType, TypedVariable entry) {
        Expr rawKeyExpr = entry.call(keyType, "getKey");
        AnyConfig mapKeyConfig = new AnyConfig(
                        List.of(new InstantiatedProperty<>(
                                Delegation.DELEGATE_FROM, LocationKind.PROPERTY, false, "map key")),
                        ctx)
                .merge(config.propagateTo(PropagationKind.SUBSTITUTE));

        WrittenType keyWrittenType = nest(keyType, new Return(), "key", new RHS(rawKeyExpr, true), mapKeyConfig).wt;
        if (keyWrittenType instanceof Text text && text.kind() == StringKind.STRING) {
            return new Member(rawKeyExpr);
        } else if (keyWrittenType instanceof Boxed) {
            return new Member(e(ctx.commonTypes.string, "$T.valueOf($C)", String.class, rawKeyExpr));
        } else if (keyWrittenType instanceof Converted converted
                && ctx.commonTypes.isString(converted.convertedType())) {
            return new Member(converted.converter().apply(rawKeyExpr));
        } else if (keyWrittenType instanceof EnumValue) {
            return new Member(e(ctx.commonTypes.string, Enums.serializationCode(generatedClass, keyType, rawKeyExpr)));
        }

        throw new ContextedRuntimeException("Map keys must be serialized as strings")
                .addContextValue("keyType", keyType);
    }

    protected void writeObjectAsMap(DeclaredType declaredType) {
        Optional<Setup> referenceSetup = ctx.references.resolveSetup(config, contextParameter());
        if (referenceSetup.isPresent()) {
            Setup setup = referenceSetup.get();
            TypeMirror idType = setup.finalIdType(type, ctx);
            RHS idVar = new RHS(createVariable(idType, "id"), false);
            addStatement("$T $C = $C", idType, idVar, setup.previouslyWritten(rhs));
            beginControlFlow("if ($C != null)", idVar);
            nest(idType, lhs, "id", idVar, config.propagateTo(PropagationKind.PROPERTY))
                    .build();
            nextControlFlow("else");
        }
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        Polymorphism.of(typeElement, ctx)
                .ifPresentOrElse(
                        this::writePolymorphicObject,
                        () -> inGuardedObject(() -> {
                            code.add("\n");
                            writeObjectPropertiesAsFields();
                            code.add("\n");
                        }));
        referenceSetup.ifPresent(__ -> endControlFlow());
    }

    private void inGuardedObject(Runnable r) {
        Optional<TypedVariable> alreadyOpen = contextParameter().map(contextParameter -> {
            TypedVariable ao = createVariable(ctx.types.getPrimitiveType(TypeKind.BOOLEAN), "objectOpen");
            addStatement("boolean $C = $C.isObjectOpen(true)", ao, contextParameter);
            return ao;
        });
        alreadyOpen.ifPresentOrElse(
                ao -> {
                    beginControlFlow("if (!$C)", ao);
                    startObject();
                    endControlFlow();
                },
                this::startObject);

        r.run();

        alreadyOpen.ifPresentOrElse(
                ao -> {
                    beginControlFlow("if (!$C)", ao);
                    endObject();
                    endControlFlow();
                },
                this::endObject);
    }

    private void writePolymorphicObject(Polymorphism polymorphism) {
        Branch branch = Branch.IF;
        for (Polymorphism.Child child : polymorphism.children()) {
            beginControlFlow(branch, "$C instanceof $T", rhs, child.type());
            branch = Branch.ELSE_IF;
            RHS casted = new RHS(createVariable(child.type(), pvn + "Cast"), false);
            addStatement("$T $C = ($T) $C", child.type(), casted, child.type(), rhs);

            AnyConfig childConfig = config.propagateTo(
                    PropagationKind.SUBSTITUTE /* this is fine with the configuration options that we
                 currently have */);
            TemplatablePrototypeKind target = kind.withTypesPrefix(List.of(child.type()));
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
                                startObject();
                                code.add("\n");
                                nest(
                                                ctx.commonTypes.string,
                                                new Member(
                                                        e(ctx.commonTypes.string, "$S", polymorphism.discriminator())),
                                                "discriminator",
                                                new RHS(ctx.commonTypes.stringLiteral(child.name()), false),
                                                config.propagateTo(PropagationKind.PROPERTY))
                                        .build();
                                addStatement("$C.markObjectOpen()", callerContext);
                                Exceptions.runWithContext(
                                        "instance",
                                        child.type(),
                                        () -> addStatement(delegatee.call(
                                                nest(child.type(), lhs, "instance", casted, childConfig).prototype,
                                                List.of(casted),
                                                generatedClass)));
                                endObject();
                            },
                            () -> {
                                startObject();
                                code.add("\n");
                                nest(
                                                ctx.commonTypes.string,
                                                new Member(
                                                        e(ctx.commonTypes.string, "$S", polymorphism.discriminator())),
                                                "discriminator",
                                                new RHS(ctx.commonTypes.stringLiteral(child.name()), false),
                                                config.propagateTo(PropagationKind.PROPERTY))
                                        .build();
                                Exceptions.runWithContext(
                                        "instance",
                                        child.type(),
                                        () -> nest(child.type(), lhs, "instance", casted, childConfig)
                                                .writeObjectPropertiesAsFields());
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
        Optional<Setup> referencesSetup = ctx.references.resolveSetup(config, contextParameter());
        referencesSetup.ifPresent(setup -> setup.generateId(rhs)
                .ifPresent(id -> nest(
                                setup.idType(),
                                new Member(e(ctx.commonTypes.string, "$S", setup.externalName())),
                                "id",
                                new RHS(id, false),
                                config.propagateTo(PropagationKind.PROPERTY))
                        .build()));

        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addWriter(prototype, type);
        Set<String> ignoredProperties =
                config.resolveProperty(IgnoreProperties.IGNORED_PROPERTIES).value();

        List<OutputProperty> properties = ctx.properties.outputProperties(type, this.config);
        List<OutputProperty> sortedProperties =
                referencesSetup.map(setup -> setup.sortProperties(properties)).orElseGet(() -> properties);

        for (OutputProperty property : sortedProperties) {
            if (IgnoreProperty.isIgnoredForJson(property.config())
                    || ignoredProperties.contains(property.externalName())
                    || ignoredProperties.contains(property.canonicalName())) {
                continue;
            }
            verificationForDto.addProperty(
                    property.externalName(), property.accessor().type(), property.config());

            LHS lhs = new Member(e(ctx.commonTypes.string, "$S", property.externalName()));
            RHS accessorCall = new RHS(property.accessor().read(rhs), true);
            SELF nested = nest(
                    property.accessor().type(),
                    lhs,
                    property.canonicalName(),
                    accessorCall,
                    property.config().propagateTo(PropagationKind.PROPERTY));
            Exceptions.runWithContext("property", property.canonicalName(), nested::build);
            referencesSetup
                    .flatMap(s -> s.rememberId(rhs, accessorCall, property.externalName()))
                    .ifPresent(this::addStatement);
            code.add("\n");
        }
    }

    private void writeEnum() {
        RHS representation = new RHS(createVariable(ctx.commonTypes.string, pvn), false);
        Code reprCode = Enums.serializationCode(generatedClass, type, rhs);
        addStatement("$T $C = $C", ctx.commonTypes.string, representation, reprCode);
        nest(ctx.commonTypes.string, lhs, "string", representation, config.propagateTo(PropagationKind.SUBSTITUTE))
                .build();
    }

    protected Features features() {
        return new Features(false);
    }

    protected abstract void writeNull();

    protected abstract void writeString(StringKind stringKind);

    protected abstract void writeBinary(BinaryKind binaryKind);

    /** @param typeMirror if non-null, the type is a primitive wrapper and this is the primitive type */
    public abstract void writePrimitive(PrimitiveType typeMirror);

    protected abstract void startArray();

    protected abstract void endArray();

    protected abstract void startObject();

    protected abstract void endObject();

    boolean needsToWriteComma() {
        return false;
    }

    protected void writeComma() {}

    protected abstract void callDelegate(Delegatee delegatee);

    protected abstract SELF nest(TypeMirror type, LHS lhs, String potentialVariableName, RHS rhs, AnyConfig config);

    Code base64Encode(Code code) {
        return c("$T.getEncoder().encodeToString($C)", Base64.class, code);
    }

    Code charArrayToString(Code code) {
        return c("new $T($C)", String.class, code);
    }

    /** Left-hand side of the serialization process. */
    protected sealed interface LHS {

        record Return() implements LHS {}

        record Array() implements LHS {}

        final class Member extends ExprWrapper<Member> implements LHS, Code {
            Member(Expr wrapped) {
                super(wrapped, Member::new);
            }
        }
    }

    /** Right-hand side of the deserialization process. */
    @Getter
    protected static class RHS extends ExprWrapper<RHS> {
        private final boolean nullable;

        protected RHS(Expr wrapped, boolean nullable) {
            super(wrapped, e -> new RHS(wrapped, nullable));
            this.nullable = nullable;
        }
    }

    protected record Features(boolean onlySupportsFiniteNumbers) {}

    protected sealed interface WrittenType {
        record Delegated(Delegatee delegatee) implements WrittenType {}

        record Primitive(PrimitiveType pt) implements WrittenType {}

        /** The value is converted before writing. The converter is applied to the current {@link #rhs}. */
        record Converted(Function<Expr, Expr> converter, TypeMirror convertedType) implements WrittenType {}

        /** A boxed primitive that is written as its {@code unboxed} primitive type. */
        record Boxed(PrimitiveType unboxed) implements WrittenType {}

        /** A {@link String} or {@code char[]} written as a string. */
        record Text(StringKind kind) implements WrittenType {}

        record EnumValue() implements WrittenType {}

        record Binary(BinaryKind kind) implements WrittenType {}

        record IterableValue(TypeMirror componentType) implements WrittenType {}

        record MapValue(TypeMirror keyType, TypeMirror valueType) implements WrittenType {}

        record ObjectAsMap(DeclaredType declaredType) implements WrittenType {}
    }
}
