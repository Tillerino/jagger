package org.tillerino.jagger.processor.features;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.databind.AbstractCodeGeneratorStack;
import org.tillerino.jagger.processor.util.Snippet;

public class Alias {
    public static ConfigProperty<Set<String>> ALIASES = ConfigProperty.createConfigProperty(
            "ALIASES", List.of(LocationKind.PROPERTY), Set.of(), MergeFunction.mergeSets(), PropagationKind.none());

    public static Snippet caseSnippet(AbstractCodeGeneratorStack.Property property) {
        Stream<String> allValues = Stream.concat(
                        Stream.of(property.serializedName()),
                        property.config().resolveProperty(ALIASES).value().stream())
                .distinct();
        return Snippet.join(allValues.map(name -> Snippet.of("case $S:", name)).toList(), "\n");
    }
}
