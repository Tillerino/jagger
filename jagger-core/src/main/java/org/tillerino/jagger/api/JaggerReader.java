package org.tillerino.jagger.api;

public interface JaggerReader<E extends Exception> {
    boolean isObjectStart(Advance advance) throws E;

    boolean isObjectEnd(Advance advance) throws E;

    boolean isArrayStart(Advance advance) throws E;

    boolean isArrayEnd(Advance advance) throws E;

    boolean isNull(Advance advance) throws E;

    boolean isBoolean() throws E;

    boolean isNumber() throws E;

    boolean isText() throws E;

    boolean isFieldName() throws E;

    boolean getBoolean(Advance advance) throws E;

    byte getByte(Advance advance) throws E;

    short getShort(Advance advance) throws E;

    int getInt(Advance advance) throws E;

    long getLong(Advance advance) throws E;

    float getFloat(Advance advance) throws E;

    double getDouble(Advance advance) throws E;

    String getText(Advance advance) throws E;

    String getFieldName(Advance advance) throws E;

    /**
     * Read the discriminator of a polymorphic type. If the discriminator is the next token in the stream, this call
     * will behave like {@link #getFieldName(Advance)} followed by {@link #getText(Advance)}. However, for adapters that
     * can buffer the token stream, this allows reading the discriminator out of order.
     *
     * @param expectedName The expected property name of the discriminator. An unbuffered implementation will validate
     *     that this is the current field name. A buffered implementation will fetch this property from the buffer.
     * @param visible if true, the discriminator will be repeated once it shows up naturally in the token stream.
     *     Unsupported by some implementations.
     * @return The discriminator value.
     */
    String getDiscriminator(String expectedName, boolean visible) throws E;

    /**
     * If at array start / object start, skips all children. Will then point to array end / object.
     *
     * @param advance if {@link Advance#CONSUME}, will consume array end / object end as well.
     */
    void skipChildren(Advance advance) throws E;

    E unexpectedToken(String expectedToken);

    E unrecognizedProperty(String propertyName);

    /** Controls whether to advance while inspecting tokens or keep the token. */
    enum Advance {
        KEEP,
        CONSUME,
    }
}
