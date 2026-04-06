package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.tillerino.jagger.annotations.JsonConfig;

public interface UnknownPropertiesModel {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonIgnorePropertiesIgnoreUnknown(String name, int value) {}

    @JsonConfig(unknownProperties = JsonConfig.UnknownPropertiesMode.IGNORE)
    record JsonConfigIgnoreUnknown(String name, int value) {}
}
