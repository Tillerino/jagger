package org.tillerino.jagger.processor.jdbc;

import com.squareup.javapoet.CodeBlock.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PlainTypeName;

public class JdbcPrototypeDetector implements PrototypeDetector {
    static final String JDBC_SELECT = "org.tillerino.jagger.annotations.JdbcSelect";
    static final String JDBC_INSERT = "org.tillerino.jagger.annotations.JdbcInsert";
    static final String JDBC_UPDATE = "org.tillerino.jagger.annotations.JdbcUpdate";

    private final JaggerContext ctx;

    private final TypeElement jdbcSelect;
    private final TypeElement jdbcInsert;
    private final TypeElement jdbcUpdate;

    public JdbcPrototypeDetector(JaggerContext ctx) {
        this.ctx = ctx;
        jdbcSelect = ctx.elements.getTypeElement(JDBC_SELECT);
        jdbcInsert = ctx.elements.getTypeElement(JDBC_INSERT);
        jdbcUpdate = ctx.elements.getTypeElement(JDBC_UPDATE);
    }

    @Override
    public List<TypeElement> supportedAnnotationTypes() {
        return Arrays.asList(jdbcSelect, jdbcInsert, jdbcUpdate);
    }

    @Override
    public Optional<PrototypeKind> detect(InstantiatedMethod m) {
        return detectJdbcSelect(m, ctx).or(() -> detectJdbcInsert(m, ctx)).or(() -> detectJdbcUpdate(m, ctx));
    }

    private Optional<PrototypeKind> detectJdbcSelect(InstantiatedMethod m, JaggerContext ctx) {
        if (ctx.annotations.findAnnotation(m.element(), jdbcSelect).isPresent()
                && m.returnType().getKind() != TypeKind.VOID
                && !m.parameters().isEmpty()) {
            return PrototypeKind.detect(
                    PrototypeKind.nullableTypeList(ctx.commonTypes.connection, ctx.commonTypes.resultSet),
                    m,
                    ctx,
                    (externalType, externalParameter, otherParameters) -> new JdbcPrototypeKind(
                            externalType, m.returnType(), externalParameter, otherParameters, Direction.JDBC_SELECT));
        }
        return Optional.empty();
    }

    private Optional<PrototypeKind> detectJdbcInsert(InstantiatedMethod m, JaggerContext ctx) {
        if (ctx.annotations.findAnnotation(m.element(), jdbcInsert).isPresent()
                && m.returnType().getKind() == TypeKind.VOID
                && m.parameters().size() >= 2) {
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
        return Optional.empty();
    }

    private Optional<PrototypeKind> detectJdbcUpdate(InstantiatedMethod m, JaggerContext ctx) {
        if (ctx.annotations.findAnnotation(m.element(), jdbcUpdate).isPresent()
                && m.returnType().getKind() == TypeKind.VOID
                && !m.parameters().isEmpty()) {
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
        return Optional.empty();
    }

    public record JdbcPrototypeKind(
            TypeMirror externalType,
            TypeMirror internalType,
            InstantiatedVariable jdbcVariable,
            List<InstantiatedVariable> otherParameters,
            Direction direction)
            implements TemplatablePrototypeKind {
        @Override
        public TemplatablePrototypeKind withInternalType(TypeMirror newType) {
            return new JdbcPrototypeKind(externalType, newType, jdbcVariable, otherParameters, direction);
        }

        public Direction direction() {
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
            return prefix + PlainTypeName.of(internalType());
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
