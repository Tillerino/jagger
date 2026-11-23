package org.tillerino.jagger.helpers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import org.tillerino.jagger.api.JaggerDatabindException;

public class Jackson2JsonParserReaderHelper {
    public static String readDiscriminator(String discriminatorName, JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.FIELD_NAME) {
            throw new JaggerDatabindException(
                    "Expected field name, got " + parser.currentToken() + " at " + parser.currentLocation());
        } else if (!parser.currentName().equals(discriminatorName)) {
            throw new JaggerDatabindException("Expected field name " + discriminatorName + ", got "
                    + parser.currentName() + " at " + parser.currentLocation());
        } else if (parser.nextToken() != JsonToken.VALUE_STRING) {
            throw new JaggerDatabindException(
                    "Expected string, got " + parser.currentToken() + " at " + parser.currentLocation());
        } else {
            String value = parser.getText();
            parser.nextToken();
            return value;
        }
    }

    public static boolean nextIfCurrentTokenIs(JsonParser parser, JsonToken token) throws IOException {
        if (parser.currentToken() != token) {
            return false;
        }
        parser.nextToken();
        return true;
    }
}
