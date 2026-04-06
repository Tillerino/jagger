package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.AnEnum;

/**
 * This interface must be separated from {@link PrimitiveScalarsSerde} because otherwise the boxed writers delegate to
 * the non-boxed ones.
 */
public interface BoxedScalarsSerde {
    @JsonOutput
    void writeBoxedBoolean(Boolean b, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedByte(Byte b, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedShort(Short s, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedInt(Integer i, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedLong(Long l, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedChar(Character c, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedFloat(Float f, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedDouble(Double d, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeString(String s, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeEnum(AnEnum e, JsonGenerator generator) throws Exception;

    @JsonInput
    Boolean readBoxedBoolean(JsonParser parser) throws Exception;

    @JsonInput
    Byte readBoxedByte(JsonParser parser) throws Exception;

    @JsonInput
    Short readBoxedShort(JsonParser parser) throws Exception;

    @JsonInput
    Integer readBoxedInt(JsonParser parser) throws Exception;

    @JsonInput
    Long readBoxedLong(JsonParser parser) throws Exception;

    @JsonInput
    Character readBoxedChar(JsonParser parser) throws Exception;

    @JsonInput
    Float readBoxedFloat(JsonParser parser) throws Exception;

    @JsonInput
    Double readBoxedDouble(JsonParser parser) throws Exception;

    @JsonInput
    String readString(JsonParser parser) throws Exception;

    @JsonInput
    AnEnum readEnum(JsonParser parser) throws Exception;
}
