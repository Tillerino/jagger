package org.tillerino.jagger.processor.config;

import java.util.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty.*;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever.AnnotationPropertyRetriever;
import org.tillerino.jagger.processor.features.IgnoreProperty;

public class ConfigProperties {
    protected final JaggerContext ctx;

    private final List<PropertyAndRetrievers<?>> retrievers = new ArrayList<>();

    public ConfigProperties(JaggerContext ctx) {
        this.ctx = ctx;

        addConfig(
                IgnoreProperty.TRANSIENT_FIELD,
                (element, __) -> element instanceof VariableElement ve
                                && ve.getModifiers().contains(Modifier.TRANSIENT)
                        ? Optional.of(new PropertyOccurrence<>(true, element + " is transient"))
                        : Optional.empty());
    }

    public <T> void addConfig(ConfigProperty<T> property, ConfigPropertyRetriever<T> retriever) {
        PropertyAndRetrievers<T> insert = new PropertyAndRetrievers<>(property, null);
        int index = Collections.binarySearch(retrievers, insert, Comparator.comparingInt(p -> p.prop.index));
        if (index >= 0) {
            retrievers.get(index).retrievers.add((ConfigPropertyRetriever) retriever);
        } else {
            retrievers.add(-index - 1, new PropertyAndRetrievers(property, new ArrayList<>(List.of(retriever))));
        }
    }

    public <T> void addConfigAnnotation(
            ConfigProperty<T> property, String annotationClassName, AnnotationPropertyRetriever<T> retriever) {
        addConfig(property, new AnnotationConfigPropertyRetriever<>(annotationClassName, retriever));
    }

    List<InstantiatedProperty> instantiate(Element element, LocationKind elementType) {
        return retrievers.stream()
                .<InstantiatedProperty>flatMap(prop -> prop.instantiate(element, elementType, ctx).stream())
                .toList();
    }

    record PropertyAndRetrievers<T>(ConfigProperty<T> prop, List<ConfigPropertyRetriever<T>> retrievers) {

        Optional<InstantiatedProperty<T>> instantiate(Element element, LocationKind elementType, JaggerContext ctx) {
            if (!prop.locationKind.contains(elementType)) {
                return Optional.empty();
            }
            return retrievers.stream()
                    .flatMap(retriever -> retriever
                            .retrieve(element, ctx)
                            .map(occurrence -> new InstantiatedProperty<>(
                                    prop, elementType, occurrence.value(), occurrence.sourceLocation()))
                            .stream())
                    .reduce(prop.merger::merge);
        }
    }
}
