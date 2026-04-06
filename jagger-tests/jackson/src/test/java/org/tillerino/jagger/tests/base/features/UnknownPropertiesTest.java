package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;

class UnknownPropertiesTest extends ReferenceTest {

    UnknownPropertiesSerde serde = SerdeUtil.impl(UnknownPropertiesSerde.class);

    @Test
    void jsonIgnorePropertiesIgnoreUnknownStringProperty() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": \"blergh\", \"value\": 123 }",
                serde::readJsonIgnorePropertiesIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonIgnorePropertiesIgnoreUnknownObjectProperty() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": { \"hello\": 456 }, \"value\": 123 }",
                serde::readJsonIgnorePropertiesIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonIgnorePropertiesIgnoreUnknownArrayProperty() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": [ \"blergh\" ], \"value\": 123 }",
                serde::readJsonIgnorePropertiesIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonConfigIgnoreStringProperty() throws Exception {
        inputUtils.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": \"blergh\", \"value\": 123 }",
                serde::readJsonConfigIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonConfigIgnoreObjectProperty() throws Exception {
        inputUtils.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": { \"hello\": 456 }, \"value\": 123 }",
                serde::readJsonConfigIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonConfigIgnoreArrayProperty() throws Exception {
        inputUtils.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": [ \"blergh\" ], \"value\": 123 }",
                serde::readJsonConfigIgnoreUnknown,
                new TypeReference<>() {});
    }
}
