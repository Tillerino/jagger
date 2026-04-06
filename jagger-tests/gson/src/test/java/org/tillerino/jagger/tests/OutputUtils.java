package org.tillerino.jagger.tests;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.tillerino.jagger.api.SerializationContext;

public record OutputUtils(InputUtils inputUtils) {

    public String withGsonJsonWriter(FailableConsumer<JsonWriter, Exception> output) throws Exception {
        StringWriter out = new StringWriter();
        JsonWriter generator = new JsonWriter(out);
        output.accept(generator);
        generator.flush();
        return out.toString();
    }

    public <T> String assertIsEqualToDatabind(T obj, FailableBiConsumer<T, JsonWriter, Exception> output)
            throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize(obj, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T, U> String assertIsEqualToDatabind2(
            T obj, U obj2, FailableTriConsumer<T, JsonWriter, U, Exception> output) throws Exception {
        return assertIsEqualToDatabind(obj, (o, writer) -> output.accept(o, writer, obj2));
    }

    public <T> String serialize(T obj, FailableBiConsumer<T, JsonWriter, Exception> output) throws Exception {
        return withGsonJsonWriter(generator -> output.accept(obj, generator));
    }

    public <T, U> String serialize2(T obj, U obj2, FailableTriConsumer<T, JsonWriter, U, Exception> output)
            throws Exception {
        return withGsonJsonWriter(generator -> output.accept(obj, generator, obj2));
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableTriConsumer<T, JsonWriter, SerializationContext, Exception> output) throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = withGsonJsonWriter(generator -> output.accept(obj, generator, new SerializationContext()));
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T roundTrip(
            T obj,
            FailableBiConsumer<T, JsonWriter, Exception> output,
            FailableFunction<JsonReader, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabind(json, input, typeRef);
    }

    public <T, U> T roundTrip2(
            T obj,
            U obj2,
            FailableTriConsumer<T, JsonWriter, U, Exception> output,
            FailableBiFunction<JsonReader, U, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind2(obj, obj2, output);
        return inputUtils.assertIsEqualToDatabind2(json, obj2, input, typeRef);
    }

    public <T> T roundTripRecursive(
            T obj,
            FailableBiConsumer<T, JsonWriter, Exception> output,
            FailableFunction<JsonReader, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabindComparingRecursively(json, input, typeRef);
    }

    public interface FailableTriConsumer<T, U, V, E extends Throwable> {
        void accept(T t, U u, V v) throws E;
    }
}
