package org.tillerino.jagger.tests;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;

interface CustomizedSerialization {
    @JsonOutput
    void writeMyObj(MyObj o, JsonGenerator generator) throws IOException;

    @JsonInput
    MyObj readMyObj(JsonParser parser) throws IOException;

    @JsonOutput
    default void writeOffsetDate(OffsetDateTime timestamp, JsonGenerator generator) throws IOException {
        generator.writeString(timestamp.toString());
    }

    @JsonInput
    default OffsetDateTime readOffsetDate(JsonParser parser) throws IOException {
        OffsetDateTime parse = OffsetDateTime.parse(parser.getText());
        parser.nextToken();
        return parse;
    }

    record MyObj(OffsetDateTime t) {}
}
