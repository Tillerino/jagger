package org.tillerino.jagger.helpers;

import java.io.IOException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class JacksonJsonParserReaderHelper {
    public static String readDiscriminator(String discriminatorName, JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.PROPERTY_NAME) {
            throw new IOException(
                    "Expected field name, got " + parser.currentToken() + " at " + parser.currentLocation());
        } else if (!parser.currentName().equals(discriminatorName)) {
            throw new IOException("Expected field name " + discriminatorName + ", got " + parser.currentName() + " at "
                    + parser.currentLocation());
        } else if (parser.nextToken() != JsonToken.VALUE_STRING) {
            throw new IOException("Expected string, got " + parser.currentToken() + " at " + parser.currentLocation());
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
