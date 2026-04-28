package org.tillerino.jagger.processor.jdbc;

import com.squareup.javapoet.CodeBlock;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.Snippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;

public class JdbcInsertGenerator extends AbstractJdbcGenerator<JdbcInsertGenerator> {

    public JdbcInsertGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
    }

    public CodeBlock.Builder build() {
        String sqlTemplate = config.resolveProperty(Jdbc.SQL_QUERY).value();

        InstantiatedVariable toInsert = kind.otherParameters().get(0);
        TypeMirror entityType = kind.internalType();

        if (ctx.commonTypes.isIterableOrArray(entityType)) {
            entityType = ctx.commonTypes.unwrapContainer(entityType);
        }

        if (sqlTemplate.isEmpty()) {
            AnyConfig dtoConfig = AnyConfig.create(ctx.commonTypes.asElement(entityType), LocationKind.DTO, ctx);

            String quoteChar =
                    dtoConfig.merge(config).resolveProperty(Jdbc.QUOTE_CHAR).value();

            String tableName = Jdbc.determineTableName(config, dtoConfig, toInsert.name());
            sqlTemplate = "INSERT INTO %s%s%s (%s%s.#insertColumns%s) VALUES (:%s.#insertValues)"
                    .formatted(quoteChar, tableName, quoteChar, quoteChar, toInsert.name(), quoteChar, toInsert.name());
            code.add("// Generated: $L\n", sqlTemplate);
        }

        Jdbc.ParsedSql parsed =
                jdbc.parseTemplate(sqlTemplate, prototype.method()).addCommentIfPreprocessed(code);

        tryPrepareStatement(parsed).withBody(psVar -> {
            if (ctx.commonTypes.isIterableOrArray(toInsert.type())) {
                TypeMirror elementType = ctx.commonTypes.unwrapContainer(toInsert.type());

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
