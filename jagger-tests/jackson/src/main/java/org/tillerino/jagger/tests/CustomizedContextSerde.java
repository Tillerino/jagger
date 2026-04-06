package org.tillerino.jagger.tests;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;

@JsonConfig(uses = CustomizedContextSerde.WeirdIntSerde.class)
public interface CustomizedContextSerde {
    @JsonOutput
    void writeMyObj(List<MyObj> o, JsonGenerator generator, CustomSerializationContext ctx) throws IOException;

    @JsonInput
    List<MyObj> readMyObj(JsonParser parser, CustomDeserializationContext ctx) throws IOException;

    class CustomDeserializationContext extends DeserializationContext {
        int counter = 0;
    }

    class CustomSerializationContext extends SerializationContext {
        int counter = 0;
    }

    record MyObj(int i) {}

    interface WeirdIntSerde {
        @JsonInput
        default int deserializeInt(JsonParser p, CustomDeserializationContext ctx) throws IOException {
            Validate.isTrue(p.currentToken().isNumeric());
            int val = p.getIntValue() + ctx.counter++;
            p.nextToken();
            return val;
        }

        @JsonOutput
        default void serializeInt(int i, JsonGenerator g, CustomSerializationContext ctx) throws IOException {
            g.writeNumber(i + ctx.counter++);
        }
    }
}
