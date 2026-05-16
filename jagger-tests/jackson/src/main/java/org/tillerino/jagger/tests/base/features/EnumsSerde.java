package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.CustomNamedEnum;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.JsonValueWithCustomNames;
import org.tillerino.jagger.tests.model.features.EnumsModel.JsonValueEnum;

public interface EnumsSerde {
    @JsonOutput
    void writeJsonValueEnum(JsonValueEnum enumValue, JsonGenerator gen) throws Exception;

    @JsonInput
    JsonValueEnum readJsonValueEnum(JsonParser parser) throws Exception;

    @JsonOutput
    void writeCustomNamed(CustomNamedEnum enumValue, JsonGenerator gen) throws Exception;

    @JsonInput
    CustomNamedEnum readCustomNamed(JsonParser parser) throws Exception;

    @JsonOutput
    void writeJsonValueWithCustomNames(JsonValueWithCustomNames enumValue, JsonGenerator gen) throws Exception;

    @JsonInput
    JsonValueWithCustomNames readJsonValueWithCustomNames(JsonParser parser) throws Exception;
}
