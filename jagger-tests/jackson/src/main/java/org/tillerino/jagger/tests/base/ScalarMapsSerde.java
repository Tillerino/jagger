package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.Map;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.AnEnum;

interface ScalarMapsSerde {
    @JsonOutput
    void writeStringBooleanMap(Map<String, Boolean> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringByteMap(Map<String, Byte> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringShortMap(Map<String, Short> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringIntMap(Map<String, Integer> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringLongMap(Map<String, Long> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringCharMap(Map<String, Character> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringFloatMap(Map<String, Float> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringDoubleMap(Map<String, Double> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringStringMap(Map<String, String> map, JsonGenerator generator) throws Exception;

    @JsonOutput
    void writeStringEnumMap(Map<String, AnEnum> map, JsonGenerator generator) throws Exception;

    @JsonInput
    Map<String, Boolean> readStringBooleanMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Byte> readStringByteMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Short> readStringShortMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Integer> readStringIntMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Long> readStringLongMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Character> readStringCharMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Float> readStringFloatMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, Double> readStringDoubleMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, String> readStringStringMap(JsonParser parser) throws Exception;

    @JsonInput
    Map<String, AnEnum> readStringEnumMap(JsonParser parser) throws Exception;
}
