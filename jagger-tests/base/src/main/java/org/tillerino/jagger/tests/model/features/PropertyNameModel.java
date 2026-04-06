package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

public interface PropertyNameModel {
    record JsonPropertyCustomName(@JsonProperty("notS") String s) {}

    @EqualsAndHashCode
    class JsonPropertyCustomNameWithSetter {
        private @JsonProperty("notS") String s;

        public JsonPropertyCustomNameWithSetter(String s) {
            this.s = s;
        }

        public JsonPropertyCustomNameWithSetter() {}

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }
}
