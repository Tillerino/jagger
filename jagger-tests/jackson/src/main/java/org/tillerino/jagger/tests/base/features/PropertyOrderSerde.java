package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.AlphabeticOrderedProperties;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.ExplicitZThenAlphabetic;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.ExplicitZThenDeclaration;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.OrderedProperties;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface PropertyOrderSerde {
    @JsonOutput
    void writeOrderedProperties(OrderedProperties obj, JsonGenerator out) throws Exception;

    @JsonInput
    OrderedProperties readOrderedProperties(JsonParser in) throws Exception;

    @JsonOutput
    void writeAlphabeticOrderedProperties(AlphabeticOrderedProperties obj, JsonGenerator out) throws Exception;

    @JsonInput
    AlphabeticOrderedProperties readAlphabeticOrderedProperties(JsonParser in) throws Exception;

    @JsonOutput
    void writeExplicitZThenAlphabetic(ExplicitZThenAlphabetic obj, JsonGenerator out) throws Exception;

    @JsonInput
    ExplicitZThenAlphabetic readExplicitZThenAlphabetic(JsonParser in) throws Exception;

    @JsonOutput
    void writeExplicitZThenDeclaration(ExplicitZThenDeclaration obj, JsonGenerator out) throws Exception;

    @JsonInput
    ExplicitZThenDeclaration readExplicitZThenDeclaration(JsonParser in) throws Exception;
}
