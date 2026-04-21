package org.tillerino.jagger.processor.apis;

import static org.tillerino.jagger.processor.config.AnyConfig.fromAccessorConsideringField;
import static org.tillerino.jagger.processor.features.PropertyName.resolvePropertyName;

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
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.Literal;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.TypedVariable;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Creators;
import org.tillerino.jagger.processor.features.Creators.Creator;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.features.Jdbc;
import org.tillerino.jagger.processor.features.Jdbc.ParsedSql;
import org.tillerino.jagger.processor.util.Accessor.AccessorKind;
import org.tillerino.jagger.processor.util.Accessor.ElementAccessor;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JdbcSelectGenerator extends AbstractJdbcGenerator<JdbcSelectGenerator> {

    public JdbcSelectGenerator(JaggerPrototype prototype, AnnotationProcessorUtils utils) {
        super(utils, prototype);
    }

    public CodeBlock.Builder build() {
        if (utils.types.isSameType(kind.jsonType(), utils.commonTypes.resultSet)) {
            return buildFromResultSet();
        }

        String sqlTemplate = config.resolveProperty(Jdbc.SQL_QUERY).value();

        if (sqlTemplate.isEmpty()) {
            AnyConfig dtoConfig = AnyConfig.create(
                    utils.commonTypes.asElement(utils.commonTypes.unwrapContainer(kind.javaType())),
                    LocationKind.DTO,
                    utils);

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

        Jdbc.ParsedSql parsed = utils.jdbc
                .parseTemplate(sqlTemplate, prototype.asInstantiatedMethod())
                .addCommentIfPreprocessed(code);

        if (utils.types.isSameType(
                utils.types.erasure(kind.javaType()), utils.types.erasure(utils.commonTypes.type(Iterable.class)))) {
            selectIterable(parsed, utils.commonTypes.getComponentType(kind.javaType(), Iterable.class));
            return code;
        } else if (utils.commonTypes.isErasureAssignableTo(kind.javaType(), Iterator.class)) {
            selectIterator(parsed, utils.commonTypes.getComponentType(kind.javaType(), Iterator.class));
            return code;
        }

        tryPrepareStatement(parsed).withBody(psVar -> {
            setPreparedStatementProperties(parsed, psVar);

            int fetchSize = config.resolveProperty(Jdbc.FETCH_SIZE).value();
            if (fetchSize > 0) {
                addStatement("$C.setFetchSize($L)", psVar, fetchSize);
            }

            TypedVariable rsVar = createVariable("rs").withType(utils.commonTypes.resultSet);
            addStatement("$T $C = $C.executeQuery()", ResultSet.class, rsVar, psVar);
            if (utils.commonTypes.isIterableOrArray(kind.javaType())) {
                selectList(rsVar);
            } else if (utils.commonTypes.isErasureAssignableTo(kind.javaType(), Optional.class)) {
                selectOptional(rsVar);
            } else {
                selectSingle(rsVar);
            }
        });
        return code;
    }

    private CodeBlock.Builder buildFromResultSet() {
        if (utils.commonTypes.isErasureAssignableTo(kind.javaType(), Iterator.class)) {
            returnIterator(
                    utils.commonTypes.getComponentType(kind.javaType(), Iterator.class),
                    kind.jdbcVariable(),
                    new Literal(utils.commonTypes.preparedStatement, "null"));
            return code;
        }

        if (utils.commonTypes.isIterableOrArray(kind.javaType())) {
            selectList(kind.jdbcVariable());
        } else if (utils.commonTypes.isErasureAssignableTo(kind.javaType(), Optional.class)) {
            selectOptional(kind.jdbcVariable());
        } else {
            selectSingle(kind.jdbcVariable());
        }
        return code;
    }

    private void selectSingle(PerfectSnippet rsVar) {
        throwIfNoResults(rsVar);
        PerfectSnippet snippet = read(kind.javaType(), rsVar);
        if (!(snippet instanceof TypedVariable)) {
            TypedVariable tv = createVariable("result").withType(kind.javaType());
            addStatement("$T $C = $C", kind.javaType(), tv, snippet);
            snippet = tv;
        }
        throwIfMoreResults(rsVar);
        addStatement("return $C", snippet);
    }

    private void selectOptional(PerfectSnippet rsVar) {
        beginControlFlow("if (!$C.next())", rsVar).withBody(() -> {
            addStatement("return $T.empty()", Optional.class);
        });
        TypeMirror type = utils.commonTypes.getComponentType(kind.javaType(), Optional.class);
        PerfectSnippet snippet = read(type, rsVar);
        throwIfMoreResults(rsVar);
        addStatement("return $T.of($C)", Optional.class, snippet);
    }

    private void selectList(PerfectSnippet rsVar) {
        TypedVariable results = createVariable("results").withType(kind.javaType());
        addStatement("$T $C = new $T<>()", results.type(), results, ArrayList.class);
        TypeMirror type = utils.commonTypes.unwrapContainer(kind.javaType());
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

        TypedVariable rsVar = createVariable("rs").withType(utils.commonTypes.resultSet);
        addStatement("$T $C = $C.executeQuery()", ResultSet.class, rsVar, psVar);
        returnIterator(type, rsVar, psVar);
    }

    private void returnIterator(TypeMirror type, PerfectSnippet rsVar, PerfectSnippet psVar) {
        TypedVariable innerRsVar = createVariable("rs").withType(utils.commonTypes.resultSet);
        beginControlFlow("return new $T<>($C, $C, $C ->", ResultSetIterator.class, rsVar, psVar, innerRsVar);
        PerfectSnippet read = read(type, innerRsVar);
        addStatement("return $C", read);
        code.unindent();
        popVariablesStack();
        code.addStatement("})");
    }

    private void selectIterable(ParsedSql parsed, TypeMirror componentType) {
        beginControlFlow("return () ->");
        ScopedVar e = createVariable("e");
        beginControlFlow("try");
        selectIterator(parsed, componentType);
        nextControlFlow(Snippet.of("catch ($T $C)", SQLException.class, e))
                .withBody(() -> addStatement(Snippet.of(
                        "throw new $T($C)",
                        ClassName.get("org.tillerino.jagger.helpers.jdbc", "UncheckedSQLException"),
                        e)));
        code.unindent();
        popVariablesStack();
        addStatement("}");
    }

    private PerfectSnippet read(TypeMirror type, PerfectSnippet rsVar) {
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

        Optional<Creator> creatorOpt = utils.creators.findJsonCreatorMethod(type);
        if (creatorOpt.isPresent()) {
            if (creatorOpt.get() instanceof Creators.Creator.Converter) {
                throw new UnsupportedOperationException("not implemented");
            }
            return ((Creator.Properties) creatorOpt.get()).method();
        }

        if (dt.asElement().getKind() == ElementKind.RECORD) {
            Map<TypeVar, TypeMirror> typeBindings = utils.generics.recordTypeBindings(dt);
            return utils.generics.instantiateMethod(
                    ElementFilter.constructorsIn(dt.asElement().getEnclosedElements())
                            .get(0),
                    typeBindings,
                    LocationKind.CREATOR);
        }

        return null;
    }

    private PerfectSnippet fromCreator(TypeMirror type, InstantiatedMethod creator, PerfectSnippet rsVar) {
        List<? extends InstantiatedVariable> creatorParams = creator.parameters();

        List<PerfectSnippet> values = new ArrayList<>();
        for (InstantiatedVariable param : creatorParams) {
            AnyConfig propertyConfig;
            if (creator.element().getKind() == ElementKind.CONSTRUCTOR
                    && creator.element().getEnclosingElement().getKind() == ElementKind.RECORD) {
                propertyConfig = AnyConfig.fromAccessorConsideringField(
                        new ElementAccessor(param.type(), param.elem(), AccessorKind.PARAMETER),
                        param.name(),
                        type,
                        param.name(),
                        utils);
            } else {
                propertyConfig = param.config();
            }
            propertyConfig = propertyConfig.merge(config);

            String canonicalPropertyName = param.name();
            TypedVariable var = createVariable(canonicalPropertyName).withType(param.type());
            values.add(var);

            if (IgnoreProperty.isIgnoredForJdbc(propertyConfig)) {
                addStatement(
                        "$T $C = $C",
                        param.type(),
                        var,
                        utils.defaultValues.getDefaultValue(prototype, param.type(), propertyConfig));
                continue;
            }

            String propertyName = resolvePropertyName(propertyConfig, canonicalPropertyName);

            Snippet getter = getResultSetGetter(param.type(), propertyName, rsVar);
            Snippet write = Snippet.of("$T $C = $C", param.type(), var, getter);
            if (param.type().getKind().isPrimitive()) {
                write = Snippet.of("$C; $T.throwOnNull($C, $S)", write, JdbcHelper.class, rsVar, propertyName);
            }
            addStatement(write);
        }

        return creator.invoke(utils, values);
    }

    private PerfectSnippet fromWriteAccessors(TypeMirror type, PerfectSnippet rsVar) {
        TypedVariable resultVar = createVariable("result").withType(type);
        addStatement("$T $C = new $T()", type, resultVar, type);

        utils.properties.listWriteAccessors(type).forEach((canonicalPropertyName, accessor) -> {
            AnyConfig propertyConfig = fromAccessorConsideringField(
                            accessor, accessor.name(), type, canonicalPropertyName, utils)
                    .merge(config);
            if (IgnoreProperty.isIgnoredForJdbc(propertyConfig)) {
                return;
            }
            String propertyName = resolvePropertyName(propertyConfig, canonicalPropertyName);

            Snippet getter = getResultSetGetter(accessor.type(), propertyName, rsVar);
            Snippet write = accessor.writeSnippet(resultVar, getter);
            if (accessor.type().getKind().isPrimitive()) {
                write = Snippet.of("$C; $T.throwOnNull($C, $S)", write, JdbcHelper.class, rsVar, propertyName);
            }
            addStatement(write);
        });

        return new PerfectSnippet.TypedVariable(type, resultVar.name());
    }

    private Snippet getResultSetGetter(TypeMirror type, String name, PerfectSnippet rsVar) {
        if (type instanceof PrimitiveType) {
            String upper = StringUtils.capitalize(type.toString());
            return Snippet.of("$C.get$L($S)", rsVar, upper, name);
        }
        if (utils.isBoxed(type)) {
            return Snippet.of("$C.getObject($S, $T.class)", rsVar, name, ClassName.get(type));
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return Snippet.of("$C.getBytes($S)", rsVar, name);
        }

        String typeStr = type.toString();
        return switch (typeStr) {
            case "java.lang.String" -> Snippet.of("$C.getString($S)", rsVar, name);
            case "java.math.BigDecimal" -> Snippet.of("$C.getBigDecimal($S)", rsVar, name);
            case "java.sql.Date" -> Snippet.of("$C.getDate($S)", rsVar, name);
            case "java.sql.Time" -> Snippet.of("$C.getTime($S)", rsVar, name);
            case "java.sql.Timestamp" -> Snippet.of("$C.getTimestamp($S)", rsVar, name);
            default -> throw new IllegalArgumentException("Unsupported type: " + typeStr);
        };
    }

    private void throwIfNoResults(PerfectSnippet rsVar1) {
        beginControlFlow("if (!$C.next())", rsVar1).withBody(() -> {
            addStatement(
                    "throw new $T($S)",
                    ClassName.get("jakarta.persistence", "NoResultException"),
                    "The query did not return any results.");
        });
    }

    private void throwIfMoreResults(PerfectSnippet rsVar) {
        beginControlFlow("if ($C.next())", rsVar).withBody(() -> {
            addStatement(
                    "throw new $T($S)",
                    ClassName.get("jakarta.persistence", "NonUniqueResultException"),
                    "The query returned more than one result.");
        });
    }
}
