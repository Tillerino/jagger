package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.util.Annotations;

public class UnknownProperties {
    public static ConfigProperty<JsonConfig.UnknownPropertiesMode> UNKNOWN_PROPERTIES =
            ConfigProperty.createConfigProperty(
                    List.of(
                            ConfigProperty.LocationKind.BLUEPRINT,
                            ConfigProperty.LocationKind.PROTOTYPE,
                            ConfigProperty.LocationKind.CREATOR,
                            ConfigProperty.LocationKind.DTO),
                    List.of(
                            new AnnotationConfigPropertyRetriever<>(
                                    "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
                                    (wrapper, utils) -> wrapper.method("ignoreUnknown", true)
                                            .map(Annotations.AnnotationValueWrapper::asBoolean)
                                            .map(i -> i
                                                    ? JsonConfig.UnknownPropertiesMode.IGNORE
                                                    : JsonConfig.UnknownPropertiesMode.THROW)),
                            AnnotationConfigPropertyRetriever.jsonConfigPropertyRetriever(
                                    "unknownProperties", JsonConfig.UnknownPropertiesMode.class)),
                    JsonConfig.UnknownPropertiesMode.DEFAULT,
                    ConfigProperty.MergeFunction.notDefault(JsonConfig.UnknownPropertiesMode.DEFAULT),
                    PropagationKind.all());

    public static boolean shouldThrow(AnyConfig config) {
        return config.resolveProperty(UNKNOWN_PROPERTIES).value() != JsonConfig.UnknownPropertiesMode.IGNORE;
    }
}
