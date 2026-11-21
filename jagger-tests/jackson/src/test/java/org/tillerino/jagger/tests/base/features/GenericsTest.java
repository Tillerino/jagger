package org.tillerino.jagger.tests.base.features;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.features.GenericsSerde.*;
import org.tillerino.jagger.tests.model.features.EnumsModel.CustomNames.CustomNamedEnum;
import org.tillerino.jagger.tests.model.features.GenericsModel.GenericRecord;
import org.tillerino.jagger.tests.model.features.GenericsModel.PointlessGenericsRecord;
import org.tillerino.jagger.tests.model.features.GenericsModel.UsesGenericRecord;

class GenericsTest extends ReferenceTest {
    StringSerde stringSerde = SerdeUtil.impl(StringSerde.class);

    GenericRecordSerde genericRecordSerde = SerdeUtil.impl(GenericRecordSerde.class);

    StringRecordSerde stringRecordSerde = SerdeUtil.impl(StringRecordSerde.class);

    IntegerRecordSerde integerRecordSerde = SerdeUtil.impl(IntegerRecordSerde.class);

    GenericContainersSerde genericContainersSerde = SerdeUtil.impl(GenericContainersSerde.class);

    ConcreteContainerSerde concreteContainerSerde = SerdeUtil.impl(ConcreteContainerSerde.class);

    PointlessGenericsSerde pointlessGenericsSerde = SerdeUtil.impl(PointlessGenericsSerde.class);

    DeepGenericSerde deepGenericSerde = SerdeUtil.impl(DeepGenericSerde.class);

    IntegerStringMapSerde integerStringMapSerde = SerdeUtil.impl(IntegerStringMapSerde.class);

    CustomNamedEnumStringMapSerde customNamedEnumStringMapSerde = SerdeUtil.impl(CustomNamedEnumStringMapSerde.class);

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
    void genericMapWithKeyConverterSerde() throws Exception {
        outputUtils.roundTrip(
                Map.of(1, "a", 2, "b"),
                integerStringMapSerde::writeIntegerStringMap,
                integerStringMapSerde::readIntegerStringMap,
                new TypeReference<>() {});

        assertThatImpl(IntegerStringMapSerde.class)
                .method("writeIntegerStringMap")
                .calls("writeGenericMap")
                .bodyContains("from -> Objects.toString(from, null)");

        assertThatImpl(GenericKeyMapSerde.class)
                .method("writeGenericMap")
                .bodyContains("keyConverter.convert(entry.getKey())");

        assertThatImpl(IntegerStringMapSerde.class)
                .method("readIntegerStringMap")
                .calls("readGenericMap")
                .bodyContains("from -> Integer.valueOf(from)");

        assertThatImpl(GenericKeyMapSerde.class).method("readGenericMap").bodyContains("keyConverter.convert(key)");
    }

    @Test
    void genericMapWithEnumKeyConverterSerde() throws Exception {
        Map<CustomNamedEnum, String> map = new LinkedHashMap<>();
        map.put(CustomNamedEnum.VALUE3, "a");
        map.put(CustomNamedEnum.VALUE1, "b");
        map.put(CustomNamedEnum.VALUE2, null);

        outputUtils.roundTrip(
                map,
                customNamedEnumStringMapSerde::writeCustomNamedEnumStringMap,
                customNamedEnumStringMapSerde::readCustomNamedEnumStringMap,
                new TypeReference<>() {});

        assertThatImpl(CustomNamedEnumStringMapSerde.class)
                .method("writeCustomNamedEnumStringMap")
                .calls("writeGenericMap")
                .bodyContains("$values.get");

        assertThatImpl(CustomNamedEnumStringMapSerde.class)
                .method("readCustomNamedEnumStringMap")
                .calls("readGenericMap")
                .bodyContains("EnumHelper.deserialize");
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
    void deepGenericInheritance() throws Exception {
        outputUtils.roundTrip(
                "hello",
                deepGenericSerde::writeGenericBase,
                deepGenericSerde::readGenericBase,
                new TypeReference<String>() {});
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

    @Nested
    class GenericConvertersSerdeTest {
        GenericConvertersSerde.ConcreteSerde serde = SerdeUtil.impl(GenericConvertersSerde.ConcreteSerde.class);

        @Test
        void genericConverterAndDefaultValueArePassedToTheMethod() throws Exception {
            UsesGenericRecord record = new UsesGenericRecord(new GenericRecord<>(42));

            String json = outputUtils.serialize(record, serde::write);
            assertThatJson(json).isEqualTo("{\"gi\":{\"f\":\"42\"}}");
            assertThat(inputUtils.deserialize(json, serde::read)).isEqualTo(record);

            assertThat(inputUtils.deserialize("{\"gi\":{}}", serde::read))
                    .isEqualTo(new UsesGenericRecord(new GenericRecord<>(7)));
            assertThat(inputUtils.deserialize("{\"gi\":null}", serde::read)).isEqualTo(new UsesGenericRecord(null));

            assertThatImpl(GenericConvertersSerde.ConcreteSerde.class)
                    .method("write")
                    .bodyContains("writeGeneric");
            assertThatImpl(GenericConvertersSerde.ConcreteSerde.class)
                    .method("read")
                    .bodyContains("readGeneric");

            assertThatImpl(GenericConvertersSerde.GenericRecordSerde.class)
                    .method("readGeneric")
                    .bodyContains("defaultValue.defaultValue()")
                    .bodyContains("converter.convert(fToConvert)");
            assertThatImpl(GenericConvertersSerde.GenericRecordSerde.class)
                    .method("writeGeneric")
                    .bodyContains("converter.convert(f)");
        }
    }
}
