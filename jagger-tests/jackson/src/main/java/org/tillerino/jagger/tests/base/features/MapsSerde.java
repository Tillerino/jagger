package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.Map;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.converters.AllOutputConverters;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.model.features.CreatorsModel.ConvertersInheritance.JsonValueChild;
import org.tillerino.jagger.tests.model.features.MapsModel.Keys.OffsetDateTimeMapRecord;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

interface MapsSerde {
    @JsonConfig(uses = AllOutputConverters.class)
    interface Keys {
        @JsonOutput
        void writeOffsetDateTimeMapRecord(OffsetDateTimeMapRecord record, JsonGenerator generator) throws Exception;

        @JsonOutput
        void writeJsonValueMap(Map<JsonValueChild, String> map, JsonGenerator generator) throws Exception;

        @Variants({
            @Variant({
                @Replacement(regex = "2", replacement = "Boolean.TRUE"),
                @Replacement(regex = "1", replacement = "Boolean.FALSE"),
                @Replacement(regex = "Integer", replacement = "Boolean")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "(byte) 2"),
                @Replacement(regex = "1", replacement = "(byte) 1"),
                @Replacement(regex = "Integer", replacement = "Byte")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "(short) 2"),
                @Replacement(regex = "1", replacement = "(short) 1"),
                @Replacement(regex = "Integer", replacement = "Short")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "2L"),
                @Replacement(regex = "1", replacement = "1L"),
                @Replacement(regex = "Integer", replacement = "Long")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "Float.NaN"),
                @Replacement(regex = "1", replacement = "Float.POSITIVE_INFINITY"),
                @Replacement(regex = "Integer", replacement = "Float")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "Double.NaN"),
                @Replacement(regex = "1", replacement = "Double.POSITIVE_INFINITY"),
                @Replacement(regex = "Integer", replacement = "Double")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "'2'"),
                @Replacement(regex = "1", replacement = "'1'"),
                @Replacement(regex = "Integer", replacement = "Character")
            })
        })
        @JsonOutput
        void writeMapInteger(Map<Integer, String> map, JsonGenerator generator) throws Exception;

        /* GENERATED CODE. DO NOT MODIFY BELOW!  2 -> Boolean.TRUE, 1 -> Boolean.FALSE, Integer -> Boolean */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapBoolean(Map<Boolean, String> map, JsonGenerator generator) throws Exception;

        /* 2 -> (byte) 2, 1 -> (byte) 1, Integer -> Byte */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapByte(Map<Byte, String> map, JsonGenerator generator) throws Exception;

        /* 2 -> (short) 2, 1 -> (short) 1, Integer -> Short */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapShort(Map<Short, String> map, JsonGenerator generator) throws Exception;

        /* 2 -> 2L, 1 -> 1L, Integer -> Long */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapLong(Map<Long, String> map, JsonGenerator generator) throws Exception;

        /* 2 -> Float.NaN, 1 -> Float.POSITIVE_INFINITY, Integer -> Float */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapFloat(Map<Float, String> map, JsonGenerator generator) throws Exception;

        /* 2 -> Double.NaN, 1 -> Double.POSITIVE_INFINITY, Integer -> Double */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapDouble(Map<Double, String> map, JsonGenerator generator) throws Exception;

        /* 2 -> '2', 1 -> '1', Integer -> Character */
        @JsonOutput
        @GeneratedVariant("Variant of writeMapInteger")
        void writeMapCharacter(Map<Character, String> map, JsonGenerator generator) throws Exception;
    }
}
