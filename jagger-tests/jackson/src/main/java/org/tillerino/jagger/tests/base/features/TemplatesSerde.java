package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.List;
import org.tillerino.jagger.annotations.JaggerTemplate;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.base.features.GenericsSerde.GenericInputSerde;
import org.tillerino.jagger.tests.base.features.GenericsSerde.GenericOutputSerde;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;
import org.tillerino.jagger.tests.model.features.TemplatesModel.HasAnEnumArrayProperty;
import org.tillerino.jagger.tests.model.features.TemplatesModel.SimpleRecord;

public interface TemplatesSerde {
    @JaggerTemplate(
            templates = {GenericInputSerde.class, GenericOutputSerde.class},
            types = {double.class, AnEnum.class, double[].class, AnEnum[].class})
    interface TemplatedSerde {
        @JsonOutput
        <T> void writeGenericArray(T[] ts, JsonGenerator gen, GenericOutputSerde<T> serde) throws Exception;
    }

    @JsonConfig(uses = TemplatedSerde.class)
    @JaggerTemplate(
            templates = {GenericInputSerde.class, GenericOutputSerde.class},
            types = {HasAnEnumArrayProperty.class})
    interface CallsTemplatePrototypes {}

    @JsonConfig(uses = TemplatedSerde.class)
    @JaggerTemplate(
            templates = {GenericOutputSerde.class},
            types = {AnEnum.class})
    @JaggerTemplate(
            templates = {GenericInputSerde.class},
            types = {AnEnum.class})
    interface MultipleTemplateAnnotationsAndOneCustom {
        @JsonOutput
        void writeAnEnumList(List<AnEnum> anEnums, JsonGenerator gen) throws Exception;
    }

    @JaggerTemplate(
            templates = {GenericInputSerde.class, GenericOutputSerde.class},
            types = {ScalarFieldsRecord.class},
            auto = true)
    interface AutoTemplatesSerde {}

    interface GenericContainerSerde {
        @JsonInput
        <T> List<T> readList(JsonParser parser, GenericInputSerde<T> componentReader) throws Exception;

        @JsonOutput
        <T> void writeList(List<T> list, JsonGenerator gen, GenericOutputSerde<T> componentWriter) throws Exception;
    }

    @JaggerTemplate(
            templates = {GenericInputSerde.class, GenericOutputSerde.class},
            auto = true)
    interface AutoTemplatesForGenericContainers {}

    @JsonConfig(uses = {GenericContainerSerde.class, AutoTemplatesForGenericContainers.class})
    interface UsesAutoTemplatesForGenericContainers {
        @JsonInput
        List<SimpleRecord> readSimpleRecords(JsonParser parser) throws Exception;

        @JsonOutput
        void writeSimpleRecords(List<SimpleRecord> simpleRecords, JsonGenerator gen) throws Exception;
    }

    interface ForeignAutoSerde {
        @JaggerTemplate(
                templates = {GenericOutputSerde.class},
                auto = true)
        interface AutoTemplatesGoHereSerde {}

        @JsonConfig(uses = AutoTemplatesGoHereSerde.class)
        interface TriggersAutoTemplatesSerde {
            @JsonOutput
            void writeSimpleRecord(SimpleRecord obj, JsonGenerator gen) throws Exception;
        }
    }
}
