package org.tillerino.jagger.processor.config;

import jakarta.annotation.Nullable;
import java.util.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty.InstantiatedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.util.Accessor;
import org.tillerino.jagger.processor.util.Accessor.AccessorKind;
import org.tillerino.jagger.processor.util.Accessor.ElementAccessor;

public final class AnyConfig {
    public static ConfigProperty<Set<TypeElement>> USES = ConfigProperty.createConfigProperty(
            "USES", List.of(LocationKind.values()), Set.of(), MergeFunction.mergeSets(), PropagationKind.all());

    static final Comparator<InstantiatedProperty> COMPARATOR = Comparator.<InstantiatedProperty>comparingInt(
                    prop -> prop.property().index)
            .thenComparing(InstantiatedProperty::locationKind);

    private final List<InstantiatedProperty> properties;

    private final JaggerContext ctx;

    private final UsesHolder uses = new UsesHolder();

    public AnyConfig(List<InstantiatedProperty> properties, JaggerContext ctx) {
        this.ctx = ctx;
        // validate that the properties are sorted
        for (int i = 1; i < properties.size(); i++) {
            if (COMPARATOR.compare(properties.get(i - 1), properties.get(i)) >= 0) {
                throw new ContextedRuntimeException("properties not sorted monotonously")
                        .addContextValue("properties", properties);
            }
        }
        this.properties = List.copyOf(properties); // copy so order cannot be changed externally
    }

    public static AnyConfig create(Element element, @Nullable LocationKind elementType, JaggerContext ctx) {
        if (elementType == null) {
            return empty(ctx);
        }

        // TODO resolve config
        List<InstantiatedProperty> list = ctx.configProperties.instantiate(element, elementType);

        if (element instanceof TypeElement) {
            AnyConfig superConfig = null;
            for (DeclaredType directSupertype : Polymorphism.directSupertypes(element.asType(), ctx)) {
                AnyConfig thisSuperConfig = create(directSupertype.asElement(), elementType, ctx);
                superConfig = superConfig != null ? thisSuperConfig.merge(superConfig) : thisSuperConfig;
            }
            if (superConfig != null) {
                return new AnyConfig(list, ctx).merge(superConfig);
            }
        }

        return new AnyConfig(list, ctx);
    }

    public static AnyConfig empty(JaggerContext ctx) {
        return new AnyConfig(List.of(), ctx);
    }

    /**
     * Currently only using "DTO" and only calling in AbstractCodeGeneratorStack constructor. This might be the only
     * kind required.
     */
    public AnyConfig propagateTo(PropagationKind newLocation) {
        return new AnyConfig(
                properties.stream()
                        .filter(p -> p.property().propagateTo.contains(newLocation))
                        .toList(),
                ctx);
    }

    /**
     * @param property the accessor. in recursive calls the element might be missing
     * @param accessorName to reconstruct the element in recursive calls
     * @param dtoType the dto containing the property
     * @param canonicalPropertyName to find the field
     * @return can return null in a recursive call
     */
    public static AnyConfig fromAccessorConsideringField(
            Accessor property,
            String accessorName,
            TypeMirror dtoType,
            String canonicalPropertyName,
            JaggerContext ctx) {

        // nullable during recursion. if element is null, this means the accessor does not exist in this type, but maybe
        // parent types.
        AnyConfig accessorConfig = fromAccessorAndField(property, dtoType, canonicalPropertyName, ctx);

        if (property.kind() != AccessorKind.GETTER && property.kind() != AccessorKind.SETTER) {
            return accessorConfig;
        }

        for (DeclaredType d : Polymorphism.directSupertypes(dtoType, ctx)) {
            TypeElement superTypeElement = (TypeElement) d.asElement();
            Optional<ExecutableElement> superMethod =
                    getFirstWithName(ElementFilter.methodsIn(superTypeElement.getEnclosedElements()), accessorName);
            ElementAccessor parentAccessor =
                    new ElementAccessor(property.type(), superMethod.orElse(null), property.kind());
            AnyConfig superConfig =
                    fromAccessorConsideringField(parentAccessor, accessorName, d, canonicalPropertyName, ctx);
            if (superConfig != null) {
                accessorConfig = accessorConfig != null ? accessorConfig.merge(superConfig) : superConfig;
            }
        }

        return accessorConfig;
    }

    private static AnyConfig fromAccessorAndField(
            Accessor property, TypeMirror dtoType, String canonicalPropertyName, JaggerContext ctx) {
        AnyConfig config = property.element() != null ? create(property.element(), LocationKind.PROPERTY, ctx) : null;
        if (property.kind() == AccessorKind.FIELD) {
            return config;
        }
        if (!(dtoType instanceof DeclaredType d)) {
            return config;
        }

        AnyConfig fieldConfig = getFirstWithName(
                        ElementFilter.fieldsIn(d.asElement().getEnclosedElements()), canonicalPropertyName)
                .map(f -> create(f, LocationKind.PROPERTY, ctx))
                .orElse(null);
        if (fieldConfig != null) {
            config = config != null ? fieldConfig.merge(config) : fieldConfig;
        }

        AnyConfig recordComponentConfig = getFirstWithName(
                        ElementFilter.recordComponentsIn(d.asElement().getEnclosedElements()), canonicalPropertyName)
                .map(f -> create(f, LocationKind.PROPERTY, ctx))
                .orElse(null);
        if (recordComponentConfig != null) {
            config = config != null ? recordComponentConfig.merge(config) : recordComponentConfig;
        }

        return config;
    }

    private static <T extends Element> Optional<T> getFirstWithName(List<T> elements, String canonicalPropertyName) {
        return elements.stream()
                .filter(f -> f.getSimpleName().contentEquals(canonicalPropertyName))
                .findFirst();
    }

    /**
     * Uses are appended during merging of configuration. So when we search them, we do it in reverse to make sure that
     * the latest gets prio.
     */
    public List<JaggerBlueprint> reversedUses() {
        List<JaggerBlueprint> reversedUses = new ArrayList<>(uses.get());
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

        return new AnyConfig(mergedProperties, ctx);
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

    class UsesHolder {
        List<JaggerBlueprint> uses = null;

        List<JaggerBlueprint> get() {
            if (uses == null) {
                Set<JaggerBlueprint> acc = new LinkedHashSet<>();
                LinkedHashSet<UsesHolder> visited = new LinkedHashSet<>();
                visited.add(this);
                resolveProperty(USES).value().forEach(use -> {
                    JaggerBlueprint blueprint = ctx.blueprint(use);
                    acc.add(blueprint);
                    blueprint.config.uses.putUses(acc, visited);
                });
                uses = acc.stream().toList();
            }
            return uses;
        }

        void putUses(Set<JaggerBlueprint> acc, Set<UsesHolder> visited) {
            if (!visited.add(this)) {
                return;
            }
            resolveProperty(USES).value().forEach(use -> {
                JaggerBlueprint blueprint = ctx.blueprint(use);
                acc.add(blueprint);
                blueprint.config.uses.putUses(acc, visited);
            });
        }
    }
}
