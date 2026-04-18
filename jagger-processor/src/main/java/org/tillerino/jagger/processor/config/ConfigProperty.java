package org.tillerino.jagger.processor.config;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public final class ConfigProperty<T> {
    private static final AtomicInteger counter = new AtomicInteger();

    public static ConfigProperty<Set<JaggerBlueprint>> USES = createConfigProperty(
            List.of(LocationKind.values()),
            List.of(new AnnotationConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig", (wrapper, utils) -> wrapper.method("uses", true)
                            .map(AnnotationValueWrapper::asArray)
                            .map(classNames -> classNames.stream()
                                    .map(className -> utils.blueprint(utils.elements.getTypeElement(
                                            className.asTypeMirror().toString())))
                                    .flatMap(JaggerBlueprint::includeUses)
                                    .collect(toUnmodifiableSet())))),
            Set.of(),
            MergeFunction.mergeSets(),
            PropagationKind.all());

    final int index = counter.incrementAndGet();

    public final List<LocationKind> locationKind;
    public final List<ConfigPropertyRetriever<T>> retrievers;
    public final T defaultValue;
    public final MergeFunction<T> merger;
    public final List<PropagationKind> propagateTo;

    public ConfigProperty(
            List<LocationKind> locationKind,
            List<ConfigPropertyRetriever<T>> retrievers,
            T defaultValue,
            MergeFunction<T> merger,
            List<PropagationKind> propagateTo) {
        this.locationKind = locationKind;
        this.retrievers = retrievers;
        this.defaultValue = defaultValue;
        this.merger = merger;
        this.propagateTo = propagateTo;
    }

    public static <T> ConfigProperty<T> createConfigProperty(
            List<LocationKind> locationKind,
            List<ConfigPropertyRetriever<T>> retriever,
            T defaultValue,
            MergeFunction<T> merger,
            List<PropagationKind> doNotPropagateTo) {
        return new ConfigProperty<>(locationKind, retriever, defaultValue, merger, doNotPropagateTo);
    }

    Optional<InstantiatedProperty<T>> instantiate(
            Element element, LocationKind elementType, AnnotationProcessorUtils utils) {
        if (!this.locationKind.contains(elementType)) {
            return Optional.empty();
        }
        return retrievers.stream()
                .flatMap(retriever -> retriever
                        .retrieve(element, utils)
                        .map(occurrence -> new InstantiatedProperty<>(
                                ConfigProperty.this, elementType, occurrence.value, occurrence.sourceLocation))
                        .stream())
                .reduce(merger::merge);
    }

    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        // this set preserves order
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet);
    }

    public interface MergeFunction<T> {
        InstantiatedProperty<T> merge(InstantiatedProperty<T> strong, InstantiatedProperty<T> weak);

        static <T> MergeFunction<T> notDefault(T defaultValue) {
            return (strong, weak) -> {
                if (strong.property.equals(defaultValue)) {
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

        public static List<LocationKind> all() {
            return List.of(values());
        }
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
        Optional<PropertyOccurrence<T>> retrieve(Element element, AnnotationProcessorUtils utils);
    }

    public record AnnotationConfigPropertyRetriever<T>(
            String annotationClass, AnnotationPropertyRetriever<T> valueRetriever)
            implements ConfigPropertyRetriever<T> {

        public static <T extends Enum<T>> AnnotationConfigPropertyRetriever<T> jsonConfigPropertyRetriever(
                String method, Class<T> enumClass) {
            return new AnnotationConfigPropertyRetriever<>(
                    "org.tillerino.jagger.annotations.JsonConfig", (wrapper, utils) -> wrapper.method(method, false)
                            .map(annotationValueWrapper -> annotationValueWrapper.asEnum(enumClass)));
        }

        @Override
        public Optional<PropertyOccurrence<T>> retrieve(Element element, AnnotationProcessorUtils utils) {
            return utils
                    .annotations
                    .findAnnotation(element, annotationClass)
                    .flatMap(annotation -> valueRetriever.retrieve(annotation, utils))
                    .map(value -> new PropertyOccurrence<>(value, annotationClass + " on " + element))
                    .stream()
                    .findFirst();
        }

        public interface AnnotationPropertyRetriever<T> {
            Optional<T> retrieve(AnnotationMirrorWrapper annotation, AnnotationProcessorUtils utils);
        }
    }

    public record InstantiatedProperty<T>(
            ConfigProperty<T> property, LocationKind locationKind, T value, String sourceLocation) {}

    public record PropertyOccurrence<T>(T value, String sourceLocation) {}
}
