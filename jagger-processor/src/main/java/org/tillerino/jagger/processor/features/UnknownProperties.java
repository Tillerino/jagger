package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;

public class UnknownProperties {
    public static ConfigProperty<JsonConfig.UnknownPropertiesMode> UNKNOWN_PROPERTIES =
            ConfigProperty.createConfigProperty(
                    "UNKNOWN_PROPERTIES",
                    List.of(
                            ConfigProperty.LocationKind.BLUEPRINT,
                            ConfigProperty.LocationKind.PROTOTYPE,
                            ConfigProperty.LocationKind.CREATOR,
                            ConfigProperty.LocationKind.DTO),
                    JsonConfig.UnknownPropertiesMode.DEFAULT,
                    ConfigProperty.MergeFunction.notDefault(),
                    PropagationKind.all());

    public static boolean shouldThrow(AnyConfig config) {
        return config.resolveProperty(UNKNOWN_PROPERTIES).value() != JsonConfig.UnknownPropertiesMode.IGNORE;
    }
}
