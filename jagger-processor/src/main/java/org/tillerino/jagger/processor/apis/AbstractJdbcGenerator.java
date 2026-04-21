package org.tillerino.jagger.processor.apis;

import java.sql.PreparedStatement;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.TypedVariable;
import org.tillerino.jagger.processor.Snippet.TypedSnippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Jdbc.ParsedSql;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeKind.JdbcPrototypeKind;

public abstract class AbstractJdbcGenerator<SELF extends AbstractJdbcGenerator<SELF>>
        extends AbstractCodeGenerator<SELF> {
    protected final AnnotationProcessorUtils utils;
    protected final JaggerPrototype prototype;
    protected final AnyConfig config;
    protected final JdbcPrototypeKind kind;

    public AbstractJdbcGenerator(AnnotationProcessorUtils utils, JaggerPrototype prototype) {
        super(prototype.asInstantiatedMethod());
        this.utils = utils;
        this.prototype = prototype;
        this.config = prototype.config();
        this.kind = (JdbcPrototypeKind) prototype.kind();
    }

    protected TypedVariable prepareStatement(ParsedSql parsed) {
        TypedVariable psVar = createVariable("ps").withType(utils.commonTypes.preparedStatement);
        addStatement(
                "$T $C = $L.prepareStatement($S)",
                PreparedStatement.class,
                psVar,
                kind.jdbcVariable().name(),
                parsed.sql());
        return psVar;
    }

    protected UnaryControlFlowScope<TypedVariable> tryPrepareStatement(ParsedSql parsed) {
        TypedVariable psVar = createVariable("ps").withType(utils.commonTypes.preparedStatement);
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
