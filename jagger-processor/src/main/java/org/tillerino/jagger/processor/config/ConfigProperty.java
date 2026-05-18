package org.tillerino.jagger.processor.config;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;

public final class ConfigProperty<T> {
    private static final AtomicInteger counter = new AtomicInteger();

    final int index = counter.incrementAndGet();

    public final String name;
    public final List<LocationKind> locationKind;
    public final T defaultValue;
    public final MergeFunction<T> merger;
    public final List<PropagationKind> propagateTo;

    public ConfigProperty(
            String name,
            List<LocationKind> locationKind,
            T defaultValue,
            MergeFunction<T> merger,
            List<PropagationKind> propagateTo) {
        this.name = name;
        this.locationKind = locationKind;
        this.defaultValue = defaultValue;
        this.merger = merger;
        this.propagateTo = propagateTo;
    }

    public static <T> ConfigProperty<T> createConfigProperty(
            String name,
            List<LocationKind> locationKind,
            T defaultValue,
            MergeFunction<T> merger,
            List<PropagationKind> propagateTo) {
        return new ConfigProperty<>(name, locationKind, defaultValue, merger, propagateTo);
    }

    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        // this set preserves order
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet);
    }

    @Override
    public String toString() {
        return name;
    }

    public interface MergeFunction<T> {
        InstantiatedProperty<T> merge(InstantiatedProperty<T> strong, InstantiatedProperty<T> weak);

        static <T> MergeFunction<T> notDefault() {
            return (strong, weak) -> {
                if (strong.property.equals(strong.property.defaultValue)) {
                    return weak;
                }
                return strong;
            };
        }

        static <T> MergeFunction<Set<T>> mergeSets() {
            return (strong, weak) -> {
                if (weak.value.isEmpty()) {
                    return strong;
                }
                if (strong.value.isEmpty()) {
                    return weak;
                }
                LinkedHashSet<T> merged = new LinkedHashSet<>();
                merged.addAll(strong.value);
                merged.addAll(weak.value);
                return new InstantiatedProperty<>(
                        strong.property,
                        strong.locationKind,
                        Collections.unmodifiableSet(merged),
                        "Merged " + strong.sourceLocation + " and " + weak.sourceLocation);
            };
        }
    }

    public enum LocationKind {
        // these are sorted from strongest to weakest
        PROPERTY,
        CREATOR,
        DTO,
        PROTOTYPE,
        BLUEPRINT,
        ;
    }

    public enum PropagationKind {
        /** During code generation, a type is substituted, e.g. when converting or @JsonCreator/@JsonValue. */
        SUBSTITUTE,
        /**
         * During code generation, we descend into a property, map value, array component, etc: a new type is pushed
         * onto the stack, which is not a substitute.
         */
        PROPERTY,
        ;

        public static List<PropagationKind> all() {
            return List.of(values());
        }

        public static List<PropagationKind> none() {
            return List.of();
        }
    }

    public interface ConfigPropertyRetriever<T> {
        Optional<PropertyOccurrence<T>> retrieve(Element element, JaggerContext ctx);
    }

    public record AnnotationConfigPropertyRetriever<T>(
            String annotationClass, AnnotationPropertyRetriever<T> valueRetriever)
            implements ConfigPropertyRetriever<T> {

        @Override
        public Optional<PropertyOccurrence<T>> retrieve(Element element, JaggerContext ctx) {
            return ctx
                    .annotations
                    .findAnnotation(element, annotationClass)
                    .flatMap(valueRetriever::retrieve)
                    .map(value -> new PropertyOccurrence<>(value, annotationClass + " on " + element))
                    .stream()
                    .findFirst();
        }

        public interface AnnotationPropertyRetriever<T> {
            Optional<T> retrieve(AnnotationMirrorWrapper annotation);
        }
    }

    public record InstantiatedProperty<T>(
            ConfigProperty<T> property, LocationKind locationKind, T value, String sourceLocation) {}

    public record PropertyOccurrence<T>(T value, String sourceLocation) {}
}
