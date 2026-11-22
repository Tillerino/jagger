package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.JsonIgnorePropertiesValue;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.OuterJsonIgnoreProperties;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface IgnorePropertiesSerde {
    @JsonInput
    JsonIgnorePropertiesValue readJsonIgnorePropertiesValue(JsonParser in) throws Exception;

    @JsonOutput
    void writeJsonIgnorePropertiesValue(JsonIgnorePropertiesValue value, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeOuterJsonIgnoreProperties(OuterJsonIgnoreProperties outerJsonIgnoreProperties, JsonGenerator gen)
            throws Exception;

    @JsonInput
    OuterJsonIgnoreProperties readOuterJsonIgnoreProperties(JsonParser parser) throws Exception;
}
