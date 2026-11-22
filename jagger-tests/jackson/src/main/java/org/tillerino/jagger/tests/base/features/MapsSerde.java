package org.tillerino.jagger.tests.base.features;

import java.util.Map;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.converters.AllInputConverters;
import org.tillerino.jagger.converters.AllOutputConverters;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.features.CreatorsModel.ConvertersInheritance.JsonValueChild;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.CustomNamedEnum;
import org.tillerino.jagger.tests.model.features.EnumsModel.JsonValueEnum;
import org.tillerino.jagger.tests.model.features.MapsModel.CustomKeys.OffsetDateTimeMapRecord;
import org.tillerino.jagger.tests.variants.ApplyVariantsToChildren;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

interface MapsSerde {
    @ApplyVariantsToChildren
    @Variants({
        @Variant(@Replacement(regex = "Integer", replacement = "Boolean")),
        @Variant(@Replacement(regex = "Integer", replacement = "Byte")),
        @Variant(@Replacement(regex = "Integer", replacement = "Short")),
        @Variant(@Replacement(regex = "Integer", replacement = "Long")),
        @Variant(@Replacement(regex = "Integer", replacement = "Character")),
        @Variant(@Replacement(regex = "Integer", replacement = "Float")),
        @Variant(@Replacement(regex = "Integer", replacement = "Double")),
        @Variant(@Replacement(regex = "Integer", replacement = "String")),
        @Variant(@Replacement(regex = "Integer", replacement = "AnEnum"))
    })
    interface ScalarValues {
        @JsonOutput
        void writeStringIntegerMap(Map<String, Integer> map, JsonGenerator generator) throws Exception;

        @JsonInput
        Map<String, Integer> readStringIntegerMap(JsonParser parser) throws Exception;

        /* GENERATED CODE. DO NOT MODIFY BELOW!  Integer -> Boolean */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringBooleanMap(Map<String, Boolean> map, JsonGenerator generator) throws Exception;

        /* Integer -> Boolean */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Boolean> readStringBooleanMap(JsonParser parser) throws Exception;

        /* Integer -> Byte */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringByteMap(Map<String, Byte> map, JsonGenerator generator) throws Exception;

        /* Integer -> Byte */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Byte> readStringByteMap(JsonParser parser) throws Exception;

        /* Integer -> Short */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringShortMap(Map<String, Short> map, JsonGenerator generator) throws Exception;

        /* Integer -> Short */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Short> readStringShortMap(JsonParser parser) throws Exception;

        /* Integer -> Long */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringLongMap(Map<String, Long> map, JsonGenerator generator) throws Exception;

        /* Integer -> Long */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Long> readStringLongMap(JsonParser parser) throws Exception;

        /* Integer -> Character */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringCharacterMap(Map<String, Character> map, JsonGenerator generator) throws Exception;

        /* Integer -> Character */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Character> readStringCharacterMap(JsonParser parser) throws Exception;

        /* Integer -> Float */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringFloatMap(Map<String, Float> map, JsonGenerator generator) throws Exception;

        /* Integer -> Float */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Float> readStringFloatMap(JsonParser parser) throws Exception;

        /* Integer -> Double */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringDoubleMap(Map<String, Double> map, JsonGenerator generator) throws Exception;

        /* Integer -> Double */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, Double> readStringDoubleMap(JsonParser parser) throws Exception;

        /* Integer -> String */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringStringMap(Map<String, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> String */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, String> readStringStringMap(JsonParser parser) throws Exception;

        /* Integer -> AnEnum */
        @JsonOutput
        @GeneratedVariant("Variant of writeStringIntegerMap")
        void writeStringAnEnumMap(Map<String, AnEnum> map, JsonGenerator generator) throws Exception;

        /* Integer -> AnEnum */
        @JsonInput
        @GeneratedVariant("Variant of readStringIntegerMap")
        Map<String, AnEnum> readStringAnEnumMap(JsonParser parser) throws Exception;
    }

