package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.PropertyNameModel.JsonPropertyCustomName;
import org.tillerino.jagger.tests.model.features.PropertyNameModel.JsonPropertyCustomNameWithSetter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface PropertyNameSerde {
    @JsonOutput
    void writeCustomName(JsonPropertyCustomName obj, JsonGenerator out) throws Exception;

    @JsonInput
    JsonPropertyCustomName readCustomName(JsonParser in) throws Exception;

    @JsonOutput
    void writeCustomNameWithSetter(JsonPropertyCustomNameWithSetter obj, JsonGenerator out) throws Exception;

    @JsonInput
    JsonPropertyCustomNameWithSetter readCustomNameWithSetter(JsonParser in) throws Exception;
}
