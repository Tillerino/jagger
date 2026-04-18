package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.*;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public class IgnoreProperty {
    public static ConfigProperty<Boolean> IGNORE_PROPERTY = ConfigProperty.createConfigProperty(
            List.of(LocationKind.PROPERTY),
            List.of(new AnnotationConfigPropertyRetriever<>(
                    "com.fasterxml.jackson.annotation.JsonIgnore",
                    (ann, utils) -> ann.method("value", true).map(AnnotationValueWrapper::asBoolean))),
            false,
            MergeFunction.notDefault(false),
            PropagationKind.none());

    public static Boolean isIgnoredForJson(AnyConfig propertyConfig) {
        return propertyConfig.resolveProperty(IGNORE_PROPERTY).value();
    }
}
