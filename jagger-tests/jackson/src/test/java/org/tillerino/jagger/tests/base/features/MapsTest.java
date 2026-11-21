package org.tillerino.jagger.tests.base.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.features.CreatorsModel.ConvertersInheritance.JsonValueChild;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.CustomNamedEnum;
import org.tillerino.jagger.tests.model.features.EnumsModel.JsonValueEnum;
import org.tillerino.jagger.tests.model.features.MapsModel.CustomKeys.OffsetDateTimeMapRecord;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

class MapsTest extends ReferenceTest {
    @Nested
    class CustomKeysTest {
        MapsSerde.CustomKeys serde = SerdeUtil.impl(MapsSerde.CustomKeys.class);

        @Test
        void offsetDateTimeMapKey() throws Exception {
            Map<OffsetDateTime, String> map = new LinkedHashMap<>();
            map.put(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "first");
            map.put(OffsetDateTime.of(2020, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), "second");
            map.put(OffsetDateTime.of(2019, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC), null);
            OffsetDateTimeMapRecord record = new OffsetDateTimeMapRecord(map);
            // databind can't read OffsetDateTime map keys without the JavaTimeModule
            String json = outputUtils.assertIsEqualToDatabind(record, serde::writeOffsetDateTimeMapRecord);
            assertThat(inputUtils.deserialize(json, serde::readOffsetDateTimeMapRecord))
                    .isEqualTo(record);
        }

        @Test
        void enumMapKeyUnknown() {
            assertThatThrownBy(() -> inputUtils.deserialize("{\"VALUE1\":\"value\"}", serde::readMapCustomNamedEnum))
                    .hasMessageContaining("Unexpected enum value: \"VALUE1\"");
            assertThatThrownBy(() -> inputUtils.deserialize("{\"UNKNOWN\":\"value\"}", serde::readMapAnEnum))
                    .hasMessageContaining("Unexpected enum value: \"UNKNOWN\"");
            assertThatThrownBy(() -> inputUtils.deserialize("{\"VALUE1\":\"value\"}", serde::readMapJsonValueEnum))
                    .hasMessageContaining("Unexpected enum value: \"VALUE1\"");
        }

        @Variants({
            @Variant({
                @Replacement(regex = "2", replacement = "CustomNamedEnum.VALUE3"),
                @Replacement(regex = "1", replacement = "CustomNamedEnum.VALUE1"),
                @Replacement(regex = "Integer", replacement = "CustomNamedEnum")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "new JsonValueChild()"),
                @Replacement(regex = "1", replacement = "new JsonValueChild()"),
                @Replacement(regex = "Integer", replacement = "JsonValueChild")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "AnEnum.ANOTHER_VALUE"),
                @Replacement(regex = "1", replacement = "AnEnum.SOME_VALUE"),
                @Replacement(regex = "Integer", replacement = "AnEnum")
            }),
            @Variant({
                @Replacement(regex = "2", replacement = "JsonValueEnum.VALUE2"),
                @Replacement(regex = "1", replacement = "JsonValueEnum.VALUE1"),
                @Replacement(regex = "Integer", replacement = "JsonValueEnum")
            }),
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
            outputUtils.roundTrip(map, serde::writeMapInteger, serde::readMapInteger, new TypeReference<>() {});
        }

        @Test
        void mapKeyCharacterMustHaveLengthOne() {
            assertThatThrownBy(() -> inputUtils.deserialize("{\"ab\":\"value\"}", serde::readMapCharacter))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("single character");
        }

        @Test
        void mapKeyFloatNegativeInfinity() throws Exception {
            Map<Float, String> map = new LinkedHashMap<>();
            map.put(Float.NEGATIVE_INFINITY, "value");
            outputUtils.roundTrip(map, serde::writeMapFloat, serde::readMapFloat, new TypeReference<>() {});
        }

        @Test
        void mapKeyDoubleNegativeInfinity() throws Exception {
            Map<Double, String> map = new LinkedHashMap<>();
            map.put(Double.NEGATIVE_INFINITY, "value");
            outputUtils.roundTrip(map, serde::writeMapDouble, serde::readMapDouble, new TypeReference<>() {});
        }

        /* GENERATED CODE. DO NOT MODIFY BELOW!  2 -> CustomNamedEnum.VALUE3, 1 -> CustomNamedEnum.VALUE1, Integer -> CustomNamedEnum */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyCustomNamedEnum() throws Exception {
            Map<CustomNamedEnum, String> map = new LinkedHashMap<>();
            map.put(CustomNamedEnum.VALUE3, "first");
            map.put(CustomNamedEnum.VALUE1, null);
            outputUtils.roundTrip(
                    map, serde::writeMapCustomNamedEnum, serde::readMapCustomNamedEnum, new TypeReference<>() {});
        }

