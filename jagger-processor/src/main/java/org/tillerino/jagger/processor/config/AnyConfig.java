package org.tillerino.jagger.processor.config;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.config.ConfigProperty.InstantiatedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.util.Accessor;
import org.tillerino.jagger.processor.util.Accessor.AccessorKind;
import org.tillerino.jagger.processor.util.Accessor.ElementAccessor;

public final class AnyConfig {
    static final ConfigProperty[] available = {
        UnknownProperties.UNKNOWN_PROPERTIES,
        ConfigProperty.USES,
        Delegation.DELEGATE_TO,
        ConfigProperty.IMPLEMENT,
        PropertyName.PROPERTY_NAME,
        IgnoreProperty.IGNORE_PROPERTY,
        IgnoreProperties.IGNORED_PROPERTIES,
        Verification.VERIFY_SYMMETRY,
        References.REFERENCES,
        RequiredProperty.REQUIRED_PROPERTY,
        // leave a trailing comma for cleaner diffs :)
    };

    static {
        Arrays.sort(available, Comparator.comparingInt(p -> p.index));
    }

    static final Comparator<InstantiatedProperty> COMPARATOR = Comparator.<InstantiatedProperty>comparingInt(
                    prop -> prop.property().index)
            .thenComparing(InstantiatedProperty::locationKind);

    private final List<InstantiatedProperty> properties;

    public AnyConfig(List<InstantiatedProperty> properties) {
        // validate that the properties are sorted
        for (int i = 1; i < properties.size(); i++) {
            if (COMPARATOR.compare(properties.get(i - 1), properties.get(i)) >= 0) {
                throw new ContextedRuntimeException("properties not sorted monotonously")
                        .addContextValue("properties", properties);
            }
        }
        this.properties = List.copyOf(properties); // copy so order cannot be changed externally
    }

    public static AnyConfig create(Element element, LocationKind elementType, AnnotationProcessorUtils utils) {
        // TODO resolve config
        List<InstantiatedProperty> list = Stream.of(available)
                .flatMap(prop -> prop.instantiate(element, elementType, utils).stream())
                .toList();

        if (element instanceof TypeElement) {
            AnyConfig superConfig = null;
            for (DeclaredType directSupertype : Polymorphism.directSupertypes(element.asType(), utils)) {
                AnyConfig thisSuperConfig = create(directSupertype.asElement(), elementType, utils);
                superConfig = superConfig != null ? thisSuperConfig.merge(superConfig) : thisSuperConfig;
            }
            if (superConfig != null) {
                return new AnyConfig(list).merge(superConfig);
            }
        }

        return new AnyConfig(list);
    }

    public static AnyConfig empty() {
        return new AnyConfig(List.of());
    }

    /**
     * Currently only using "DTO" and only calling in AbstractCodeGeneratorStack constructor. This might be the only
     * kind required.
     */
    public AnyConfig propagateTo(PropagationKind newLocation) {
        return new AnyConfig(properties.stream()
                .filter(p -> p.property().propagateTo.contains(newLocation))
                .toList());
    }

    /**
     * @param property the accessor. in recursive calls the element might be missing
     * @param accessorName to reconstruct the element in recursive calls
     * @param dto the dto containing the property
     * @param canonicalPropertyName to find the field
     * @return can return null in a recursive call
     */
    public static AnyConfig fromAccessorConsideringField(
            Accessor property,
            String accessorName,
            TypeMirror dto,
            String canonicalPropertyName,
            AnnotationProcessorUtils utils) {

        // nullable during recursion. if element is null, this means the accessor does not exist in this type, but maybe
        // parent types.
        AnyConfig accessorConfig = fromAccessorAndField(property, dto, canonicalPropertyName, utils);

        if (property.kind() != AccessorKind.GETTER && property.kind() != AccessorKind.SETTER) {
            return accessorConfig;
        }

        for (DeclaredType d : Polymorphism.directSupertypes(dto, utils)) {
            TypeElement superTypeElement = (TypeElement) d.asElement();
            Optional<ExecutableElement> superMethod =
                    ElementFilter.methodsIn(superTypeElement.getEnclosedElements()).stream()
                            .filter(m -> m.getSimpleName().contentEquals(accessorName))
                            .findFirst();
            ElementAccessor parentAccessor =
                    new ElementAccessor(property.type(), superMethod.orElse(null), property.kind());
            AnyConfig superConfig =
                    fromAccessorConsideringField(parentAccessor, accessorName, d, canonicalPropertyName, utils);
            if (superConfig != null) {
                accessorConfig = accessorConfig != null ? accessorConfig.merge(superConfig) : superConfig;
            }
        }

        return accessorConfig;
    }

