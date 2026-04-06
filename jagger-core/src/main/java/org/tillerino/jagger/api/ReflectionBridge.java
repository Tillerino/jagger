package org.tillerino.jagger.api;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.function.Failable;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;

/**
 * This class acts as a bridge between reflection-based frameworks and Jagger. Wherever you are doing reflection to find
 * a serializer or deserializer, you can use this class to get the corresponding implementation.
 */
public class ReflectionBridge {
    static {
        try {
            MethodUtils.class.toString();
        } catch (Throwable e) {
            throw new RuntimeException("Cannot find MethodUtils. Add commons-lang3 to your classpath.");
        }
    }

    private final List<Object> instances;

    public ReflectionBridge(List<Object> instances) {
        this.instances = new ArrayList<>(instances);
    }

    /** Cache the returned value. This is slow. */
    public <T> Optional<DeserializerDescription<T>> findDeserializer(Type type, Class<T> parserClass) {
        for (Object instance : instances) {
            for (Method method : instance.getClass().getMethods()) {
                if (!method.getReturnType().equals(type)) {
                    continue;
                }
                if (MethodUtils.getAnnotation(method, JsonInput.class, true, false) == null) {
                    continue;
                }
                if (method.getParameterTypes()[0].isAssignableFrom(parserClass)) {
                    return Optional.of(
                            new DeserializerDescription<>(instance, method, method.getParameterCount() == 2));
                }
            }
        }
        return Optional.empty();
    }

    public <T> Optional<SerializerDescription<T>> findSerializer(Type type, Class<T> writerClass) {
        for (Object instance : instances) {
            for (Method method : instance.getClass().getMethods()) {
                if (!method.getReturnType().equals(void.class)) {
                    continue;
                }
                if (MethodUtils.getAnnotation(method, JsonOutput.class, true, false) == null) {
                    continue;
                }
                if (method.getParameterTypes()[0].equals(type)
                        && method.getParameterTypes()[1].isAssignableFrom(writerClass)) {
                    return Optional.of(new SerializerDescription<>(instance, method, method.getParameterCount() == 3));
                }
            }
        }
        return Optional.empty();
    }

    public static class DeserializerDescription<T> {
        private final Object instance;
        private final Method method;
        private final boolean requiresContext;

        public DeserializerDescription(Object instance, Method method, boolean requiresContext) {
            this.instance = instance;
            this.method = method;
            this.requiresContext = requiresContext;
        }

        /**
         * Invoke the method with the given reader.
         *
         * <p>Will throw any exception thrown by the method, even if not declared.
         *
         * @param reader The reader to pass to the method.
         * @return the deserialized object.
         */
        public Object invoke(T reader) {
            try {
                if (requiresContext) {
                    return method.invoke(instance, reader, new DeserializationContext());
                }
                return method.invoke(instance, reader);
            } catch (ReflectiveOperationException e) {
                if (e.getCause() != null) {
                    throw Failable.rethrow(e.getCause());
                }
                throw new RuntimeException(e);
            }
        }
    }

    public static class SerializerDescription<T> {
        private final Object instance;
        private final Method method;
        private final boolean requiresContext;

        public SerializerDescription(Object instance, Method method, boolean requiresContext) {
            this.instance = instance;
            this.method = method;
            this.requiresContext = requiresContext;
        }

        public void invoke(Object value, T writer) {
            try {
                if (requiresContext) {
                    method.invoke(instance, value, writer, new SerializationContext());
                } else {
                    method.invoke(instance, value, writer);
                }
            } catch (ReflectiveOperationException e) {
                if (e.getCause() != null) {
                    throw Failable.rethrow(e.getCause());
                }
                throw new RuntimeException(e);
            }
        }
    }
}
