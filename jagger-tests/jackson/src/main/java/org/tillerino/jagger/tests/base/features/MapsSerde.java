package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import org.tillerino.jagger.tests.model.features.MapsModel.Keys.OffsetDateTimeMapRecord;
import org.tillerino.jagger.tests.variants.ApplyVariantsToChildren;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

interface MapsSerde {
    @JsonConfig(uses = {AllInputConverters.class, AllOutputConverters.class})
    interface Keys {
        @JsonOutput
        void writeOffsetDateTimeMapRecord(OffsetDateTimeMapRecord record, JsonGenerator generator) throws Exception;

        @JsonInput
        OffsetDateTimeMapRecord readOffsetDateTimeMapRecord(JsonParser parser) throws Exception;

        @JsonOutput
        void writeJsonValueMap(Map<JsonValueChild, String> map, JsonGenerator generator) throws Exception;

        @JsonInput
        Map<JsonValueChild, String> readJsonValueMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writeEnumMap(Map<CustomNamedEnum, String> map, JsonGenerator generator) throws Exception;

        @JsonInput
        Map<CustomNamedEnum, String> readEnumMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writePlainEnumMap(Map<AnEnum, String> map, JsonGenerator generator) throws Exception;

        @JsonInput
        Map<AnEnum, String> readPlainEnumMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writeJsonValueEnumMap(Map<JsonValueEnum, String> map, JsonGenerator generator) throws Exception;

        @JsonInput
        Map<JsonValueEnum, String> readJsonValueEnumMap(JsonParser parser) throws Exception;

        @ApplyVariantsToChildren
        @Variants({
            @Variant(@Replacement(regex = "Integer", replacement = "Boolean")),
            @Variant(@Replacement(regex = "Integer", replacement = "Byte")),
            @Variant(@Replacement(regex = "Integer", replacement = "Short")),
            @Variant(@Replacement(regex = "Integer", replacement = "Long")),
            @Variant(@Replacement(regex = "Integer", replacement = "Float")),
            @Variant(@Replacement(regex = "Integer", replacement = "Double")),
            @Variant(@Replacement(regex = "Integer", replacement = "Character"))
        })
        interface Boxed {
            @JsonOutput
            void writeMapInteger(Map<Integer, String> map, JsonGenerator generator) throws Exception;

            @JsonInput
            Map<Integer, String> readMapInteger(JsonParser parser) throws Exception;

            /* GENERATED CODE. DO NOT MODIFY BELOW!  Integer -> Boolean */
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
        }
    }
}
