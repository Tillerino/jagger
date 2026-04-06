package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface IgnorePropertiesModel {
    @JsonIgnoreProperties({"ignoredValue", "ignoredArray", "ignoredObject"})
    record JsonIgnorePropertiesValue(
            String name,
            int value,
            String ignoredValue,
            // this is renamed, so it will not actually be ignored
            @JsonProperty("renamed") String ignoredArray) {}

    // make sure that ignore properties is not propagated to the inner class
    @JsonIgnoreProperties({"ignored"})
    record OuterJsonIgnoreProperties(String ignored, InnerJsonIgnoreProperties inner) {}

    record InnerJsonIgnoreProperties(String ignored) {}
}
