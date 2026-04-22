package org.tillerino.jagger.processor.jdbc;

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import javax.lang.model.type.TypeMirror;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.MultiPartName;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.TypedVariable;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.features.Properties.OutputProperty;
import org.tillerino.jagger.processor.util.Accessor.ReadAccessor;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public record Jdbc(JaggerContext ctx) {
    public static ConfigProperty<Boolean> ID_PROPERTY = ConfigProperty.createConfigProperty(
            "ID_PROPERTY", List.of(LocationKind.PROPERTY), false, MergeFunction.notDefault(), PropagationKind.none());

    public static ConfigProperty<Integer> FETCH_SIZE = ConfigProperty.createConfigProperty(
            "FETCH_SIZE", List.of(LocationKind.PROTOTYPE), 0, MergeFunction.notDefault(), PropagationKind.none());

    public static ConfigProperty<String> TABLE_NAME_ON_DTO = ConfigProperty.createConfigProperty(
            "TABLE_NAME_ON_DTO", List.of(LocationKind.DTO), "", MergeFunction.notDefault(), PropagationKind.none());

    public static ConfigProperty<String> TABLE_NAME_ON_PROTOTYPE = ConfigProperty.createConfigProperty(
            "TABLE_NAME_ON_PROTOTYPE",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE),
            "",
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static ConfigProperty<String> SQL_QUERY = ConfigProperty.createConfigProperty(
            "SQL_QUERY", List.of(LocationKind.PROTOTYPE), "", MergeFunction.notDefault(), PropagationKind.none());

    public static ConfigProperty<String> WHERE_CLAUSE = ConfigProperty.createConfigProperty(
            "WHERE_CLAUSE", List.of(LocationKind.PROTOTYPE), "", MergeFunction.notDefault(), PropagationKind.none());

    public static ConfigProperty<String> QUOTE_CHAR = ConfigProperty.createConfigProperty(
            "QUOTE_CHAR",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE, LocationKind.DTO),
            "\"",
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static ConfigProperty<GenerationType> GENERATION_TYPE = ConfigProperty.createConfigProperty(
            "GENERATION_TYPE",
            List.of(LocationKind.PROPERTY),
            GenerationType.NONE,
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static String determineTableName(AnyConfig prototypeConfig, AnyConfig dtoConfig, String parameterName) {
        // annotation on method will override table name on property
        String tableName =
                prototypeConfig.resolveProperty(TABLE_NAME_ON_PROTOTYPE).value();
        if (tableName.isEmpty()) {
            tableName = dtoConfig.resolveProperty(TABLE_NAME_ON_DTO).value();
        }
        if (tableName.isEmpty()) {
            throw new ContextedRuntimeException(
                    "No table name available for " + parameterName + "\n" + "Configure with either of:\n"
                            + " - jakarta.persistence.Table\n"
                            + " - javax.persistence.Table\n"
                            + " - org.tillerino.jagger.annotations.JdbcConfig");
        }
        return tableName;
    }

    public record ParsedSql(String sqlTemplate, String preprocessedSql, String sql, List<PerfectSnippet> parameters) {
        public ParsedSql addCommentIfPreprocessed(CodeBlock.Builder code) {
            if (!sqlTemplate.equals(preprocessedSql)) {
                code.add("// Preprocessed: $L\n", preprocessedSql);
            }
            return this;
        }
    }

    public ParsedSql parseTemplate(String sqlTemplate, InstantiatedMethod method) {

        Map<String, InstantiatedVariable> paramMap = new java.util.HashMap<>();
        for (InstantiatedVariable param : method.parameters()) {
            paramMap.put(param.name(), param);
        }

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sqlTemplate);
        } catch (JSQLParserException e) {
            throw new RuntimeException("Failed to parse SQL: " + sqlTemplate, e);
        }

        if (statement instanceof Insert insert) {
            insert.setColumns(processColumns(paramMap, insert.getColumns()));
        }

        if (statement instanceof Update update) {
            for (UpdateSet updateSet : update.getUpdateSets()) {
                updateSet.setColumns(processColumns(paramMap, updateSet.getColumns()));

                updateSet.setValues(processExpressions(paramMap, (ExpressionList<Expression>) updateSet.getValues()));
            }
        }

        String preprocessedSql = statement.toString();

        List<PerfectSnippet> paramSnippets = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder();

        statement.accept(
                new StatementDeParser(
                        new MyExpressionDeParser(paramMap, paramSnippets), new SelectDeParser(), sqlBuilder) {});

        return new ParsedSql(sqlTemplate, preprocessedSql, sqlBuilder.toString(), paramSnippets);
    }

    private ExpressionList<Column> processColumns(
            Map<String, InstantiatedVariable> paramMap, ExpressionList<Column> columns) {
        List<Column> originalColumns = new ArrayList<>(columns);
        columns.clear();

        for (Column col : originalColumns) {
            Fixes f = Fixes.of(
                    MultiPartName.unquote(col.getFullyQualifiedName()),
                    ".#columns",
                    ".#insertColumns",
                    ".#keyColumns",
                    ".#updateColumns");
            Optional<String> columnQuoteChar = getQuoteChar(col);
            if (f.suffix.isEmpty()) {
                columns.add(col);
                continue;
            }

            InstantiatedVariable methodParam = getParam(paramMap, f.prefix);
            TypeMirror paramType = ctx.commonTypes.unwrapContainer(methodParam.type());
            List<OutputProperty> props =
                    switch (f.suffix) {
                        case ".#columns" -> ctx.properties.outputProperties(paramType, AnyConfig.empty());
                        case ".#insertColumns" -> getInsertProperties(paramType);
                        case ".#keyColumns" -> getPropertiesWhereIdIs(paramType, true);
                        case ".#updateColumns" -> getPropertiesWhereIdIs(paramType, false);
                        default -> throw new IllegalStateException();
                    };

            for (OutputProperty prop : props) {
                if (IgnoreProperty.isIgnoredForJdbc(prop.config())) {
                    continue;
                }
                columns.add(quoteColumnName(prop.externalName(), columnQuoteChar));
            }
        }
        return columns;
    }

    private ExpressionList<Expression> processExpressions(
            Map<String, InstantiatedVariable> paramMap, ExpressionList<Expression> values) {
        List<Expression> originalValues = new ArrayList<>(values);
        values.clear();

        for (Object raw : originalValues) {
            Expression val = (Expression) raw;
            if (!(val instanceof JdbcNamedParameter jdbcParam)) {
                values.add(val);
                continue;
            }

            Fixes f = Fixes.of(jdbcParam.getName(), ".#values", ".#insertValues", ".#keyValues", ".#updateValues");
            if (f.suffix.isEmpty()) {
                values.add(val);
                continue;
            }

            InstantiatedVariable methodParam = getParam(paramMap, f.prefix);
            TypeMirror paramType = ctx.commonTypes.unwrapContainer(methodParam.type());
            List<OutputProperty> props =
                    switch (f.suffix) {
                        case ".#values" -> ctx.properties.outputProperties(paramType, AnyConfig.empty());
                        case ".#insertValues" -> getInsertProperties(paramType);
                        case ".#keyValues" -> getPropertiesWhereIdIs(paramType, true);
                        case ".#updateValues" -> getPropertiesWhereIdIs(paramType, false);
                        default -> throw new IllegalStateException();
                    };

            for (OutputProperty prop : props) {
                if (IgnoreProperty.isIgnoredForJdbc(prop.config())) {
                    continue;
                }
                values.add(new JdbcNamedParameter(f.prefix + "." + prop.canonicalName()));
            }
        }
        return values;
    }

    private List<OutputProperty> getPropertiesWhereIdIs(TypeMirror paramType, boolean value) {
        List<OutputProperty> allProps = ctx.properties.outputProperties(paramType, AnyConfig.empty());
        return allProps.stream()
                .filter(p -> p.config().resolveProperty(ID_PROPERTY).value() == value)
                .toList();
    }

    private List<OutputProperty> getInsertProperties(TypeMirror paramType) {
        List<OutputProperty> allProps = ctx.properties.outputProperties(paramType, AnyConfig.empty());
        return allProps.stream()
                .filter(p -> p.config().resolveProperty(GENERATION_TYPE).value() == GenerationType.NONE)
                .toList();
    }

    private static InstantiatedVariable getParam(Map<String, InstantiatedVariable> paramMap, String paramName) {
        InstantiatedVariable methodParam = paramMap.get(paramName);
        if (methodParam == null) {
            throw new IllegalArgumentException("Unknown parameter: " + paramName);
        }
        return methodParam;
    }

    static String resolveSuffixAliases(String s) {
        if (s.endsWith(".#c")) {
            return s.substring(0, s.length() - 1) + "columns";
        }
        if (s.endsWith(".#v")) {
            return s.substring(0, s.length() - 1) + "values";
        }
        if (s.endsWith(".#kc")) {
            return s.substring(0, s.length() - 1) + "keyColumns";
        }
        if (s.endsWith(".#kv")) {
            return s.substring(0, s.length() - 1) + "keyValues";
        }
        if (s.endsWith(".#uc")) {
            return s.substring(0, s.length() - 1) + "updateColumns";
        }
        if (s.endsWith(".#uv")) {
            return s.substring(0, s.length() - 1) + "updateValues";
        }
        return s;
    }

    Optional<String> getQuoteChar(Column c) {
        Matcher matcher = MultiPartName.LEADING_TRAILING_QUOTES_PATTERN.matcher(c.getFullyQualifiedName());
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    Column quoteColumnName(String name, Optional<String> quoteChar) {
        return quoteChar.map(c -> new Column(c + name + c)).orElseGet(() -> new Column(name));
    }

    private class MyExpressionDeParser extends ExpressionDeParser {
        private final Map<String, InstantiatedVariable> paramMap;
        private final List<PerfectSnippet> paramSnippets;

        public MyExpressionDeParser(Map<String, InstantiatedVariable> paramMap, List<PerfectSnippet> paramSnippets) {
            this.paramMap = paramMap;
            this.paramSnippets = paramSnippets;
        }

        @Override
        public <S> StringBuilder visit(JdbcNamedParameter param, S context) {
            List<Expression> exprs = processExpressions(paramMap, new ExpressionList<>(param));

            List<String> questionMarks = new ArrayList<>();

            for (Expression expr : exprs) {
                if (!(expr instanceof JdbcNamedParameter p)) {
                    throw new IllegalStateException();
                }
                String name = p.getName();
                if (name.contains(".")) {
                    String[] parts = name.split("\\.");
                    if (parts.length == 2) {
                        String paramName = parts[0];
                        String propertyName = parts[1];
                        InstantiatedVariable methodParam = paramMap.get(paramName);
                        if (methodParam != null) {
                            TypeMirror paramType = ctx.commonTypes.unwrapContainer(methodParam.type());
                            Map<String, ReadAccessor> properties = ctx.properties.listReadAccessors(paramType);
                            ReadAccessor accessor = properties.get(propertyName);
                            if (accessor == null) {
                                throw new ContextedRuntimeException("Missing property: " + propertyName)
                                        .addContextValue("type", paramType);
                            }
                            paramSnippets.add(accessor.readSnippet(new TypedVariable(paramType, paramName)));
                            questionMarks.add("?");
                            continue;
                        }
                    }
                }

                InstantiatedVariable var = paramMap.get(name);
                if (var != null) {
                    paramSnippets.add(var);
                    questionMarks.add("?");
                } else {
                    super.visit(param, context);
                }
            }
            return builder.append(String.join(", ", questionMarks));
        }

        @Override
        public <S> StringBuilder visit(Column tableColumn, S context) {
            return builder.append(processColumns(paramMap, new ExpressionList<>(tableColumn)));
        }
    }

    record Fixes(String prefix, String suffix) {
        static Fixes of(String haystack, String... suffixes) {
            haystack = resolveSuffixAliases(haystack);
            for (String suffix : suffixes) {
                if (haystack.endsWith(suffix)) {
                    return new Fixes(haystack.substring(0, haystack.length() - suffix.length()), suffix);
                }
            }
            return new Fixes(haystack, "");
        }
    }

    public enum GenerationType {
        IDENTITY,
        NONE,
    }
}
