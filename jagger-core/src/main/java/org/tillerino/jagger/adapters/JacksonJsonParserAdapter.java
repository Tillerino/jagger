package org.tillerino.jagger.adapters;

import java.io.IOException;
import org.tillerino.jagger.api.JaggerDatabindException;
import org.tillerino.jagger.api.JaggerReader;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

public class JacksonJsonParserAdapter implements JaggerReader<IOException> {
    protected final JsonParser parser;

    /**
     * Create a new JacksonJsonParserAdapter.
     *
     * @param parser you can easily create a {@link JsonParser} through
     *     {@link tools.jackson.core.json.JsonFactory#createParser(java.io.InputStream)} and similar methods.
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
        return parser.currentToken() == JsonToken.PROPERTY_NAME;
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
            throw new JaggerDatabindException("Expected discriminator " + expectedName + " but got " + fieldName
                    + " at " + parser.currentLocation());
        }
        if (!isText()) {
            throw new JaggerDatabindException(
                    "Expected text, got " + parser.currentToken() + " at " + parser.currentLocation());
        }
        return getText(Advance.CONSUME);
    }

    @Override
    public void skipChildren(Advance advance) throws IOException {
        parser.skipChildren();
        advance(advance);
    }

    private void advance(Advance advance) throws IOException {
        if (advance == Advance.CONSUME) {
            parser.nextToken();
        }
    }
}
