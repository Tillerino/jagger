package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.AnEnum;

interface ScalarArraysSerde {
    @JsonOutput
    void writeBooleanArray(boolean[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeByteArray(byte[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeShortArray(short[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeIntArray(int[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeLongArray(long[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeCharArray(char[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeFloatArray(float[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeDoubleArray(double[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedBooleanArray(Boolean[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedByteArray(Byte[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedShortArray(Short[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedIntArray(Integer[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedLongArray(Long[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedCharArray(Character[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedFloatArray(Float[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeBoxedDoubleArray(Double[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringArray(String[] input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeEnumArray(AnEnum[] input, JsonGenerator generator) throws Exception;

    @JsonInput
    boolean[] readBooleanArray(JsonParser parser) throws Exception;

    @JsonInput
    byte[] readByteArray(JsonParser parser) throws Exception;

    @JsonInput
    char[] readCharArray(JsonParser parser) throws Exception;

    @JsonInput
    short[] readShortArray(JsonParser parser) throws Exception;

    @JsonInput
    int[] readIntArray(JsonParser parser) throws Exception;

    @JsonInput
    long[] readLongArray(JsonParser parser) throws Exception;

    @JsonInput
    float[] readFloatArray(JsonParser parser) throws Exception;

    @JsonInput
    double[] readDoubleArray(JsonParser parser) throws Exception;

    @JsonInput
    Boolean[] readBoxedBooleanArray(JsonParser parser) throws Exception;

    @JsonInput
    Byte[] readBoxedByteArray(JsonParser parser) throws Exception;

    @JsonInput
    Short[] readBoxedShortArray(JsonParser parser) throws Exception;

    @JsonInput
    Integer[] readBoxedIntArray(JsonParser parser) throws Exception;

    @JsonInput
    Long[] readBoxedLongArray(JsonParser parser) throws Exception;

    @JsonInput
    Float[] readBoxedFloatArray(JsonParser parser) throws Exception;

    @JsonInput
    Double[] readBoxedDoubleArray(JsonParser parser) throws Exception;

    @JsonInput
    String[] readStringArray(JsonParser parser) throws Exception;

    @JsonInput
    AnEnum[] readEnumArray(JsonParser parser) throws Exception;
}
