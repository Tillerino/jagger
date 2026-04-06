package org.tillerino.jagger.tests;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.tillerino.jagger.api.SerializationContext;

public record OutputUtils(InputUtils inputUtils) {

    public String withWriter(FailableConsumer<JSONWriter, Exception> output) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONWriter writer = JSONWriter.of();
        output.accept(writer);
        writer.flushTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    public <T> String assertIsEqualToDatabind(T obj, FailableBiConsumer<T, JSONWriter, Exception> output)
            throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = serialize(obj, output);
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T, U> String assertIsEqualToDatabind2(
            T obj, U obj2, FailableTriConsumer<T, JSONWriter, U, Exception> output) throws Exception {
        return assertIsEqualToDatabind(obj, (o, writer) -> output.accept(o, writer, obj2));
    }

    public <T> String serialize(T obj, FailableBiConsumer<T, JSONWriter, Exception> output) throws Exception {
        return withWriter(generator -> output.accept(obj, generator));
    }

    public <T, U> String serialize2(T obj, U obj2, FailableTriConsumer<T, JSONWriter, U, Exception> output)
            throws Exception {
        return withWriter(generator -> output.accept(obj, generator, obj2));
    }

    public <T> String assertIsEqualToDatabind(
            T obj, FailableTriConsumer<T, JSONWriter, SerializationContext, Exception> output) throws Exception {
        String databind = inputUtils.objectMapper.writeValueAsString(obj);
        String ours = withWriter(generator -> output.accept(obj, generator, new SerializationContext()));
        assertThatJson(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T roundTrip(
            T obj,
            FailableBiConsumer<T, JSONWriter, Exception> output,
            FailableFunction<JSONReader, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabind(json, input, typeRef);
    }

    public <T, U> T roundTrip2(
            T obj,
            U obj2,
            FailableTriConsumer<T, JSONWriter, U, Exception> output,
            FailableBiFunction<JSONReader, U, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind2(obj, obj2, output);
        return inputUtils.assertIsEqualToDatabind2(json, obj2, input, typeRef);
    }

    public <T> T roundTripRecursive(
            T obj,
            FailableBiConsumer<T, JSONWriter, Exception> output,
            FailableFunction<JSONReader, T, Exception> input,
            TypeReference<T> typeRef)
            throws Exception {
        String json = assertIsEqualToDatabind(obj, output);
        return inputUtils.assertIsEqualToDatabindComparingRecursively(json, input, typeRef);
    }

    public interface FailableTriConsumer<T, U, V, E extends Throwable> {
        void accept(T t, U u, V v) throws E;
    }
}
