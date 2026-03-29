package org.tillerino.jagger.adapters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import org.tillerino.jagger.api.JaggerReader;

public class JacksonJsonParserAdapter implements JaggerReader<IOException> {
    protected final JsonParser parser;

    /**
     * Create a new JacksonJsonParserAdapter.
     *
     * @param parser you can easily create a {@link JsonParser} through
     *     {@link com.fasterxml.jackson.core.JsonFactory#createParser(java.io.InputStream)} and similar methods.
     * @throws IOException when retrieving the first token fails
     */
    public JacksonJsonParserAdapter(JsonParser parser) throws IOException {
        this.parser = parser;
        if (!parser.hasCurrentToken()) {
            parser.nextToken();
        }
    }

    @Override
    public boolean isObjectStart(Advance advance) throws IOException {
        return advanceIfTokenIs(JsonToken.START_OBJECT, advance);
    }

    @Override
    public boolean isObjectEnd(Advance advance) throws IOException {
        return advanceIfTokenIs(JsonToken.END_OBJECT, advance);
    }

    @Override
    public boolean isArrayStart(Advance advance) throws IOException {
        return advanceIfTokenIs(JsonToken.START_ARRAY, advance);
    }

    @Override
    public boolean isArrayEnd(Advance advance) throws IOException {
        return advanceIfTokenIs(JsonToken.END_ARRAY, advance);
    }

    @Override
    public boolean isNull(Advance advance) throws IOException {
        return advanceIfTokenIs(JsonToken.VALUE_NULL, advance);
    }

    private boolean advanceIfTokenIs(JsonToken expectedToken, Advance advance) throws IOException {
        if (parser.currentToken() == expectedToken) {
            advance(advance);
            return true;
        }
        return false;
    }

    @Override
    public boolean isBoolean() throws IOException {
        return parser.currentToken().isBoolean();
    }

    @Override
    public boolean isNumber() throws IOException {
        return parser.currentToken().isNumeric();
    }

    @Override
    public boolean isText() throws IOException {
        return parser.currentToken() == JsonToken.VALUE_STRING;
    }

    @Override
    public boolean isFieldName() throws IOException {
        return parser.currentToken() == JsonToken.FIELD_NAME;
    }

    @Override
    public boolean getBoolean(Advance advance) throws IOException {
        boolean value = parser.getBooleanValue();
        advance(advance);
        return value;
    }

    @Override
    public byte getByte(Advance advance) throws IOException {
        byte value = parser.getByteValue();
        advance(advance);
        return value;
    }

    @Override
    public short getShort(Advance advance) throws IOException {
        short value = parser.getShortValue();
        advance(advance);
        return value;
    }

    @Override
    public int getInt(Advance advance) throws IOException {
        int value = parser.getIntValue();
        advance(advance);
        return value;
    }

    @Override
    public long getLong(Advance advance) throws IOException {
        long value = parser.getLongValue();
        advance(advance);
        return value;
    }

    @Override
    public float getFloat(Advance advance) throws IOException {
        float value = parser.getFloatValue();
        advance(advance);
        return value;
    }

    @Override
    public double getDouble(Advance advance) throws IOException {
        double value = parser.getDoubleValue();
        advance(advance);
        return value;
    }

    @Override
    public String getText(Advance advance) throws IOException {
        String value = parser.getText();
        advance(advance);
        return value;
    }

    @Override
    public String getFieldName(Advance advance) throws IOException {
        String value = parser.currentName();
        advance(advance);
        return value;
    }

    @Override
    public String getDiscriminator(String expectedName, boolean visible) throws IOException {
        if (visible) {
            throw new UnsupportedOperationException("visible discriminator not implemented yet");
        }
        String fieldName = getFieldName(Advance.CONSUME);
        if (!fieldName.equals(expectedName)) {
            throw new IOException("Expected discriminator " + expectedName + " but got " + fieldName + " at "
                    + parser.currentLocation());
        }
        if (!isText()) {
            throw unexpectedToken("text");
        }
        return getText(Advance.CONSUME);
    }

    @Override
    public void skipChildren(Advance advance) throws IOException {
        parser.skipChildren();
        advance(advance);
    }

    @Override
    public IOException unexpectedToken(String expectedToken) {
        return new IOException(
                "Expected " + expectedToken + " but got " + parser.currentToken() + " at " + parser.currentLocation());
    }

    @Override
    public IOException unrecognizedProperty(String propertyName) {
        return new IOException("Unrecognized field \"" + propertyName + "\"" + " at " + parser.currentLocation());
    }

    private void advance(Advance advance) throws IOException {
        if (advance == Advance.CONSUME) {
            parser.nextToken();
        }
    }
}
