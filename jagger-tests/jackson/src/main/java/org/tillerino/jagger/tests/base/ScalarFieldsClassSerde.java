package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarFieldsClass;

public interface ScalarFieldsClassSerde {
    @JsonOutput
    void write(ScalarFieldsClass record, JsonGenerator generator) throws Exception;

    @JsonInput
    ScalarFieldsClass read(JsonParser parser) throws Exception;
}
