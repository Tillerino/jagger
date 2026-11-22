package org.tillerino.jagger.tests.base;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.PrimitiveArrayFieldsRecord;
import org.tillerino.jagger.tests.model.ReferenceArrayFieldsRecord;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface ArrayFieldsRecordSerde {
    @JsonOutput
    void writePrimitive(PrimitiveArrayFieldsRecord record, JsonGenerator out) throws Exception;

    @JsonInput
    PrimitiveArrayFieldsRecord readPrimitive(JsonParser in) throws Exception;

    @JsonOutput
    void writeReference(ReferenceArrayFieldsRecord record, JsonGenerator out) throws Exception;

    @JsonInput
    ReferenceArrayFieldsRecord readReference(JsonParser in) throws Exception;
}
