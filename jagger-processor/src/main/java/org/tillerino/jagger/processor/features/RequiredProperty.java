package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public class RequiredProperty {
    public static ConfigProperty<Boolean> REQUIRED_PROPERTY = ConfigProperty.createConfigProperty(
            List.of(LocationKind.PROPERTY),
            List.of(new AnnotationConfigPropertyRetriever<>(
                    "com.fasterxml.jackson.annotation.JsonProperty",
                    (ann, utils) -> ann.method("required", false).map(AnnotationValueWrapper::asBoolean))),
            false,
            MergeFunction.notDefault(false),
            PropagationKind.none());

    public static boolean isRequired(AnyConfig config) {
        return config.resolveProperty(REQUIRED_PROPERTY).value();
    }
}
