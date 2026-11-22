package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.PropertiesModel.Child;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface PropertiesSerde {
    @JsonOutput
    void writeChild(Child parent, JsonGenerator gen) throws Exception;

    @JsonInput
    Child readChild(JsonParser parser) throws Exception;
}
