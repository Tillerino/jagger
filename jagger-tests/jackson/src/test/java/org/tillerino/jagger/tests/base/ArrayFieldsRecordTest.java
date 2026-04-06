package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.TestSettings;
import org.tillerino.jagger.tests.model.PrimitiveArrayFieldsRecord;
import org.tillerino.jagger.tests.model.ReferenceArrayFieldsRecord;

class ArrayFieldsRecordTest extends ReferenceTest {
    ArrayFieldsRecordSerde serde = SerdeUtil.impl(ArrayFieldsRecordSerde.class);

    @Test
    void roundtripPrimitive() throws Exception {
        for (PrimitiveArrayFieldsRecord record : PrimitiveArrayFieldsRecord.instances(TestSettings.SETTINGS)) {
            outputUtils.roundTripRecursive(
                    record,
                    serde::writePrimitive,
                    serde::readPrimitive,
                    new TypeReference<PrimitiveArrayFieldsRecord>() {});
        }
    }

    @Test
    void roundtripReference() throws Exception {
        for (ReferenceArrayFieldsRecord record : ReferenceArrayFieldsRecord.instances(TestSettings.SETTINGS)) {
            outputUtils.roundTripRecursive(
                    record,
                    serde::writeReference,
                    serde::readReference,
                    new TypeReference<ReferenceArrayFieldsRecord>() {});
        }
    }
}
