package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.MixedFields;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.RequiredFields;
import org.tillerino.jagger.tests.model.features.RequiredPropertyModel.RequiredPropertyWithSetter;

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
