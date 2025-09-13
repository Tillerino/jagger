package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Optional;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;

public sealed interface PrototypeKind {
    String JACKSON_JSON_GENERATOR = "com.fasterxml.jackson.core.JsonGenerator";
    String JACKSON_JSON_PARSER = "com.fasterxml.jackson.core.JsonParser";

    String GSON_JSON_READER = "com.google.gson.stream.JsonReader";
    String GSON_JSON_WRITER = "com.google.gson.stream.JsonWriter";

    String FASTJSON_2_JSONREADER = "com.alibaba.fastjson2.JSONReader";
    String FASTJSON_2_JSONWRITER = "com.alibaba.fastjson2.JSONWriter";

    String JAKARTA_JSON_PARSER = "org.tillerino.jagger.helpers.JakartaJsonParserHelper.JsonParserWrapper";
    String JAKARTA_JSON_GENERATOR = "jakarta.json.stream.JsonGenerator";

    String NANOJSON_JSON_WRITER = "com.grack.nanojson.JsonAppendableWriter";

    String JAGGER_READER = "org.tillerino.jagger.api.JaggerReader";
    String JAGGER_WRITER = "org.tillerino.jagger.api.JaggerWriter";

    default Direction direction() {
        return this instanceof Input ? Direction.INPUT : Direction.OUTPUT;
    }

    TypeMirror jsonType();

    TypeMirror javaType();

    List<InstantiatedMethod.InstantiatedVariable> otherParameters();

    default String defaultMethodName() {
        String prefix = this instanceof Input ? "read" : "write";
        return prefix + simpleTypeName(javaType());
    }

    PrototypeKind withJavaType(TypeMirror newType);

    default boolean matchesWithJavaType(PrototypeKind other, TypeMirror javaType, AnnotationProcessorUtils utils) {
        return direction() == other.direction()
                && utils.types.isSameType(jsonType(), other.jsonType())
                && utils.types.isSameType(javaType(), javaType);
    }

    static Optional<PrototypeKind> of(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        if (m.element().getAnnotation(JsonInput.class) != null
                && m.returnType().getKind() != TypeKind.VOID
                && !m.parameters().isEmpty()) {
            if (List.of(JACKSON_JSON_PARSER, GSON_JSON_READER, FASTJSON_2_JSONREADER, JAKARTA_JSON_PARSER)
                    .contains(m.parameters().get(0).type().toString())) {
                return Optional.of(new Input(
                        m.parameters().get(0).type(),
                        m.returnType(),
                        m.parameters().subList(1, m.parameters().size())));
            }
            TypeMirror jaggerReaderRaw = utils.types.erasure(
                    utils.elements.getTypeElement(JAGGER_READER).asType());
            if (utils.types.isAssignable(m.parameters().get(0).type(), jaggerReaderRaw)) {
                return Optional.of(new Input(
                        jaggerReaderRaw,
                        m.returnType(),
                        m.parameters().subList(1, m.parameters().size())));
            }
        }
        if (m.element().getAnnotation(JsonOutput.class) != null
                && m.returnType().getKind() == TypeKind.VOID
                && m.parameters().size() >= 2) {
            if (List.of(
                            JACKSON_JSON_GENERATOR,
                            GSON_JSON_WRITER,
                            FASTJSON_2_JSONWRITER,
                            JAKARTA_JSON_GENERATOR,
                            NANOJSON_JSON_WRITER)
                    .contains(m.parameters().get(1).type().toString())) {
                return Optional.of(new Output(
                        m.parameters().get(1).type(),
                        m.parameters().get(0).type(),
                        m.parameters().subList(2, m.parameters().size())));
            }
            TypeMirror jaggerWriterRaw = utils.types.erasure(
                    utils.elements.getTypeElement(JAGGER_WRITER).asType());
            if (utils.types.isAssignable(m.parameters().get(1).type(), jaggerWriterRaw)) {
                return Optional.of(new Output(
                        jaggerWriterRaw,
                        m.parameters().get(0).type(),
                        m.parameters().subList(2, m.parameters().size())));
            }
        }
        return Optional.empty();
    }

    static String simpleTypeName(TypeMirror t) {
        if (t.getKind().isPrimitive()) {
            return "Primitive" + StringUtils.capitalize(t.toString());
        }

        if (t instanceof ArrayType a) {
            return "ArrayOf" + simpleTypeName(a.getComponentType());
        }

        if (!(t instanceof DeclaredType d)) {
            throw new ContextedRuntimeException("Only primitives or declared types expected").addContextValue("t", t);
        }

        return d.asElement().getSimpleName().toString();
    }

    record Input(
            TypeMirror jsonType, TypeMirror javaType, List<InstantiatedMethod.InstantiatedVariable> otherParameters)
            implements PrototypeKind {
        @Override
        public PrototypeKind withJavaType(TypeMirror newType) {
            return new Input(jsonType, newType, otherParameters);
        }
    }

    record Output(
            TypeMirror jsonType, TypeMirror javaType, List<InstantiatedMethod.InstantiatedVariable> otherParameters)
            implements PrototypeKind {
        @Override
        public PrototypeKind withJavaType(TypeMirror newType) {
            return new Output(jsonType, newType, otherParameters);
        }
    }

    enum Direction {
        INPUT,
        OUTPUT;
    }
}
