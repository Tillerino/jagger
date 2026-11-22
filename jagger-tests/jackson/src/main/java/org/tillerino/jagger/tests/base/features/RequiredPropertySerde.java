package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.MixedFields;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.RequiredFields;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.RequiredPropertyWithSetter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface RequiredPropertySerde {
    @JsonOutput
    void writeRequiredFields(RequiredFields obj, JsonGenerator out) throws Exception;

    @JsonInput
    RequiredFields readRequiredFields(JsonParser in) throws Exception;

    @JsonOutput
    void writeMixedFields(MixedFields obj, JsonGenerator out) throws Exception;

    @JsonInput
    MixedFields readMixedFields(JsonParser in) throws Exception;

    @JsonOutput
    void writeRequiredPropertyWithSetter(RequiredPropertyWithSetter obj, JsonGenerator out) throws Exception;

    @JsonInput
    RequiredPropertyWithSetter readRequiredPropertyWithSetter(JsonParser in) throws Exception;
}