    private static AnyConfig fromAccessorAndField(
            Accessor property, TypeMirror dto, String canonicalPropertyName, AnnotationProcessorUtils utils) {
        AnyConfig accessorConfig =
                property.element() != null ? create(property.element(), LocationKind.PROPERTY, utils) : null;
        if (property.kind() == AccessorKind.FIELD) {
            return accessorConfig;
        }
        if (!(dto instanceof DeclaredType d)) {
            return accessorConfig;
        }

        Optional<AnyConfig> maybeFieldConfig = ElementFilter.fieldsIn(
                        d.asElement().getEnclosedElements())
                .stream()
                .filter(f -> f.getSimpleName().contentEquals(canonicalPropertyName))
                .findFirst()
                .map(f -> create(f, LocationKind.PROPERTY, utils));

        return maybeFieldConfig
                .map(anyConfig -> accessorConfig != null ? anyConfig.merge(accessorConfig) : anyConfig)
                .orElse(accessorConfig);
    }

    /**
     * Uses are appended during merging of configuration. So when we search them, we do it in reverse to make sure that
     * the latest gets prio.
     */
    public List<JaggerBlueprint> reversedUses() {
        Set<JaggerBlueprint> uses = resolveProperty(ConfigProperty.USES).value();
        List<JaggerBlueprint> reversedUses = new ArrayList<>(uses);
        Collections.reverse(reversedUses);
        return reversedUses;
    }

    public AnyConfig merge(AnyConfig weaker) {
        List<InstantiatedProperty> mergedProperties = new ArrayList<>();

        for (int i = 0, j = 0; i < properties.size() || j < weaker.properties.size(); ) {
            if (i == properties.size()) {
                mergedProperties.add(weaker.properties.get(j++));
            } else if (j == weaker.properties.size()) {
                mergedProperties.add(properties.get(i++));
            } else if (COMPARATOR.compare(properties.get(i), weaker.properties.get(j)) < 0) {
                mergedProperties.add(properties.get(i++));
            } else if (COMPARATOR.compare(properties.get(i), weaker.properties.get(j)) > 0) {
                mergedProperties.add(weaker.properties.get(j++));
            } else {
                InstantiatedProperty strong = properties.get(i++);
                InstantiatedProperty weak = weaker.properties.get(j++);
                mergedProperties.add(strong.property().merger.merge(strong, weak));
            }
        }

        return new AnyConfig(mergedProperties);
    }

    public <T> ResolvedProperty<T> resolveProperty(ConfigProperty<T> prop) {
        Optional<InstantiatedProperty<T>> value = Optional.empty();
        for (InstantiatedProperty<?> p : properties) {
            // properties are sorted from stronges to weakest
            if (p.property() == prop) {
                InstantiatedProperty<T> weaker = (InstantiatedProperty<T>) p;
                value = value.map(stronger -> prop.merger.merge(stronger, weaker))
                        .or(() -> Optional.of(weaker));
            }
        }
        return value.map(inst -> new ResolvedProperty<>(inst.value(), inst.sourceLocation()))
                .orElseGet(() -> new ResolvedProperty<>(prop.defaultValue, null));
    }

    public record ResolvedProperty<T>(T value, @Nullable String location) {}
}
