package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarAccessorsClass;

public interface ScalarAccessorsClassSerde {
    @JsonOutput
    void write(ScalarAccessorsClass record, JsonGenerator generator) throws Exception;

    @JsonInput
    ScalarAccessorsClass read(JsonParser parser) throws Exception;
}
