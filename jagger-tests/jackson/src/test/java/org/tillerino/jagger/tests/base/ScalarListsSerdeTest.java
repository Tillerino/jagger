package org.tillerino.jagger.tests.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tillerino.jagger.tests.TestSettings.SETTINGS;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;

class ScalarListsSerdeTest extends ReferenceTest {
    ScalarListsSerde impl = SerdeUtil.impl(ScalarListsSerde.class);

    @Test
    void testBooleanList() throws Exception {
        for (List<Boolean> object : SETTINGS.javaData().BOOLEAN_LISTS) {
            List<Boolean> list = outputUtils.roundTrip(
                    object, impl::writeBooleanList, impl::readBooleanList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testByteList() throws Exception {
        for (List<Byte> object : SETTINGS.javaData().BYTE_LISTS) {
            List<Byte> list =
                    outputUtils.roundTrip(object, impl::writeByteList, impl::readByteList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testShortList() throws Exception {
        for (List<Short> object : SETTINGS.javaData().SHORT_LISTS) {
            List<Short> list =
                    outputUtils.roundTrip(object, impl::writeShortList, impl::readShortList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testIntList() throws Exception {
        for (List<Integer> object : SETTINGS.javaData().INT_LISTS) {
            List<Integer> list =
                    outputUtils.roundTrip(object, impl::writeIntList, impl::readIntegerList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testLongList() throws Exception {
        for (List<Long> object : SETTINGS.javaData().LONG_LISTS) {
            List<Long> list =
                    outputUtils.roundTrip(object, impl::writeLongList, impl::readLongList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testCharList() throws Exception {
        for (List<Character> object : SETTINGS.javaData().CHAR_LISTS) {
            List<Character> list = outputUtils.roundTrip(
                    object, impl::writeCharList, impl::readCharacterList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testFloatList() throws Exception {
        for (List<Float> object : SETTINGS.javaData().floatLists) {
            List<Float> list =
                    outputUtils.roundTrip(object, impl::writeFloatList, impl::readFloatList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testDoubleList() throws Exception {
        for (List<Double> object : SETTINGS.javaData().DOUBLE_LISTS) {
            List<Double> list = outputUtils.roundTrip(
                    object, impl::writeDoubleList, impl::readDoubleList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testStringList() throws Exception {
        for (List<String> object : SETTINGS.javaData().STRING_LISTS) {
            List<String> list = outputUtils.roundTrip(
                    object, impl::writeStringList, impl::readStringList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }

    @Test
    void testEnumList() throws Exception {
        for (List<AnEnum> object : SETTINGS.javaData().ENUM_LISTS) {
            List<AnEnum> list =
                    outputUtils.roundTrip(object, impl::writeEnumList, impl::readEnumList, new TypeReference<>() {});
            if (list != null) {
                assertThat(list).isInstanceOf(ArrayList.class);
            }
        }
    }
}
