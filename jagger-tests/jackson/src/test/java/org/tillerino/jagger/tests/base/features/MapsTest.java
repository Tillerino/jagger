package org.tillerino.jagger.tests.base.features;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.CreatorsModel.ConvertersInheritance.JsonValueChild;
import org.tillerino.jagger.tests.model.features.MapsModel.Keys.OffsetDateTimeMapRecord;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

class MapsTest extends ReferenceTest {
    @Nested
    class KeysTest {
        MapsSerde.Keys serde = SerdeUtil.impl(MapsSerde.Keys.class);

        @Test
        void offsetDateTimeMapKey() throws Exception {
            Map<OffsetDateTime, String> map = new LinkedHashMap<>();
            map.put(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "first");
            map.put(OffsetDateTime.of(2020, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), "second");
            map.put(OffsetDateTime.of(2019, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC), null);
            outputUtils.assertIsEqualToDatabind(new OffsetDateTimeMapRecord(map), serde::writeOffsetDateTimeMapRecord);
        }

        @Test
        void jsonValueMapKey() throws Exception {
            Map<JsonValueChild, String> map = new LinkedHashMap<>();
            map.put(new JsonValueChild(), "value");
            outputUtils.assertIsEqualToDatabind(map, serde::writeJsonValueMap);
        }

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
        @Test
        void mapKeyInteger() throws Exception {
            Map<Integer, String> map = new LinkedHashMap<>();
            map.put(2, "first");
            map.put(1, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapInteger);
        }

        /* GENERATED CODE. DO NOT MODIFY BELOW!  2 -> Boolean.TRUE, 1 -> Boolean.FALSE, Integer -> Boolean */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyBoolean() throws Exception {
            Map<Boolean, String> map = new LinkedHashMap<>();
            map.put(Boolean.TRUE, "first");
            map.put(Boolean.FALSE, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapBoolean);
        }

        /* 2 -> (byte) 2, 1 -> (byte) 1, Integer -> Byte */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyByte() throws Exception {
            Map<Byte, String> map = new LinkedHashMap<>();
            map.put((byte) 2, "first");
            map.put((byte) 1, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapByte);
        }

        /* 2 -> (short) 2, 1 -> (short) 1, Integer -> Short */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyShort() throws Exception {
            Map<Short, String> map = new LinkedHashMap<>();
            map.put((short) 2, "first");
            map.put((short) 1, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapShort);
        }

        /* 2 -> 2L, 1 -> 1L, Integer -> Long */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyLong() throws Exception {
            Map<Long, String> map = new LinkedHashMap<>();
            map.put(2L, "first");
            map.put(1L, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapLong);
        }

        /* 2 -> Float.NaN, 1 -> Float.POSITIVE_INFINITY, Integer -> Float */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyFloat() throws Exception {
            Map<Float, String> map = new LinkedHashMap<>();
            map.put(Float.NaN, "first");
            map.put(Float.POSITIVE_INFINITY, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapFloat);
        }

        /* 2 -> Double.NaN, 1 -> Double.POSITIVE_INFINITY, Integer -> Double */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyDouble() throws Exception {
            Map<Double, String> map = new LinkedHashMap<>();
            map.put(Double.NaN, "first");
            map.put(Double.POSITIVE_INFINITY, null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapDouble);
        }

        /* 2 -> '2', 1 -> '1', Integer -> Character */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyCharacter() throws Exception {
            Map<Character, String> map = new LinkedHashMap<>();
            map.put('2', "first");
            map.put('1', null);
            outputUtils.assertIsEqualToDatabind(map, serde::writeMapCharacter);
        }
    }
}
