package org.tillerino.jagger.tests;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.json.stream.JsonGenerator;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableFunction;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.helpers.JakartaJsonParserHelper;

public record OutputUtils(InputUtils inputUtils) {

    public <T> String assertIsEqualToDatabind(T obj, FailableBiConsumer<T, JsonGenerator, Exception> output)
            throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize(obj, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T, U> String assertIsEqualToDatabind2(
            T obj, U obj2, FailableTriConsumer<T, JsonGenerator, U, Exception> output) throws Exception {
        return assertIsEqualToDatabind(obj, (o, writer) -> output.accept(o, writer, obj2));
    }

    public <T> String serialize(T obj, FailableBiConsumer<T, JsonGenerator, Exception> output) throws Exception {
        return ToShadeUtils.withGsonJsonWriter(generator -> output.accept(obj, generator));
    }

    public <T, U> String serialize2(T obj, U obj2, FailableTriConsumer<T, JsonGenerator, U, Exception> output)
            throws Exception {
        return ToShadeUtils.withGsonJsonWriter(generator -> output.accept(obj, generator, obj2));
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableTriConsumer<T, JsonGenerator, SerializationContext, Exception> output) throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours =
                ToShadeUtils.withGsonJsonWriter(generator -> output.accept(obj, generator, new SerializationContext()));
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T roundTrip(
            T obj,
            FailableBiConsumer<T, JsonGenerator, Exception> output,
            FailableFunction<JakartaJsonParserHelper.JsonParserWrapper, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabind(json, input, typeRef);
    }

    public <T, U> T roundTrip2(
            T obj,
            U obj2,
            FailableTriConsumer<T, JsonGenerator, U, Exception> output,
            FailableBiFunction<JakartaJsonParserHelper.JsonParserWrapper, U, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind2(obj, obj2, output);
        return inputUtils.assertIsEqualToDatabind2(json, obj2, input, typeRef);
    }

    public <T> T roundTripRecursive(
            T obj,
            FailableBiConsumer<T, JsonGenerator, Exception> output,
            FailableFunction<JakartaJsonParserHelper.JsonParserWrapper, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabindComparingRecursively(json, input, typeRef);
    }

    public interface FailableTriConsumer<T, U, V, E extends Throwable> {
        void accept(T t, U u, V v) throws E;
    }
}