    @JsonConfig(uses = {AllInputConverters.class, AllOutputConverters.class})
    @ApplyVariantsToChildren
    @Variants({
        @Variant(@Replacement(regex = "Integer", replacement = "CustomNamedEnum")),
        @Variant(@Replacement(regex = "Integer", replacement = "JsonValueChild")),
        @Variant(@Replacement(regex = "Integer", replacement = "AnEnum")),
        @Variant(@Replacement(regex = "Integer", replacement = "JsonValueEnum")),
        @Variant(@Replacement(regex = "Integer", replacement = "Boolean")),
        @Variant(@Replacement(regex = "Integer", replacement = "Byte")),
        @Variant(@Replacement(regex = "Integer", replacement = "Short")),
        @Variant(@Replacement(regex = "Integer", replacement = "Long")),
        @Variant(@Replacement(regex = "Integer", replacement = "Float")),
        @Variant(@Replacement(regex = "Integer", replacement = "Double")),
        @Variant(@Replacement(regex = "Integer", replacement = "Character")),
        @Variant(@Replacement(regex = "Integer", replacement = "String"))
    })
    interface CustomKeys {
        @JsonOutput
        void writeOffsetDateTimeMapRecord(OffsetDateTimeMapRecord record, JsonGenerator generator) throws Exception;

        @JsonInput
        OffsetDateTimeMapRecord readOffsetDateTimeMapRecord(JsonParser parser) throws Exception;

        @JsonOutput
        void writeMapInteger(Map<Integer, String> map, JsonGenerator generator) throws Exception;

        @JsonInput
        Map<Integer, String> readMapInteger(JsonParser parser) throws Exception;

        /* GENERATED CODE. DO NOT MODIFY BELOW!  Integer -> CustomNamedEnum */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapCustomNamedEnum(Map<CustomNamedEnum, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> CustomNamedEnum */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<CustomNamedEnum, String> readMapCustomNamedEnum(JsonParser parser) throws Exception;

        /* Integer -> JsonValueChild */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapJsonValueChild(Map<JsonValueChild, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> JsonValueChild */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<JsonValueChild, String> readMapJsonValueChild(JsonParser parser) throws Exception;

        /* Integer -> AnEnum */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapAnEnum(Map<AnEnum, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> AnEnum */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<AnEnum, String> readMapAnEnum(JsonParser parser) throws Exception;

        /* Integer -> JsonValueEnum */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapJsonValueEnum(Map<JsonValueEnum, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> JsonValueEnum */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<JsonValueEnum, String> readMapJsonValueEnum(JsonParser parser) throws Exception;

        /* Integer -> Boolean */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapBoolean(Map<Boolean, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Boolean */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Boolean, String> readMapBoolean(JsonParser parser) throws Exception;

        /* Integer -> Byte */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapByte(Map<Byte, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Byte */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Byte, String> readMapByte(JsonParser parser) throws Exception;

        /* Integer -> Short */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapShort(Map<Short, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Short */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Short, String> readMapShort(JsonParser parser) throws Exception;

        /* Integer -> Long */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapLong(Map<Long, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Long */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Long, String> readMapLong(JsonParser parser) throws Exception;

        /* Integer -> Float */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapFloat(Map<Float, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Float */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Float, String> readMapFloat(JsonParser parser) throws Exception;

        /* Integer -> Double */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapDouble(Map<Double, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Double */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Double, String> readMapDouble(JsonParser parser) throws Exception;

        /* Integer -> Character */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapCharacter(Map<Character, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> Character */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<Character, String> readMapCharacter(JsonParser parser) throws Exception;

        /* Integer -> String */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapString(Map<String, String> map, JsonGenerator generator) throws Exception;

        /* Integer -> String */
        @JsonInput
        @GeneratedVariant("Variant of readMapInteger")
        Map<String, String> readMapString(JsonParser parser) throws Exception;
    }
}
