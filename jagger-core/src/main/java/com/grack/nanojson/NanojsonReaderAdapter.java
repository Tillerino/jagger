package com.grack.nanojson;

import java.io.InputStream;
import java.io.Reader;
import org.tillerino.jagger.api.JaggerReader;

public class NanojsonReaderAdapter implements JaggerReader<JsonParserException> {
    private final JsonTokener tokener;
    private int token = JsonTokener.TOKEN_VALUE_MIN;

    public static final int TOKEN_EOF = JsonTokener.TOKEN_EOF;
    public static final int TOKEN_COMMA = JsonTokener.TOKEN_COMMA;
    public static final int TOKEN_COLON = JsonTokener.TOKEN_COLON;
    public static final int TOKEN_OBJECT_END = JsonTokener.TOKEN_OBJECT_END;
    public static final int TOKEN_ARRAY_END = JsonTokener.TOKEN_ARRAY_END;
    public static final int TOKEN_NULL = JsonTokener.TOKEN_NULL;
    public static final int TOKEN_TRUE = JsonTokener.TOKEN_TRUE;
    public static final int TOKEN_FALSE = JsonTokener.TOKEN_FALSE;
    public static final int TOKEN_STRING = JsonTokener.TOKEN_STRING;
    public static final int TOKEN_NUMBER = JsonTokener.TOKEN_NUMBER;
    public static final int TOKEN_OBJECT_START = JsonTokener.TOKEN_OBJECT_START;
    public static final int TOKEN_ARRAY_START = JsonTokener.TOKEN_ARRAY_START;

    public NanojsonReaderAdapter(Reader reader) throws JsonParserException {
        tokener = new JsonTokener(reader);
        token = tokener.advanceToToken();
    }

    public NanojsonReaderAdapter(InputStream stm) throws JsonParserException {
        tokener = new JsonTokener(stm);
        token = tokener.advanceToToken();
    }

    @Override
    public boolean isObjectStart(Advance advance) throws JsonParserException {
        return advanceIfTokenIs(TOKEN_OBJECT_START, advance);
    }

    @Override
    public boolean isObjectEnd(Advance advance) throws JsonParserException {
        return advanceIfTokenIs(TOKEN_OBJECT_END, advance);
    }

    @Override
    public boolean isArrayStart(Advance advance) throws JsonParserException {
        return advanceIfTokenIs(TOKEN_ARRAY_START, advance);
    }

    @Override
    public boolean isArrayEnd(Advance advance) throws JsonParserException {
        return advanceIfTokenIs(TOKEN_ARRAY_END, advance);
    }

    @Override
    public boolean isNull(Advance advance) throws JsonParserException {
        return advanceIfTokenIs(TOKEN_NULL, advance);
    }

    private boolean advanceIfTokenIs(int expectedToken, Advance advance) throws JsonParserException {
        if (this.token == expectedToken) {
            advance(advance);
            return true;
        }
        return false;
    }

    @Override
    public boolean isBoolean() throws JsonParserException {
        return token == TOKEN_TRUE || token == TOKEN_FALSE;
    }

    @Override
    public boolean isNumber() throws JsonParserException {
        return token == TOKEN_NUMBER;
    }

    @Override
    public boolean isText() throws JsonParserException {
        return token == TOKEN_STRING;
    }

    @Override
    public boolean isFieldName() throws JsonParserException {
        return token == TOKEN_STRING;
    }

    @Override
    public boolean getBoolean(Advance advance) throws JsonParserException {
        boolean b = token == TOKEN_TRUE;
        advance(advance);
        return b;
    }

    @Override
    public byte getByte(Advance advance) throws JsonParserException {
        return (byte) getInt(advance);
    }

    @Override
    public short getShort(Advance advance) throws JsonParserException {
        return (short) getInt(advance);
    }

    @Override
    public int getInt(Advance advance) throws JsonParserException {
        String s = tokener.reusableBuffer.toString();
        int i = tokener.isDouble ? (int) Double.parseDouble(s) : Integer.parseInt(s);
        advance(advance);
        return i;
    }

    @Override
    public long getLong(Advance advance) throws JsonParserException {
        String s = tokener.reusableBuffer.toString();
        long l = tokener.isDouble ? (long) Double.parseDouble(s) : Long.parseLong(s);
        advance(advance);
        return l;
    }

    @Override
    public float getFloat(Advance advance) throws JsonParserException {
        String s = tokener.reusableBuffer.toString();
        float f = Float.parseFloat(s);
        advance(advance);
        return f;
    }

    @Override
    public double getDouble(Advance advance) throws JsonParserException {
        String s = tokener.reusableBuffer.toString();
        double d = Double.parseDouble(s);
        advance(advance);
        return d;
    }

    @Override
    public String getText(Advance advance) throws JsonParserException {
        String s = tokener.reusableBuffer.toString();
        advance(advance);
        return s;
    }

    @Override
    public String getFieldName(Advance advance) throws JsonParserException {
        String f = tokener.reusableBuffer.toString();
        if (advance == Advance.CONSUME) {
            advance(advance);
            if (token != TOKEN_COLON) {
                throw unexpectedToken("colon");
            }
            advance(advance);
        }
        return f;
    }

    @Override
    public String getDiscriminator(String expectedName, boolean visible) throws JsonParserException {
        if (visible) {
            throw new UnsupportedOperationException("visible discriminator not implemented yet");
        }
        String fieldName = getFieldName(Advance.CONSUME);
        if (!fieldName.equals(expectedName)) {
            throw tokener.createParseException(
                    null, "Expected field name " + expectedName + ", but was " + fieldName, true);
        }
        if (!isText()) {
            throw unexpectedToken("text");
        }
        return getText(Advance.CONSUME);
    }

    @Override
    public void skipChildren(Advance advance) throws JsonParserException {
        if (token == TOKEN_ARRAY_START) {
            advance(Advance.CONSUME);
            while (token != TOKEN_ARRAY_END && token != TOKEN_EOF) {
                skipChildren(Advance.CONSUME);
            }
        }
        if (token == TOKEN_OBJECT_START) {
            advance(Advance.CONSUME);
            while (token != TOKEN_OBJECT_END && token != TOKEN_EOF) {
                skipChildren(Advance.CONSUME);
            }
        }
        advance(advance);
    }

    @Override
    public JsonParserException unexpectedToken(String expectedToken) {
        return tokener.createParseException(null, "Expected " + expectedToken, true);
    }

    @Override
    public JsonParserException unrecognizedProperty(String propertyName) {
        return tokener.createParseException(null, "Unrecognized field \"" + propertyName + "\"", true);
    }

    private void advance(Advance advance) throws JsonParserException {
        if (advance == Advance.CONSUME) {
            token = tokener.advanceToToken();
            if (token == TOKEN_COMMA) {
                token = tokener.advanceToToken();
            }
        }
    }
}
