package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

public interface ScalarFieldsRecordSerde {
    @JsonOutput
    void write(ScalarFieldsRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    ScalarFieldsRecord read(JsonParser parser) throws Exception;
}
