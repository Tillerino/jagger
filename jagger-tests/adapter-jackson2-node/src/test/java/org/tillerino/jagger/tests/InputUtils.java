package org.tillerino.jagger.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableFunction;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.tillerino.jagger.adapters.Jackson2JsonNodeReaderAdapter;
import org.tillerino.jagger.api.DeserializationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class InputUtils {

    public JsonMapper objectMapper = JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();

    public <T> T withReader(String json, FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> consumer)
            throws Exception {
        return consumer.apply(new Jackson2JsonNodeReaderAdapter(ToShadeHelper.readNode(json)));
    }

    public <T> T assertIsEqualToDatabind(
            String json,
            FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> consumer,
            TypeReference<T> typeRef)
            throws Exception {
        T ours = deserialize(json, consumer);
        T databind = objectMapper.readValue(json, typeRef);
        assertThat(ours).isEqualTo(databind);
        return ours;
    }

    public <T> void assertException(
            String json,
            FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> consumer,
            TypeReference<T> typeRef,
            String ourMessage,
            String theirMessage) {
        assertThatThrownBy(() -> deserialize(json, consumer)).hasMessageContaining(ourMessage);
        assertThatThrownBy(() -> objectMapper.readValue(json, typeRef)).hasMessageContaining(theirMessage);
    }

    public <T> T deserialize(String json, FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> consumer)
            throws Exception {
        return withReader(json, consumer);
    }

    public <T, U> T deserialize2(
            String json, U obj2, FailableBiFunction<Jackson2JsonNodeReaderAdapter, U, T, Exception> consumer)
            throws Exception {
        return withReader(json, parser -> consumer.apply(parser, obj2));
    }

    public <T> T assertIsEqualToDatabind(
            String json,
            FailableBiFunction<Jackson2JsonNodeReaderAdapter, DeserializationContext, T, Exception> consumer,
            TypeReference<T> typeRef)
            throws Exception {
        return withReader(json, parser -> {
            T ours = consumer.apply(parser, new DeserializationContext());
            T databind = objectMapper.readValue(json, typeRef);
            assertThat(ours).isEqualTo(databind);
            return ours;
        });
    }

    public <T, U> T assertIsEqualToDatabind2(
            String json,
            U arg2,
            FailableBiFunction<Jackson2JsonNodeReaderAdapter, U, T, Exception> consumer,
            TypeReference<T> typeRef)
            throws Exception {
        T ours = deserialize2(json, arg2, consumer);
        T databind = objectMapper.readValue(json, typeRef);
        assertThat(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T assertIsEqualToDatabindComparingRecursively(
            String json,
            FailableFunction<Jackson2JsonNodeReaderAdapter, T, Exception> consumer,
            TypeReference<T> typeRef)
            throws Exception {
        return withReader(json, parser -> {
            T ours = consumer.apply(parser);
            T databind = objectMapper.readValue(json, typeRef);
            assertEqualsComparingRecursively(ours, databind);
            return ours;
        });
    }

    private <T> void assertEqualsComparingRecursively(T ours, T databind) {
        assertThat(ours)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withStrictTypeChecking(true)
                        .withComparatorForType(Float::compare, Float.class)
                        .withComparatorForType(Double::compare, Double.class)
                        .build())
                .isEqualTo(databind);
    }
}
