package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.List;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.AnEnum;

interface ScalarListsSerde {

    @JsonOutput
    void writeBooleanList(List<Boolean> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeByteList(List<Byte> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeShortList(List<Short> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeIntList(List<Integer> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeLongList(List<Long> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeCharList(List<Character> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeFloatList(List<Float> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeDoubleList(List<Double> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringList(List<String> input, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeEnumList(List<AnEnum> input, JsonGenerator generator) throws Exception;

    @JsonInput
    List<Boolean> readBooleanList(JsonParser parser) throws Exception;

    @JsonInput
    List<Byte> readByteList(JsonParser parser) throws Exception;

    @JsonInput
    List<Character> readCharacterList(JsonParser parser) throws Exception;

    @JsonInput
    List<Short> readShortList(JsonParser parser) throws Exception;

    @JsonInput
    List<Integer> readIntegerList(JsonParser parser) throws Exception;

    @JsonInput
    List<Long> readLongList(JsonParser parser) throws Exception;

    @JsonInput
    List<Float> readFloatList(JsonParser parser) throws Exception;

    @JsonInput
    List<Double> readDoubleList(JsonParser parser) throws Exception;

    @JsonInput
    List<String> readStringList(JsonParser parser) throws Exception;

    @JsonInput
    List<AnEnum> readEnumList(JsonParser parser) throws Exception;
}
