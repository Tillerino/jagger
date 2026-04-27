package org.tillerino.jagger.processor.jdbc;

import com.squareup.javapoet.CodeBlock;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.Snippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;

public class JdbcUpdateGenerator extends AbstractJdbcGenerator<JdbcUpdateGenerator> {

    public JdbcUpdateGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
    }

    public CodeBlock.Builder build() {
        String sqlTemplate = config.resolveProperty(Jdbc.SQL_QUERY).value();

        if (sqlTemplate.isEmpty()) {
            InstantiatedVariable payloadParameter = getPayloadParameter();
            AnyConfig dtoConfig = AnyConfig.create(
                    ctx.commonTypes.asElement(ctx.commonTypes.unwrapContainer(payloadParameter.type())),
                    LocationKind.DTO,
                    ctx);

            String quoteChar =
                    dtoConfig.merge(config).resolveProperty(Jdbc.QUOTE_CHAR).value();

            String paramName = payloadParameter.name();

            sqlTemplate =
                    "UPDATE %s%s%s SET (%s%s.#updateColumns%s) = (:%s.#updateValues) WHERE (%s%s.#keyColumns%s) = (:%s.#keyValues)"
                            .formatted(
                                    quoteChar,
                                    Jdbc.determineTableName(config, dtoConfig, paramName),
                                    quoteChar,
                                    quoteChar,
                                    paramName,
                                    quoteChar,
                                    paramName,
                                    quoteChar,
                                    paramName,
                                    quoteChar,
                                    paramName);
            code.add("// Generated: $L\n", sqlTemplate);
        }

        Jdbc.ParsedSql parsed = jdbc.parseTemplate(sqlTemplate, prototype.asInstantiatedMethod())
                .addCommentIfPreprocessed(code);

        tryPrepareStatement(parsed).withBody(psVar -> {
            if (parsed.parameters().isEmpty()) {
                setPreparedStatementProperties(parsed, psVar);
                addStatement("$C.executeUpdate()", psVar);
                return;
            }

            InstantiatedVariable toUpdate = getPayloadParameter();

            if (ctx.commonTypes.isIterableOrArray(toUpdate.type())) {
                TypeMirror elementType = ctx.commonTypes.unwrapContainer(toUpdate.type());

                ScopedVar loopVar = createVariable("item");
                Snippet loopItems = Snippet.of("for ($T $C : $L)", elementType, loopVar, toUpdate.name());
                beginControlFlow(loopItems).withBody(() -> {
                    int paramIndex = 1;
                    for (PerfectSnippet param : parsed.parameters()) {
                        Snippet setter = preparedStatementSetter(
                                param.replaceVar(toUpdate.name(), loopVar.withType(elementType)), paramIndex, psVar);
                        addStatement(setter);
                        paramIndex++;
                    }

                    addStatement("$C.addBatch()", psVar);
                });

                addStatement("$C.executeBatch()", psVar);
            } else {
                setPreparedStatementProperties(parsed, psVar);
                addStatement("$C.executeUpdate()", psVar);
            }
        });

        return code;
    }
}
