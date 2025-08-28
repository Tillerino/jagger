package org.tillerino.scruse.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.List;
import org.tillerino.scruse.annotations.JsonConfig;
import org.tillerino.scruse.annotations.JsonOutput;
import org.tillerino.scruse.annotations.JsonTemplate;
import org.tillerino.scruse.tests.base.features.GenericsSerde.GenericInputSerde;
import org.tillerino.scruse.tests.base.features.GenericsSerde.GenericOutputSerde;
import org.tillerino.scruse.tests.model.AnEnum;
import org.tillerino.scruse.tests.model.features.TemplatesModel.HasAnEnumArrayProperty;

public interface TemplatesSerde {
    @JsonTemplate(
            templates = {GenericInputSerde.class, GenericOutputSerde.class},
            types = {double.class, AnEnum.class, double[].class, AnEnum[].class})
    interface TemplatedSerde {
        @JsonOutput
        <T> void writeGenericArray(T[] ts, JsonGenerator gen, GenericOutputSerde<T> serde) throws Exception;
    }

    @JsonConfig(uses = TemplatedSerde.class)
    @JsonTemplate(
            templates = {GenericInputSerde.class, GenericOutputSerde.class},
            types = {HasAnEnumArrayProperty.class})
    interface CallsTemplatePrototypes {}

    @JsonConfig(uses = TemplatedSerde.class)
    @JsonTemplate(
            templates = {GenericOutputSerde.class},
            types = {AnEnum.class})
    @JsonTemplate(
            templates = {GenericInputSerde.class},
            types = {AnEnum.class})
    interface MultipleTemplateAnnotationsAndOneCustom {
        @JsonOutput
        void writeAnEnumList(List<AnEnum> anEnums, JsonGenerator gen) throws Exception;
    }
}
