package org.tillerino.jagger.processor.features;

import java.util.List;
import java.util.Set;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.util.Snippet;

public class IgnoreProperties {
    public static ConfigProperty<Set<String>> IGNORED_PROPERTIES = ConfigProperty.createConfigProperty(
            "IGNORED_PROPERTIES",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE, LocationKind.CREATOR, LocationKind.DTO),
            Set.of(),
            MergeFunction.mergeSets(),
            PropagationKind.none());

    public static Snippet toSnippet(Set<String> ignoredProperties) {
        return Snippet.join(
                ignoredProperties.stream().map(prop -> Snippet.of("$S", prop)).toList(), ", ");
    }
}
