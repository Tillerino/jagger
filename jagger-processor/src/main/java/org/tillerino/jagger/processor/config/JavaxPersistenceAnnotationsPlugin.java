package org.tillerino.jagger.processor.config;

import com.google.auto.service.AutoService;
import java.util.Optional;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPlugin;
import org.tillerino.jagger.processor.features.IgnoreProperty;
import org.tillerino.jagger.processor.features.PropertyName;
import org.tillerino.jagger.processor.jdbc.Jdbc;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

@AutoService(JaggerPlugin.class)
public class JavaxPersistenceAnnotationsPlugin implements JaggerPlugin {
    @Override
    public void configure(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                IgnoreProperty.IGNORE_PROPERTY, "javax.persistence.Transient", ann -> Optional.of(true));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                PropertyName.PROPERTY_NAME,
                "javax.persistence.Column",
                ann -> ann.method("name", false).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.ID_PROPERTY, "javax.persistence.Id", ann -> Optional.of(true));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.TABLE_NAME_ON_DTO,
                "javax.persistence.Table",
                ann -> ann.method("name", false).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Jdbc.GENERATION_TYPE,
                "javax.persistence.GeneratedValue",
                ann -> ann.method("strategy", false).map(w -> w.asEnum(Jdbc.GenerationType.class)));
    }
}
