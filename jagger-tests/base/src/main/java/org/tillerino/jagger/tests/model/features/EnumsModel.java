package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    enum JsonValueWithCustomOverride {
        @JsonProperty("custom-one")
        A,
        B;

        @JsonValue
        public String value() {
            return "serialized-" + name().toLowerCase();
        }
    }

    interface CustomNames {
        enum CustomNamedEnum {
            @JsonProperty("value-one")
            VALUE1,
            VALUE2,
            @JsonProperty("value-three")
            VALUE3;
        }

        enum JsonValueWithCustomNames {
            @JsonProperty("custom-one")
            A,
            B;

            @JsonValue
            public String value() {
                return "serialized-" + name().toLowerCase();
            }
        }
    }
}
