package org.tillerino.jagger.processor.jdbc;

import static org.tillerino.jagger.processor.util.Code.c;

import java.sql.PreparedStatement;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.processor.AbstractCodeGenerator;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.jdbc.Jdbc.ParsedSql;
import org.tillerino.jagger.processor.jdbc.JdbcPrototypeDetector.JdbcPrototypeKind;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public abstract class AbstractJdbcGenerator<SELF extends AbstractJdbcGenerator<SELF>>
        extends AbstractCodeGenerator<SELF> {
    protected final AnyConfig config;
    protected final JdbcPrototypeKind kind;
    protected final Jdbc jdbc;

    public AbstractJdbcGenerator(CodeGeneratorContext generatorContext) {
        super(generatorContext);
        this.config = prototype.config();
        this.kind = (JdbcPrototypeKind) prototype.kind();
        this.jdbc = new Jdbc(ctx);
    }

    protected TypedVariable prepareStatement(ParsedSql parsed) {
        TypedVariable psVar = createVariable(ctx.commonTypes.preparedStatement, "ps");
        addStatement(
                "$T $C = $C.prepareStatement($S)", PreparedStatement.class, psVar, kind.jdbcVariable(), parsed.sql());
        return psVar;
    }

    protected UnaryControlFlowScope<TypedVariable> tryPrepareStatement(ParsedSql parsed) {
        TypedVariable psVar = createVariable(ctx.commonTypes.preparedStatement, "ps");
        return beginControlFlow(
                        "try ($T $C = $C.prepareStatement($S))",
                        PreparedStatement.class,
                        psVar,
                        kind.jdbcVariable(),
                        parsed.sql())
                .withPayload(psVar);
    }

    protected void setPreparedStatementProperties(ParsedSql parsed, TypedVariable psVar) {
        int paramIndex = 1;
        for (Expr param : parsed.parameters()) {
            addStatement(preparedStatementSetter(param, paramIndex, psVar));
            paramIndex++;
        }
    }

    static Code preparedStatementSetter(Expr value, int paramIndex, TypedVariable psVar) {
        if (value.type().getKind().isPrimitive()) {
            String capitalized = StringUtils.capitalize(value.type().toString());
            return c("$C.set$L($L, $C)", psVar, capitalized, paramIndex, value);
        }
        return c("$C.setObject($L, $C)", psVar, paramIndex, value);
    }

    protected InstantiatedVariable getPayloadParameter() {
        return kind.otherParameters().get(0);
    }
}
