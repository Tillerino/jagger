package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;

public class PropertyName {
    public static ConfigProperty<String> PROPERTY_NAME = ConfigProperty.createConfigProperty(
            "PROPERTY_NAME",
            List.of(ConfigProperty.LocationKind.PROPERTY),
            "",
            ConfigProperty.MergeFunction.notDefault(),
            PropagationKind.none());

    public static String resolvePropertyName(AnyConfig config, String canonicalPropertyName) {
        String customPropertyName = config.resolveProperty(PROPERTY_NAME).value();
        if (!customPropertyName.isEmpty()) {
            return customPropertyName;
        }
        return canonicalPropertyName;
    }
}
