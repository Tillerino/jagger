package org.tillerino.jagger.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableFunction;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.tillerino.jagger.adapters.JacksonJsonParserAdapter;
import org.tillerino.jagger.api.DeserializationContext;

public class InputUtils {

    public ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    public <T> T withJsonParser(String json, FailableFunction<JacksonJsonParserAdapter, T, Exception> consumer)
            throws Exception {
        try (JsonParser parser = new JsonFactory().createParser(json)) {
            return consumer.apply(new JacksonJsonParserAdapter(parser));
        }
    }

    public <T> T assertIsEqualToDatabind(
            String json, FailableFunction<JacksonJsonParserAdapter, T, Exception> consumer, TypeReference<T> typeRef)
            throws Exception {
        T ours = deserialize(json, consumer);
        T databind = objectMapper.readValue(json, typeRef);
        assertThat(ours).isEqualTo(databind);
        return ours;
    }

    public <T> void assertException(
            String json,
            FailableFunction<JacksonJsonParserAdapter, T, Exception> consumer,
            TypeReference<T> typeRef,
            String ourMessage,
            String theirMessage) {
        assertThatThrownBy(() -> deserialize(json, consumer)).hasMessageContaining(ourMessage);
        assertThatThrownBy(() -> objectMapper.readValue(json, typeRef)).hasMessageContaining(theirMessage);
    }

    public <T> T deserialize(String json, FailableFunction<JacksonJsonParserAdapter, T, Exception> consumer)
            throws Exception {
        return withJsonParser(json, consumer);
    }

    public <T, U> T deserialize2(
            String json, U obj2, FailableBiFunction<JacksonJsonParserAdapter, U, T, Exception> consumer)
            throws Exception {
        return withJsonParser(json, parser -> consumer.apply(parser, obj2));
    }

    public <T> T assertIsEqualToDatabind(
            String json,
            FailableBiFunction<JacksonJsonParserAdapter, DeserializationContext, T, Exception> consumer,
            TypeReference<T> typeRef)
            throws Exception {
        return withJsonParser(json, parser -> {
            T ours = consumer.apply(parser, new DeserializationContext());
            T databind = objectMapper.readValue(json, typeRef);
            assertThat(ours).isEqualTo(databind);
            return ours;
        });
    }

    public <T, U> T assertIsEqualToDatabind2(
            String json,
            U arg2,
            FailableBiFunction<JacksonJsonParserAdapter, U, T, Exception> consumer,
            TypeReference<T> typeRef)
            throws Exception {
        T ours = deserialize2(json, arg2, consumer);
        T databind = objectMapper.readValue(json, typeRef);
        assertThat(ours).isEqualTo(databind);
        return ours;
    }

    public <T> T assertIsEqualToDatabindComparingRecursively(
            String json, FailableFunction<JacksonJsonParserAdapter, T, Exception> consumer, TypeReference<T> typeRef)
            throws Exception {
        return withJsonParser(json, parser -> {
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
