package org.tillerino.jagger.tests.base;

import static org.tillerino.jagger.tests.TestSettings.SETTINGS;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;

class ScalarsSerdeTest extends ReferenceTest {
    PrimitiveScalarsSerde primitive = SerdeUtil.impl(PrimitiveScalarsSerde.class);
    BoxedScalarsSerde boxed = SerdeUtil.impl(BoxedScalarsSerde.class);

    @Test
    void testBoolean() throws Exception {
        for (boolean b : SETTINGS.javaData().BOOLEANS) {
            outputUtils.roundTrip(
                    b,
                    primitive::writePrimitiveBooleanX,
                    primitive::readPrimitiveBooleanX,
                    new TypeReference<Boolean>() {});
        }
    }

    @Test
    void testByte() throws Exception {
        for (byte b : SETTINGS.javaData().BYTES) {
            outputUtils.roundTrip(
                    b, primitive::writePrimitiveByteX, primitive::readPrimitiveByteX, new TypeReference<Byte>() {});
        }
    }

    @Test
    void testShort() throws Exception {
        for (short s : SETTINGS.javaData().SHORTS) {
            outputUtils.roundTrip(
                    s, primitive::writePrimitiveShortX, primitive::readPrimitiveShortX, new TypeReference<Short>() {});
        }
    }

    @Test
    void testInt() throws Exception {
        for (int i : SETTINGS.javaData().INTS) {
            outputUtils.roundTrip(
                    i, primitive::writePrimitiveIntX, primitive::readPrimitiveIntX, new TypeReference<Integer>() {});
        }
    }

    @Test
    void testLong() throws Exception {
        for (long l : SETTINGS.javaData().LONGS) {
            outputUtils.roundTrip(
                    l, primitive::writePrimitiveLongX, primitive::readPrimitiveLongX, new TypeReference<Long>() {});
        }
    }

    @Test
    void testChar() throws Exception {
        for (char c : SETTINGS.javaData().CHARS) {
            outputUtils.roundTrip(
                    c,
                    primitive::writePrimitiveCharX,
                    primitive::readPrimitiveCharX,
                    new TypeReference<Character>() {});
        }
    }

    @Test
    void testFloat() throws Exception {
        for (float f : SETTINGS.javaData().floats) {
            outputUtils.roundTrip(
                    f, primitive::writePrimitiveFloatX, primitive::readPrimitiveFloatX, new TypeReference<Float>() {});
        }
    }

    @Test
    void testDouble() throws Exception {
        for (double d : SETTINGS.javaData().DOUBLES) {
            outputUtils.roundTrip(
                    d,
                    primitive::writePrimitiveDoubleX,
                    primitive::readPrimitiveDoubleX,
                    new TypeReference<Double>() {});
        }
    }

    @Test
    void testBoxedBoolean() throws Exception {
        for (Boolean b : SETTINGS.javaData().BOXED_BOOLEANS) {
            outputUtils.roundTrip(
                    b, boxed::writeBoxedBoolean, boxed::readBoxedBoolean, new TypeReference<Boolean>() {});
        }
    }

    @Test
    void testBoxedByte() throws Exception {
        for (Byte b : SETTINGS.javaData().BOXED_BYTES) {
            outputUtils.roundTrip(b, boxed::writeBoxedByte, boxed::readBoxedByte, new TypeReference<Byte>() {});
        }
    }

    @Test
    void testBoxedShort() throws Exception {
        for (Short s : SETTINGS.javaData().BOXED_SHORTS) {
            outputUtils.roundTrip(s, boxed::writeBoxedShort, boxed::readBoxedShort, new TypeReference<Short>() {});
        }
    }

    @Test
    void testBoxedInteger() throws Exception {
        for (Integer i : SETTINGS.javaData().BOXED_INTS) {
            outputUtils.roundTrip(i, boxed::writeBoxedInt, boxed::readBoxedInt, new TypeReference<Integer>() {});
        }
    }

    @Test
    void testBoxedLong() throws Exception {
        for (Long l : SETTINGS.javaData().BOXED_LONGS) {
            outputUtils.roundTrip(l, boxed::writeBoxedLong, boxed::readBoxedLong, new TypeReference<Long>() {});
        }
    }

    @Test
    void testBoxedChar() throws Exception {
        for (Character c : SETTINGS.javaData().BOXED_CHARS) {
            outputUtils.roundTrip(c, boxed::writeBoxedChar, boxed::readBoxedChar, new TypeReference<Character>() {});
        }
    }

    @Test
    void testBoxedFloat() throws Exception {
        for (Float f : SETTINGS.javaData().boxedFloats) {
            outputUtils.roundTrip(f, boxed::writeBoxedFloat, boxed::readBoxedFloat, new TypeReference<Float>() {});
        }
    }

    @Test
    void testBoxedDouble() throws Exception {
        for (Double d : SETTINGS.javaData().BOXED_DOUBLES) {
            outputUtils.roundTrip(d, boxed::writeBoxedDouble, boxed::readBoxedDouble, new TypeReference<Double>() {});
        }
    }

    @Test
    void testString() throws Exception {
        for (String s : SETTINGS.javaData().STRINGS) {
            outputUtils.roundTrip(s, boxed::writeString, boxed::readString, new TypeReference<String>() {});
        }
    }

    @Test
    void testEnum() throws Exception {
        for (AnEnum e : SETTINGS.javaData().ENUMS) {
            outputUtils.roundTrip(e, boxed::writeEnum, boxed::readEnum, new TypeReference<AnEnum>() {});
        }
    }
}
