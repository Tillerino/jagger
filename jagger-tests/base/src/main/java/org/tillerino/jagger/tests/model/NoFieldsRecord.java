package org.tillerino.jagger.tests.model;

import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;

public record NoFieldsRecord() {
    interface Input {
        @JsonInput
        NoFieldsRecord read(JsonParser parser) throws Exception;
    }
}
