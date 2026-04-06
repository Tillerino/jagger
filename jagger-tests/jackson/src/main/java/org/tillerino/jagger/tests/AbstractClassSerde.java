package org.tillerino.jagger.tests;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

public abstract class AbstractClassSerde {
    final ClassLoader cl;

    protected AbstractClassSerde(ClassLoader cl) {
        this.cl = cl;
    }

    @JsonOutput
    abstract void writeScalarFieldsRecord(ScalarFieldsRecord scalarFieldsRecord, JsonGenerator gen) throws Exception;

    @JsonInput
    abstract ScalarFieldsRecord readScalarFieldsRecord(JsonParser parser) throws Exception;
}
