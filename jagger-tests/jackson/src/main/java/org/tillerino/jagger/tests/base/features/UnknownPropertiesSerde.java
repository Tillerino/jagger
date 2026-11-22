package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.tests.model.features.UnknownPropertiesModel.JsonConfigIgnoreUnknown;
import org.tillerino.jagger.tests.model.features.UnknownPropertiesModel.JsonConfigThrow;
import org.tillerino.jagger.tests.model.features.UnknownPropertiesModel.JsonIgnorePropertiesIgnoreUnknown;
import tools.jackson.core.JsonParser;

public interface UnknownPropertiesSerde {
    @JsonInput
    JsonIgnorePropertiesIgnoreUnknown readJsonIgnorePropertiesIgnoreUnknown(JsonParser in) throws Exception;

    @JsonInput
    JsonConfigIgnoreUnknown readJsonConfigIgnoreUnknown(JsonParser in) throws Exception;

    @JsonInput
    JsonConfigThrow readJsonConfigThrow(JsonParser in) throws Exception;
}
