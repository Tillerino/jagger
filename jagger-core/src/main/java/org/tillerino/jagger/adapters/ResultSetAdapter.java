package org.tillerino.jagger.adapters;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.tillerino.jagger.api.JaggerReader;

public class ResultSetAdapter implements JaggerReader<SQLException> {
    private final ResultSet resultSet;

    private List<TokenRetriever> tokenRetrievers;

    private int index = Integer.MIN_VALUE;

    public ResultSetAdapter(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    protected void prepareMetaData() throws SQLException {
        if (tokenRetrievers != null) {
            return;
        }

        // Track dots and use those emulate nested objects
        List<String> prefix = new ArrayList<>();

        tokenRetrievers = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        tokenRetrievers.add(TokenRetriever.marker(TokenKind.START_OBJECT));
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String name = metaData.getColumnName(i);

            String[] parts = name.split("\\.");
            int prefixLength = 0;
            for (int j = 0; j < parts.length - 1; j++) {
                if (prefix.size() > j && prefix.get(j).equals(parts[j])) {
                    prefixLength++;
                } else {
                    break;
                }
            }

            for (int j = prefix.size() - 1; j >= prefixLength; j--) {
                tokenRetrievers.add(TokenRetriever.marker(TokenKind.END_OBJECT));
                prefix.remove(j);
            }

            for (int j = prefixLength; j < parts.length - 1; j++) {
                prefix.add(parts[j]);
                tokenRetrievers.add(TokenRetriever.fieldName(prefix.get(j)));
                tokenRetrievers.add(TokenRetriever.marker(TokenKind.START_OBJECT));
            }

            tokenRetrievers.add(TokenRetriever.fieldName(name));
            switch (metaData.getColumnType(i)) {
                case Types.BOOLEAN:
                    tokenRetrievers.add(TokenRetriever.column(i, TokenKind.BOOLEAN));
                    break;
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                case Types.BIGINT:
                case Types.REAL:
                case Types.FLOAT:
                case Types.DOUBLE:
                case Types.DECIMAL:
                case Types.NUMERIC:
                    tokenRetrievers.add(TokenRetriever.column(i, TokenKind.NUMBER));
                    break;
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.LONGVARCHAR:
                case Types.LONGNVARCHAR:
                    tokenRetrievers.add(TokenRetriever.column(i, TokenKind.TEXT));
                    break;
                default:
                    throw new SQLException("Cannot map SQL type " + metaData.getColumnType(i));
            }
        }
        tokenRetrievers.add(TokenRetriever.marker(TokenKind.END_OBJECT));
    }

    @Override
    public boolean isObjectStart(Advance advance) throws SQLException {
        return isToken(TokenKind.START_OBJECT, advance);
    }

    @Override
    public boolean isObjectEnd(Advance advance) throws SQLException {
        return isToken(TokenKind.END_OBJECT, advance);
    }

    @Override
    public boolean isArrayStart(Advance advance) throws SQLException {
        return isToken(TokenKind.START_ARRAY, advance);
    }

    @Override
    public boolean isArrayEnd(Advance advance) throws SQLException {
        return isToken(TokenKind.END_ARRAY, advance);
    }

    @Override
    public boolean isNull(Advance advance) throws SQLException {
        if (index < 0 || index >= tokenRetrievers.size()) {
            return false;
        }

        TokenRetriever tokenRetriever = tokenRetrievers.get(index);
        switch (currentToken()) {
            case START_ARRAY:
            case END_ARRAY:
            case START_OBJECT:
            case END_OBJECT:
            case END:
                return false;
            case FIELD_NAME:
            case TEXT:
                resultSet.getString(tokenRetriever.columnNumber);
                break;
            case NUMBER:
                resultSet.getByte(tokenRetriever.columnNumber);
                break;
            case BOOLEAN:
                resultSet.getBoolean(tokenRetriever.columnNumber);
                break;
        }
        if (resultSet.wasNull()) {
            advance(advance);
            return true;
        }
        return false;
    }

    @Override
    public boolean isBoolean() throws SQLException {
        return isToken(TokenKind.BOOLEAN, Advance.KEEP);
    }

    @Override
    public boolean isNumber() throws SQLException {
        return isToken(TokenKind.NUMBER, Advance.KEEP);
    }

    @Override
    public boolean isText() throws SQLException {
        return isToken(TokenKind.TEXT, Advance.KEEP);
    }

    @Override
    public boolean isFieldName() throws SQLException {
        return isToken(TokenKind.FIELD_NAME, Advance.KEEP);
    }

    @Override
    public boolean getBoolean(Advance advance) throws SQLException {
        boolean b = resultSet.getBoolean(columnNumberValidatingTokenKind(TokenKind.BOOLEAN));
        validateWasNotNull(advance);
        return b;
    }

    @Override
    public byte getByte(Advance advance) throws SQLException {
        byte b = resultSet.getByte(columnNumberValidatingTokenKind(TokenKind.NUMBER));
        validateWasNotNull(advance);
        return b;
    }

    @Override
    public short getShort(Advance advance) throws SQLException {
        short s = resultSet.getShort(columnNumberValidatingTokenKind(TokenKind.NUMBER));
        validateWasNotNull(advance);
        return s;
    }

