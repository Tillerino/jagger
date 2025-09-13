package org.tillerino.jagger.processor.apis;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.tillerino.jagger.processor.Snippet.joinPrependingCommaToEach;
import static org.tillerino.jagger.processor.Snippet.of;
import static org.tillerino.jagger.processor.apis.AbstractCodeGeneratorStack.Property.ITEM;
import static org.tillerino.jagger.processor.apis.AbstractCodeGeneratorStack.StringKind.STRING;
import static org.tillerino.jagger.processor.apis.AbstractReaderGenerator.Branch.ELSE_IF;
import static org.tillerino.jagger.processor.apis.AbstractReaderGenerator.Branch.IF;
import static org.tillerino.jagger.processor.apis.AbstractReaderGenerator.LHS.Collection;
import static org.tillerino.jagger.processor.apis.AbstractReaderGenerator.LHS.Variable;
import static org.tillerino.jagger.processor.apis.AbstractReaderGenerator.LHS.from;
import static org.tillerino.jagger.processor.config.AnyConfig.fromAccessorConsideringField;
import static org.tillerino.jagger.processor.features.IgnoreProperty.IGNORE_PROPERTY;
import static org.tillerino.jagger.processor.features.PropertyName.resolvePropertyName;
import static org.tillerino.jagger.processor.util.Exceptions.runWithContext;

