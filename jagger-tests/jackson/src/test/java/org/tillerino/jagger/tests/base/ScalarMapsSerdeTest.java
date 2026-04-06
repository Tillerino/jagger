package org.tillerino.jagger.tests.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tillerino.jagger.tests.TestSettings.SETTINGS;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;

class ScalarMapsSerdeTest extends ReferenceTest {
    ScalarMapsSerde impl = SerdeUtil.impl(ScalarMapsSerde.class);

    @Test
    void testStringBooleanMap() throws Exception {
        for (Map<String, Boolean> object : SETTINGS.javaData().STRING_BOOLEAN_MAPS) {
            Map<String, Boolean> map = outputUtils.roundTrip(
                    object, impl::writeStringBooleanMap, impl::readStringBooleanMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringByteMap() throws Exception {
        for (Map<String, Byte> object : SETTINGS.javaData().STRING_BYTE_MAPS) {
            Map<String, Byte> map = outputUtils.roundTrip(
                    object, impl::writeStringByteMap, impl::readStringByteMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringShortMap() throws Exception {
        for (Map<String, Short> object : SETTINGS.javaData().STRING_SHORT_MAPS) {
            Map<String, Short> map = outputUtils.roundTrip(
                    object, impl::writeStringShortMap, impl::readStringShortMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringIntMap() throws Exception {
        for (Map<String, Integer> object : SETTINGS.javaData().STRING_INT_MAPS) {
            Map<String, Integer> map = outputUtils.roundTrip(
                    object, impl::writeStringIntMap, impl::readStringIntMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringLongMap() throws Exception {
        for (Map<String, Long> object : SETTINGS.javaData().STRING_LONG_MAPS) {
            Map<String, Long> map = outputUtils.roundTrip(
                    object, impl::writeStringLongMap, impl::readStringLongMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringCharMap() throws Exception {
        for (Map<String, Character> object : SETTINGS.javaData().STRING_CHAR_MAPS) {
            Map<String, Character> map = outputUtils.roundTrip(
                    object, impl::writeStringCharMap, impl::readStringCharMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringFloatMap() throws Exception {
        for (Map<String, Float> object : SETTINGS.javaData().STRING_FLOAT_MAPS) {
            Map<String, Float> map = outputUtils.roundTrip(
                    object, impl::writeStringFloatMap, impl::readStringFloatMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringDoubleMap() throws Exception {
        for (Map<String, Double> object : SETTINGS.javaData().STRING_DOUBLE_MAPS) {
            Map<String, Double> map = outputUtils.roundTrip(
                    object, impl::writeStringDoubleMap, impl::readStringDoubleMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringStringMap() throws Exception {
        for (Map<String, String> object : SETTINGS.javaData().STRING_STRING_MAPS) {
            Map<String, String> map = outputUtils.roundTrip(
                    object, impl::writeStringStringMap, impl::readStringStringMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }

    @Test
    void testStringEnumMap() throws Exception {
        for (Map<String, AnEnum> object : SETTINGS.javaData().STRING_ENUM_MAPS) {
            Map<String, AnEnum> map = outputUtils.roundTrip(
                    object, impl::writeStringEnumMap, impl::readStringEnumMap, new TypeReference<>() {});
            if (map != null) {
                assertThat(map).isInstanceOf(LinkedHashMap.class);
            }
        }
    }
}
