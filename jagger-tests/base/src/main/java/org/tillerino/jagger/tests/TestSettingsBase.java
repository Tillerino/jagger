package org.tillerino.jagger.tests;

import static java.util.Arrays.asList;

import java.util.*;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.tillerino.jagger.tests.model.AnEnum;

@Builder
@Getter
@Accessors(fluent = true)
public final class TestSettingsBase {
    private final boolean canReadIntArrayNatively;
    private final boolean canReadLongArrayNatively;
    private final boolean canReadStringArrayNatively;
    private final boolean canWriteBooleanArrayNatively;
    private final boolean canWriteShortArrayNatively;
    private final boolean canWriteIntArrayNatively;
    private final boolean canWriteLongArrayNatively;
    private final boolean canWriteFloatArrayNatively;
    private final boolean canWriteDoubleArrayNatively;
    private final boolean canWriteStringArrayNatively;

    @Builder.Default
    private final boolean canWriteFloat = true;

    @Getter(lazy = true)
    private final JavaData javaData = createJavaData();

    private JavaData createJavaData() {
        return new JavaData();
    }

    public class JavaData {
        // If the JSON generator does not support floats, the result will differ once we cast the float to a double.
        // In this case, we choose numbers which we know will not be affected by the cast.
        final float smallFloat = canWriteFloat ? Float.MIN_VALUE : 1f / 1024;
        final float largeFloat = canWriteFloat ? Float.MAX_VALUE : 1024f;

