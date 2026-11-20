package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.IntSequenceGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator;
import lombok.Data;

public interface ReferencesModel {
    @JsonIdentityInfo(generator = IntSequenceGenerator.class)
    record IntSequenceIdRecord(String prop) {}

    @JsonIdentityInfo(generator = IntSequenceGenerator.class)
    @Data
    class IntSequenceIdPojo {
        private String prop;
    }

    @JsonIdentityInfo(generator = UUIDGenerator.class)
    record UuidIdRecord(String prop) {}

    @JsonIdentityInfo(generator = PropertyGenerator.class, property = "prop")
    record PropertyIdRecord(String prop) {}

    @JsonIdentityInfo(generator = PropertyGenerator.class, property = "prop")
    @Data
    class PropertyIdPojo {
        String prop;
    }

    @JsonIdentityInfo(generator = IntSequenceGenerator.class)
    @JsonTypeInfo(use = Id.MINIMAL_CLASS)
    sealed interface IntSequenceParent {
        record IntSequenceChild() implements IntSequenceParent {}
    }

    @JsonIdentityInfo(generator = UUIDGenerator.class)
    @JsonTypeInfo(use = Id.MINIMAL_CLASS)
    sealed interface UuidParent {
        record UuidChild() implements UuidParent {}
    }

    /** This case where we have an identity, but also converters, is not supported. Only the converter is used. */
    @JsonIdentityInfo(generator = UUIDGenerator.class)
    record UuidIdConvertedRecord(String prop) {
        public record Repr(String other) {}

        @JsonValue
        public Repr repr() {
            return new Repr(prop);
        }

        @JsonCreator
        public static UuidIdConvertedRecord create(Repr repr) {
            return new UuidIdConvertedRecord(repr.other);
        }
    }
}
