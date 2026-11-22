package org.tillerino.jagger.tests;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.tillerino.jagger.adapters.Jackson2JsonNodeReaderAdapter;
import org.tillerino.jagger.adapters.Jackson2JsonNodeWriterAdapter;
import org.tillerino.jagger.api.SerializationContext;
import tools.jackson.core.type.TypeReference;

public record OutputUtils(InputUtils inputUtils) {
    public String withWriter(FailableConsumer<Jackson2JsonNodeWriterAdapter, Exception> output) throws Exception {
        Jackson2JsonNodeWriterAdapter adapter = new Jackson2JsonNodeWriterAdapter(ToShadeHelper.jsonNodeFactory());
        output.accept(adapter);
        return adapter.getResult().toString();
    }

    public <T> String serialize(T obj, FailableBiConsumer<T, Jackson2JsonNodeWriterAdapter, Exception> output)
            throws Exception {
        return withWriter(generator -> output.accept(obj, generator));
    }

    public <T, U> String serialize2(
            T obj, U obj2, FailableTriConsumer<T, Jackson2JsonNodeWriterAdapter, U, Exception> output)
            throws Exception {
        return withWriter(generator -> output.accept(obj, generator, obj2));
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableBiConsumer<T, Jackson2JsonNodeWriterAdapter, Exception> output) throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize(obj, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableTriConsumer<T, Jackson2JsonNodeWriterAdapter, SerializationContext, Exception> output)
            throws Exception {
        return assertIsEqualToDatabind2(
                obj, new SerializationContext(), (obj2, generator, context) -> output.accept(obj, generator, context));
    }

    public <T, U> String assertIsEqualToDatabind2(
            T obj, U obj2, FailableTriConsumer<T, Jackson2JsonNodeWriterAdapter, U, Exception> output)
            throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize2(obj, obj2, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T roundTrip(
            T obj,
            FailableBiConsumer<T, Jackson2JsonNodeWriterAdapter, Exception> output,
            FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabind(json, input, typeRef);
    }

    public <T, U> T roundTrip2(
            T obj,
            U obj2,
            FailableTriConsumer<T, Jackson2JsonNodeWriterAdapter, U, Exception> output,
            FailableBiFunction<Jackson2JsonNodeReaderAdapter, U, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind2(obj, obj2, output);
        return inputUtils.assertIsEqualToDatabind2(json, obj2, input, typeRef);
    }

    public <T> T roundTripRecursive(
            T obj,
            FailableBiConsumer<T, Jackson2JsonNodeWriterAdapter, Exception> output,
            FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabindComparingRecursively(json, input, typeRef);
    }

    public interface FailableTriConsumer<T, U, V, E extends Throwable> {
        void accept(T t, U u, V v) throws E;
    }
}