        // Primitives
        public final boolean[] BOOLEANS = {true, false};
        public final byte[] BYTES = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};
        public final short[] SHORTS = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};
        public final int[] INTS = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        public final long[] LONGS = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
        public final char[] CHARS = {Character.MIN_VALUE, 'a', 'A', 'ö', 'Ö', Character.MAX_VALUE};

        public final float[] floats = {
            smallFloat, -1, 0, 1, largeFloat, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
        };

        public final double[] DOUBLES = {
            Double.MIN_VALUE, -1, 0, 1, Double.MAX_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
        };

        // Boxed scalars
        public final Boolean[] BOXED_BOOLEANS = {true, false, null};
        public final Byte[] BOXED_BYTES = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE, null};
        public final Short[] BOXED_SHORTS = {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE, null};
        public final Integer[] BOXED_INTS = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, null};
        public final Long[] BOXED_LONGS = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE, null};
        public final Character[] BOXED_CHARS = {Character.MIN_VALUE, 'a', 'A', 'ö', 'Ö', Character.MAX_VALUE, null};

        // one longer, last will be null
        public final Float[] boxedFloats = new Float[floats.length + 1];

        {
            for (int i = 0; i < floats.length; i++) {
                boxedFloats[i] = floats[i];
            }
        }

        // one longer, last will be null
        public final Double[] BOXED_DOUBLES = new Double[DOUBLES.length + 1];

        {
            for (int i = 0; i < DOUBLES.length; i++) {
                BOXED_DOUBLES[i] = DOUBLES[i];
            }
        }

        public final String[] STRINGS = new String[] {
            "",
            " ",
            "a",
            "A",
            "ö",
            "Ö",
            "a b",
            "a\tb",
            "a\nb",
            "a\rb",
            "a\"b",
            "a\\b",
            "a/b",
            "a\b",
            "a\f",
            "a\b\f\n\r\t",
            null
        };
        public final AnEnum[] ENUMS = {null, AnEnum.SOME_VALUE, AnEnum.ANOTHER_VALUE};

        // Optionals
        public final List<OptionalInt> OPTIONAL_INTS = Stream.of(BOXED_INTS)
                .map(i -> i != null ? OptionalInt.of(i) : OptionalInt.empty())
                .toList();
        public final List<OptionalLong> OPTIONAL_LONGS = Stream.of(BOXED_LONGS)
                .map(i -> i != null ? OptionalLong.of(i) : OptionalLong.empty())
                .toList();
        public final List<OptionalDouble> OPTIONAL_DOUBLES = Stream.of(BOXED_DOUBLES)
                .map(i -> i != null ? OptionalDouble.of(i) : OptionalDouble.empty())
                .toList();
        public final List<Optional<String>> OPTIONAL_STRINGS =
                Stream.of(STRINGS).map(Optional::ofNullable).toList();

        // Primitive arrays
        public final List<boolean[]> BOOLEAN_ARRAYS = asList(null, new boolean[0], BOOLEANS);
        public final List<byte[]> BYTE_ARRAYS = asList(null, new byte[0], BYTES);
        public final List<short[]> SHORT_ARRAYS = asList(null, new short[0], SHORTS);
        public final List<int[]> INT_ARRAYS = asList(null, new int[0], INTS);
        public final List<long[]> LONG_ARRAYS = asList(null, new long[0], LONGS);
        public final List<char[]> CHAR_ARRAYS = asList(null, new char[0], CHARS);

        public final List<float[]> floatArrays = asList(null, new float[0], floats);

        public final List<double[]> DOUBLE_ARRAYS = asList(null, new double[0], DOUBLES);

        // Boxed arrays
        public final List<Boolean[]> BOXED_BOOLEAN_ARRAYS = asList(null, new Boolean[0], BOXED_BOOLEANS);
        public final List<Byte[]> BOXED_BYTE_ARRAYS = asList(null, new Byte[0], BOXED_BYTES);
        public final List<Short[]> BOXED_SHORT_ARRAYS = asList(null, new Short[0], BOXED_SHORTS);
        public final List<Integer[]> BOXED_INT_ARRAYS = asList(null, new Integer[0], BOXED_INTS);
        public final List<Long[]> BOXED_LONG_ARRAYS = asList(null, new Long[0], BOXED_LONGS);
        public final List<Character[]> BOXED_CHAR_ARRAYS = asList(null, new Character[0], BOXED_CHARS);

        public final List<Float[]> boxedFloatArrays = asList(null, new Float[0], boxedFloats);

        public final List<Double[]> BOXED_DOUBLE_ARRAYS = asList(null, new Double[0], BOXED_DOUBLES);
        public final List<String[]> STRING_ARRAYS = asList(null, new String[0], STRINGS);
        public final List<AnEnum[]> ENUM_ARRAYS = asList(null, new AnEnum[0], ENUMS);

        // Scalar lists
        public final List<List<Boolean>> BOOLEAN_LISTS = asList(null, List.of(), asList(BOXED_BOOLEANS));
        public final List<List<Byte>> BYTE_LISTS = asList(null, List.of(), asList(BOXED_BYTES));
        public final List<List<Short>> SHORT_LISTS = asList(null, List.of(), asList(BOXED_SHORTS));
        public final List<List<Integer>> INT_LISTS = asList(null, List.of(), asList(BOXED_INTS));
        public final List<List<Long>> LONG_LISTS = asList(null, List.of(), asList(BOXED_LONGS));
        public final List<List<Character>> CHAR_LISTS = asList(null, List.of(), asList(BOXED_CHARS));

        public final List<List<Float>> floatLists = asList(null, List.of(), asList(boxedFloats));

        public final List<List<Double>> DOUBLE_LISTS = asList(null, List.of(), asList(BOXED_DOUBLES));
        public final List<List<String>> STRING_LISTS = asList(null, List.of(), asList(STRINGS));
        public final List<List<AnEnum>> ENUM_LISTS = asList(null, List.of(), asList(ENUMS));

        // Scalar maps
        public final List<Map<String, Boolean>> STRING_BOOLEAN_MAPS =
                asList(null, map(), map("a", true, "b", null, "c", false));
        public final List<Map<String, Byte>> STRING_BYTE_MAPS =
                asList(null, map(), map("a", (byte) 1, "b", null, "c", (byte) Byte.MIN_VALUE, "d", Byte.MAX_VALUE));
        public final List<Map<String, Short>> STRING_SHORT_MAPS =
                asList(null, map(), map("a", (short) 1, "b", null, "c", (short) Short.MIN_VALUE, "d", Short.MAX_VALUE));
        public final List<Map<String, Integer>> STRING_INT_MAPS =
                asList(null, map(), map("a", 1, "b", null, "c", Integer.MIN_VALUE, "d", Integer.MAX_VALUE));
        public final List<Map<String, Long>> STRING_LONG_MAPS =
                asList(null, map(), map("a", 1L, "b", null, "c", Long.MIN_VALUE, "d", Long.MAX_VALUE));
        public final List<Map<String, Character>> STRING_CHAR_MAPS =
                asList(null, map(), map("a", 'a', "b", null, "c", '❆', "d", Character.MAX_VALUE));
        public final List<Map<String, Float>> STRING_FLOAT_MAPS = asList(
                null,
                map(),
                map(
                        "a",
                        1.0f,
                        "b",
                        null,
                        "c",
                        smallFloat,
                        "d",
                        largeFloat,
                        "e",
                        Float.NaN,
                        "f",
                        Float.NEGATIVE_INFINITY,
                        "g",
                        Float.POSITIVE_INFINITY));
        public final List<Map<String, Double>> STRING_DOUBLE_MAPS = asList(
                null,
                map(),
                map(
                        "a",
                        1.0,
                        "b",
                        null,
                        "c",
                        Double.MIN_VALUE,
                        "d",
                        Double.MAX_VALUE,
                        "e",
                        Double.NaN,
                        "f",
                        Double.NEGATIVE_INFINITY,
                        "g",
                        Double.POSITIVE_INFINITY));
        public final List<Map<String, String>> STRING_STRING_MAPS =
                asList(null, map(), map("a", "a", "b", null, "c", "❆", "d", "a\b\f\n\r\t"));
        public final List<Map<String, AnEnum>> STRING_ENUM_MAPS =
                asList(null, map(), map("a", AnEnum.SOME_VALUE, "b", null, "c", AnEnum.ANOTHER_VALUE));

        public static <K, V> Map<K, V> map() {
            return Map.of();
        }

        public static <K, V> Map<K, V> map(K k1, V v1) {
            Map<K, V> map = new LinkedHashMap<>();
            map.put(k1, v1);
            return map;
        }

        static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
            Map<K, V> map = map(k1, v1);
            map.put(k2, v2);
            return map;
        }

        public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3) {
            Map<K, V> map = map(k1, v1, k2, v2);
            map.put(k3, v3);
            return map;
        }

        public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
            Map<K, V> map = map(k1, v1, k2, v2, k3, v3);
            map.put(k4, v4);
            return map;
        }

        static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
            Map<K, V> map = map(k1, v1, k2, v2, k3, v3, k4, v4);
            map.put(k5, v5);
            return map;
        }

        static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
            Map<K, V> map = map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
            map.put(k6, v6);
            return map;
        }

        public static <K, V> Map<K, V> map(
                K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
            Map<K, V> map = map(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
            map.put(k7, v7);
            return map;
        }
    }
}
