package org.tillerino.jagger.processor.features;

import java.util.List;
import java.util.Set;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public class IgnoreProperties {
    public static ConfigProperty<Set<String>> IGNORED_PROPERTIES = ConfigProperty.createConfigProperty(
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE, LocationKind.CREATOR, LocationKind.DTO),
            List.of(new AnnotationConfigPropertyRetriever<>(
                    "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
                    (wrapper, utils) -> wrapper.method("value", true)
                            .map(AnnotationValueWrapper::asArray)
                            .map(arr -> arr.stream()
                                    .map(AnnotationValueWrapper::asString)
                                    .collect(ConfigProperty.toUnmodifiableSet())))),
            Set.of(),
            MergeFunction.mergeSets(),
            PropagationKind.none());

    public static Snippet toSnippet(Set<String> ignoredProperties) {
        return Snippet.join(
                ignoredProperties.stream().map(prop -> Snippet.of("$S", prop)).toList(), ", ");
    }
}
