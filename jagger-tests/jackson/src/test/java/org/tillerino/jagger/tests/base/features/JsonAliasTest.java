package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.JsonAliasModel;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.InnerJsonAlias;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.JsonAliasValue;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.NestedJsonAlias;

class JsonAliasTest extends ReferenceTest {
    JsonAliasSerde serde = SerdeUtil.impl(JsonAliasSerde.class);

    @Test
    void readWithAlias() throws Exception {
        inputUtils.assertIsEqualToDatabind("""
                { "name": "Moopsy", "value": 123 }
            """, serde::readJsonAliasValue, new TypeReference<>() {});
    }

    @Test
    void readWithFullNameAlias() throws Exception {
        inputUtils.assertIsEqualToDatabind("""
                { "fullName": "Moopsy", "val": 123 }
            """, serde::readJsonAliasValue, new TypeReference<>() {});
    }

    @Test
    void writeJsonAliasValue() throws Exception {
        JsonAliasValue value = new JsonAliasValue("Moopsy", 123);
        outputUtils.assertIsEqualToDatabind(value, serde::writeJsonAliasValue);
    }

    @Test
    void nestedReadWithAlias() throws Exception {
        inputUtils.assertIsEqualToDatabind("""
                { "outer": "foo", "inner": { "value": "bar" } }
            """, serde::readNestedJsonAlias, new TypeReference<>() {});
    }

    @Test
    void nestedReadWithInnerAlias() throws Exception {
        inputUtils.assertIsEqualToDatabind("""
                { "outer": "foo", "inner": { "innerAlias2": "bar" } }
            """, serde::readNestedJsonAlias, new TypeReference<>() {});
    }

    @Test
    void nestedWriteJsonAlias() throws Exception {
        NestedJsonAlias value = new NestedJsonAlias("foo", new InnerJsonAlias("bar"));
        outputUtils.assertIsEqualToDatabind(value, serde::writeNestedJsonAlias);
    }

    @Test
    void innerReadWithAlias() throws Exception {
        inputUtils.assertIsEqualToDatabind("""
                { "innerAlias1": "Moopsy" }
            """, serde::readInnerJsonAlias, new TypeReference<>() {});
    }

    @Nested
    class Enums {
        JsonAliasSerde.Enums serde = SerdeUtil.impl(JsonAliasSerde.Enums.class);

        @Test
        void onlyAliasesRoundTrip() throws Exception {
            for (JsonAliasModel.Enums.OnlyAliases value : JsonAliasModel.Enums.OnlyAliases.values()) {
                outputUtils.roundTrip(value, serde::writeOnlyAliases, serde::readOnlyAliases, new TypeReference<>() {});
            }
        }

        @Test
        void onlyAliasesDeserializeWithAlias() throws Exception {
            inputUtils.assertIsEqualToDatabind("\"alias-one\"", serde::readOnlyAliases, new TypeReference<>() {});
            inputUtils.assertIsEqualToDatabind("\"alias1\"", serde::readOnlyAliases, new TypeReference<>() {});
            inputUtils.assertIsEqualToDatabind("\"alias-two\"", serde::readOnlyAliases, new TypeReference<>() {});
        }

        @Test
        void onlyAliasesDeserializeWithEnumName() throws Exception {
            inputUtils.assertIsEqualToDatabind("\"VALUE1\"", serde::readOnlyAliases, new TypeReference<>() {});
            inputUtils.assertIsEqualToDatabind("\"VALUE2\"", serde::readOnlyAliases, new TypeReference<>() {});
            inputUtils.assertIsEqualToDatabind("\"VALUE3\"", serde::readOnlyAliases, new TypeReference<>() {});
        }

        @Test
        void aliasAndJsonPropertyRoundTrip() throws Exception {
            for (JsonAliasModel.Enums.AliasAndJsonProperty value : JsonAliasModel.Enums.AliasAndJsonProperty.values()) {
                outputUtils.roundTrip(
                        value,
                        serde::writeAliasAndJsonProperty,
                        serde::readAliasAndJsonProperty,
                        new TypeReference<>() {});
            }
        }

        @Test
        void aliasAndJsonPropertyDeserializeWithAlias() throws Exception {
            inputUtils.assertIsEqualToDatabind(
                    "\"customAlias\"", serde::readAliasAndJsonProperty, new TypeReference<>() {});
            inputUtils.assertIsEqualToDatabind("\"cv\"", serde::readAliasAndJsonProperty, new TypeReference<>() {});
        }

        @Test
        void aliasAndJsonPropertyDeserializeWithCustomName() throws Exception {
            inputUtils.assertIsEqualToDatabind(
                    "\"custom-value\"", serde::readAliasAndJsonProperty, new TypeReference<>() {});
        }
    }
}