        /* 2 -> new JsonValueChild(), 1 -> new JsonValueChild(), Integer -> JsonValueChild */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyJsonValueChild() throws Exception {
            Map<JsonValueChild, String> map = new LinkedHashMap<>();
            map.put(new JsonValueChild(), "first");
            map.put(new JsonValueChild(), null);
            outputUtils.roundTrip(
                    map, serde::writeMapJsonValueChild, serde::readMapJsonValueChild, new TypeReference<>() {});
        }

        /* 2 -> AnEnum.ANOTHER_VALUE, 1 -> AnEnum.SOME_VALUE, Integer -> AnEnum */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyAnEnum() throws Exception {
            Map<AnEnum, String> map = new LinkedHashMap<>();
            map.put(AnEnum.ANOTHER_VALUE, "first");
            map.put(AnEnum.SOME_VALUE, null);
            outputUtils.roundTrip(map, serde::writeMapAnEnum, serde::readMapAnEnum, new TypeReference<>() {});
        }

        /* 2 -> JsonValueEnum.VALUE2, 1 -> JsonValueEnum.VALUE1, Integer -> JsonValueEnum */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyJsonValueEnum() throws Exception {
            Map<JsonValueEnum, String> map = new LinkedHashMap<>();
            map.put(JsonValueEnum.VALUE2, "first");
            map.put(JsonValueEnum.VALUE1, null);
            outputUtils.roundTrip(
                    map, serde::writeMapJsonValueEnum, serde::readMapJsonValueEnum, new TypeReference<>() {});
        }

        /* 2 -> Boolean.TRUE, 1 -> Boolean.FALSE, Integer -> Boolean */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyBoolean() throws Exception {
            Map<Boolean, String> map = new LinkedHashMap<>();
            map.put(Boolean.TRUE, "first");
            map.put(Boolean.FALSE, null);
            outputUtils.roundTrip(map, serde::writeMapBoolean, serde::readMapBoolean, new TypeReference<>() {});
        }

        /* 2 -> (byte) 2, 1 -> (byte) 1, Integer -> Byte */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyByte() throws Exception {
            Map<Byte, String> map = new LinkedHashMap<>();
            map.put((byte) 2, "first");
            map.put((byte) 1, null);
            outputUtils.roundTrip(map, serde::writeMapByte, serde::readMapByte, new TypeReference<>() {});
        }

        /* 2 -> (short) 2, 1 -> (short) 1, Integer -> Short */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyShort() throws Exception {
            Map<Short, String> map = new LinkedHashMap<>();
            map.put((short) 2, "first");
            map.put((short) 1, null);
            outputUtils.roundTrip(map, serde::writeMapShort, serde::readMapShort, new TypeReference<>() {});
        }

        /* 2 -> 2L, 1 -> 1L, Integer -> Long */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyLong() throws Exception {
            Map<Long, String> map = new LinkedHashMap<>();
            map.put(2L, "first");
            map.put(1L, null);
            outputUtils.roundTrip(map, serde::writeMapLong, serde::readMapLong, new TypeReference<>() {});
        }

        /* 2 -> Float.NaN, 1 -> Float.POSITIVE_INFINITY, Integer -> Float */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyFloat() throws Exception {
            Map<Float, String> map = new LinkedHashMap<>();
            map.put(Float.NaN, "first");
            map.put(Float.POSITIVE_INFINITY, null);
            outputUtils.roundTrip(map, serde::writeMapFloat, serde::readMapFloat, new TypeReference<>() {});
        }

        /* 2 -> Double.NaN, 1 -> Double.POSITIVE_INFINITY, Integer -> Double */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyDouble() throws Exception {
            Map<Double, String> map = new LinkedHashMap<>();
            map.put(Double.NaN, "first");
            map.put(Double.POSITIVE_INFINITY, null);
            outputUtils.roundTrip(map, serde::writeMapDouble, serde::readMapDouble, new TypeReference<>() {});
        }

        /* 2 -> '2', 1 -> '1', Integer -> Character */
        @Test
        @GeneratedVariant("Variant of mapKeyInteger")
        void mapKeyCharacter() throws Exception {
            Map<Character, String> map = new LinkedHashMap<>();
            map.put('2', "first");
            map.put('1', null);
            outputUtils.roundTrip(map, serde::writeMapCharacter, serde::readMapCharacter, new TypeReference<>() {});
        }
    }
}
