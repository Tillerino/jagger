package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.EnumsModel.JsonValueEnum;

public interface EnumsSerde {
    @JsonOutput
    void writeJsonValueEnum(JsonValueEnum enumValue, JsonGenerator gen) throws Exception;

    @JsonInput
    JsonValueEnum readJsonValueEnum(JsonParser parser) throws Exception;
}
