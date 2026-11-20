package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.UUID;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonInputConverter;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.annotations.JsonOutputConverter;
import org.tillerino.jagger.converters.AllInputConverters;
import org.tillerino.jagger.converters.AllOutputConverters;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OffsetDateTimeRecord;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OptionalComponentsRecord;
import org.tillerino.jagger.tests.model.features.ConvertersModel.UuidRecord;

@JsonConfig(uses = {AllInputConverters.class, AllOutputConverters.class})
interface ConvertersSerde {
    @JsonOutput
    void writeOffsetDateTimeRecord(OffsetDateTimeRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    OffsetDateTimeRecord readOffsetDateTimeRecord(JsonParser parser) throws Exception;

    @JsonOutput
    void writeOptionalComponentsRecord(OptionalComponentsRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    OptionalComponentsRecord readOptionalComponentsRecord(JsonParser parser) throws Exception;

    class NonStaticConverters {
        @JsonOutputConverter
        public String uuidToString(UUID uuid) {
            return uuid.toString();
        }

        @JsonInputConverter
        public UUID stringToUuid(String string) {
            return UUID.fromString(string);
        }
    }

    interface UuidConverter {
        @JsonOutputConverter
        String uuidToString(UUID uuid);

        @JsonInputConverter
        UUID stringToUuid(String string);
    }

    @JsonConfig(uses = NonStaticConverters.class)
    interface NonStaticConverterSerde {
        @JsonOutput
        void writeUuidRecord(UuidRecord record, JsonGenerator generator) throws Exception;

        @JsonInput
        UuidRecord readUuidRecord(JsonParser parser) throws Exception;
    }

    @JsonConfig(uses = UuidConverter.class)
    interface AbstractConverterSerde {
        @JsonOutput
        void writeUuidRecord(UuidRecord record, JsonGenerator generator) throws Exception;

        @JsonInput
        UuidRecord readUuidRecord(JsonParser parser) throws Exception;
    }

    interface ParameterConverterSerde {
        @JsonOutput
        void writeUuidRecord(UuidRecord record, JsonGenerator generator, UuidConverter converter) throws Exception;

        @JsonInput
        UuidRecord readUuidRecord(JsonParser parser, UuidConverter converter) throws Exception;
    }

    interface SelfConverterSerde {
        @JsonOutputConverter
        default String uuidToString(UUID uuid) {
            return uuid.toString();
        }

        @JsonInputConverter
        default UUID stringToUuid(String string) {
            return UUID.fromString(string);
        }

        @JsonOutput
        void writeUuidRecord(UuidRecord record, JsonGenerator generator) throws Exception;

        @JsonInput
        UuidRecord readUuidRecord(JsonParser parser) throws Exception;
    }
}
