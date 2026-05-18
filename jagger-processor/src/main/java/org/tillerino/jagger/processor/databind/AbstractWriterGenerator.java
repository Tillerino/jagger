package org.tillerino.jagger.processor.databind;

import static org.tillerino.jagger.processor.util.Code.c;
import static org.tillerino.jagger.processor.util.Expr.e;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import lombok.Getter;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.databind.AbstractWriterGenerator.LHS.Member;
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

    protected AbstractWriterGenerator(
            SELF parent, TypeMirror type, String potentialVariableName, RHS rhs, LHS lhs, AnyConfig config) {
        super(parent, type, potentialVariableName, config);
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
                kind.withTypesPrefix(List.of(type)),
                prototype,
                !(lhs instanceof Return),
                stackDepth() > 1,
                config,
                generatedClass);
        if (delegate.isPresent()) {
            callDelegate(delegate.get());
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
                RHS asString = new RHS(e(ctx.commonTypes.string, "$T.toString($C)", boxedType, rhs), false);
                nest(ctx.commonTypes.string, lhs, pvn, asString, config.propagateTo(PropagationKind.SUBSTITUTE))
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
        if (rhs.isNullable()) {
            if (rhs.isQuick()) {
                beginControlFlow("if ($C != null)", rhs);
            } else {
                RHS nest = new RHS(createVariable(rhs.type(), pvn), true);
                addStatement("$T $C = $C", type, nest, rhs);
                nest(type, lhs, "nullChecked", nest, config).writeNullable();
                return;
            }
        }

        writeNullCheckedObject();

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
    protected void writeNullCheckedObject() {
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

        Optional<Expr> converter = ctx.converters
                .findOutputConverter(rhs, prototype, config, generatedClass)
                .or(() -> ctx.converters.findJsonValueMethod(rhs));
        if (converter.isPresent()) {
            Expr converted = converter.get();
            RHS newValue = new RHS(createVariable(converted.type(), "converted"), true);
            addStatement(c("$T $C = $C", converted.type(), newValue, converted));
            nest(converted.type(), lhs, pvn, newValue, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build();
            return;
        }

        if (ctx.commonTypes.isBoxed(type)) {
            nest(ctx.types.unboxedType(type), lhs, pvn, rhs, config.propagateTo(PropagationKind.SUBSTITUTE))
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

    private void writeMap() {
        TypeMirror[] typeBindings = ctx.generics
                .recordTypeBindingsFor((DeclaredType) type, ctx.commonTypes.elem(Map.class))
                .values()
                .toArray(TypeMirror[]::new);
        TypeMirror keyType = typeBindings[0];
        TypeMirror valueType = typeBindings[1];

        TypedVariable entry = createVariable(null, "entry");

        RHS value = new RHS(entry.call(valueType, "getValue"), true);
        Member key = new Member(e(ctx.commonTypes.string, "$C.getKey()", entry));
        SELF valueNested = nest(valueType, key, "value", value, config.propagateTo(PropagationKind.PROPERTY));

        startObject();
        beginControlFlow("for ($T<$T, $T> $C : $C.entrySet())", Map.Entry.class, keyType, valueType, entry, rhs);
        Exceptions.runWithContext("value", valueType, valueNested::build);
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
                                addStatement(
                                        "$C.setPendingDiscriminator($S, $S)",
                                        callerContext,
                                        polymorphism.discriminator(),
                                        child.name());
                                Exceptions.runWithContext(
                                        "instance",
                                        child.type(),
                                        () -> nest(child.type(), lhs, "instance", casted, childConfig)
                                                .callDelegate(delegatee));
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
        if (canBePolyChild) {
            InstantiatedVariable context = contextParameter().orElseThrow();
            beginControlFlow("if ($C.isDiscriminatorPending())", context);
            nest(
                            ctx.commonTypes.string,
                            new Member(e(ctx.commonTypes.string, "$C.pendingDiscriminatorProperty", context)),
                            "discriminator",
                            new RHS(context.field(ctx.commonTypes.string, "pendingDiscriminatorValue"), false),
                            config.propagateTo(PropagationKind.PROPERTY))
                    .build();
            addStatement("$C.pendingDiscriminatorProperty = null", context);
            endControlFlow();
        }

        Optional<Setup> referencesSetup = ctx.references.resolveSetup(config, contextParameter());
        referencesSetup.ifPresent(setup -> setup.generateId(rhs)
                .ifPresent(id -> nest(
                                setup.idType(),
                                new Member(e(ctx.commonTypes.string, "$S", setup.property())),
                                "id",
                                new RHS(id, false),
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

            LHS lhs = new Member(e(ctx.commonTypes.string, "$S", property.externalName()));
            RHS accessorCall = new RHS(property.accessor().read(rhs), true);
            SELF nested = nest(
                    property.accessor().type(),
                    lhs,
                    property.canonicalName(),
                    accessorCall,
                    property.config().propagateTo(PropagationKind.PROPERTY));
            Exceptions.runWithContext("property", property.canonicalName(), nested::build);
            referencesSetup.flatMap(s -> s.rememberId(rhs, accessorCall)).ifPresent(this::addStatement);
            code.add("\n");
        });
    }

    private void writeEnum() {
        RHS representation = new RHS(createVariable(ctx.commonTypes.string, pvn), false);
        Code reprCode = Enums.serializationCode(ctx, generatedClass, type, rhs);
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
    public abstract void writePrimitive(TypeMirror typeMirror);

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

    record Features(boolean onlySupportsFiniteNumbers) {}
}
