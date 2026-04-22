package org.tillerino.jagger.processor.features;

import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;

public class RequiredProperty {
    public static ConfigProperty<Boolean> REQUIRED_PROPERTY = ConfigProperty.createConfigProperty(
            "REQUIRED_PROPERTY",
            List.of(LocationKind.PROPERTY),
            false,
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static boolean isRequired(AnyConfig config) {
        return config.resolveProperty(REQUIRED_PROPERTY).value();
    }
}
