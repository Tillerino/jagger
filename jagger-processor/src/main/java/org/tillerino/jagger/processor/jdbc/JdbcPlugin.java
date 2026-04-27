package org.tillerino.jagger.processor.jdbc;

import com.google.auto.service.AutoService;
import java.util.Set;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

@AutoService(JaggerPlugin.class)
public class JdbcPlugin implements JaggerPlugin {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                JdbcPrototypeDetector.JDBC_SELECT,
                JdbcPrototypeDetector.JDBC_INSERT,
                JdbcPrototypeDetector.JDBC_UPDATE);
    }

    @Override
    public void configure(JaggerContext ctx) {
        ctx.detectors.add(new JdbcPrototypeDetector(ctx));

        configureJdbcConfigAnnotation(ctx);
        configureJdbcSelectAnnotation(ctx);
        configureJdbcInsertAnnotation(ctx);
        configureJdbcUpdateAnnotation(ctx);
    }

    private static void configureJdbcConfigAnnotation(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.TABLE_NAME_ON_DTO,
                "org.tillerino.jagger.annotations.JdbcConfig",
                ann -> ann.method("table", false).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.TABLE_NAME_ON_PROTOTYPE,
                "org.tillerino.jagger.annotations.JdbcConfig",
                ann -> ann.method("table", false).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.QUOTE_CHAR,
                "org.tillerino.jagger.annotations.JdbcConfig",
                ann -> ann.method("quoteChar", false).map(AnnotationValueWrapper::asString));
    }

    private static void configureJdbcSelectAnnotation(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.FETCH_SIZE,
                "org.tillerino.jagger.annotations.JdbcSelect",
                ann -> ann.method("fetchSize", false).map(AnnotationValueWrapper::asInt));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.SQL_QUERY,
                "org.tillerino.jagger.annotations.JdbcSelect",
                ann -> ann.method("value", false).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.WHERE_CLAUSE,
                "org.tillerino.jagger.annotations.JdbcSelect",
                ann -> ann.method("where", false).map(AnnotationValueWrapper::asString));
    }

    private static void configureJdbcInsertAnnotation(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.SQL_QUERY,
                "org.tillerino.jagger.annotations.JdbcInsert",
                ann -> ann.method("value", false).map(AnnotationValueWrapper::asString));
    }

    private static void configureJdbcUpdateAnnotation(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.SQL_QUERY,
                "org.tillerino.jagger.annotations.JdbcUpdate",
                ann -> ann.method("value", false).map(AnnotationValueWrapper::asString));
    }
}
