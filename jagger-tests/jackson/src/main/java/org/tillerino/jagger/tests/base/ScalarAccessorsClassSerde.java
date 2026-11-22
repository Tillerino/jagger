package org.tillerino.jagger.tests.base;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarAccessorsClass;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface ScalarAccessorsClassSerde {
    @JsonOutput
    void write(ScalarAccessorsClass record, JsonGenerator generator) throws Exception;

    @JsonInput
    ScalarAccessorsClass read(JsonParser parser) throws Exception;
}
