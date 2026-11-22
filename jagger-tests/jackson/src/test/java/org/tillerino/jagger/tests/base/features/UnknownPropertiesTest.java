package org.tillerino.jagger.tests.base.features;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.UnknownPropertiesModel.JsonConfigThrow;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;

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
        inputUtils.objectMapper = inputUtils
                .objectMapper
                .rebuild()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": \"blergh\", \"value\": 123 }",
                serde::readJsonConfigIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonConfigIgnoreObjectProperty() throws Exception {
        inputUtils.objectMapper = inputUtils
                .objectMapper
                .rebuild()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": { \"hello\": 456 }, \"value\": 123 }",
                serde::readJsonConfigIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonConfigIgnoreArrayProperty() throws Exception {
        inputUtils.objectMapper = inputUtils
                .objectMapper
                .rebuild()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\", \"unknown\": [ \"blergh\" ], \"value\": 123 }",
                serde::readJsonConfigIgnoreUnknown,
                new TypeReference<>() {});
    }

    @Test
    void jsonConfigThrowUnknownProperty() {
        inputUtils.objectMapper = inputUtils
                .objectMapper
                .rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        inputUtils.assertException(
                "{ \"name\": \"Moopsy\", \"unknown\": \"blergh\", \"value\": 123 }",
                serde::readJsonConfigThrow,
                new TypeReference<JsonConfigThrow>() {},
                "Unrecognized field \"unknown\"",
                "Unrecognized property \"unknown\"");
    }
}
