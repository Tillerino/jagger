package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Optional;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.*;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

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
    String JAVA_SQL_CONNECTION = "java.sql.Connection";
    String JAVA_SQL_RESULT_SET = "java.sql.ResultSet";

    Direction direction();

    TypeMirror jsonType();

    TypeMirror javaType();

    List<InstantiatedMethod.InstantiatedVariable> otherParameters();

    default String defaultMethodName() {
        String prefix = direction() == Direction.INPUT ? "read" : "write";
        return prefix + simpleTypeName(javaType());
    }

    PrototypeKind withJavaType(TypeMirror newType);

    default boolean matchesWithJavaType(PrototypeKind other, TypeMirror javaType, AnnotationProcessorUtils utils) {
        return direction() == other.direction()
                && utils.types.isSameType(jsonType(), other.jsonType())
                && utils.types.isSameType(javaType(), javaType);
    }

    static Optional<PrototypeKind> of(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        return detectJsonInput(m, utils)
                .or(() -> detectJsonOutput(m, utils))
                .or(() -> detectJdbcSelect(m, utils))
                .or(() -> detectJdbcInsert(m, utils))
                .or(() -> detectJdbcUpdate(m, utils));
    }

    private static Optional<PrototypeKind> detectJsonInput(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        if (m.element().getAnnotation(org.tillerino.jagger.annotations.JsonInput.class) != null
                && m.returnType().getKind() != TypeKind.VOID
                && !m.parameters().isEmpty()) {
            if (List.of(JACKSON_JSON_PARSER, GSON_JSON_READER, FASTJSON_2_JSONREADER, JAKARTA_JSON_PARSER)
                    .contains(m.parameters().get(0).type().toString())) {
                return Optional.of(new JsonInput(
                        m.parameters().get(0).type(),
                        m.returnType(),
                        m.parameters().subList(1, m.parameters().size())));
            }
            TypeMirror jaggerReaderRaw = utils.types.erasure(
                    utils.elements.getTypeElement(JAGGER_READER).asType());
            if (utils.commonTypes.isAssignable(m.parameters().get(0).type(), jaggerReaderRaw)) {
                return Optional.of(new JsonInput(
                        jaggerReaderRaw,
                        m.returnType(),
                        m.parameters().subList(1, m.parameters().size())));
            }
        }
        return Optional.empty();
    }

    private static Optional<PrototypeKind> detectJsonOutput(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        if (m.element().getAnnotation(org.tillerino.jagger.annotations.JsonOutput.class) != null
                && m.returnType().getKind() == TypeKind.VOID
                && m.parameters().size() >= 2) {
            if (List.of(
                            JACKSON_JSON_GENERATOR,
                            GSON_JSON_WRITER,
                            FASTJSON_2_JSONWRITER,
                            JAKARTA_JSON_GENERATOR,
                            NANOJSON_JSON_WRITER)
                    .contains(m.parameters().get(1).type().toString())) {
                return Optional.of(new JsonOutput(
                        m.parameters().get(1).type(),
                        m.parameters().get(0).type(),
                        m.parameters().subList(2, m.parameters().size())));
            }
            TypeMirror jaggerWriterRaw = utils.types.erasure(
                    utils.elements.getTypeElement(JAGGER_WRITER).asType());
            if (utils.commonTypes.isAssignable(m.parameters().get(1).type(), jaggerWriterRaw)) {
                return Optional.of(new JsonOutput(
                        jaggerWriterRaw,
                        m.parameters().get(0).type(),
                        m.parameters().subList(2, m.parameters().size())));
            }
        }
        return Optional.empty();
    }

    private static Optional<PrototypeKind> detectJdbcSelect(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        JdbcSelect annotation = m.element().getAnnotation(JdbcSelect.class);
        if (annotation != null
                && m.returnType().getKind() != TypeKind.VOID
                && !m.parameters().isEmpty()) {
            if (List.of(JAVA_SQL_CONNECTION, JAVA_SQL_RESULT_SET)
                    .contains(m.parameters().get(0).type().toString())) {
                return Optional.of(new JdbcPrototypeKind(
                        m.parameters().get(0).type(),
                        m.returnType(),
                        m.parameters().get(0),
                        variablesExcept(m.parameters(), JAVA_SQL_CONNECTION),
                        Direction.JDBC_SELECT,
                        annotation.value()));
            }
        }
        return Optional.empty();
    }

    private static Optional<PrototypeKind> detectJdbcInsert(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        JdbcInsert annotation = m.element().getAnnotation(JdbcInsert.class);
        if (annotation != null
                && m.returnType().getKind() == TypeKind.VOID
                && m.parameters().size() >= 2) {
            if (variablesContain(m.parameters(), JAVA_SQL_CONNECTION)) {
                List<InstantiatedVariable> otherParameters = variablesExcept(m.parameters(), JAVA_SQL_CONNECTION);
                return Optional.of(new JdbcPrototypeKind(
                        utils.commonTypes.connection,
                        otherParameters.stream()
                                .map(InstantiatedVariable::type)
                                .findFirst()
                                .orElse(null),
                        variableOfType(m.parameters(), JAVA_SQL_CONNECTION),
                        otherParameters,
                        Direction.JDBC_INSERT,
                        annotation.value()));
            }
        }
        return Optional.empty();
    }

    private static Optional<PrototypeKind> detectJdbcUpdate(InstantiatedMethod m, AnnotationProcessorUtils utils) {
        JdbcUpdate annotation = m.element().getAnnotation(JdbcUpdate.class);
        if (annotation != null
                && m.returnType().getKind() == TypeKind.VOID
                && !m.parameters().isEmpty()) {
            if (variablesContain(m.parameters(), JAVA_SQL_CONNECTION)) {
                List<InstantiatedVariable> otherParameters = variablesExcept(m.parameters(), JAVA_SQL_CONNECTION);
                return Optional.of(new JdbcPrototypeKind(
                        utils.commonTypes.connection,
                        otherParameters.stream()
                                .map(InstantiatedVariable::type)
                                .findFirst()
                                .orElse(null),
                        variableOfType(m.parameters(), JAVA_SQL_CONNECTION),
                        otherParameters,
                        Direction.JDBC_UPDATE,
                        annotation.value()));
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

    static boolean variablesContain(List<InstantiatedVariable> variables, String fullyQualified) {
        for (InstantiatedVariable variable : variables) {
            if (variable.type().toString().equals(fullyQualified)) {
                return true;
            }
        }
        return false;
    }

    static InstantiatedVariable variableOfType(List<InstantiatedVariable> variables, String fullyQualified) {
        for (InstantiatedVariable variable : variables) {
            if (variable.type().toString().equals(fullyQualified)) {
                return variable;
            }
        }
        throw new IllegalStateException();
    }

    static List<InstantiatedVariable> variablesExcept(
            List<InstantiatedVariable> variables, String excludeFullyQualified) {
        return variables.stream()
                .filter(v -> !v.type().toString().equals(excludeFullyQualified))
                .toList();
    }

    record JsonInput(
            TypeMirror jsonType, TypeMirror javaType, List<InstantiatedMethod.InstantiatedVariable> otherParameters)
            implements PrototypeKind {
        @Override
        public PrototypeKind withJavaType(TypeMirror newType) {
            return new JsonInput(jsonType, newType, otherParameters);
        }

        @Override
        public Direction direction() {
            return Direction.INPUT;
        }
    }

    record JsonOutput(
            TypeMirror jsonType, TypeMirror javaType, List<InstantiatedMethod.InstantiatedVariable> otherParameters)
            implements PrototypeKind {
        @Override
        public PrototypeKind withJavaType(TypeMirror newType) {
            return new JsonOutput(jsonType, newType, otherParameters);
        }

        @Override
        public Direction direction() {
            return Direction.OUTPUT;
        }
    }

    record JdbcPrototypeKind(
            TypeMirror jsonType,
            TypeMirror javaType,
            InstantiatedVariable jdbcVariable,
            List<InstantiatedMethod.InstantiatedVariable> otherParameters,
            Direction direction,
            String sqlTemplate)
            implements PrototypeKind {
        @Override
        public PrototypeKind withJavaType(TypeMirror newType) {
            return new JdbcPrototypeKind(jsonType, newType, jdbcVariable, otherParameters, direction, sqlTemplate);
        }

        @Override
        public Direction direction() {
            return direction;
        }
    }

    enum Direction {
        INPUT,
        OUTPUT,
        JDBC_SELECT,
        JDBC_UPDATE,
        JDBC_INSERT;
    }
}
