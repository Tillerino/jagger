package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.EnumsModel.JsonValueEnum;

public class EnumsTest extends ReferenceTest {
    EnumsSerde serde = SerdeUtil.impl(EnumsSerde.class);

    @Test
    public void jsonValueEnumRoundTrip() throws Exception {
        for (JsonValueEnum value : JsonValueEnum.values()) {
            outputUtils.roundTrip(value, serde::writeJsonValueEnum, serde::readJsonValueEnum, new TypeReference<>() {});
        }
    }
}
