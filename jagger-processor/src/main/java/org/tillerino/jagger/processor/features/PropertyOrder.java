package org.tillerino.jagger.processor.features;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;

public class PropertyOrder {
    public static ConfigProperty<List<String>> PROPERTY_ORDER = ConfigProperty.createConfigProperty(
            "PROPERTY_ORDER",
            List.of(ConfigProperty.LocationKind.PROPERTY),
            Collections.emptyList(),
            ConfigProperty.MergeFunction.notDefault(),
            PropagationKind.none());

    public static ConfigProperty<Boolean> PROPERTY_ORDER_ALPHABETIC = ConfigProperty.createConfigProperty(
            "PROPERTY_ORDER_ALPHABETIC",
            List.of(ConfigProperty.LocationKind.PROPERTY),
            false,
            ConfigProperty.MergeFunction.notDefault(),
            PropagationKind.none());

    public static Comparator<String> comparator(AnyConfig config) {
        List<String> propertyOrder = config.resolveProperty(PROPERTY_ORDER).value();
        boolean alphabetic = config.resolveProperty(PROPERTY_ORDER_ALPHABETIC).value();

        return (a, b) -> {
            int indexA = propertyOrder.indexOf(a);
            int indexB = propertyOrder.indexOf(b);
            boolean aInOrder = indexA >= 0;
            boolean bInOrder = indexB >= 0;

            if (aInOrder && bInOrder) {
                return Integer.compare(indexA, indexB);
            }
            if (aInOrder) {
                return -1;
            }
            if (bInOrder) {
                return 1;
            }
            if (alphabetic) {
                return a.compareTo(b);
            }
            return 0;
        };
    }
}
