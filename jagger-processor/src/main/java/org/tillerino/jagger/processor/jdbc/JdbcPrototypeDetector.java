package org.tillerino.jagger.processor.jdbc;

import com.squareup.javapoet.CodeBlock.Builder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PlainTypeName;

public class JdbcPrototypeDetector implements PrototypeDetector {
    static final String JDBC_SELECT = "org.tillerino.jagger.annotations.JdbcSelect";
    static final String JDBC_INSERT = "org.tillerino.jagger.annotations.JdbcInsert";
    static final String JDBC_UPDATE = "org.tillerino.jagger.annotations.JdbcUpdate";

    private final JaggerContext ctx;

    public JdbcPrototypeDetector(JaggerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Collection<String> supportedAnnotationTypes() {
        return Arrays.asList(JDBC_SELECT, JDBC_INSERT, JDBC_UPDATE);
    }

    @Override
    public Optional<PrototypeKind> detect(InstantiatedMethod m, AnnotationMirrorWrapper annotation) {
        return switch (annotation.mirror().getAnnotationType().toString()) {
            case JDBC_SELECT -> detectJdbcSelect(m);
            case JDBC_INSERT -> detectJdbcInsert(m);
            case JDBC_UPDATE -> detectJdbcUpdate(m);
            default -> throw Exceptions.unexpected();
        };
    }

    private Optional<PrototypeKind> detectJdbcSelect(InstantiatedMethod m) {
        if (m.returnType().getKind() == TypeKind.VOID || m.parameters().isEmpty()) {
            return Optional.empty();
        }
        return PrototypeKind.detect(
                PrototypeKind.nullableTypeList(ctx.commonTypes.connection, ctx.commonTypes.resultSet),
                m,
                ctx,
                (externalType, externalParameter, otherParameters) -> new JdbcPrototypeKind(
                        externalType, m.returnType(), externalParameter, otherParameters, Direction.JDBC_SELECT));
    }

    private Optional<PrototypeKind> detectJdbcInsert(InstantiatedMethod m) {
        if (m.returnType().getKind() != TypeKind.VOID || m.parameters().size() < 2) {
            return Optional.empty();
        }
        return PrototypeKind.detect(
                PrototypeKind.nullableTypeList(ctx.commonTypes.connection),
                m,
                ctx,
                (externalType, externalParameter, otherParameters) -> new JdbcPrototypeKind(
                        externalType,
                        otherParameters.stream()
                                .map(InstantiatedVariable::type)
                                .findFirst()
                                .orElse(null),
                        externalParameter,
                        otherParameters,
                        Direction.JDBC_INSERT));
    }

    private Optional<PrototypeKind> detectJdbcUpdate(InstantiatedMethod m) {
        if (m.returnType().getKind() != TypeKind.VOID || m.parameters().isEmpty()) {
            return Optional.empty();
        }
        return PrototypeKind.detect(
                PrototypeKind.nullableTypeList(ctx.commonTypes.connection),
                m,
                ctx,
                (externalType, externalParameter, otherParameters) -> new JdbcPrototypeKind(
                        externalType,
                        otherParameters.stream()
                                .map(InstantiatedVariable::type)
                                .findFirst()
                                .orElse(null),
                        externalParameter,
                        otherParameters,
                        Direction.JDBC_UPDATE));
    }

    public record JdbcPrototypeKind(
            TypeMirror externalType,
            TypeMirror internalType,
            InstantiatedVariable jdbcVariable,
            List<InstantiatedVariable> otherParameters,
            Direction direction)
            implements TemplatablePrototypeKind {
        @Override
        public List<TypeMirror> types() {
            return List.of(internalType, externalType);
        }

        @Override
        public TemplatablePrototypeKind withTypes(List<TypeMirror> newTypes) {
            return new JdbcPrototypeKind(externalType, newTypes.get(0), jdbcVariable, otherParameters, direction);
        }

        public Direction specialization() {
            return direction;
        }

        @Override
        public String defaultMethodName() {
            String prefix =
                    switch (direction) {
                        case JDBC_SELECT -> "select";
                        case JDBC_INSERT -> "insert";
                        case JDBC_UPDATE -> "update";
                    };
            return prefix + PlainTypeName.of(types().get(0));
        }

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return switch (direction) {
                case JDBC_SELECT -> new JdbcSelectGenerator(context).build();
                case JDBC_INSERT -> new JdbcInsertGenerator(context).build();
                case JDBC_UPDATE -> new JdbcUpdateGenerator(context).build();
            };
        }
    }

    public enum Direction {
        JDBC_SELECT,
        JDBC_UPDATE,
        JDBC_INSERT,
        ;
    }
}
