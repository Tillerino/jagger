package org.tillerino.jagger.processor.jdbc;

import java.sql.PreparedStatement;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.processor.AbstractCodeGenerator;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.TypedVariable;
import org.tillerino.jagger.processor.Snippet.TypedSnippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.jdbc.Jdbc.ParsedSql;
import org.tillerino.jagger.processor.jdbc.JdbcDetector.JdbcPrototypeKind;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public abstract class AbstractJdbcGenerator<SELF extends AbstractJdbcGenerator<SELF>>
        extends AbstractCodeGenerator<SELF> {
    protected final JaggerContext ctx;
    protected final JaggerPrototype prototype;
    protected final AnyConfig config;
    protected final JdbcPrototypeKind kind;
    protected final Jdbc jdbc;

    public AbstractJdbcGenerator(JaggerContext ctx, JaggerPrototype prototype) {
        super(prototype.asInstantiatedMethod());
        this.ctx = ctx;
        this.prototype = prototype;
        this.config = prototype.config();
        this.kind = (JdbcPrototypeKind) prototype.kind();
        this.jdbc = new Jdbc(ctx);
    }

    protected TypedVariable prepareStatement(ParsedSql parsed) {
        TypedVariable psVar = createVariable("ps").withType(ctx.commonTypes.preparedStatement);
        addStatement(
                "$T $C = $L.prepareStatement($S)",
                PreparedStatement.class,
                psVar,
                kind.jdbcVariable().name(),
                parsed.sql());
        return psVar;
    }

    protected UnaryControlFlowScope<TypedVariable> tryPrepareStatement(ParsedSql parsed) {
        TypedVariable psVar = createVariable("ps").withType(ctx.commonTypes.preparedStatement);
        return beginControlFlow(
                        "try ($T $C = $L.prepareStatement($S))",
                        PreparedStatement.class,
                        psVar,
                        kind.jdbcVariable().name(),
                        parsed.sql())
                .withPayload(psVar);
    }

    protected void setPreparedStatementProperties(ParsedSql parsed, TypedVariable psVar) {
        int paramIndex = 1;
        for (TypedSnippet param : parsed.parameters()) {
            addStatement(preparedStatementSetter(param, paramIndex, psVar));
            paramIndex++;
        }
    }

    static Snippet preparedStatementSetter(TypedSnippet value, int paramIndex, TypedVariable psVar) {
        if (value.type().getKind().isPrimitive()) {
            String capitalized = StringUtils.capitalize(value.type().toString());
            return Snippet.of("$C.set$L($L, $C)", psVar, capitalized, paramIndex, value);
        }
        return Snippet.of("$C.setObject($L, $C)", psVar, paramIndex, value);
    }

    protected InstantiatedVariable getPayloadParameter() {
        return kind.otherParameters().get(0);
    }
}
