package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public interface GenericsModel {
    record GenericRecord<F>(F f) {}

    record PointlessGenericsRecord<F>(String prop) {}

    record UsesGenericRecord(GenericRecord<Integer> gi) {}

    record ToStringSelfConverter(String prop) {
        @JsonCreator
        public static ToStringSelfConverter fromString(String prop) {
            return new ToStringSelfConverter(prop);
        }

        @JsonValue
        public String asString() {
            return prop;
        }
    }
}
