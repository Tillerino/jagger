package org.tillerino.jagger.processor.databind;

import static java.util.stream.Collectors.toMap;
import static org.tillerino.jagger.processor.config.AnyConfig.fromAccessorConsideringField;
import static org.tillerino.jagger.processor.databind.AbstractCodeGeneratorStack.Branch.ELSE_IF;
import static org.tillerino.jagger.processor.databind.AbstractCodeGeneratorStack.Branch.IF;
import static org.tillerino.jagger.processor.features.PropertyName.resolvePropertyName;
import static org.tillerino.jagger.processor.util.Code.c;
import static org.tillerino.jagger.processor.util.Exceptions.runWithContext;
import static org.tillerino.jagger.processor.util.Expr.e;

import com.squareup.javapoet.CodeBlock;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import lombok.With;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.input.EmptyArrays;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.InstantiatedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.features.Creators.Creator;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.features.References.Setup;
import org.tillerino.jagger.processor.features.Verification.ProtoAndProps;
import org.tillerino.jagger.processor.util.Accessor.WriteAccessor;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.Expr.ExprWrapper;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public abstract class AbstractReaderGenerator<SELF extends AbstractReaderGenerator<SELF>>
        extends AbstractCodeGeneratorStack<SELF> {
    protected final LHS lhs;

    protected final boolean exhaust;

    protected Optional<TypedVariable> alreadyOpen;

    AbstractReaderGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext, generatorContext.prototype().returnType());
        lhs = new LHS.Return(generatorContext.prototype().returnType());
        exhaust = true;
    }

    protected AbstractReaderGenerator(
            @Nonnull SELF parent, String potentialVariableName, LHS lhs, AnyConfig config, boolean exhaust) {
        super(parent, lhs.internalType(), potentialVariableName, config);
        this.lhs = lhs;
        this.exhaust = exhaust;
        this.alreadyOpen = lhs instanceof LHS.Return ? parent.alreadyOpen : Optional.empty();
    }

    protected AbstractReaderGenerator(@Nonnull SELF parent, Props props) {
        super(parent, props.lhs.internalType(), props.potentialVariableName, props.config);
        this.lhs = props.lhs;
        this.exhaust = props.exhaust;
        this.alreadyOpen = props.alreadyOpen;
    }

    public CodeBlock.Builder build() {
        initializeParser();
        alreadyOpen = contextParameter().map(contextParameter -> {
            TypedVariable ao = createVariable(ctx.types.getPrimitiveType(TypeKind.BOOLEAN), "objectOpen");
            addStatement("boolean $C = $C.isObjectOpen(true)", ao, contextParameter);
            return ao;
        });
        return build(Branch.IF, true);
    }

    CodeBlock.Builder build(Branch branch, boolean nullable) {
        Optional<Setup> resolveSetup = ctx.references.resolveSetup(config, contextParameter());
        if (resolveSetup.isPresent()) {
            resolveId(branch, resolveSetup.get());
            branch = ELSE_IF;
        }
        Optional<Delegatee> delegate = ctx.delegation.findDelegatee(
                kind.withTypesPrefix(List.of(type)),
                prototype,
                !(lhs instanceof LHS.Return),
                stackDepth() > 1,
                config,
                generatedClass);
        if (delegate.isPresent()) {
            lastBranch(branch).withBody(() -> {
                addStatement(lhs.assign(delegate.get().call(prototype, List.of(), generatedClass)));
            });
            return code;
        }
        Optional<InstantiatedMethod> converter = ctx.converters.findInputConverter(prototype.blueprint(), type, config);
        if (converter.isPresent()) {
            lastBranch(branch).withBody(() -> {
                InstantiatedMethod method = converter.get();

                LHS.Variable replacementVar =
                        declareLhsVariable(method.parameters().get(0).type(), pvn + "ToConvert");

                AnyConfig nestedConfig = config.propagateTo(PropagationKind.SUBSTITUTE);
                SELF nested = nest(pvn, replacementVar, nestedConfig, exhaust, alreadyOpen);

                runWithContext("converter", converter.get(), () -> nested.build(IF, nullable));
                addStatement(lhs.assign(method.callStatic(List.of(replacementVar))));
            });

            return code;
        }
        detectSelfReferencingType();
        if (type.getKind().isPrimitive()) {
            readPrimitive(branch, type);
        } else {
            readNullable(branch, nullable);
        }
        return code;
    }

    private LHS.Variable declareLhsVariable(TypeMirror replacementType, String name) {
        LHS.Variable converterArg = createLhsVariable(replacementType, name);
        addStatement(c("final $T $C", replacementType, converterArg));
        return converterArg;
    }

    private void resolveId(Branch branch, Setup setup) {
        TypeMirror idType = setup.finalIdType(type, ctx);
        LHS.Variable idVar = declareLhsVariable(idType, "id");

        // We cannot call a delegator from this nested serializer or an else-branch is forced!
        AnyConfig nestedConfig = new AnyConfig(
                        List.of(new InstantiatedProperty(
                                Delegation.DELEGATE_FROM, LocationKind.PROPERTY, false, "(internal)")),
                        ctx)
                .merge(config.propagateTo(PropagationKind.PROPERTY));
        nest("id", idVar, nestedConfig, false, alreadyOpen).build(branch, false);
        TypedVariable resolvedVar = createVariable(type, "resolved");
        addStatement("$T $C = ($T) $C", type, resolvedVar, type, setup.resolveId(idVar));

        beginControlFlow("if ($C == null)", resolvedVar);
        addStatement("throw new $T($S + $C)", IllegalArgumentException.class, "Unresolved ID: ", idVar);
        endControlFlow();

        addStatement(lhs.assign(resolvedVar));

        // a bunch of else-if follow after this
    }

    /**
     * Reads non-primitive types. This is a good method to override if you want to add specializations for some types
     * that work with null values.
     */
    protected void readNullable(Branch branch, boolean nullable) {
        if (nullable) {
            Code cond = nullCaseCondition();
            beginControlFlow(branch, safeNonObjectCase(cond));
            addStatement(lhs.assign("null"));
            readNullCheckedObject(Branch.ELSE_IF);
        } else {
            readNullCheckedObject(branch);
        }
    }

    protected void readPrimitive(Branch branch, TypeMirror type) {
        String typeName;
        switch (type.getKind()) {
            case BOOLEAN -> {
                beginControlFlow(branch, safeNonObjectCase(booleanCaseCondition()));
                typeName = "boolean";
            }
            case BYTE, SHORT, INT, LONG -> {
                beginControlFlow(branch, safeNonObjectCase(numberCaseCondition()));
                typeName = "number";
            }
            case FLOAT, DOUBLE -> {
                beginControlFlow(branch, safeNonObjectCase(stringCaseCondition()));
                readNumberFromString((PrimitiveType) type);
                beginControlFlow(ELSE_IF, safeNonObjectCase(numberCaseCondition()));
                typeName = "number";
            }
            case CHAR -> {
                beginControlFlow(branch, safeNonObjectCase(stringCaseCondition()));
                typeName = "string";
            }
            default -> throw new ContextedRuntimeException(type.getKind().toString());
        }
        if (type.getKind() == TypeKind.CHAR) {
            readCharFromString();
        } else {
            readPrimitive(type);
        }
        elseThrowUnexpected(typeName);
    }

    private void readCharFromString() {
        Expr stringVar = readStringInstead();
        beginControlFlow("if ($C.length() == 1)", stringVar);
        addStatement(lhs.assign("$C.charAt(0)", stringVar));
        nextControlFlow("else");
        addStatement("throw new $T()", IllegalArgumentException.class);
        endControlFlow();
    }

    private void readNumberFromString(PrimitiveType type) {
        Expr stringVar = readStringInstead();
        TypeElement boxed = ctx.types.boxedClass(type);

        beginControlFlow("if ($C.equals($S))", stringVar, "NaN");
        addStatement(lhs.assign("$T.NaN", boxed));

        nextControlFlow("else if ($C.equals($S))", stringVar, "Infinity");
        addStatement(lhs.assign("$T.POSITIVE_INFINITY", boxed));

        nextControlFlow("else if ($C.equals($S))", stringVar, "-Infinity");
        addStatement(lhs.assign("$T.NEGATIVE_INFINITY", boxed));

        nextControlFlow("else");
        addStatement("throw new $T()", IllegalArgumentException.class);
        endControlFlow();
    }

    private Expr readStringInstead() {
        LHS.Variable stringVar = declareLhsVariable(ctx.commonTypes.string, "string");

        AnyConfig nestedConfig = config.propagateTo(PropagationKind.SUBSTITUTE);
        nest("string", stringVar, nestedConfig, exhaust, alreadyOpen).readString(StringKind.STRING);

        return stringVar;
    }

    void readNullCheckedObject(Branch branch) {
        Optional<Creator> jsonCreatorMethod = ctx.creators.findJsonCreatorMethod(type);
        if (jsonCreatorMethod.isPresent()) {
            if (jsonCreatorMethod.get() instanceof Creator.Converter c) {
                readFactory(branch, c.method());
            } else if (jsonCreatorMethod.get() instanceof Creator.Properties p) {
                readObject(branch, p, (TypeElement) ((DeclaredType) type).asElement());
            } else {
                throw Exceptions.unexpected();
            }
        } else if (ctx.commonTypes.isBoxed(type)) {
            PrimitiveType unboxed = ctx.types.unboxedType(type);
            nest(
                            pvn,
                            lhs.withInternalType(unboxed),
                            config.propagateTo(PropagationKind.SUBSTITUTE),
                            exhaust,
                            alreadyOpen)
                    .build(branch, true);
        } else if (ctx.commonTypes.isString(type) || ctx.commonTypes.isArrayOf(type, TypeKind.CHAR)) {
            readString(branch, ctx.commonTypes.isString(type) ? StringKind.STRING : StringKind.CHAR_ARRAY);
        } else if (ctx.commonTypes.isEnum(type)) {
            readEnum(branch);
        } else if (type.getKind() == TypeKind.ARRAY) {
            readArray(branch);
        } else if (ctx.commonTypes.isIterableOrArray(type)) {
            readCollection(branch);
        } else if (ctx.commonTypes.isErasureAssignableTo(type, Map.class)) {
            readMap(branch);
        } else if (type.getKind() == TypeKind.TYPEVAR) {
            throw new ContextedRuntimeException("Missing deserializer for type variable " + type);
        } else {
            if (!(type instanceof DeclaredType dt)) {
                throw new ContextedRuntimeException("I don't know what to do with this type: " + type);
            }
            readObject(branch, null, (TypeElement) dt.asElement());
        }
    }

    private void readFactory(Branch branch, InstantiatedMethod method) {
        if (branch == ELSE_IF) {
            nextControlFlow("else");
        }

        TypeMirror creatorType = method.parameters().get(0).type();
        LHS.Variable creatorArg = declareLhsVariable(creatorType, "creator");

        SELF nested = nest(pvn, creatorArg, config.propagateTo(PropagationKind.SUBSTITUTE), exhaust, alreadyOpen);
        runWithContext("creator", method, () -> nested.build(IF, false));
        addStatement(lhs.assign(method.callStaticFindingArguments(prototype, List.of(creatorArg), generatedClass)));
        if (branch == ELSE_IF) {
            endControlFlow();
        }
    }

    private void readString(Branch branch, StringKind stringKind) {
        beginControlFlow(branch, safeNonObjectCase(stringCaseCondition()));
        readString(stringKind);
        elseThrowUnexpected("string");
    }

    private void readEnum(Branch branch) {
        beginControlFlow(branch, stringCaseCondition());
        {
            String enumValuesField = generatedClass.getOrCreateEnumField(type).name();
            Expr enumVar = readStringInstead();
            beginControlFlow("if ($L.containsKey($C))", enumValuesField, enumVar);
            addStatement(lhs.assign("$L.get($C)", enumValuesField, enumVar));
            nextControlFlow("else");
            throwUnexpectedValue(c("$S + $C + $S", "Unexpected enum value: \"", enumVar, "\""));
            endControlFlow();
        }
        elseThrowUnexpected("string");
    }

    private void readArray(Branch branch) {
        TypeMirror componentType = ctx.commonTypes.getArrayComponentType(type);
        beginControlFlow(branch, arrayCaseCondition());
        {
            TypeMirror rawComponentType = ctx.types.erasure(componentType);
            TypeMirror rawRawComponentType =
                    rawComponentType.getKind().isPrimitive() ? rawComponentType : ctx.commonTypes.object;
            code.add("// Like ArrayList\n");
            TypedVariable varName = createVariable(null, "array");
            addStatement(
                    "$T[] $C = $T.EMPTY_$L_ARRAY",
                    rawRawComponentType,
                    varName,
                    EmptyArrays.class,
                    rawRawComponentType.getKind().isPrimitive()
                            ? rawRawComponentType.toString().toUpperCase()
                            : "OBJECT");
            TypedVariable len = createVariable(ctx.types.getPrimitiveType(TypeKind.INT), "len");
            addStatement("int $C = 0", len);
            iterateOverElements();
            {
                beginControlFlow("if ($C == $C.length)", len, varName);
                code.add("// simplified version of ArrayList growth\n");
                addStatement(
                        "$C = $T.copyOf($C, $T.max(10, $C.length + ($C.length >> 1)))",
                        varName,
                        Arrays.class,
                        varName,
                        Math.class,
                        varName,
                        varName);
                endControlFlow();

                AnyConfig nestedConfig = config.propagateTo(PropagationKind.PROPERTY);
                SELF nested =
                        nest("item", new LHS.Array(componentType, varName, len), nestedConfig, true, Optional.empty());
                Exceptions.runWithContext("component", componentType, () -> nested.build(Branch.IF, true));
            }
            endControlFlow(); // end of loop
            afterArray();
            if (componentType.getKind() == TypeKind.TYPEVAR) {
                Optional<Code> classParameter = ctx.generics.findClassParameter(prototype.method(), type);
                if (classParameter.isEmpty()) {
                    throw new ContextedRuntimeException(
                            "You are trying to read a generic array. For this, you need the array class at runtime.\n"
                                    + " Add a parameter Class<%s> to %s.".formatted(type, prototype));
                }
                addStatement(lhs.assign("$T.copyOf($C, $C, $C)", Arrays.class, varName, len, classParameter.get()));
            } else if (ctx.types.isSameType(rawRawComponentType, rawComponentType)) {
                addStatement(lhs.assign("$T.copyOf($C, $C)", Arrays.class, varName, len));
            } else {
                addStatement(lhs.assign("$T.copyOf($C, $C, $T[].class)", Arrays.class, varName, len, rawComponentType));
            }
        }
        if (componentType.getKind() == TypeKind.BYTE) {
            beginControlFlow(ELSE_IF, stringCaseCondition());
            Expr stringVar = readStringInstead();
            addStatement(lhs.assign("$T.getDecoder().decode($C)", Base64.class, stringVar));
        }
        elseThrowUnexpected("array");
    }

    private void readCollection(Branch branch) {
        beginControlFlow(branch, arrayCaseCondition());
        {
            TypeMirror componentType = ctx.commonTypes.getComponentType(type, Iterable.class);
            TypeMirror collectionType = determineCollectionType();
            Code containerVar = instantiateContainer(collectionType);

            iterateOverElements();
            {
                AnyConfig nestedConfig = config.propagateTo(PropagationKind.PROPERTY);
                SELF nested = nest(
                        "item", new LHS.Collection(componentType, containerVar), nestedConfig, true, Optional.empty());
                runWithContext("component", componentType, () -> nested.build(IF, true));
            }
            endControlFlow(); // end of loop
            afterArray();
            if (lhs != containerVar) {
                addStatement(lhs.assign("$C", containerVar));
            }
        }
        elseThrowUnexpected("array");
    }

    private TypeMirror determineCollectionType() {
        TypeMirror rawType = ctx.types.erasure(type);
        if (!((DeclaredType) rawType).asElement().getModifiers().contains(Modifier.ABSTRACT)) {
            return rawType;
        } else if (ctx.commonTypes.isAssignable(rawType, Set.class)) {
            return ctx.types.erasure(ctx.commonTypes.type(LinkedHashSet.class));
        } else if (ctx.commonTypes.isAssignable(rawType, List.class)) {
            return ctx.types.erasure(ctx.commonTypes.type(ArrayList.class));
        } else if (ctx.commonTypes.isErasureAssignableTo(rawType, Map.class)) {
            return ctx.types.erasure(ctx.commonTypes.type(LinkedHashMap.class));
        } else {
            throw new ContextedRuntimeException(type.toString());
        }
    }

    private Code instantiateContainer(TypeMirror collectionType) {
        if (lhs instanceof LHS.Variable v) {
            addStatement("$C = new $T<>()", v, collectionType);
            return v;
        } else {
            TypedVariable variable = createVariable(type, "container");
            addStatement("$T $C = new $T<>()", type, variable, collectionType);
            return variable;
        }
    }

    private void readMap(Branch branch) {
        inGuardedObject(branch, () -> {
            TypeMirror[] typeBindings = ctx.generics
                    .recordTypeBindingsFor((DeclaredType) type, ctx.commonTypes.elem(Map.class))
                    .values()
                    .toArray(TypeMirror[]::new);
            TypeMirror keyType = typeBindings[0];
            if (!ctx.types.isSameType(keyType, ctx.commonTypes.string)) {
                throw new ContextedRuntimeException("Only String keys supported for now.");
            }
            TypeMirror valueType = typeBindings[1];
            TypeMirror mapType = determineCollectionType();
            Code mapVar = instantiateContainer(mapType);
            iterateOverFields();
            {
                beginControlFlow(IF, memberCaseCondition());
                TypedVariable keyVar = createVariable(keyType, "key");
                readMemberNameInIteration(keyVar.name());
                LHS.Map lhsValue = new LHS.Map(valueType, mapVar, keyVar);
                SELF nested =
                        nest("value", lhsValue, config.propagateTo(PropagationKind.PROPERTY), true, Optional.empty());
                Exceptions.runWithContext("value", valueType, () -> nested.build(Branch.IF, true));
                elseThrowUnexpected("field name");
            }
            endControlFlow(); // end of loop

            return () -> {
                if (mapVar != lhs) {
                    addStatement(lhs.assign("$C", mapVar));
                }
            };
        });
    }

    private void readObject(Branch branch, @Nullable Creator.Properties properties, TypeElement element) {
        Code cond = objectCaseCondition();
        if (alreadyOpen.isPresent()) {
            cond = c("$C || $C", alreadyOpen.get(), cond);
        }
        beginControlFlow(branch, cond);

        if (properties != null) {
            readCreator(properties.method());
        } else {
            Polymorphism.of(element, ctx)
                    .ifPresentOrElse(
                            polymorphism -> readPolymorphicObject(polymorphism), () -> readObjectFields(element));
        }

        elseThrowUnexpected("object");
    }

    private void inGuardedObject(Branch branch, Supplier<Runnable> r) {
        alreadyOpen.ifPresentOrElse(
                ao -> beginControlFlow(branch, "$C || $C", ao, objectCaseCondition()),
                () -> beginControlFlow(branch, "$C", objectCaseCondition()));

        Runnable s = r.get();

        alreadyOpen.ifPresentOrElse(
                ao -> {
                    beginControlFlow("if (!$C)", ao);
                    afterObject();
                    endControlFlow();
                },
                this::afterObject);

        if (s != null) {
            s.run();
        }
        elseThrowUnexpected("object");
    }

    private void readPolymorphicObject(Polymorphism polymorphism) {
        LHS.Variable discriminator = declareLhsVariable(ctx.commonTypes.string, "discriminator");
        nest("discriminator", discriminator, config.propagateTo(PropagationKind.PROPERTY), true, Optional.empty())
                .readDiscriminator(polymorphism.discriminator());

        Branch branch = Branch.IF;
        for (Polymorphism.Child child : polymorphism.children()) {
            SELF nested = nest(
                    "instance",
                    lhs.withInternalType(child.type()),
                    config.propagateTo(
                            PropagationKind.SUBSTITUTE /* this is fine with the configuration options that we
               currently have */),
                    exhaust,
                    Optional.empty());

            beginControlFlow(branch, "$C.equals($S)", discriminator, child.name());
            Exceptions.runWithContext("instance", child.type(), () -> readPolymorphicChild(nested));
            branch = Branch.ELSE_IF;
        }
        if (branch == Branch.IF) {
            throw new ContextedRuntimeException("No children for " + type);
        }
        nextControlFlow("else");
        addStatement("throw new $T($S + $C)", IllegalArgumentException.class, "Unknown type ", discriminator);
        endControlFlow(); // ends the loop
    }

    private void readPolymorphicChild(SELF nested) {
        Optional<Delegatee> delegateeMaybe = ctx.delegation.findDelegatee(
                kind.withTypesPrefix(List.of(nested.type)), prototype, false, true, config, generatedClass);
        if (delegateeMaybe.isPresent()) {
            Delegatee delegatee = delegateeMaybe.get();
            InstantiatedVariable callerContext = contextParameter()
                    .orElseThrow(() -> new ContextedRuntimeException("Prototype method must have a context parameter"));
            if (!delegatee.method().hasParameterAssignableFrom(callerContext.type(), ctx)) {
                throw new ContextedRuntimeException("Delegate method must have a context parameter");
            }
            addStatement("$C.markObjectOpen()", callerContext);
            nested.lhs.assignAnd(
                    delegatee.call(nested.prototype, List.of(), nested.generatedClass), this, __ -> afterObject());
        } else {
            ctx.creators
                    .findJsonCreatorMethod(nested.type)
                    .map(c -> c instanceof Creator.Properties p ? p : null)
                    .ifPresentOrElse(
                            c -> nested.readCreator(c.method()),
                            () -> nested.readObjectFields((TypeElement) ((DeclaredType) nested.type).asElement()));
        }
    }

    void readObjectFields(TypeElement element) {
        if (element.getKind() == ElementKind.RECORD) {
            Map<TypeVar, TypeMirror> typeBindings = ctx.generics.recordTypeBindings((DeclaredType) type);
            InstantiatedMethod instantiatedConstructor = ctx.generics
                    .instantiateMethod(
                            ElementFilter.constructorsIn(element.getEnclosedElements())
                                    .get(0),
                            typeBindings,
                            LocationKind.CREATOR)
                    .withReturnType(type);
            readCreator(instantiatedConstructor);
        } else {
            readObjectFromAccessors();
        }
    }

    void readCreator(InstantiatedMethod method) {
        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addReader(prototype, type);

        List<NestedProperty> nested = new ArrayList<>();
        AnyConfig creatorConfig = method.config().merge(config);

        for (InstantiatedVariable parameter : method.parameters()) {
            AnyConfig propertyConfig = parameter.config().merge(creatorConfig);

            // use parameter name as variable name because we know it is valid - unlike the configured property name
            String propertyName = resolvePropertyName(propertyConfig, parameter.name());

            LHS.Variable propVar = createLhsVariable(parameter.type(), parameter.name());
            Code defaultValue = ctx.defaultValues.getDefaultValue(prototype, parameter.type(), propertyConfig);
            addStatement(c("$T $C = $C", parameter.type(), propVar, defaultValue));

            SELF nest = nest(
                    parameter.name(),
                    propVar,
                    propertyConfig.propagateTo(PropagationKind.PROPERTY),
                    true,
                    Optional.empty());
            if (IgnoreProperty.isIgnoredForJson(propertyConfig)) {
                // we do need the default value to call the creator, so we only skip reading the value
                continue;
            }
            verificationForDto.addProperty(propertyName, parameter.type(), propertyConfig);
            nested.add(new NestedProperty(parameter.name(), propertyName, propertyConfig, nest));
        }

        TypedVariable idVar = readProperties(nested);
        List<Expr> args =
                nested.stream().<Expr>map(p -> ((LHS.Variable) p.generator.lhs)).toList();
        Expr creatorCall = method.callStatic(args);
        ctx.references
                .resolveSetup(config, contextParameter())
                .ifPresentOrElse(
                        setup -> lhs.assignAnd(
                                creatorCall, this, objectVar -> addStatement(setup.bindItem(idVar, objectVar))),
                        () -> addStatement(lhs.assign(creatorCall)));
    }

    private void readObjectFromAccessors() {
        TypedVariable objectVar = createVariable(type, "object");
        addStatement("$T $C = new $T()", type, objectVar, type);

        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addReader(prototype, type);
        List<NestedProperty> nested = new ArrayList<>();
        ctx.properties.listWriteAccessors(type).forEach((canonicalPropertyName, accessor) -> {
            AnyConfig propertyConfig = fromAccessorConsideringField(
                            accessor, accessor.name(), type, canonicalPropertyName, ctx)
                    .merge(config);
            if (IgnoreProperty.isIgnoredForJson(propertyConfig)) {
                return;
            }

            LHS lhs = LHS.from(accessor, objectVar);
            String propertyName = resolvePropertyName(propertyConfig, canonicalPropertyName);
            verificationForDto.addProperty(propertyName, accessor.type(), propertyConfig);
            SELF nest = nest(
                    canonicalPropertyName,
                    lhs,
                    propertyConfig.propagateTo(PropagationKind.PROPERTY),
                    true,
                    Optional.empty());
            nested.add(new NestedProperty(canonicalPropertyName, propertyName, propertyConfig, nest));
        });

        TypedVariable idVar = readProperties(nested);
        ctx.references.resolveSetup(config, contextParameter()).ifPresent(setup -> {
            addStatement(setup.bindItem(idVar, objectVar));
        });

        addStatement(lhs.assign("$C", objectVar));
    }

    private TypedVariable readProperties(List<NestedProperty> properties) {
        Optional<Setup> referencesSetup = ctx.references.resolveSetup(config, contextParameter());
        TypedVariable idVar = referencesSetup
                .map(setup -> {
                    TypeMirror idType = setup.finalIdType(type, ctx);
                    TypedVariable variable = createVariable(idType, "id");
                    addStatement("$T $C = null", idType, variable);
                    return variable;
                })
                .orElse(null);

        List<NestedProperty> requiredProperties = properties.stream()
                .filter(p -> RequiredProperty.isRequired(p.config))
                .toList();
        Map<String, TypedVariable> propertyPresentByCanonicalName = createPropertyPresentBooleans(requiredProperties);

        iterateOverFields();
        beginControlFlow(IF, memberCaseCondition());
        TypedVariable nameVar = createVariable(ctx.commonTypes.string, "name");
        readMemberNameInIteration(nameVar.name());

        Set<String> ignoredProperties =
                config.resolveProperty(IgnoreProperties.IGNORED_PROPERTIES).value();

        beginControlFlow("switch($C)", nameVar);
        for (NestedProperty nest : properties) {
            if (ignoredProperties.contains(nest.serializedName)) {
                continue;
            }
            beginControlFlow("case $C:", Aliases.cases(nest));
            Exceptions.runWithContext("property", nest.serializedName, () -> {
                if (referencesSetup
                        .filter(setup ->
                                setup.isPropertyBased() && setup.property().equals(nest.serializedName))
                        .isPresent()) {
                    SELF tmpNest = nest(
                            nest.generator.pvn,
                            new LHS.Variable(nest.generator.type, idVar),
                            nest.generator.config,
                            true,
                            Optional.empty());
                    tmpNest.build(IF, true);
                    addStatement(nest.generator.lhs.assign(idVar));
                } else {
                    nest.generator.build(Branch.IF, true);
                }
            });
            TypedVariable presentVar = propertyPresentByCanonicalName.get(nest.canonicalName);
            if (presentVar != null) {
                addStatement("$C = true", presentVar);
            }
            addStatement("break");
            endControlFlow();
        }
        if (!ignoredProperties.isEmpty()) {
            beginControlFlow(c("case $C:", IgnoreProperties.cases(ignoredProperties)));
            skipValue();
            addStatement("break");
            endControlFlow();
        }
        referencesSetup.filter(setup -> !setup.isPropertyBased()).ifPresent(setup -> {
            beginControlFlow("case $S:", setup.property());
            nest(
                            "id",
                            new LHS.Variable(setup.idType(), idVar),
                            config.propagateTo(PropagationKind.PROPERTY),
                            true,
                            Optional.empty())
                    .build(IF, true);
            addStatement("break");
            endControlFlow();
        });
        {
            beginControlFlow("default:");
            if (UnknownProperties.shouldThrow(config)) {
                throwUnrecognizedProperty(nameVar);
            } else {
                skipValue();
            }
            endControlFlow();
        }
        endControlFlow(); // ends the last field
        elseThrowUnexpected("field name");
        endControlFlow(); // ends the loop

        for (NestedProperty property : requiredProperties) {
            addStatement(
                    "if (!$C) throw new $T($S)",
                    propertyPresentByCanonicalName.get(property.canonicalName),
                    IOException.class,
                    "Missing property " + property.serializedName);
        }

        alreadyOpen.ifPresentOrElse(
                ao -> {
                    beginControlFlow("if (!$C)", ao);
                    afterObject();
                    endControlFlow();
                },
                this::afterObject);
        return idVar;
    }

    private Map<String, TypedVariable> createPropertyPresentBooleans(List<NestedProperty> requiredProperties) {
        Map<String, TypedVariable> propertyPresentByCanonicalName = requiredProperties.stream()
                .collect(toMap(
                        p -> p.canonicalName,
                        p -> createVariable(ctx.types.getPrimitiveType(TypeKind.BOOLEAN), p.canonicalName + "Present"),
                        (x, y) -> {
                            throw Exceptions.unexpected();
                        },
                        LinkedHashMap::new));

        if (!requiredProperties.isEmpty()) {
            List<Code> assignments = propertyPresentByCanonicalName.values().stream()
                    .map(v -> c("$C = false", v))
                    .toList();
            addStatement("boolean $C", Code.join(assignments, ", "));
        }
        return propertyPresentByCanonicalName;
    }

    private void elseThrowUnexpected(String typeName) {
        if (exhaust) {
            nextControlFlow("else");
            throwUnexpected(typeName);
            endControlFlow();
        }
    }

    private Code safeNonObjectCase(Code leCase) {
        return alreadyOpen.map(ao -> c("!$C && $C", ao, leCase)).orElse(leCase);
    }

    private LHS.Variable createLhsVariable(TypeMirror type, String name) {
        TypedVariable stringVar = createVariable(type, name);
        return new LHS.Variable(type, stringVar);
    }

    protected abstract void initializeParser();

    protected abstract Code memberCaseCondition();

    protected abstract Code stringCaseCondition();

    protected abstract Code numberCaseCondition();

    protected abstract Code objectCaseCondition();

    protected abstract Code arrayCaseCondition();

    protected abstract Code booleanCaseCondition();

    protected abstract Code nullCaseCondition();

    protected abstract void readPrimitive(TypeMirror type);

    protected abstract void readString(StringKind stringKind);

    protected abstract void readMemberNameInIteration(String variableName);

    protected abstract void iterateOverFields();

    protected abstract void skipValue();

    protected abstract void afterObject();

    protected abstract void readDiscriminator(String propertyName);

    protected abstract void iterateOverElements();

    protected abstract void afterArray();

    protected abstract void throwUnexpected(String expected);

    protected abstract void throwUnexpectedValue(Code message);

    protected abstract void throwUnrecognizedProperty(Code propertyName);

    protected abstract SELF nest(Props props);

    protected SELF nest(
            String potentialVariableName,
            LHS lhs,
            AnyConfig config,
            boolean exhaust,
            Optional<TypedVariable> alreadyOpen) {
        return nest(new Props(potentialVariableName, lhs, config, exhaust, alreadyOpen));
    }

    /** Left-hand side of the deserialization process. */
    protected sealed interface LHS {
        TypeMirror internalType();

        LHS withInternalType(TypeMirror typeMirror);

        default Code assign(String string, Object... args) {
            return assign(c(string, args));
        }

        default Code assign(Code s) {
            if (this instanceof Return) {
                return c("return $C", s);
            } else if (this instanceof Variable v) {
                return c("$C = $C", v, s);
            } else if (this instanceof Array a) {
                return c("$C[$C++] = $C", a.arrayVar(), a.indexVar(), s);
            } else if (this instanceof Collection c) {
                return c("$C.add($C)", c.variable(), s);
            } else if (this instanceof Map m) {
                return c("$C.put($C, $C)", m.mapVar(), m.keyVar(), s);
            } else if (this instanceof Field f) {
                return c("$C = $C", f, s);
            } else if (this instanceof Setter set) {
                return c("$C.$L($C)", set.objectVar(), set.methodName(), s);
            } else {
                throw new ContextedRuntimeException(this.toString());
            }
        }

        default void assignAnd(Expr rhs, AbstractCodeGeneratorStack stack, Consumer<Code> tmpAction) {
            if (this instanceof Variable v) {
                stack.addStatement(assign(rhs));
                tmpAction.accept(v);
            } else if (this instanceof Field f) {
                stack.addStatement(assign(rhs));
                tmpAction.accept(f);
            } else {
                TypedVariable tmp = stack.createVariable(rhs.type(), "tmp");
                stack.addStatement("$T $C = $C", rhs.type(), tmp, rhs);
                tmpAction.accept(tmp);
                stack.addStatement(assign(tmp));
            }
        }

        static LHS from(WriteAccessor a, Code objectVar) {
            return switch (a.kind()) {
                case FIELD ->
                    new Field(
                            a.type(),
                            e(
                                    a.type(),
                                    "$C.$L",
                                    objectVar,
                                    a.element().getSimpleName().toString()));
                case SETTER ->
                    new Setter(a.type(), objectVar, a.element().getSimpleName().toString());
                default -> throw new ContextedRuntimeException(a.kind().toString());
            };
        }

        record Return(@With TypeMirror internalType) implements LHS {}

        final class Variable extends ExprWrapper<Variable> implements LHS {
            private final TypeMirror internalType;

            private Variable(TypeMirror internalType, Expr wrapped) {
                super(wrapped, e -> new Variable(internalType, e));
                this.internalType = internalType;
            }

            @Override
            public TypeMirror internalType() {
                return internalType;
            }

            @Override
            public LHS withInternalType(TypeMirror typeMirror) {
                return new Variable(typeMirror, wrapped);
            }
        }

        record Array(@With TypeMirror internalType, Code arrayVar, TypedVariable indexVar) implements LHS {}

        record Collection(@With TypeMirror internalType, Code variable) implements LHS {}

        record Map(@With TypeMirror internalType, Code mapVar, Code keyVar) implements LHS {}

        final class Field extends ExprWrapper<Field> implements LHS {
            private final TypeMirror internalType;

            Field(TypeMirror internalType, Expr wrapped) {
                super(wrapped, e -> new Field(internalType, e));
                this.internalType = internalType;
            }

            @Override
            public TypeMirror internalType() {
                return internalType;
            }

            @Override
            public LHS withInternalType(TypeMirror typeMirror) {
                return new Field(typeMirror, wrapped);
            }
        }

        record Setter(@With TypeMirror internalType, Code objectVar, String methodName) implements LHS {}
    }

    protected record Props(
            String potentialVariableName,
            LHS lhs,
            AnyConfig config,
            boolean exhaust,
            Optional<TypedVariable> alreadyOpen) {}
}