import com.squareup.javapoet.CodeBlock;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.input.EmptyArrays;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.InstantiatedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.features.Creators.Creator;
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.features.References.Setup;
import org.tillerino.jagger.processor.features.Verification.ProtoAndProps;
import org.tillerino.jagger.processor.util.Accessor.WriteAccessor;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public abstract class AbstractReaderGenerator<SELF extends AbstractReaderGenerator<SELF>>
        extends AbstractCodeGeneratorStack<SELF> {
    protected final LHS lhs;

    AbstractReaderGenerator(AnnotationProcessorUtils utils, JaggerPrototype prototype, GeneratedClass generatedClass) {
        super(
                utils,
                generatedClass,
                prototype,
                CodeBlock.builder(),
                null,
                prototype.instantiatedReturnType(),
                true,
                null,
                null);
        lhs = new LHS.Return();
    }

    protected AbstractReaderGenerator(
            @Nonnull SELF parent,
            TypeMirror type,
            boolean stackRelevantType,
            Property property,
            LHS lhs,
            AnyConfig config) {
        super(parent, type, stackRelevantType, property, config);
        this.lhs = lhs;
    }

    public CodeBlock.Builder build() {
        initializeParser();
        return build(Branch.IF, true, true);
    }

    CodeBlock.Builder build(Branch branch, boolean nullable, boolean lastCase) {
        Optional<Setup> resolveSetup = utils.references.resolveSetup(config, prototype, type);
        if (resolveSetup.isPresent()) {
            resolveId(branch, resolveSetup.get());
            branch = ELSE_IF;
        }
        Optional<Delegatee> delegate = utils.delegation.findDelegatee(
                type, prototype, !(lhs instanceof LHS.Return), stackDepth() > 1, config, generatedClass);
        if (delegate.isPresent()) {
            if (branch != Branch.IF) {
                nextControlFlow("else");
            }
            invokeDelegate(delegate.get().fieldOrParameter(), delegate.get().method());
            if (branch != Branch.IF) {
                endControlFlow();
            }
            return code;
        }
        Optional<InstantiatedMethod> converter =
                utils.converters.findInputConverter(prototype.blueprint(), type, config);
        if (converter.isPresent()) {
            if (branch != IF) {
                nextControlFlow("else");
            }
            InstantiatedMethod method = converter.get();

            Variable converterArg = Variable.from(createVariable("toConvert"));
            addStatement(of("$T $C", method.parameters().get(0).type(), converterArg));
            runWithContext(
                    () -> nest(
                                    method.parameters().get(0).type(),
                                    null,
                                    converterArg,
                                    true,
                                    config.propagateTo(PropagationKind.SUBSTITUTE))
                            .build(IF, nullable, lastCase),
                    "converter",
                    converter.get());
            addStatement(lhs.assign(of("$C($C)", method.callSymbol(utils), converterArg)));

            if (branch != IF) {
                endControlFlow();
            }
            return code;
        }
        detectSelfReferencingType();
        if (type.getKind().isPrimitive()) {
            readPrimitive(branch, type, lastCase);
        } else {
            readNullable(branch, nullable, lastCase);
        }
        return code;
    }

    private void resolveId(Branch branch, Setup setup) {
        ScopedVar idVar = createVariable("id");
        TypeMirror idType = setup.finalIdType(type, utils);
        addStatement("$T $C", idType, idVar);
        // We cannot call a delegator from this nested serializer or an else-branch is forced!
        AnyConfig nestedConfig = new AnyConfig(List.of(
                        new InstantiatedProperty(Delegation.DELEGATE_FROM, LocationKind.PROPERTY, false, "(internal)")))
                .merge(config.propagateTo(PropagationKind.PROPERTY));
        nest(idType, new Property("id", "id", null), Variable.from(idVar), false, nestedConfig)
                .build(branch, false, false);
        ScopedVar resolvedVar = createVariable("resolved");
        addStatement("$T $C = ($T) $C", type, resolvedVar, type, setup.resolveId(idVar));
        beginControlFlow("if ($C == null)", resolvedVar);
        addStatement("throw new IOException($S + $C)", "Unresolved ID: ", idVar);
        endControlFlow();
        addStatement(lhs.assign(resolvedVar));

        // a bunch of else-if follow after this
    }

    /**
     * Reads non-primitive types. This is a good method to override if you want to add specializations for some types
     * that work with null values.
     */
    protected void readNullable(Branch branch, boolean nullable, boolean lastCase) {
        if (nullable) {
            Snippet cond = nullCaseCondition();
            if (canBePolyChild) {
                branch.controlFlow(
                        this,
                        "!$L.isObjectOpen(false) && " + cond.format(),
                        flatten(prototype.contextParameter().get(), cond.args()));
            } else {
                branch.controlFlow(this, cond.format(), cond.args());
            }
            addStatement(lhs.assign("null"));
            readNullCheckedObject(Branch.ELSE_IF, lastCase);
        } else {
            readNullCheckedObject(branch, lastCase);
        }
    }

    protected void readPrimitive(Branch branch, TypeMirror type, boolean lastCase) {
        String typeName;
        switch (type.getKind()) {
            case BOOLEAN -> {
                branch.controlFlow(this, booleanCaseCondition());
                typeName = "boolean";
            }
            case BYTE, SHORT, INT, LONG -> {
                branch.controlFlow(this, numberCaseCondition());
                typeName = "number";
            }
            case FLOAT, DOUBLE -> {
                branch.controlFlow(this, stringCaseCondition());
                readNumberFromString(type);
                ELSE_IF.controlFlow(this, numberCaseCondition());
                typeName = "number";
            }
            case CHAR -> {
                branch.controlFlow(this, stringCaseCondition());
                typeName = "string";
            }
            default -> throw new ContextedRuntimeException(type.getKind().toString());
        }
        if (type.getKind() == TypeKind.CHAR) {
            readCharFromString();
        } else {
            readPrimitive(type);
        }
        elseThrowUnexpected(typeName, lastCase);
    }

    private void readCharFromString() {
        ScopedVar stringVar = readStringInstead();
        beginControlFlow("if ($C.length() == 1)", stringVar);
        addStatement(lhs.assign("$C.charAt(0)", stringVar));
        nextControlFlow("else");
        addStatement("throw new $T()", IOException.class);
        endControlFlow();
    }

    private void readNumberFromString(TypeMirror type) {
        ScopedVar stringVar = readStringInstead();

        beginControlFlow("if ($C.equals($S))", stringVar, "NaN");
        addStatement(lhs.assign("$L.NaN", capitalize(type.toString())));

        nextControlFlow("else if ($C.equals($S))", stringVar, "Infinity");
        addStatement(lhs.assign("$L.POSITIVE_INFINITY", capitalize(type.toString())));

        nextControlFlow("else if ($C.equals($S))", stringVar, "-Infinity");
        addStatement(lhs.assign("$L.NEGATIVE_INFINITY", capitalize(type.toString())));

        nextControlFlow("else");
        addStatement("throw new $T()", IOException.class);
        endControlFlow();
    }

    private ScopedVar readStringInstead() {
        ScopedVar stringVar = createVariable("string");
        SELF nested = nest(
                utils.commonTypes.string,
                null,
                LHS.Variable.from(stringVar),
                false,
                config.propagateTo(PropagationKind.SUBSTITUTE));
        addStatement("$T $C", nested.type, stringVar);
        nested.readString(StringKind.STRING);
        return stringVar;
    }

    void readNullCheckedObject(Branch branch, boolean lastCase) {
        Optional<Creator> jsonCreatorMethod = utils.creators.findJsonCreatorMethod(type);
        if (jsonCreatorMethod.isPresent()) {
            if (jsonCreatorMethod.get() instanceof Creator.Converter c) {
                readFactory(branch, c.method(), lastCase);
            } else if (jsonCreatorMethod.get() instanceof Creator.Properties p) {
                readObject(branch, p, (TypeElement) ((DeclaredType) type).asElement(), lastCase);
            } else {
                throw Exceptions.unexpected();
            }
        } else if (utils.isBoxed(type)) {
            nest(utils.types.unboxedType(type), null, lhs, false, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .build(branch, true, lastCase);
        } else if (utils.commonTypes.isString(type) || utils.commonTypes.isArrayOf(type, TypeKind.CHAR)) {
            readString(branch, utils.commonTypes.isString(type) ? StringKind.STRING : StringKind.CHAR_ARRAY, lastCase);
        } else if (utils.commonTypes.isEnum(type)) {
            readEnum(branch, lastCase);
        } else if (type.getKind() == TypeKind.ARRAY) {
            readArray(branch, lastCase);
        } else if (utils.commonTypes.isIterable(type)) {
            readCollection(branch, lastCase);
        } else if (utils.commonTypes.isMap(type)) {
            readMap(branch, lastCase);
        } else {
            if (!(type instanceof DeclaredType dt)) {
                throw Exceptions.unexpected();
            }
            readObject(branch, null, (TypeElement) dt.asElement(), lastCase);
        }
    }

    private void readFactory(Branch branch, InstantiatedMethod method, boolean lastCase) {
        if (branch == ELSE_IF) {
            nextControlFlow("else");
        }
        Variable creatorArg = Variable.from(createVariable("creator"));
        addStatement(of("$T $L", method.parameters().get(0).type(), creatorArg.name));
        runWithContext(
                () -> nest(
                                method.parameters().get(0).type(),
                                null,
                                creatorArg,
                                true,
                                config.propagateTo(PropagationKind.SUBSTITUTE))
                        .build(IF, false, lastCase),
                "creator",
                method);
        addStatement(lhs.assign(of(
                "$C($L$C)",
                method.callSymbol(utils),
                creatorArg.name,
                joinPrependingCommaToEach(utils.delegation.findArguments(prototype, method, 1, generatedClass)))));
        if (branch == ELSE_IF) {
            endControlFlow();
        }
    }

    private void readString(Branch branch, StringKind stringKind, boolean lastCase) {
        branch.controlFlow(this, safeNonObjectCase(stringCaseCondition()));
        readString(stringKind);
        elseThrowUnexpected("string", lastCase);
    }

    private void readEnum(Branch branch, boolean lastCase) {
        branch.controlFlow(this, stringCaseCondition());
        {
            String enumValuesField = generatedClass.getOrCreateEnumField(type);
            Variable enumVar = Variable.from(createVariable("string"));
            addStatement("$T $L", utils.commonTypes.string, enumVar.name);
            nest(utils.commonTypes.string, null, enumVar, false, config.propagateTo(PropagationKind.SUBSTITUTE))
                    .readString(STRING);
            beginControlFlow("if ($L.containsKey($L))", enumValuesField, enumVar.name);
            addStatement(lhs.assign("$L.get($L)", enumValuesField, enumVar.name));
            elseThrowUnexpected("enum value", lastCase);
        }
        elseThrowUnexpected("string", lastCase);
    }

    private void readArray(Branch branch, boolean lastCase) {
        TypeMirror componentType = utils.commonTypes.getArrayComponentType(type);
        branch.controlFlow(this, arrayCaseCondition());
        {
            TypeMirror rawComponentType = utils.types.erasure(componentType);
            TypeMirror rawRawComponentType =
                    rawComponentType.getKind().isPrimitive() ? rawComponentType : utils.commonTypes.object;
            code.add("// Like ArrayList\n");
            ScopedVar varName = createVariable("array");
            addStatement(
                    "$T[] $C = $T.EMPTY_$L_ARRAY",
                    rawRawComponentType,
                    varName,
                    EmptyArrays.class,
                    rawRawComponentType.getKind().isPrimitive()
                            ? rawRawComponentType.toString().toUpperCase()
                            : "OBJECT");
            String len = createVariable("len").name();
            addStatement("int $L = 0", len);
            iterateOverElements();
            {
                beginControlFlow("if ($L == $C.length)", len, varName);
                code.add("// simplified version of ArrayList growth\n");
                addStatement(
                        "$C = $T.copyOf($C, $T.max(10, $C.length + ($C.length >> 1)))",
                        varName,
                        java.util.Arrays.class,
                        varName,
                        Math.class,
                        varName,
                        varName);
                endControlFlow();

                Exceptions.runWithContext(
                        () -> nest(
                                        componentType,
                                        Property.ITEM,
                                        new LHS.Array(varName.name(), len),
                                        true,
                                        config.propagateTo(PropagationKind.PROPERTY))
                                .build(Branch.IF, true, lastCase),
                        "component",
                        componentType);
            }
            endControlFlow(); // end of loop
            afterArray();
            if (componentType.getKind() == TypeKind.TYPEVAR) {
                Optional<Snippet> classParameter =
                        utils.generics.findClassParameter(prototype.asInstantiatedMethod(), type);
                if (classParameter.isEmpty()) {
                    throw new ContextedRuntimeException(
                            "You are trying to read a generic array. For this, you need the array class at runtime.\n"
                                    + " Add a parameter Class<%s> to %s.".formatted(type, prototype));
                }
                addStatement(lhs.assign("$T.copyOf($C, $L, $C)", Arrays.class, varName, len, classParameter.get()));
            } else if (utils.types.isSameType(rawRawComponentType, rawComponentType)) {
                addStatement(lhs.assign("$T.copyOf($C, $L)", Arrays.class, varName, len));
            } else {
                addStatement(lhs.assign("$T.copyOf($C, $L, $T[].class)", Arrays.class, varName, len, rawComponentType));
            }
        }
        if (componentType.getKind() == TypeKind.BYTE) {
            ELSE_IF.controlFlow(this, stringCaseCondition());
            ScopedVar stringVar = readStringInstead();
            addStatement(lhs.assign("$T.getDecoder().decode($C)", Base64.class, stringVar));
        }
        elseThrowUnexpected("array", lastCase);
    }

    private void readCollection(Branch branch, boolean lastCase) {
        branch.controlFlow(this, arrayCaseCondition());
        {
            TypeMirror componentType = utils.generics
                    .recordTypeBindingsFor((DeclaredType) type, utils.elements.getTypeElement(Iterable.class.getName()))
                    .values()
                    .iterator()
                    .next();
            TypeMirror collectionType = determineCollectionType();
            String varName = instantiateContainer(collectionType);

            iterateOverElements();
            {
                runWithContext(
                        () -> nest(
                                        componentType,
                                        ITEM,
                                        new Collection(varName),
                                        true,
                                        config.propagateTo(PropagationKind.PROPERTY))
                                .build(IF, true, lastCase),
                        "component",
                        componentType);
            }
            endControlFlow(); // end of loop
            afterArray();
            addStatement(lhs.assign("$L", varName));
        }
        elseThrowUnexpected("array", lastCase);
    }

    private TypeMirror determineCollectionType() {
        TypeMirror rawType = utils.types.erasure(type);
        if (!((DeclaredType) rawType).asElement().getModifiers().contains(Modifier.ABSTRACT)) {
            return rawType;
        } else if (utils.commonTypes.isAssignableTo(rawType, Set.class)) {
            return utils.types.erasure(utils.commonTypes.type(LinkedHashSet.class));
        } else if (utils.commonTypes.isAssignableTo(rawType, List.class)) {
            return utils.types.erasure(utils.commonTypes.type(ArrayList.class));
        } else {
            throw new ContextedRuntimeException(type.toString());
        }
    }

    private String instantiateContainer(TypeMirror collectionType) {
        if (lhs instanceof LHS.Variable v) {
            addStatement("$L = new $T<>()", v.name(), collectionType);
            return v.name();
        } else {
            ScopedVar variable = createVariable("container");
            addStatement("$T $C = new $T<>()", type, variable, collectionType);
            return variable.name();
        }
    }

    private void readMap(Branch branch, boolean lastCase) {
        branch.controlFlow(this, objectCaseCondition());
        {
            TypeMirror[] typeBindings = utils.generics
                    .recordTypeBindingsFor((DeclaredType) type, utils.commonTypes.elem(Map.class))
                    .values()
                    .toArray(TypeMirror[]::new);
            TypeMirror keyType = typeBindings[0];
            if (!utils.types.isSameType(keyType, utils.commonTypes.string)) {
                throw new ContextedRuntimeException("Only String keys supported for now.");
            }
            TypeMirror valueType = typeBindings[1];
            TypeMirror mapType = determineMapType();
            String varName = instantiateContainer(mapType);
            iterateOverFields();
            {
                IF.controlFlow(this, fieldCaseCondition());
                String keyVar = createVariable("key").name();
                readFieldNameInIteration(keyVar);
                Exceptions.runWithContext(
                        () -> nest(
                                        valueType,
                                        Property.VALUE,
                                        new LHS.Map(varName, keyVar),
                                        true,
                                        config.propagateTo(PropagationKind.PROPERTY))
                                .build(Branch.IF, true, lastCase),
                        "value",
                        valueType);
                elseThrowUnexpected("field name", lastCase);
            }
            endControlFlow(); // end of loop
            afterObject();

            if (!(lhs instanceof LHS.Variable)) {
                addStatement(lhs.assign("$L", varName));
            }
        }
        elseThrowUnexpected("object", lastCase);
    }

    private TypeMirror determineMapType() {
        TypeMirror rawType = utils.types.erasure(type);
        if (!((DeclaredType) rawType).asElement().getModifiers().contains(Modifier.ABSTRACT)) {
            return rawType;
        } else {
            return utils.types.erasure(utils.commonTypes.type(LinkedHashMap.class));
        }
    }

    private void readObject(
            Branch branch, @Nullable Creator.Properties properties, TypeElement element, boolean lastCase) {
        Snippet cond = objectCaseCondition();
        if (canBePolyChild) {
            cond = Snippet.of(
                    "$L.isObjectOpen(true) || $C", prototype.contextParameter().get(), cond);
        }
        branch.controlFlow(this, cond);

        if (properties != null) {
            readCreator(properties.method(), lastCase);
        } else {
            Polymorphism.of(element, utils)
                    .ifPresentOrElse(
                            polymorphism -> readPolymorphicObject(polymorphism, element, lastCase),
                            () -> readObjectFields(element, lastCase));
        }

        elseThrowUnexpected("object", lastCase);
    }

    private void readPolymorphicObject(Polymorphism polymorphism, TypeElement element, boolean lastCase) {
        LHS.Variable discriminator = LHS.Variable.from(createVariable("discriminator"));
        addStatement("$T $L = null", utils.commonTypes.string, discriminator.name());
        nest(
                        utils.commonTypes.string,
                        new Property("discriminator", polymorphism.discriminator(), null),
                        discriminator,
                        false,
                        config.propagateTo(PropagationKind.PROPERTY))
                .readDiscriminator(polymorphism.discriminator());
        Branch branch = Branch.IF;
        for (Polymorphism.Child child : polymorphism.children()) {
            branch.controlFlow(this, "$L.equals($S)", discriminator.name(), child.name());
            SELF nested = nest(
                    child.type(),
                    Property.INSTANCE,
                    lhs,
                    true,
                    config.propagateTo(
                            PropagationKind.SUBSTITUTE /* this is fine with the configuration options that we
                 currently have */));
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
                                addStatement(
                                        "$L.markObjectOpen()",
                                        prototype.contextParameter().get());
                                Exceptions.runWithContext(
                                        () -> nested.invokeDelegate(delegatee.fieldOrParameter(), delegatee.method()),
                                        "instance",
                                        child.type());
                            },
                            () -> {
                                Exceptions.runWithContext(
                                        () -> utils.creators
                                                .findJsonCreatorMethod(child.type())
                                                .map(c -> c instanceof Creator.Properties p ? p : null)
                                                .ifPresentOrElse(
                                                        c -> nested.readCreator(c.method(), lastCase),
                                                        () -> nested.readObjectFields(
                                                                (TypeElement) ((DeclaredType) nested.type).asElement(),
                                                                lastCase)),
                                        "instance",
                                        child.type());
                            });
            branch = Branch.ELSE_IF;
        }
        if (branch == Branch.IF) {
            throw new ContextedRuntimeException("No children for " + type);
        }
        nextControlFlow("else");
        addStatement("throw new $T($S + $L)", IOException.class, "Unknown type ", discriminator.name());
        endControlFlow(); // ends the loop
    }

    void readObjectFields(TypeElement element, boolean lastCase) {
        if (element.getKind() == ElementKind.RECORD) {
            Map<TypeVar, TypeMirror> typeBindings = utils.generics.recordTypeBindings((DeclaredType) type);
            InstantiatedMethod instantiatedConstructor = utils.generics.instantiateMethod(
                    ElementFilter.constructorsIn(element.getEnclosedElements()).get(0),
                    typeBindings,
                    LocationKind.CREATOR);
            readCreator(instantiatedConstructor, lastCase);
        } else {
            readObjectFromAccessors(lastCase);
        }
    }

    void readCreator(InstantiatedMethod method, boolean lastCase) {
        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addReader(prototype, type);

        List<SELF> nested = new ArrayList<>();
        AnyConfig creatorConfig = method.config().merge(config);

        for (InstantiatedVariable parameter : method.parameters()) {
            AnyConfig propertyConfig = parameter.config().merge(creatorConfig);

            // use parameter name as variable name because we know it is valid - unlike the configured property name
            String varName = createVariable(parameter.name()).name();
            String propertyName = resolvePropertyName(propertyConfig, parameter.name());

            SELF nest = nest(
                    parameter.type(),
                    new Property(parameter.name(), propertyName, propertyConfig),
                    new Variable(varName),
                    true,
                    propertyConfig.propagateTo(PropagationKind.PROPERTY));
            Snippet defaultValue = utils.defaultValues
                    .findInputDefaultValue(prototype.blueprint(), nest.type, propertyConfig)
                    .map(m -> of("$C()", m.callSymbol(utils)))
                    .orElse(of("$L", utils.commonTypes.getNullValueRaw(nest.type)));
            addStatement(of("$T $L = $C", nest.type, varName, defaultValue));
            if (propertyConfig.resolveProperty(IGNORE_PROPERTY).value()) {
                // we do need the default value to call the creator, so we only skip reading the value
                continue;
            }
            verificationForDto.addProperty(propertyName, parameter.type(), propertyConfig);
            nested.add(nest);
        }
        ScopedVar idVar = readProperties(nested, lastCase);
        String args = nested.stream().map(p -> ((Variable) p.lhs).name()).collect(joining(", "));
        Snippet creatorCall = of("$C($L)", method.callSymbol(utils), args);
        utils.references
                .resolveSetup(config, prototype, type)
                .ifPresentOrElse(
                        setup -> lhs.assignAnd(
                                creatorCall, this, type, objectVar -> addStatement(setup.bindItem(idVar, objectVar))),
                        () -> addStatement(lhs.assign(creatorCall)));
    }

    private void readObjectFromAccessors(boolean lastCase) {
        ProtoAndProps verificationForDto = generatedClass.verificationForBlueprint.addReader(prototype, type);

        List<SELF> nested = new ArrayList<>();
        ScopedVar objectVar = createVariable("object");
        addStatement("$T $C = new $T()", type, objectVar, type);
        utils.properties.listWriteAccessors(type).forEach((canonicalPropertyName, accessor) -> {
            AnyConfig propertyConfig = fromAccessorConsideringField(
                            accessor, accessor.name(), type, canonicalPropertyName, utils)
                    .merge(config);
            if (propertyConfig.resolveProperty(IGNORE_PROPERTY).value()) {
                return;
            }

            LHS lhs = from(accessor, objectVar.name());
            String propertyName = resolvePropertyName(propertyConfig, canonicalPropertyName);
            verificationForDto.addProperty(propertyName, accessor.type(), propertyConfig);
            SELF nest = nest(
                    accessor.type(),
                    new Property(canonicalPropertyName, propertyName, propertyConfig),
                    lhs,
                    true,
                    propertyConfig.propagateTo(PropagationKind.PROPERTY));
            nested.add(nest);
        });
        ScopedVar idVar = readProperties(nested, lastCase);
        utils.references.resolveSetup(config, prototype, type).ifPresent(setup -> {
            addStatement(setup.bindItem(idVar, objectVar));
        });
        addStatement(lhs.assign("$C", objectVar));
    }

    private ScopedVar readProperties(List<SELF> properties, boolean lastCase) {
        Optional<Setup> referencesSetup = utils.references.resolveSetup(config, prototype, type);
        ScopedVar idVar = referencesSetup
                .map(setup -> {
                    ScopedVar variable = createVariable("id");
                    TypeMirror idType = setup.finalIdType(type, utils);
                    addStatement("$T $C = null", idType, variable);
                    return variable;
                })
                .orElse(null);

        List<SELF> requiredProperties = properties.stream()
                .filter(p -> RequiredProperty.isRequired(p.property.config()))
                .toList();
        Map<String, ScopedVar> propertyPresentByCanonicalName = createPropertyPresentBooleans(requiredProperties);

        iterateOverFields();
        Branch.IF.controlFlow(this, fieldCaseCondition());
        String fieldVar = createVariable("field").name();
        readFieldNameInIteration(fieldVar);

        Set<String> ignoredProperties =
                config.resolveProperty(IgnoreProperties.IGNORED_PROPERTIES).value();

        beginControlFlow("switch($L)", fieldVar);
        for (SELF nest : properties) {
            if (ignoredProperties.contains(nest.property.serializedName())) {
                continue;
            }
            beginControlFlow("case $S:", nest.property.serializedName());
            Exceptions.runWithContext(
                    () -> {
                        if (referencesSetup
                                .filter(setup -> setup.isPropertyBased()
                                        && setup.property().equals(nest.property.serializedName()))
                                .isPresent()) {
                            SELF tmpNest = nest(
                                    nest.type,
                                    nest.property,
                                    Variable.from(idVar),
                                    nest.stackRelevantType,
                                    nest.config);
                            tmpNest.build(IF, true, lastCase);
                            addStatement(nest.lhs.assign(idVar));
                        } else {
                            nest.build(Branch.IF, true, lastCase);
                        }
                    },
                    "property",
                    nest.property.serializedName());
            ScopedVar presentVar = propertyPresentByCanonicalName.get(nest.property.canonicalName());
            if (presentVar != null) {
                addStatement("$C = true", presentVar);
            }
            addStatement("break");
            endControlFlow();
        }
        if (!ignoredProperties.isEmpty()) {
            beginControlFlow(Snippet.of("case $C:", IgnoreProperties.toSnippet(ignoredProperties)));
            skipValue();
            addStatement("break");
            endControlFlow();
        }
        referencesSetup.filter(setup -> !setup.isPropertyBased()).ifPresent(setup -> {
            beginControlFlow("case $S:", setup.property());
            nest(
                            setup.idType(),
                            new Property(setup.property(), setup.property(), null),
                            LHS.Variable.from(idVar),
                            true,
                            config.propagateTo(PropagationKind.PROPERTY))
                    .build(IF, true, lastCase);
            addStatement("break");
            endControlFlow();
        });
        {
            beginControlFlow("default:");
            if (UnknownProperties.shouldThrow(config)) {
                addStatement("throw new $T($S + $L + $S)", IOException.class, "Unrecognized field \"", fieldVar, "\"");
            } else {
                skipValue();
            }
            endControlFlow();
        }
        endControlFlow(); // ends the last field
        elseThrowUnexpected("field name", lastCase);
        endControlFlow(); // ends the loop

        for (SELF property : requiredProperties) {
            addStatement(
                    "if (!$C) throw new $T($S)",
                    propertyPresentByCanonicalName.get(property.property.canonicalName()),
                    IOException.class,
                    "Missing property " + property.property.serializedName());
        }

        afterObject();
        return idVar;
    }

    private Map<String, ScopedVar> createPropertyPresentBooleans(List<SELF> requiredProperties) {
        Map<String, ScopedVar> propertyPresentByCanonicalName = requiredProperties.stream()
                .collect(toMap(
                        p -> p.property.canonicalName(),
                        p -> createVariable(p.property.canonicalName() + "Present"),
                        (x, y) -> {
                            throw Exceptions.unexpected();
                        },
                        LinkedHashMap::new));

        if (!requiredProperties.isEmpty()) {
            addStatement(
                    "boolean $C",
                    Snippet.join(
                            propertyPresentByCanonicalName.values().stream()
                                    .map(v -> Snippet.of("$C = false", v))
                                    .toList(),
                            ", "));
        }
        return propertyPresentByCanonicalName;
    }

    private void elseThrowUnexpected(String typeName, boolean lastCase) {
        if (lastCase) {
            nextControlFlow("else");
            throwUnexpected(typeName);
            endControlFlow();
        }
    }

    private Snippet safeNonObjectCase(Snippet leCase) {
        return prototype
                .contextParameter()
                .map(ctx -> Snippet.of("!$L.isObjectOpen(false) && $C", ctx, leCase))
                .orElse(leCase);
    }

    protected abstract void initializeParser();

    protected abstract Snippet fieldCaseCondition();

    protected abstract Snippet stringCaseCondition();

    protected abstract Snippet numberCaseCondition();

    protected abstract Snippet objectCaseCondition();

    protected abstract Snippet arrayCaseCondition();

    protected abstract Snippet booleanCaseCondition();

    protected abstract Snippet nullCaseCondition();

    protected abstract void readPrimitive(TypeMirror type);

    protected abstract void readString(StringKind stringKind);

    protected abstract void readFieldNameInIteration(String propertyName);

    protected abstract void iterateOverFields();

    protected abstract void skipValue();

    protected abstract void afterObject();

    protected abstract void readDiscriminator(String propertyName);

    protected abstract void iterateOverElements();

    protected abstract void afterArray();

    protected abstract void throwUnexpected(String expected);

    protected abstract void invokeDelegate(String instance, InstantiatedMethod callee);

    protected abstract SELF nest(
            TypeMirror type, @Nullable Property property, LHS lhs, boolean stackRelevantType, AnyConfig config);

    protected sealed interface LHS {
        default Snippet assign(Snippet s) {
            return assign(s.format(), s.args());
        }

        default Snippet assign(String string, Object... args) {
            if (this instanceof Return) {
                return Snippet.of("return " + string, args);
            } else if (this instanceof Variable v) {
                return Snippet.of("$L = " + string, flatten(v.name(), args));
            } else if (this instanceof Array a) {
                return Snippet.of("$L[$L++] = " + string, flatten(a.arrayVar(), a.indexVar(), args));
            } else if (this instanceof Collection c) {
                return Snippet.of("$L.add(" + string + ")", flatten(c.variable(), args));
            } else if (this instanceof Map m) {
                return Snippet.of("$L.put($L, " + string + ")", flatten(m.mapVar(), m.keyVar(), args));
            } else if (this instanceof Field f) {
                return Snippet.of("$L.$L = " + string, flatten(f.objectVar(), f.fieldName(), args));
            } else if (this instanceof Setter s) {
                return Snippet.of("$L.$L(" + string + ")", flatten(s.objectVar(), s.methodName(), args));
            } else {
                throw new ContextedRuntimeException(this.toString());
            }
        }

        default void assignAnd(
                Snippet rhs, AbstractCodeGeneratorStack stack, TypeMirror t, Consumer<Snippet> tmpAction) {
            if (this instanceof Variable v) {
                stack.addStatement(assign(rhs));
                tmpAction.accept(v);
            } else if (this instanceof Field f) {
                stack.addStatement(assign(rhs));
                tmpAction.accept(f);
            } else {
                ScopedVar tmp = stack.createVariable("tmp");
                stack.addStatement("$T $C = $C", t, tmp, rhs);
                tmpAction.accept(tmp);
                stack.addStatement(assign(tmp));
            }
        }

        static LHS from(WriteAccessor a, String objectVar) {
            return switch (a.kind()) {
                case FIELD -> new Field(objectVar, a.element().getSimpleName().toString());
                case SETTER -> new Setter(objectVar, a.element().getSimpleName().toString());
                default -> throw new ContextedRuntimeException(a.kind().toString());
            };
        }

        record Return() implements LHS {}

        record Variable(String name) implements LHS, Snippet {
            static Variable from(ScopedVar v) {
                return new Variable(v.name());
            }

            @Override
            public String format() {
                return "$L";
            }

            @Override
            public Object[] args() {
                return new Object[] {name};
            }
        }

        record Array(String arrayVar, String indexVar) implements LHS {}

        record Collection(String variable) implements LHS {}

        record Map(String mapVar, String keyVar) implements LHS {}

        record Field(String objectVar, String fieldName) implements LHS, Snippet {
            @Override
            public String format() {
                return "$L.$L";
            }

            @Override
            public Object[] args() {
                return new Object[] {objectVar, fieldName};
            }
        }

        record Setter(String objectVar, String methodName) implements LHS {}
    }

    protected enum Branch {
        IF,
        ELSE_IF,
        ;

        <T extends AbstractCodeGeneratorStack<T>> AbstractCodeGeneratorStack<T> controlFlow(
                AbstractCodeGeneratorStack<T> code, String s, Object... args) {
            return switch (this) {
                case IF -> code.beginControlFlow("if (" + s + ")", args);
                case ELSE_IF -> code.nextControlFlow("else if (" + s + ")", args);
            };
        }

        <T extends AbstractCodeGeneratorStack<T>> AbstractCodeGeneratorStack<T> controlFlow(
                AbstractCodeGeneratorStack<T> code, Snippet snippet) {
            return switch (this) {
                case IF -> code.beginControlFlow("if ($C)", snippet);
                case ELSE_IF -> code.nextControlFlow("else if ($C)", snippet);
            };
        }
    }
}
