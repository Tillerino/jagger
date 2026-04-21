package org.tillerino.jagger.processor.apis;

import com.squareup.javapoet.CodeBlock;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Jdbc;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class JdbcInsertGenerator extends AbstractJdbcGenerator<JdbcInsertGenerator> {

    public JdbcInsertGenerator(JaggerPrototype prototype, AnnotationProcessorUtils utils) {
        super(utils, prototype);
    }

    public CodeBlock.Builder build() {
        String sqlTemplate = config.resolveProperty(Jdbc.SQL_QUERY).value();

        InstantiatedVariable toInsert = kind.otherParameters().get(0);
        TypeMirror entityType = kind.javaType();

        if (utils.commonTypes.isIterableOrArray(entityType)) {
            entityType = utils.commonTypes.unwrapContainer(entityType);
        }

        if (sqlTemplate.isEmpty()) {
            AnyConfig dtoConfig = AnyConfig.create(utils.commonTypes.asElement(entityType), LocationKind.DTO, utils);

            String quoteChar =
                    dtoConfig.merge(config).resolveProperty(Jdbc.QUOTE_CHAR).value();

            String tableName = Jdbc.determineTableName(config, dtoConfig, toInsert.name());
            sqlTemplate = "INSERT INTO %s%s%s (%s%s.#insertColumns%s) VALUES (:%s.#insertValues)"
                    .formatted(quoteChar, tableName, quoteChar, quoteChar, toInsert.name(), quoteChar, toInsert.name());
            code.add("// Generated: $L\n", sqlTemplate);
        }

        Jdbc.ParsedSql parsed = utils.jdbc
                .parseTemplate(sqlTemplate, prototype.asInstantiatedMethod())
                .addCommentIfPreprocessed(code);

        tryPrepareStatement(parsed).withBody(psVar -> {
            if (utils.commonTypes.isIterableOrArray(toInsert.type())) {
                TypeMirror elementType = utils.commonTypes.unwrapContainer(toInsert.type());

                ScopedVar loopVar = createVariable("item");
                Snippet loopItems = Snippet.of("for ($T $C : $L)", elementType, loopVar, toInsert.name());
                beginControlFlow(loopItems).withBody(() -> {
                    int paramIndex = 1;
                    for (PerfectSnippet param : parsed.parameters()) {
                        Snippet setter = preparedStatementSetter(
                                param.replaceVar(toInsert.name(), loopVar.withType(elementType)), paramIndex, psVar);
                        addStatement(setter);
                        paramIndex++;
                    }

                    addStatement("$C.addBatch()", psVar);
                });

                addStatement("$C.executeBatch()", psVar);
            } else {
                setPreparedStatementProperties(parsed, psVar);
                addStatement("$C.execute()", psVar);
            }
        });

        return code;
    }
}
