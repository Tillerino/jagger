package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.List;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.converters.UuidInputConverters;
import org.tillerino.jagger.converters.UuidOutputConverters;
import org.tillerino.jagger.tests.base.features.DelegationSerde.BoxedScalarsSerde;
import org.tillerino.jagger.tests.model.features.ReferencesModel.*;
import org.tillerino.jagger.tests.model.features.ReferencesModel.IntSequenceParent.IntSequenceChild;
import org.tillerino.jagger.tests.model.features.ReferencesModel.UuidParent.UuidChild;

@JsonConfig(uses = {UuidInputConverters.class, UuidOutputConverters.class})
public interface ReferencesSerde {
    @JsonOutput
    void writeIntSequenceIdRecord(IntSequenceIdRecord hasIntId, JsonGenerator gen, SerializationContext ctx)
            throws Exception;

    @JsonInput
    IntSequenceIdRecord readIntSequenceIdRecord(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writeListOfIntSequenceIdRecord(
            List<IntSequenceIdRecord> hasIntIds, JsonGenerator gen, SerializationContext ctx) throws Exception;

    @JsonInput
    List<IntSequenceIdRecord> readListOfIntSequenceIdRecord(JsonParser parser, DeserializationContext ctx)
            throws Exception;

    @JsonOutput
    void writeIntSequenceIdPojo(IntSequenceIdPojo hasIntId, JsonGenerator gen, SerializationContext ctx)
            throws Exception;

    @JsonInput
    IntSequenceIdPojo readIntSequenceIdPojo(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writeListOfIntSequenceIdPojo(List<IntSequenceIdPojo> hasIntIds, JsonGenerator gen, SerializationContext ctx)
            throws Exception;

    @JsonInput
    List<IntSequenceIdPojo> readListOfIntSequenceIdPojo(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writeUuidIdRecord(UuidIdRecord hasIntId, JsonGenerator gen, SerializationContext ctx) throws Exception;

    @JsonInput
    UuidIdRecord readUuidIdRecord(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writeListOfUuidIdRecord(List<UuidIdRecord> hasIntIds, JsonGenerator gen, SerializationContext ctx)
            throws Exception;

    @JsonInput
    List<UuidIdRecord> readListOfUuidIdRecord(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writePropertyIdRecord(PropertyIdRecord hasIntId, JsonGenerator gen, SerializationContext ctx) throws Exception;

    @JsonInput
    PropertyIdRecord readPropertyIdRecord(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writeListOfPropertyIdRecord(List<PropertyIdRecord> hasIntIds, JsonGenerator gen, SerializationContext ctx)
            throws Exception;

    @JsonInput
    List<PropertyIdRecord> readListOfPropertyIdRecord(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writePropertyIdPojo(PropertyIdPojo hasIntId, JsonGenerator gen, SerializationContext ctx) throws Exception;

    @JsonInput
    PropertyIdPojo readPropertyIdPojo(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonOutput
    void writeListOfPropertyIdPojo(List<PropertyIdPojo> hasIntIds, JsonGenerator gen, SerializationContext ctx)
            throws Exception;

    @JsonInput
    List<PropertyIdPojo> readListOfPropertyIdPojo(JsonParser parser, DeserializationContext ctx) throws Exception;

    @JsonConfig(uses = BoxedScalarsSerde.class)
    interface DoNotDelegateInitialResolveSerde {
        @JsonInput
        IntSequenceIdRecord readIntSequenceIdRecord(JsonParser parser, DeserializationContext ctx) throws Exception;
    }

    interface ReferenceInheritanceSerde {
        @JsonOutput
        void writeIntSequenceParent(IntSequenceParent hasIntId, JsonGenerator gen, SerializationContext ctx)
                throws Exception;

        @JsonInput
        IntSequenceParent readIntSequenceParent(JsonParser parser, DeserializationContext ctx) throws Exception;

        @JsonOutput
        void writeListOfIntSequenceParent(
                List<IntSequenceParent> hasIntIds, JsonGenerator gen, SerializationContext ctx) throws Exception;

        @JsonInput
        List<IntSequenceParent> readListOfIntSequenceParent(JsonParser parser, DeserializationContext ctx)
                throws Exception;

        @JsonOutput
        void writeIntSequenceChild(IntSequenceChild hasIntId, JsonGenerator gen, SerializationContext ctx)
                throws Exception;

        @JsonInput
        IntSequenceChild readIntSequenceChild(JsonParser parser, DeserializationContext ctx) throws Exception;

        @JsonOutput
        void writeListOfIntSequenceChild(List<IntSequenceChild> hasIntIds, JsonGenerator gen, SerializationContext ctx)
                throws Exception;

        @JsonInput
        List<IntSequenceChild> readListOfIntSequenceChild(JsonParser parser, DeserializationContext ctx)
                throws Exception;
    }

    @JsonConfig(uses = {UuidInputConverters.class, UuidOutputConverters.class})
    interface PolyWithDelegatorReferencesSerde {
        @JsonOutput
        void writeParent(UuidParent parent, JsonGenerator gen, SerializationContext ctx) throws Exception;

        @JsonInput
        UuidParent readParent(JsonParser parser, DeserializationContext ctx) throws Exception;

        @JsonOutput
        void writeChild(UuidChild parent, JsonGenerator gen, SerializationContext ctx) throws Exception;

        @JsonInput
        UuidChild readChild(JsonParser parser, DeserializationContext ctx) throws Exception;
    }
}