    @Override
    public int getInt(Advance advance) throws SQLException {
        int i = resultSet.getInt(columnNumberValidatingTokenKind(TokenKind.NUMBER));
        validateWasNotNull(advance);
        return i;
    }

    @Override
    public long getLong(Advance advance) throws SQLException {
        long l = resultSet.getLong(columnNumberValidatingTokenKind(TokenKind.NUMBER));
        validateWasNotNull(advance);
        return l;
    }

    @Override
    public float getFloat(Advance advance) throws SQLException {
        float f = resultSet.getFloat(columnNumberValidatingTokenKind(TokenKind.NUMBER));
        validateWasNotNull(advance);
        return f;
    }

    @Override
    public double getDouble(Advance advance) throws SQLException {
        double d = resultSet.getDouble(columnNumberValidatingTokenKind(TokenKind.NUMBER));
        validateWasNotNull(advance);
        return d;
    }

    @Override
    public String getText(Advance advance) throws SQLException {
        String text = resultSet.getString(columnNumberValidatingTokenKind(TokenKind.TEXT));
        advance(advance);
        return text;
    }

    @Override
    public String getFieldName(Advance advance) throws SQLException {
        String fieldName = tokenRetrievers.get(index).fieldName;
        advance(advance);
        return fieldName;
    }

    @Override
    public String getDiscriminator(String expectedName, boolean visible) throws SQLException {
        if (visible) {
            // TODO implement
            throw new UnsupportedOperationException("visible discriminator not implemented yet");
        }
        // TODO also implement reading discriminator at any time such that it doesn't have to be the first returned
        //  column
        String fieldName = getFieldName(Advance.CONSUME);
        if (!fieldName.equals(expectedName)) {
            throw new SQLException("Expected discriminator " + expectedName + " but got " + fieldName);
        }
        if (!isText()) {
            throw unexpectedToken("text");
        }
        return getText(Advance.CONSUME);
    }

    @Override
    public void skipChildren(Advance advance) throws SQLException {}

    @Override
    public SQLException unexpectedToken(String expectedToken) {
        try {
            return new SQLException("Expected " + expectedToken + " but got " + currentToken());
        } catch (SQLException e) {
            return e;
        }
    }

    @Override
    public SQLException unrecognizedProperty(String propertyName) {
        return new SQLException("Unrecognized field \"" + propertyName + "\"");
    }

    protected TokenKind currentToken() throws SQLException {
        if (index == Integer.MIN_VALUE) {
            advance();
        }
        if (index == Integer.MAX_VALUE) {
            return TokenKind.END;
        }
        if (index == -1) {
            return TokenKind.START_ARRAY;
        }
        if (index == tokenRetrievers.size()) {
            return TokenKind.END_ARRAY;
        }
        return tokenRetrievers.get(index).kind;
    }

    protected void advance() throws SQLException {
        prepareMetaData();
        if (index == Integer.MIN_VALUE) {
            index = -1;
        } else if (index == -1 || index == tokenRetrievers.size() - 1) {
            if (resultSet.next()) {
                index = 0;
            } else {
                index = tokenRetrievers.size();
            }
        } else if (index < tokenRetrievers.size()) {
            index++;
        } else {
            index = Integer.MAX_VALUE;
        }
    }

    protected boolean isToken(TokenKind kind, Advance advance) throws SQLException {
        if (currentToken() == kind) {
            advance(advance);
            return true;
        }
        return false;
    }

    protected void advance(Advance advance) throws SQLException {
        if (advance == Advance.CONSUME) {
            advance();
        }
    }

    protected int columnNumberValidatingTokenKind(TokenKind tokenKind) throws SQLException {
        if (currentToken() != tokenKind) {
            throw unexpectedToken(tokenKind.name());
        }
        return tokenRetrievers.get(index).columnNumber;
    }

    protected void validateWasNotNull(Advance advance) throws SQLException {
        if (resultSet.wasNull()) {
            throw unexpectedToken("null");
        }
        advance(advance);
    }

    protected static class TokenRetriever {
        /** only set if this is a column from the result */
        protected Integer columnNumber;

        protected TokenKind kind;

        protected String fieldName;

        protected static TokenRetriever marker(TokenKind kind) {
            TokenRetriever tokenRetriever = new TokenRetriever();
            tokenRetriever.kind = kind;
            return tokenRetriever;
        }

        protected static TokenRetriever fieldName(String columnName) {
            TokenRetriever tokenRetriever = new TokenRetriever();
            tokenRetriever.kind = ResultSetAdapter.TokenKind.FIELD_NAME;
            tokenRetriever.fieldName = columnName;
            return tokenRetriever;
        }

        protected static TokenRetriever column(int index, TokenKind tokenKind) {
            TokenRetriever tokenRetriever = new TokenRetriever();
            tokenRetriever.columnNumber = index;
            tokenRetriever.kind = tokenKind;
            return tokenRetriever;
        }
    }

    protected enum TokenKind {
        START_ARRAY,
        END_ARRAY,

        START_OBJECT,
        END_OBJECT,
        FIELD_NAME,

        TEXT,
        NUMBER,
        BOOLEAN,

        END
    }
}
