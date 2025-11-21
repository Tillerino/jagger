package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.PropertiesModel.Child;

public interface PropertiesSerde {
    @JsonOutput
    void writeChild(Child parent, JsonGenerator gen) throws Exception;

    @JsonInput
    Child readChild(JsonParser parser) throws Exception;
}
