package org.tillerino.jagger.tests.base;

import static org.tillerino.jagger.tests.TestSettings.SETTINGS;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;

class ScalarArraysSerdeTest extends ReferenceTest {
    ScalarArraysSerde impl = SerdeUtil.impl(ScalarArraysSerde.class);

    @Test
    void testBooleanArray() throws Exception {
        for (boolean[] object : SETTINGS.javaData().BOOLEAN_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBooleanArray, impl::readBooleanArray, new TypeReference<boolean[]>() {});
        }
    }

    @Test
    void testByteArray() throws Exception {
        for (byte[] object : SETTINGS.javaData().BYTE_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeByteArray, impl::readByteArray, new TypeReference<byte[]>() {});
        }
    }

    @Test
    void testShortArray() throws Exception {
        for (short[] object : SETTINGS.javaData().SHORT_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeShortArray, impl::readShortArray, new TypeReference<short[]>() {});
        }
    }

    @Test
    void testIntArray() throws Exception {
        for (int[] object : SETTINGS.javaData().INT_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeIntArray, impl::readIntArray, new TypeReference<int[]>() {});
        }
    }

    @Test
    void testLongArray() throws Exception {
        for (long[] object : SETTINGS.javaData().LONG_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeLongArray, impl::readLongArray, new TypeReference<long[]>() {});
        }
    }

    @Test
    void testCharArray() throws Exception {
        for (char[] object : SETTINGS.javaData().CHAR_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeCharArray, impl::readCharArray, new TypeReference<char[]>() {});
        }
    }

    @Test
    void testFloatArray() throws Exception {
        for (float[] object : SETTINGS.javaData().floatArrays) {
            outputUtils.roundTripRecursive(
                    object, impl::writeFloatArray, impl::readFloatArray, new TypeReference<float[]>() {});
        }
    }

    @Test
    void testDoubleArray() throws Exception {
        for (double[] object : SETTINGS.javaData().DOUBLE_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeDoubleArray, impl::readDoubleArray, new TypeReference<double[]>() {});
        }
    }

    @Test
    void testBoxedBooleanArray() throws Exception {
        for (Boolean[] object : SETTINGS.javaData().BOXED_BOOLEAN_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object,
                    impl::writeBoxedBooleanArray,
                    impl::readBoxedBooleanArray,
                    new TypeReference<Boolean[]>() {});
        }
    }

    @Test
    void testBoxedByteArray() throws Exception {
        for (Byte[] object : SETTINGS.javaData().BOXED_BYTE_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBoxedByteArray, impl::readBoxedByteArray, new TypeReference<Byte[]>() {});
        }
    }

    @Test
    void testBoxedShortArray() throws Exception {
        for (Short[] object : SETTINGS.javaData().BOXED_SHORT_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBoxedShortArray, impl::readBoxedShortArray, new TypeReference<Short[]>() {});
        }
    }

    @Test
    void testBoxedIntArray() throws Exception {
        for (Integer[] object : SETTINGS.javaData().BOXED_INT_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBoxedIntArray, impl::readBoxedIntArray, new TypeReference<Integer[]>() {});
        }
    }

    @Test
    void testBoxedLongArray() throws Exception {
        for (Long[] object : SETTINGS.javaData().BOXED_LONG_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBoxedLongArray, impl::readBoxedLongArray, new TypeReference<Long[]>() {});
        }
    }

    @Test
    void testBoxedCharArray() throws Exception {
        for (Character[] object : SETTINGS.javaData().BOXED_CHAR_ARRAYS) {
            // only testing output since Jackson does not like reading boxed char arrays
            outputUtils.assertIsEqualToDatabind(object, impl::writeBoxedCharArray);
        }
    }

    @Test
    void testBoxedFloatArray() throws Exception {
        for (Float[] object : SETTINGS.javaData().boxedFloatArrays) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBoxedFloatArray, impl::readBoxedFloatArray, new TypeReference<Float[]>() {});
        }
    }

    @Test
    void testBoxedDoubleArray() throws Exception {
        for (Double[] object : SETTINGS.javaData().BOXED_DOUBLE_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeBoxedDoubleArray, impl::readBoxedDoubleArray, new TypeReference<Double[]>() {});
        }
    }

    @Test
    void testStringArray() throws Exception {
        for (String[] object : SETTINGS.javaData().STRING_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeStringArray, impl::readStringArray, new TypeReference<String[]>() {});
        }
    }

    @Test
    void testEnumArray() throws Exception {
        for (AnEnum[] object : SETTINGS.javaData().ENUM_ARRAYS) {
            outputUtils.roundTripRecursive(
                    object, impl::writeEnumArray, impl::readEnumArray, new TypeReference<AnEnum[]>() {});
        }
    }

    @Test
    void testReadingLargeIntArray() throws Exception {
        String json =
                """
			[
				0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
				10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
				20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
				30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
				40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
				50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
				60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
				70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
				80, 81, 82, 83, 84, 85, 86, 87, 88, 89,
				90, 91, 92, 93, 94, 95, 96, 97, 98, 99
			]
		""";
        inputUtils.assertIsEqualToDatabind(json, impl::readIntArray, new TypeReference<int[]>() {});
    }
}
