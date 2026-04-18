package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.util.Annotations;

public class PropertyName {
    public static ConfigProperty<String> PROPERTY_NAME = ConfigProperty.createConfigProperty(
            List.of(ConfigProperty.LocationKind.PROPERTY),
            List.of(
                    new AnnotationConfigPropertyRetriever<>(
                            "com.fasterxml.jackson.annotation.JsonProperty", (ann, utils) -> ann.method("value", true)
                                    .map(Annotations.AnnotationValueWrapper::asString)),
                    new AnnotationConfigPropertyRetriever<>(
                            "jakarta.persistence.Column",
                            (ann, utils) -> ann.method("name", true).map(Annotations.AnnotationValueWrapper::asString)),
                    new AnnotationConfigPropertyRetriever<>(
                            "javax.persistence.Column", (ann, utils) -> ann.method("name", true)
                                    .map(Annotations.AnnotationValueWrapper::asString))),
            "",
            ConfigProperty.MergeFunction.notDefault(""),
            PropagationKind.none());

    public static String resolvePropertyName(AnyConfig config, String canonicalPropertyName) {
        String customPropertyName = config.resolveProperty(PROPERTY_NAME).value();
        if (!customPropertyName.isEmpty()) {
            return customPropertyName;
        }
        return canonicalPropertyName;
    }
}
