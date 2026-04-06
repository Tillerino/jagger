package org.tillerino.jagger.tests;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.tillerino.jagger.adapters.JacksonJsonNodeReaderAdapter;
import org.tillerino.jagger.adapters.JacksonJsonNodeWriterAdapter;
import org.tillerino.jagger.api.SerializationContext;

public record OutputUtils(InputUtils inputUtils) {
    public String withJsonGenerator(FailableConsumer<JacksonJsonNodeWriterAdapter, Exception> output) throws Exception {
        JacksonJsonNodeWriterAdapter adapter = new JacksonJsonNodeWriterAdapter(ToShadeHelper.jsonNodeFactory());
        output.accept(adapter);
        return adapter.getResult().toString();
    }

    public <T> String serialize(T obj, FailableBiConsumer<T, JacksonJsonNodeWriterAdapter, Exception> output)
            throws Exception {
        return withJsonGenerator(generator -> output.accept(obj, generator));
    }

    public <T, U> String serialize2(
            T obj, U obj2, FailableTriConsumer<T, JacksonJsonNodeWriterAdapter, U, Exception> output) throws Exception {
        return withJsonGenerator(generator -> output.accept(obj, generator, obj2));
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableBiConsumer<T, JacksonJsonNodeWriterAdapter, Exception> output) throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize(obj, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableTriConsumer<T, JacksonJsonNodeWriterAdapter, SerializationContext, Exception> output)
            throws Exception {
        return assertIsEqualToDatabind2(
                obj, new SerializationContext(), (obj2, generator, context) -> output.accept(obj, generator, context));
    }

    public <T, U> String assertIsEqualToDatabind2(
            T obj, U obj2, FailableTriConsumer<T, JacksonJsonNodeWriterAdapter, U, Exception> output) throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize2(obj, obj2, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T roundTrip(
            T obj,
            FailableBiConsumer<T, JacksonJsonNodeWriterAdapter, Exception> output,
            FailableFunction<JacksonJsonNodeReaderAdapter, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabind(json, input, typeRef);
    }

    public <T, U> T roundTrip2(
            T obj,
            U obj2,
            FailableTriConsumer<T, JacksonJsonNodeWriterAdapter, U, Exception> output,
            FailableBiFunction<JacksonJsonNodeReaderAdapter, U, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind2(obj, obj2, output);
        return inputUtils.assertIsEqualToDatabind2(json, obj2, input, typeRef);
    }

    public <T> T roundTripRecursive(
            T obj,
            FailableBiConsumer<T, JacksonJsonNodeWriterAdapter, Exception> output,
            FailableFunction<JacksonJsonNodeReaderAdapter, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabindComparingRecursively(json, input, typeRef);
    }

    public interface FailableTriConsumer<T, U, V, E extends Throwable> {
        void accept(T t, U u, V v) throws E;
    }
}
