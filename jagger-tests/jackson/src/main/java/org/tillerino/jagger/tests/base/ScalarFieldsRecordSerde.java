package org.tillerino.jagger.tests.base;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface ScalarFieldsRecordSerde {
    @JsonOutput
    void write(ScalarFieldsRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    ScalarFieldsRecord read(JsonParser parser) throws Exception;
}
