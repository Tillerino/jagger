package org.tillerino.jagger.tests.base;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarFieldsClass;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface ScalarFieldsClassSerde {
    @JsonOutput
    void write(ScalarFieldsClass record, JsonGenerator generator) throws Exception;

    @JsonInput
    ScalarFieldsClass read(JsonParser parser) throws Exception;
}
