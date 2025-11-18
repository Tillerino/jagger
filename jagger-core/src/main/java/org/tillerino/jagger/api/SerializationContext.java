package org.tillerino.jagger.api;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Context for a single serialization operation. This can be used in a
 * {@link org.tillerino.jagger.annotations.JsonOutput} for additional features like polymorphic deserialization.
 *
 * <p>This class can be extended to pass additional state between custom serializer implementations.
 *
 * <p>To reduce heap allocations, you can reuse instances.
 */
public class SerializationContext {
    /**
     * Marks if the object being written is a child class of a polymorphic type and needs to have its discriminator
     * written.
     */
    public String pendingDiscriminatorProperty = null;
    /** Only relevant when {@link #pendingDiscriminatorProperty} is not null. */
    public String pendingDiscriminatorValue = null;

    private static Map<ClassAndScope, ObjectIdGenerator<?>> globalObjectIdGenerators;
    private Map<ClassAndScope, ObjectIdGenerator<?>> objectIdGenerators;
    private Map<Object, Object> knownObjectIds;

    public void setPendingDiscriminator(String property, String value) {
        pendingDiscriminatorProperty = property;
        pendingDiscriminatorValue = value;
    }

    public boolean isDiscriminatorPending() {
        return pendingDiscriminatorProperty != null;
    }

    public <T> T previouslyWrittenId(Object dto) {
        if (knownObjectIds == null) {
            return null;
        }
        return (T) knownObjectIds.get(dto);
    }

    public <D, G extends ObjectIdGenerator<D>> D generateId(
            Class<G> generatorClass, Class<?> scope, Supplier<G> instantiator, Object dto) {
        if (objectIdGenerators == null) {
            objectIdGenerators = new LinkedHashMap<>();
        }
        G generator = (G) objectIdGenerators.computeIfAbsent(new ClassAndScope(generatorClass, scope), cas -> {
            if (globalObjectIdGenerators == null) {
                globalObjectIdGenerators = new ConcurrentHashMap<>();
            }
            return globalObjectIdGenerators
                    .computeIfAbsent(cas, __ -> instantiator.get().forScope(scope))
                    .newForSerialization(this);
        });
        if (knownObjectIds == null) {
            knownObjectIds = new LinkedHashMap<>();
        }
        return (D) knownObjectIds.computeIfAbsent(dto, (Function) generator::generateId);
    }

    public void rememberId(Object dto, Object key) {
        if (knownObjectIds == null) {
            knownObjectIds = new LinkedHashMap<>();
        }
        knownObjectIds.put(dto, key);
    }

    static class ClassAndScope {
        public final Class<?> cls;

        public final Class<?> scope;

        public ClassAndScope(Class<?> cls, Class<?> scope) {
            this.cls = cls;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClassAndScope that = (ClassAndScope) o;
            return Objects.equals(cls, that.cls) && Objects.equals(scope, that.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cls, scope);
        }
    }
}
