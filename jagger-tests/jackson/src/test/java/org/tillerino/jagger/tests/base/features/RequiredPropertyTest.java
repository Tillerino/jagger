package org.tillerino.jagger.tests.base.features;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.MixedFields;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.RequiredFields;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.RequiredPropertyWithSetter;

class RequiredPropertyTest extends ReferenceTest {
    RequiredPropertySerde serde = SerdeUtil.impl(RequiredPropertySerde.class);

    @Test
    void requiredFields_success() throws Exception {
        // null properties are technically present
        outputUtils.roundTrip(
                new RequiredFields(null, null),
                serde::writeRequiredFields,
                serde::readRequiredFields,
                new TypeReference<>() {});
    }

    @Test
    void requiredFields_missingName() {
        assertThatThrownBy(() -> inputUtils.deserialize("{\"age\": 30}", serde::readRequiredFields))
                .hasMessageContaining("Missing property name");
    }

    @Test
    void requiredFields_missingAge() {
        assertThatThrownBy(() -> inputUtils.deserialize("{\"name\": \"John\"}", serde::readRequiredFields))
                .hasMessageContaining("Missing property age");
    }

    @Test
    void mixedFields_success() throws Exception {
        // null properties are technically present
        outputUtils.roundTrip(
                new MixedFields(null, null, null),
                serde::writeMixedFields,
                serde::readMixedFields,
                new TypeReference<>() {});
    }

    @Test
    void mixedFields_missingRequired() {
        assertThatThrownBy(() -> inputUtils.deserialize("{\"age\": 30}", serde::readMixedFields))
                .hasMessageContaining("Missing property name");
    }

    @Test
    void mixedFields_onlyRequired() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\" }", serde::readMixedFields, new TypeReference<>() {});
    }

    @Test
    void requiredPropertyWithSetter_success() throws Exception {
        // null properties are technically present
        outputUtils.roundTrip(
                new RequiredPropertyWithSetter(null, null),
                serde::writeRequiredPropertyWithSetter,
                serde::readRequiredPropertyWithSetter,
                new TypeReference<>() {});
    }

    @Test
    void requiredPropertyWithSetter_missingRequired() {
        assertThatThrownBy(() -> inputUtils.deserialize(
                        "{\"description\": \"Developer\"}", serde::readRequiredPropertyWithSetter))
                .hasMessageContaining("Missing property name");
    }

    @Test
    void requiredPropertyWithSetter_onlyRequired() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"name\": \"Moopsy\" }", serde::readRequiredPropertyWithSetter, new TypeReference<>() {});
    }
}
