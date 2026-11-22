package org.tillerino.jagger.tests.base.features;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.TestSettings;
import org.tillerino.jagger.tests.TestSettingsBase;
import org.tillerino.jagger.tests.base.features.ConvertersSerde.AbstractConverterSerde;
import org.tillerino.jagger.tests.base.features.ConvertersSerde.NonStaticConverterSerde;
import org.tillerino.jagger.tests.base.features.ConvertersSerde.ParameterConverterSerde;
import org.tillerino.jagger.tests.base.features.ConvertersSerde.SelfConverterSerde;
import org.tillerino.jagger.tests.base.features.ConvertersSerde.UuidConverter;
import org.tillerino.jagger.tests.function.Zip;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OffsetDateTimeRecord;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OptionalComponentsRecord;
import org.tillerino.jagger.tests.model.features.ConvertersModel.UuidRecord;
import tools.jackson.core.type.TypeReference;

class ConvertersTest extends ReferenceTest {
    ConvertersSerde serde = SerdeUtil.impl(ConvertersSerde.class);

    NonStaticConverterSerde nonStaticConverterSerde = SerdeUtil.impl(NonStaticConverterSerde.class);

    ParameterConverterSerde parameterConverterSerde = SerdeUtil.impl(ParameterConverterSerde.class);

    SelfConverterSerde selfConverterSerde = SerdeUtil.impl(SelfConverterSerde.class);

    UuidConverter uuidConverter = new UuidConverter() {
        @Override
        public String uuidToString(UUID uuid) {
            return uuid.toString();
        }

        @Override
        public UUID stringToUuid(String string) {
            return UUID.fromString(string);
        }
    };

    AbstractConverterSerde abstractConverterSerde = new ConvertersSerde$AbstractConverterSerdeImpl(uuidConverter);

    @Test
    void offsetDateTimeRecordRoundTrips() throws Exception {
        OffsetDateTimeRecord instance =
                new OffsetDateTimeRecord(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        String json = outputUtils.serialize(instance, serde::writeOffsetDateTimeRecord);
        assertThatJson(json).isEqualTo("{\"timestamp\":\"2021-01-01T00:00Z\"}");
        Assertions.assertThat(inputUtils.deserialize(json, serde::readOffsetDateTimeRecord))
                .isEqualTo(instance);
    }

    @Test
    void nonStaticConverterRoundTrips() throws Exception {
        UuidRecord record = new UuidRecord(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        outputUtils.roundTrip(
                record,
                nonStaticConverterSerde::writeUuidRecord,
                nonStaticConverterSerde::readUuidRecord,
                new TypeReference<>() {});

        assertThatImpl(NonStaticConverterSerde.class).method("writeUuidRecord").bodyContains("uuidToString");
        assertThatImpl(NonStaticConverterSerde.class).method("readUuidRecord").bodyContains("stringToUuid");
    }

    @Test
    void abstractConverterIsPassedToTheClass() throws Exception {
        UuidRecord record = new UuidRecord(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        outputUtils.roundTrip(
                record,
                abstractConverterSerde::writeUuidRecord,
                abstractConverterSerde::readUuidRecord,
                new TypeReference<>() {});

        assertThatImpl(AbstractConverterSerde.class).method("writeUuidRecord").bodyContains("uuidToString");
        assertThatImpl(AbstractConverterSerde.class).method("readUuidRecord").bodyContains("stringToUuid");
        assertThatImpl(AbstractConverterSerde.class).singleConstructor().bodyContains("uuidConverter$0$delegate");
    }

    @Test
    void converterPassedToThePrototypeIsUsedDirectly() throws Exception {
        UuidRecord record = new UuidRecord(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        outputUtils.roundTrip2(
                record,
                uuidConverter,
                parameterConverterSerde::writeUuidRecord,
                parameterConverterSerde::readUuidRecord,
                new TypeReference<>() {});

        assertThatImpl(ParameterConverterSerde.class).method("writeUuidRecord").bodyContains("converter.uuidToString");
        assertThatImpl(ParameterConverterSerde.class).method("readUuidRecord").bodyContains("converter.stringToUuid");
        assertThatImpl(ParameterConverterSerde.class).hasNoConstructor();
    }

    @Test
    void converterOnThePrototypeItselfIsCalledOnThis() throws Exception {
        UuidRecord record = new UuidRecord(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        outputUtils.roundTrip(
                record,
                selfConverterSerde::writeUuidRecord,
                selfConverterSerde::readUuidRecord,
                new TypeReference<>() {});

        assertThatImpl(SelfConverterSerde.class).method("writeUuidRecord").bodyContains("this.uuidToString");
        assertThatImpl(SelfConverterSerde.class).method("readUuidRecord").bodyContains("this.stringToUuid");
    }

    @Test
    void optionalComponentsRecordRountrips() throws Exception {
        TestSettingsBase.JavaData javaData = TestSettings.SETTINGS.javaData();
        List<OptionalComponentsRecord> instances = Zip.instantiate5(
                javaData.OPTIONAL_STRINGS,
                javaData.OPTIONAL_INTS,
                javaData.OPTIONAL_LONGS,
                javaData.OPTIONAL_DOUBLES,
                List.of(Optional.empty(), Optional.of(Optional.empty()), Optional.of(Optional.of("nested"))),
                OptionalComponentsRecord::new);
        for (OptionalComponentsRecord instance : instances) {
            outputUtils.roundTrip(
                    instance,
                    serde::writeOptionalComponentsRecord,
                    serde::readOptionalComponentsRecord,
                    new TypeReference<OptionalComponentsRecord>() {});
        }
    }

    /** FEATURE-JSON */
    @Test
    void testAllMissing() throws Exception {
        // This is a crossover-test since we also need default values configured.
        OptionalComponentsRecord deserialized = inputUtils.deserialize("{}", serde::readOptionalComponentsRecord);
        // We differ from Jackson here, which returns Optional.of(Optional.empty()) for the nested optional.
        // Since this is obscure enough, we don't mind.
        Assertions.assertThat(deserialized)
                .isEqualTo(new OptionalComponentsRecord(
                        Optional.empty(),
                        OptionalInt.empty(),
                        OptionalLong.empty(),
                        OptionalDouble.empty(),
                        Optional.empty()));
    }
}
