package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.tests.model.features.UnknownPropertiesModel.JsonConfigIgnoreUnknown;
import org.tillerino.jagger.tests.model.features.UnknownPropertiesModel.JsonIgnorePropertiesIgnoreUnknown;

public interface UnknownPropertiesSerde {
    @JsonInput
    JsonIgnorePropertiesIgnoreUnknown readJsonIgnorePropertiesIgnoreUnknown(JsonParser in) throws Exception;

    @JsonInput
    JsonConfigIgnoreUnknown readJsonConfigIgnoreUnknown(JsonParser in) throws Exception;
}
