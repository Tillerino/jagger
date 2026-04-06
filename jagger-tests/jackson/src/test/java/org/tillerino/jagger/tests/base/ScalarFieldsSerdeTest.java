package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.ScalarAccessorsClass;
import org.tillerino.jagger.tests.model.ScalarFieldsClass;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

class ScalarFieldsSerdeTest extends ReferenceTest {

    @Test
    void scalarFieldsRecordOutput() throws Exception {
        List<ScalarFieldsRecord> values = List.of(
                new ScalarFieldsRecord(
                        false,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        'c',
                        4f,
                        5.0d,
                        false,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        'c',
                        4f,
                        5.0d,
                        "six",
                        AnEnum.SOME_VALUE),
                new ScalarFieldsRecord(
                        false, (byte) 1, (short) 2, 3, 4L, 'c', Float.NaN, Float.NaN, null, null, null, null, null,
                        null, null, null, null, null));

        ScalarFieldsRecordSerde impl = SerdeUtil.impl(ScalarFieldsRecordSerde.class);

        for (ScalarFieldsRecord object : values) {
            outputUtils.roundTrip(object, impl::write, impl::read, new TypeReference<>() {});
        }
    }

    @Test
    void scalarFieldsClassOutput() throws Exception {
        List<ScalarFieldsClass> values = List.of(
                new ScalarFieldsClass(
                        false,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        'c',
                        4f,
                        5.0d,
                        false,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        'c',
                        4f,
                        5.0d,
                        "six",
                        AnEnum.SOME_VALUE),
                new ScalarFieldsClass(
                        false, (byte) 1, (short) 2, 3, 4L, 'c', Float.NaN, Float.NaN, null, null, null, null, null,
                        null, null, null, null, null));

        ScalarFieldsClassSerde impl = SerdeUtil.impl(ScalarFieldsClassSerde.class);

        for (ScalarFieldsClass object : values) {
            outputUtils.roundTrip(object, impl::write, impl::read, new TypeReference<>() {});
        }
    }

    @Test
    void scalarAccessorsClassOutput() throws Exception {
        List<ScalarAccessorsClass> values = List.of(
                new ScalarAccessorsClass(
                        false,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        'c',
                        4f,
                        5.0d,
                        false,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        'c',
                        4f,
                        5.0d,
                        "six",
                        AnEnum.SOME_VALUE),
                new ScalarAccessorsClass(
                        false, (byte) 1, (short) 2, 3, 4L, 'c', Float.NaN, Float.NaN, null, null, null, null, null,
                        null, null, null, null, null));

        ScalarAccessorsClassSerde impl = SerdeUtil.impl(ScalarAccessorsClassSerde.class);

        for (ScalarAccessorsClass object : values) {
            outputUtils.roundTrip(object, impl::write, impl::read, new TypeReference<>() {});
        }
    }

    @Test
    void testUnknownPropertiesDefault() throws Exception {
        ScalarFieldsRecordSerde impl = SerdeUtil.impl(ScalarFieldsRecordSerde.class);

        inputUtils.assertException(
                "{ \"whatIsThis\": 1 }",
                impl::read,
                new TypeReference<ScalarFieldsRecord>() {},
                "Unrecognized field \"whatIsThis\"",
                "Unrecognized field \"whatIsThis\"");
    }
}
