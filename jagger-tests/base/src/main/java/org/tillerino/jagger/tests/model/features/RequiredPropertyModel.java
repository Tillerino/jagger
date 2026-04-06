package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface RequiredPropertyModel {
    record RequiredFields(@JsonProperty(required = true) String name, @JsonProperty(required = true) Integer age) {}

    record MixedFields(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = false) String description,
            Integer age) {}

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class RequiredPropertyWithSetter {
        @JsonProperty(required = true)
        private String name;

        @JsonProperty(required = false)
        private String description;
    }
}
