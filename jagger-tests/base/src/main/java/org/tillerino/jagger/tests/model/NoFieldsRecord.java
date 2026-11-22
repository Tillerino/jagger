package org.tillerino.jagger.tests.model;

import org.tillerino.jagger.annotations.JsonInput;
import tools.jackson.core.JsonParser;

public record NoFieldsRecord() {
    interface Input {
        @JsonInput
        NoFieldsRecord read(JsonParser parser) throws Exception;
    }
}
