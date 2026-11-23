package org.tillerino.jagger.helpers;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import org.tillerino.jagger.api.JaggerDatabindException;

public class JakartaJsonParserHelper {
    public static String readDiscriminator(String discriminatorName, JsonParserWrapper parser) throws IOException {
        if (parser.currentEvent() != Event.KEY_NAME) {
            throw new JaggerDatabindException(
                    "Expected field name, got " + parser.currentEvent() + " at " + parser.getLocation());
        } else if (!parser.getString().equals(discriminatorName)) {
            throw new JaggerDatabindException("Expected field name " + discriminatorName + ", got " + parser.getString()
                    + " at " + parser.getLocation());
        } else if (parser.next() != Event.VALUE_STRING) {
            throw new JaggerDatabindException(
                    "Expected string, got " + parser.currentEvent() + " at " + parser.getLocation());
        } else {
            String value = parser.getString();
            parser.next();
            return value;
        }
    }

    public static boolean nextIfCurrentTokenIs(JsonParserWrapper parser, Event token) throws IOException {
        if (parser.currentEvent() != token) {
            return false;
        }
        parser.next();
        return true;
    }

    public static void skip(JsonParserWrapper parser) {
        if (parser.currentEvent() == Event.START_ARRAY) {
            parser.skipArray();
        }
        if (parser.currentEvent() == Event.START_OBJECT) {
            parser.skipObject();
        }
    }

    public static class JsonParserWrapper implements JsonParser {
        private boolean ended = false;

        private Event currentEvent;

        private final JsonParser parser;

        public JsonParserWrapper(JsonParser parser) {
            this.parser = parser;
        }

        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException("just call next");
        }

        @Override
        public Event next() {
            if (ended || !parser.hasNext()) {
                ended = true;
                currentEvent = null;
                return null;
            }
            return currentEvent = parser.next();
        }

        @Override
        public Event currentEvent() {
            return currentEvent;
        }

        @Override
        public String getString() {
            return parser.getString();
        }

        @Override
        public boolean isIntegralNumber() {
            return parser.isIntegralNumber();
        }

        @Override
        public int getInt() {
            return parser.getInt();
        }

        @Override
        public long getLong() {
            return parser.getLong();
        }

        @Override
        public BigDecimal getBigDecimal() {
            return parser.getBigDecimal();
        }

        @Override
        public JsonLocation getLocation() {
            return parser.getLocation();
        }

        @Override
        public JsonObject getObject() {
            return parser.getObject();
        }

        @Override
        public JsonValue getValue() {
            return parser.getValue();
        }

        @Override
        public JsonArray getArray() {
            return parser.getArray();
        }

        @Override
        public Stream<JsonValue> getArrayStream() {
            return parser.getArrayStream();
        }

        @Override
        public Stream<Map.Entry<String, JsonValue>> getObjectStream() {
            return parser.getObjectStream();
        }

        @Override
        public Stream<JsonValue> getValueStream() {
            return parser.getValueStream();
        }

        @Override
        public void skipArray() {
            parser.skipArray();
        }

        @Override
        public void skipObject() {
            parser.skipObject();
        }

        @Override
        public void close() {
            parser.close();
        }
    }
}
