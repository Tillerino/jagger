package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.List;
import org.tillerino.jagger.annotations.JaggerTemplate;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.base.features.GenericsSerde.GenericInputSerde;
import org.tillerino.jagger.tests.base.features.GenericsSerde.GenericOutputSerde;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.features.TemplatesModel.HasAnEnumArrayProperty;

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
}
