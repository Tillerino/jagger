package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;

public class IgnoreProperty {
    public static ConfigProperty<Boolean> IGNORE_PROPERTY = ConfigProperty.createConfigProperty(
            "IGNORE_PROPERTY",
            List.of(LocationKind.PROPERTY),
            false,
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static ConfigProperty<Boolean> TRANSIENT_FIELD = ConfigProperty.createConfigProperty(
            "TRANSIENT_FIELD",
            List.of(LocationKind.PROPERTY),
            false,
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static Boolean isIgnoredForJson(AnyConfig propertyConfig) {
        return propertyConfig.resolveProperty(IGNORE_PROPERTY).value();
    }

    public static boolean isIgnoredForJdbc(AnyConfig config) {
        return config.resolveProperty(IGNORE_PROPERTY).value()
                || config.resolveProperty(TRANSIENT_FIELD).value();
    }
}
