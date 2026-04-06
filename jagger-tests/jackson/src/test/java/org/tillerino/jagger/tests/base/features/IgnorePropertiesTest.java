package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.InnerJsonIgnoreProperties;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.JsonIgnorePropertiesValue;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.OuterJsonIgnoreProperties;

class IgnorePropertiesTest extends ReferenceTest {

    IgnorePropertiesSerde serde = SerdeUtil.impl(IgnorePropertiesSerde.class);

    @Test
    void ignoreSpecificPropertiesInInput() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                """
                        { "name": "Moopsy", "value": 123, "ignoredValue": "ignored", "renamed": "not ignored",
                          "ignoredArray": [ "a", "b" ], "ignoredObject": { "a": "b" } }
                    """,
                serde::readJsonIgnorePropertiesValue,
                new TypeReference<>() {});
    }

    @Test
    void ignoreSpecificPropertiesInOutput() throws Exception {
        JsonIgnorePropertiesValue value =
                new JsonIgnorePropertiesValue("Moopsy", 123, "ignored", "not actually ignored");
        outputUtils.assertIsEqualToDatabind(value, serde::writeJsonIgnorePropertiesValue);
    }

    @Test
    void nestedIgnoreSpecificPropertiesWrite() throws Exception {
        outputUtils.assertIsEqualToDatabind(
                new OuterJsonIgnoreProperties("foo", new InnerJsonIgnoreProperties("bar")),
                serde::writeOuterJsonIgnoreProperties);
    }

    @Test
    void nestedIgnoreSpecificPropertiesRead() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"ignored\": \"foo\", \"inner\": { \"ignored\": \"bar\" }}",
                serde::readOuterJsonIgnoreProperties,
                new TypeReference<OuterJsonIgnoreProperties>() {});
    }
}
