package org.tillerino.jagger.tests.base.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.features.GenericsSerde.*;
import org.tillerino.jagger.tests.model.features.GenericsModel.GenericRecord;
import org.tillerino.jagger.tests.model.features.GenericsModel.PointlessGenericsRecord;

class GenericsTest extends ReferenceTest {
    StringSerde stringSerde = SerdeUtil.impl(StringSerde.class);

    GenericRecordSerde genericRecordSerde = SerdeUtil.impl(GenericRecordSerde.class);

    StringRecordSerde stringRecordSerde = SerdeUtil.impl(StringRecordSerde.class);

    IntegerRecordSerde integerRecordSerde = SerdeUtil.impl(IntegerRecordSerde.class);

    GenericContainersSerde genericContainersSerde = SerdeUtil.impl(GenericContainersSerde.class);

    ConcreteContainerSerde concreteContainerSerde = SerdeUtil.impl(ConcreteContainerSerde.class);

    PointlessGenericsSerde pointlessGenericsSerde = SerdeUtil.impl(PointlessGenericsSerde.class);

    @Test
    void passGenericImplExplicitly() throws Exception {
        outputUtils.roundTrip2(
                new GenericRecord<>("x"),
                stringSerde,
                genericRecordSerde::writeGenericRecord,
                genericRecordSerde::readGenericRecord,
                new TypeReference<>() {});

        assertThatImpl(GenericRecordSerde.class).method("writeGenericRecord").calls("writeOnGenericInterface");

        assertThatImpl(GenericRecordSerde.class).method("readGenericRecord").calls("readOnGenericInterface");
    }

    @Test
    void takeGenericImplFromDelegatees() throws Exception {
        outputUtils.roundTrip(
                new GenericRecord<>("x"),
                stringRecordSerde::writeStringRecord,
                stringRecordSerde::readStringRecord,
                new TypeReference<>() {});

        assertThatImpl(StringRecordSerde.class).method("writeStringRecord").calls("writeGenericRecord");
        assertThatImpl(StringRecordSerde.class).method("readStringRecord").calls("readGenericRecord");
    }

    @Test
    void createLambdaFromDelegatees() throws Exception {
        outputUtils.roundTrip(
                new GenericRecord<>(1),
                integerRecordSerde::writeIntegerRecord,
                integerRecordSerde::readIntegerRecord,
                new TypeReference<>() {});

        assertThatImpl(IntegerRecordSerde.class)
                .method("writeIntegerRecord")
                .calls("writeGenericRecord")
                .bodyContains("::writeBoxedIntX");
        assertThatImpl(IntegerRecordSerde.class)
                .method("readIntegerRecord")
                .calls("readGenericRecord")
                .bodyContains("::readBoxedInt");
    }

    @Test
    void genericListSerde() throws Exception {
        outputUtils.roundTrip(
                List.of(1.0, 2.0, 3.0),
                concreteContainerSerde::writeDoubleList,
                concreteContainerSerde::readDoubleList,
                new TypeReference<>() {});

        assertThatImpl(ConcreteContainerSerde.class)
                .method("writeDoubleList")
                .calls("writeGenericList")
                .references("writeBoxedDoubleX");

        assertThatImpl(ConcreteContainerSerde.class)
                .method("readDoubleList")
                .calls("readGenericList")
                .references("readBoxedDoubleX");
    }

    @Test
    void genericMapSerde() throws Exception {
        outputUtils.roundTrip(
                Map.of("a", 1.0, "b", 2.0, "c", 3.0),
                concreteContainerSerde::writeStringDoubleMap,
                concreteContainerSerde::readStringDoubleMap,
                new TypeReference<>() {});

        assertThatImpl(ConcreteContainerSerde.class)
                .method("writeStringDoubleMap")
                .calls("writeGenericMap")
                .references("writeBoxedDoubleX");

        assertThatImpl(ConcreteContainerSerde.class)
                .method("readStringDoubleMap")
                .calls("readGenericMap")
                .references("readBoxedDoubleX");
    }

    @Test
    void pointlessGenericsSerde() throws Exception {
        outputUtils.roundTrip(
                new PointlessGenericsRecord<>("hello"),
                pointlessGenericsSerde::write,
                pointlessGenericsSerde::read,
                new TypeReference<>() {});
    }

    @Test
    void genericArrayDeserialization() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "[ \"1\", \"2\" ]",
                p -> genericContainersSerde.readGenericArray(p, stringSerde, String[].class),
                new TypeReference<String[]>() {});
    }

    @Test
    void genericArrayInGenericRecordDeserialization() throws Exception {
        GenericRecord<String[]> deserialized = inputUtils.deserialize(
                "{ \"f\": [ \"1\", \"2\" ] }",
                p -> concreteContainerSerde.readGenericRecord(p, stringSerde, String[].class));

        assertThat(deserialized.f()).containsExactly("1", "2");
    }
}
