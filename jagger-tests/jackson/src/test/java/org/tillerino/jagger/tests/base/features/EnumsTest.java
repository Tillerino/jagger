package org.tillerino.jagger.tests.base.features;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.api.JaggerDatabindException;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.CustomNamedEnum;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.JsonValueWithCustomNames;
import org.tillerino.jagger.tests.model.features.EnumsModel.JsonValueEnum;
import tools.jackson.core.type.TypeReference;

public class EnumsTest extends ReferenceTest {
    EnumsSerde serde = SerdeUtil.impl(EnumsSerde.class);

    @Test
    public void jsonValueEnumRoundTrip() throws Exception {
        for (JsonValueEnum value : JsonValueEnum.values()) {
            outputUtils.roundTrip(value, serde::writeJsonValueEnum, serde::readJsonValueEnum, new TypeReference<>() {});
        }
    }

    @Test
    public void customNamedEnumRoundTrip() throws Exception {
        for (CustomNamedEnum value : CustomNamedEnum.values()) {
            outputUtils.roundTrip(value, serde::writeCustomNamed, serde::readCustomNamed, new TypeReference<>() {});
        }
    }

    @Test
    public void jsonValueWithCustomNamesRoundTrip() throws Exception {
        for (JsonValueWithCustomNames value : JsonValueWithCustomNames.values()) {
            outputUtils.roundTrip(
                    value,
                    serde::writeJsonValueWithCustomNames,
                    serde::readJsonValueWithCustomNames,
                    new TypeReference<>() {});
        }
    }

    @Test
    public void customNamedDeserializeWithEnumName() throws Exception {
        inputUtils.assertException(
                "\"VALUE1\"",
                serde::readCustomNamed,
                new TypeReference<>() {},
                "Unexpected enum value: \"VALUE1\"",
                "from String \"VALUE1\"");
        assertThatThrownBy(() -> inputUtils.deserialize("\"VALUE1\"", serde::readCustomNamed))
                .isInstanceOf(JaggerDatabindException.class);
    }

    @Test
    public void customNamedDeserializeWithCustomName() throws Exception {
        inputUtils.assertIsEqualToDatabind("\"value-one\"", serde::readCustomNamed, new TypeReference<>() {});
    }
}
