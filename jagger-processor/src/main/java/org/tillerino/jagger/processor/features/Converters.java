package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.util.Expr.e;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import java.util.function.Function;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.helpers.CharacterHelper;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeInScope;

public class Converters {
    public static final String JSON_INPUT_CONVERTER = "org.tillerino.jagger.annotations.JsonInputConverter";
    public static final String JSON_OUTPUT_CONVERTER = "org.tillerino.jagger.annotations.JsonOutputConverter";

    protected final JaggerContext ctx;

    public Converters(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public Optional<Converter> findInputConverter(
            JaggerPrototype caller,
            TypeMirror targetType,
            AnyConfig config,
            GeneratedClass generatedClass,
            Set<String> avn) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (PrototypeInScope prototypeInScope : caller.listPrototypesInScope(config)) {
            if (!(prototypeInScope.prototype().kind() instanceof ConverterPrototypeKind converter)
                    || converter.direction != Direction.INPUT) {
                continue;
            }
            InstantiatedMethod method = prototypeInScope.prototype().method();
            typeBindings.clear();
            if (ctx.generics.typeBindingsSatisfyingEquality(
                    targetType, method.returnType(), typeBindings, method.freeTypeVars(), true)) {
                PrototypeInScope boundMethod = prototypeInScope.applyTypeBindings(typeBindings);
                JaggerPrototype prototype = boundMethod.prototype();
                return Optional.of(new Converter(
                        toConvert -> boundMethod.call(caller, List.of(toConvert), generatedClass, avn),
                        prototype.method().unaryOperatorReference(),
                        prototype.returnType(),
                        prototype.parameters().get(0).type(),
                        converter.nullable));
            }
        }
        return Optional.empty();
    }

    public Optional<Converter> findOutputConverter(
            TypeMirror toConvertType,
            JaggerPrototype caller,
            AnyConfig config,
            GeneratedClass generatedClass,
            Set<String> avn) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (PrototypeInScope prototypeInScope : caller.listPrototypesInScope(config)) {
            if (!(prototypeInScope.prototype().kind() instanceof ConverterPrototypeKind converter)
                    || converter.direction != Direction.OUTPUT) {
                continue;
            }
            InstantiatedMethod method = prototypeInScope.prototype().method();
            typeBindings.clear();
            if (ctx.generics.typeBindingsSatisfyingEquality(
                    toConvertType, method.parameters().get(0).type(), typeBindings, method.freeTypeVars(), true)) {
                PrototypeInScope boundMethod = prototypeInScope.applyTypeBindings(typeBindings);
                return Optional.of(new Converter(
                        toConvert -> boundMethod.call(caller, List.of(toConvert), generatedClass, avn),
                        boundMethod.prototype().method().unaryOperatorReference(),
                        boundMethod.prototype().parameters().get(0).type(),
                        boundMethod.prototype().returnType(),
                        converter.nullable));
            }
        }
        return Optional.empty();
    }

    public Optional<Expr> instantiateStringConverter(
            TypeMirror targetType,
            InstantiatedMethod targetMethod,
            ConverterPrototypeKind kind,
            GeneratedClass generatedClass) {
        if (!ctx.commonTypes.isString(kind.externalType)) {
            return Optional.empty();
        }
        String parameterName = targetMethod.parameters().get(0).name();
        if (ctx.commonTypes.isString(kind.internalType)) {
            return Optional.of(e(targetType, "$L -> $L", parameterName, parameterName));
        }
        return kind.direction == Direction.INPUT
                ? instantiateFromStringConverter(targetType, kind.internalType, parameterName, generatedClass)
                : instantiateToStringConverter(targetType, kind.internalType, parameterName, generatedClass);
    }

    public Optional<Expr> instantiateToStringConverter(
            TypeMirror functionalInterface,
            TypeMirror internalType,
            String parameterName,
            GeneratedClass generatedClass) {

        if (ctx.commonTypes.isBoxed(internalType)) {
            return Optional.of(
                    e(functionalInterface, "$L -> $T.toString($L, null)", parameterName, Objects.class, parameterName));
        }
        if (ctx.commonTypes.isEnum(internalType)) {
            Expr enumValue = e(internalType, "$L", parameterName);
            return Optional.of(e(
                    functionalInterface,
                    "$L -> $C",
                    parameterName,
                    Enums.serializationCode(generatedClass, internalType, enumValue)));
        }
        return Optional.empty();
    }

    public Optional<Expr> instantiateFromStringConverter(
            TypeMirror functionalInterface,
            TypeMirror internalType,
            String parameterName,
            GeneratedClass generatedClass) {

        if (ctx.commonTypes.isBoxed(internalType)) {
            if (ctx.types.isSameType(internalType, ctx.commonTypes.boxedChar)) {
                return Optional.of(e(functionalInterface, "$T::toCharacter", CharacterHelper.class));
            }
            return Optional.of(e(functionalInterface, "$T::valueOf", internalType));
        }
        if (ctx.commonTypes.isEnum(internalType)) {
            Expr key = e(ctx.commonTypes.string, "$L", parameterName);
            return Optional.of(e(
                    functionalInterface,
                    "$L -> $C",
                    parameterName,
                    Enums.deserializationCode(generatedClass, internalType, key)));
        }
        return Optional.empty();
    }

    public record Converter(
            Function<Expr, Expr> converter,
            Optional<Expr> methodReference,
            TypeMirror internalType,
            TypeMirror externalType,
            boolean nullable) {
        public Expr lambda() {
            return methodReference.orElseGet(() -> e(null, "e -> $C", converter.apply(e(null, "e"))));
        }
    }

    public record ConverterPrototypeKind(
            Direction direction, TypeMirror internalType, TypeMirror externalType, boolean nullable)
            implements PrototypeKind {
        @Override
        public CodeBlock.Builder generateCode(CodeGeneratorContext context) {
            throw new UnsupportedOperationException("Converter prototypes are not generated");
        }

        @Override
        public boolean isGenerated() {
            return false;
        }
    }

    public enum Direction {
        INPUT,
        OUTPUT,
        ;
    }

    public static class ConverterPrototypeDetector implements PrototypeDetector {
        @Override
        public Collection<String> supportedAnnotationTypes() {
            return List.of(JSON_INPUT_CONVERTER, JSON_OUTPUT_CONVERTER);
        }

        @Override
        public Optional<PrototypeKind> detect(InstantiatedMethod method, AnnotationMirrorWrapper annotation) {
            if (method.parameters().size() != 1) {
                return Optional.empty();
            }
            InstantiatedVariable firstParameter = method.parameters().get(0);
            String annotationType = annotation.mirror().getAnnotationType().toString();
            if (annotationType.equals(JSON_INPUT_CONVERTER)) {
                return Optional.of(new ConverterPrototypeKind(
                        Direction.INPUT, method.returnType(), firstParameter.type(), firstParameter.isNullable()));
            }
            if (annotationType.equals(JSON_OUTPUT_CONVERTER)) {
                return Optional.of(new ConverterPrototypeKind(
                        Direction.OUTPUT, firstParameter.type(), method.returnType(), firstParameter.isNullable()));
            }
            return Optional.empty();
        }
    }
}
