package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonValue;

public interface EnumsModel {
    enum JsonValueEnum {
        VALUE1,
        VALUE2;

        @JsonValue
        public String serializedForm() {
            return switch (this) {
                case VALUE1 -> "value-one";
                case VALUE2 -> "value-two";
            };
        }
    }
}
