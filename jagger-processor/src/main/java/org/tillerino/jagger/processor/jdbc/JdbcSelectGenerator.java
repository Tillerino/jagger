package org.tillerino.jagger.processor.jdbc;

import static org.tillerino.jagger.processor.config.AnyConfig.fromAccessorConsideringField;
import static org.tillerino.jagger.processor.features.PropertyName.resolvePropertyName;
import static org.tillerino.jagger.processor.util.Code.c;
import static org.tillerino.jagger.processor.util.Expr.e;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.helpers.jdbc.JdbcHelper;
import org.tillerino.jagger.helpers.jdbc.ResultSetIterator;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.features.Creators;
import org.tillerino.jagger.processor.features.Creators.Creator;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.jdbc.Jdbc.ParsedSql;
import org.tillerino.jagger.processor.util.Accessor.AccessorKind;
import org.tillerino.jagger.processor.util.Accessor.ElementAccessor;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JdbcSelectGenerator extends AbstractJdbcGenerator<JdbcSelectGenerator> {

    public JdbcSelectGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
    }

    public CodeBlock.Builder build() {
        if (ctx.types.isSameType(kind.types().get(1), ctx.commonTypes.resultSet)) {
            return buildFromResultSet();
        }

        String sqlTemplate = config.resolveProperty(Jdbc.SQL_QUERY).value();

        if (sqlTemplate.isEmpty()) {
            AnyConfig dtoConfig = AnyConfig.create(
                    ctx.commonTypes.asElement(
                            ctx.commonTypes.unwrapContainer(kind.types().get(0))),
                    LocationKind.DTO,
                    ctx);

            String quoteChar =
                    dtoConfig.merge(config).resolveProperty(Jdbc.QUOTE_CHAR).value();

            sqlTemplate = "SELECT * FROM %s%s%s"
                    .formatted(quoteChar, Jdbc.determineTableName(config, dtoConfig, "returned type"), quoteChar);
            code.add("// Generated: $L\n", sqlTemplate);
        }

        String whereClause = config.resolveProperty(Jdbc.WHERE_CLAUSE).value();
        if (!whereClause.isEmpty()) {
            sqlTemplate += " WHERE " + whereClause;
        }

        Jdbc.ParsedSql parsed =
                jdbc.parseTemplate(sqlTemplate, prototype.method()).addCommentIfPreprocessed(code);

        if (ctx.types.isSameType(
                ctx.types.erasure(kind.types().get(0)), ctx.types.erasure(ctx.commonTypes.type(Iterable.class)))) {
            selectIterable(parsed, ctx.commonTypes.getComponentType(kind.types().get(0), Iterable.class));
            return code;
        } else if (ctx.commonTypes.isErasureAssignableTo(kind.types().get(0), Iterator.class)) {
            selectIterator(parsed, ctx.commonTypes.getComponentType(kind.types().get(0), Iterator.class));
            return code;
        }

        tryPrepareStatement(parsed).withBody(psVar -> {
            setPreparedStatementProperties(parsed, psVar);

            int fetchSize = config.resolveProperty(Jdbc.FETCH_SIZE).value();
            if (fetchSize > 0) {
                addStatement("$C.setFetchSize($L)", psVar, fetchSize);
            }

            TypedVariable rsVar = createVariable(ctx.commonTypes.resultSet, "rs");
            addStatement("$T $C = $C.executeQuery()", ResultSet.class, rsVar, psVar);
            if (ctx.commonTypes.isIterableOrArray(kind.types().get(0))) {
                selectList(rsVar);
            } else if (ctx.commonTypes.isErasureAssignableTo(kind.types().get(0), Optional.class)) {
                selectOptional(rsVar);
            } else {
                selectSingle(rsVar);
            }
        });
        return code;
    }

    private CodeBlock.Builder buildFromResultSet() {
        if (ctx.types.isSameType(
                ctx.types.erasure(kind.types().get(0)), ctx.types.erasure(ctx.commonTypes.type(Iterable.class)))) {
            lambda(
                    c("return ()"),
                    () -> returnIterator(
                            ctx.commonTypes.getComponentType(kind.types().get(0), Iterable.class),
                            kind.jdbcVariable(),
                            e(ctx.commonTypes.preparedStatement, "null")),
                    c(";\n"));
            return code;
        }

        if (ctx.commonTypes.isErasureAssignableTo(kind.types().get(0), Iterator.class)) {
            returnIterator(
                    ctx.commonTypes.getComponentType(kind.types().get(0), Iterator.class),
                    kind.jdbcVariable(),
                    e(ctx.commonTypes.preparedStatement, "null"));
            return code;
        }

        if (ctx.commonTypes.isIterableOrArray(kind.types().get(0))) {
            selectList(kind.jdbcVariable());
        } else if (ctx.commonTypes.isErasureAssignableTo(kind.types().get(0), Optional.class)) {
            selectOptional(kind.jdbcVariable());
        } else {
            selectSingle(kind.jdbcVariable());
        }
        return code;
    }

    private void selectSingle(Expr rsVar) {
        throwIfNoResults(rsVar);
        Expr snippet = read(kind.types().get(0), rsVar);
        if (!(snippet instanceof TypedVariable)) {
            TypedVariable tv = createVariable(kind.types().get(0), "result");
            addStatement("$T $C = $C", kind.types().get(0), tv, snippet);
            snippet = tv;
        }
        throwIfMoreResults(rsVar);
        addStatement("return $C", snippet);
    }

    private void selectOptional(Expr rsVar) {
        beginControlFlow("if (!$C.next())", rsVar).withBody(() -> {
            addStatement("return $T.empty()", Optional.class);
        });
        TypeMirror type = ctx.commonTypes.getComponentType(kind.types().get(0), Optional.class);
        Expr snippet = read(type, rsVar);
        throwIfMoreResults(rsVar);
        addStatement("return $T.of($C)", Optional.class, snippet);
    }

    private void selectList(Expr rsVar) {
        TypedVariable results = createVariable(kind.types().get(0), "results");
        addStatement("$T $C = new $T<>()", results.type(), results, ArrayList.class);
        TypeMirror type = ctx.commonTypes.unwrapContainer(kind.types().get(0));
        beginControlFlow("while ($C.next())", rsVar).withBody(() -> {
            addStatement("$C.add($C)", results, read(type, rsVar));
        });
        addStatement("return $C", results);
    }

    private void selectIterator(ParsedSql parsed, TypeMirror type) {
        TypedVariable psVar = prepareStatement(parsed);
        setPreparedStatementProperties(parsed, psVar);

        int fetchSize = config.resolveProperty(Jdbc.FETCH_SIZE).value();
        if (fetchSize > 0) {
            addStatement("$C.setFetchSize($L)", psVar, fetchSize);
        }

        TypedVariable rsVar = createVariable(ctx.commonTypes.resultSet, "rs");
        addStatement("$T $C = $C.executeQuery()", ResultSet.class, rsVar, psVar);
        returnIterator(type, rsVar, psVar);
    }

    private void returnIterator(TypeMirror type, Expr rsVar, Expr psVar) {
        TypedVariable innerRsVar = createVariable(ctx.commonTypes.resultSet, "rs");
        lambda(
                c("return new $T<>($C, $C, $C", ResultSetIterator.class, rsVar, psVar, innerRsVar),
                () -> {
                    Expr read = read(type, innerRsVar);
                    addStatement("return $C", read);
                },
                c(");\n"));
    }

    private void selectIterable(ParsedSql parsed, TypeMirror componentType) {
        lambda(
                c("return ()"),
                () -> {
                    TypedVariable e = createVariable(null, "e");
                    beginControlFlow("try");
                    selectIterator(parsed, componentType);
                    nextControlFlow(c("catch ($T $C)", SQLException.class, e))
                            .withBody(() -> addStatement(c(
                                    "throw new $T($C)",
                                    ClassName.get("org.tillerino.jagger.helpers.jdbc", "UncheckedSQLException"),
                                    e)));
                },
                c(";\n"));
    }

    private Expr read(TypeMirror type, Expr rsVar) {
        InstantiatedMethod creator = findCreator(type);
        if (creator != null) {
            return fromCreator(type, creator, rsVar);
        }
        return fromWriteAccessors(type, rsVar);
    }

    private InstantiatedMethod findCreator(TypeMirror type) {
        if (!(type instanceof DeclaredType dt)) {
            return null;
        }

        Optional<Creator> creatorOpt = ctx.creators.findJsonCreatorMethod(type);
        if (creatorOpt.isPresent()) {
            if (creatorOpt.get() instanceof Creators.Creator.Converter) {
                throw new UnsupportedOperationException("not implemented");
            }
            return ((Creator.Properties) creatorOpt.get()).method();
        }

        if (dt.asElement().getKind() == ElementKind.RECORD) {
            Map<TypeVar, TypeMirror> typeBindings = ctx.generics.recordTypeBindings(dt);
            return ctx.generics.instantiateMethod(
                    ElementFilter.constructorsIn(dt.asElement().getEnclosedElements())
                            .get(0),
                    typeBindings,
                    LocationKind.CREATOR);
        }

        return null;
    }

    private Expr fromCreator(TypeMirror type, InstantiatedMethod creator, Expr rsVar) {
        List<? extends InstantiatedVariable> creatorParams = creator.parameters();

        List<Expr> values = new ArrayList<>();
        for (InstantiatedVariable param : creatorParams) {
            AnyConfig propertyConfig;
            if (creator.element().getKind() == ElementKind.CONSTRUCTOR
                    && creator.element().getEnclosingElement().getKind() == ElementKind.RECORD) {
                propertyConfig = AnyConfig.fromAccessorConsideringField(
                        new ElementAccessor(param.type(), param.elem(), AccessorKind.PARAMETER),
                        param.name(),
                        type,
                        param.name(),
                        ctx);
            } else {
                propertyConfig = param.config();
            }
            propertyConfig = propertyConfig.merge(config);

            String canonicalPropertyName = param.name();
            TypedVariable var = createVariable(param.type(), canonicalPropertyName);
            values.add(var);

            if (IgnoreProperty.isIgnoredForJdbc(propertyConfig)) {
                addStatement(
                        "$T $C = $C",
                        param.type(),
                        var,
                        ctx.defaultValues.getDefaultValue(prototype, param.type(), propertyConfig));
                continue;
            }

            String propertyName = resolvePropertyName(propertyConfig, canonicalPropertyName);

            Code getter = getResultSetGetter(param.type(), propertyName, rsVar);
            Code write = c("$T $C = $C", param.type(), var, getter);
            if (param.type().getKind().isPrimitive()) {
                write = c("$C; $T.throwOnNull($C, $S)", write, JdbcHelper.class, rsVar, propertyName);
            }
            addStatement(write);
        }

        return creator.callStatic(values);
    }

    private Expr fromWriteAccessors(TypeMirror type, Expr rsVar) {
        TypedVariable resultVar = createVariable(type, "result");
        addStatement("$T $C = new $T()", type, resultVar, type);

        ctx.properties.listWriteAccessors(type).forEach((canonicalPropertyName, accessor) -> {
            AnyConfig propertyConfig = fromAccessorConsideringField(
                            accessor, accessor.name(), type, canonicalPropertyName, ctx)
                    .merge(config);
            if (IgnoreProperty.isIgnoredForJdbc(propertyConfig)) {
                return;
            }
            String propertyName = resolvePropertyName(propertyConfig, canonicalPropertyName);

            Expr getter = getResultSetGetter(accessor.type(), propertyName, rsVar);
            Code write = accessor.write(resultVar, getter);
            if (accessor.type().getKind().isPrimitive()) {
                write = c("$C; $T.throwOnNull($C, $S)", write, JdbcHelper.class, rsVar, propertyName);
            }
            addStatement(write);
        });

        return new TypedVariable(type, resultVar.name());
    }

    private Expr getResultSetGetter(TypeMirror type, String name, Expr rsVar) {
        if (type instanceof PrimitiveType) {
            String upper = StringUtils.capitalize(type.toString());
            return e(type, "$C.get$L($S)", rsVar, upper, name);
        }
        if (ctx.commonTypes.isBoxed(type)) {
            return e(type, "$C.getObject($S, $T.class)", rsVar, name, type);
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return e(type, "$C.getBytes($S)", rsVar, name);
        }

        String typeStr = type.toString();
        return switch (typeStr) {
            case "java.lang.String" -> e(type, "$C.getString($S)", rsVar, name);
            case "java.math.BigDecimal" -> e(type, "$C.getBigDecimal($S)", rsVar, name);
            case "java.sql.Date" -> e(type, "$C.getDate($S)", rsVar, name);
            case "java.sql.Time" -> e(type, "$C.getTime($S)", rsVar, name);
            case "java.sql.Timestamp" -> e(type, "$C.getTimestamp($S)", rsVar, name);
            default -> throw new IllegalArgumentException("Unsupported type: " + typeStr);
        };
    }

    private void throwIfNoResults(Expr rsVar1) {
        beginControlFlow("if (!$C.next())", rsVar1).withBody(() -> {
            addStatement(
                    "throw new $T($S)",
                    ClassName.get("jakarta.persistence", "NoResultException"),
                    "The query did not return any results.");
        });
    }

    private void throwIfMoreResults(Expr rsVar) {
        beginControlFlow("if ($C.next())", rsVar).withBody(() -> {
            addStatement(
                    "throw new $T($S)",
                    ClassName.get("jakarta.persistence", "NonUniqueResultException"),
                    "The query returned more than one result.");
        });
    }
}
