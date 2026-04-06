package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.JsonIgnorePropertiesValue;
import org.tillerino.jagger.tests.model.features.IgnorePropertiesModel.OuterJsonIgnoreProperties;

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
