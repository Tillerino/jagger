package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.util.Code.c;

import java.util.List;
import java.util.Set;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.util.Code;

public class IgnoreProperties {
    public static ConfigProperty<Set<String>> IGNORED_PROPERTIES = ConfigProperty.createConfigProperty(
            "IGNORED_PROPERTIES",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE, LocationKind.CREATOR, LocationKind.DTO),
            Set.of(),
            MergeFunction.mergeSets(),
            PropagationKind.none());

    public static Code cases(Set<String> ignoredProperties) {
        return Code.join(ignoredProperties.stream().map(prop -> c("$S", prop)).toList(), ", ");
    }
}
