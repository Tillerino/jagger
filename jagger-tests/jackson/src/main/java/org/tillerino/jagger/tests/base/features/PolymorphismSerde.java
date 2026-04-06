package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.tests.model.features.PolymorphismModel.*;

public interface PolymorphismSerde {
    @JsonOutput
    void writePolymorphism(SealedInterface sealedInterface, JsonGenerator generator) throws Exception;

    @JsonInput
    SealedInterface readPolymorphism(JsonParser generator) throws Exception;

    interface WithDelegate {
        @JsonOutput
        void writePolymorphism(SealedInterface sealedInterface, JsonGenerator generator, SerializationContext context)
                throws Exception;

        @JsonOutput
        void writeOne(RecordOne recordOne, JsonGenerator generator, SerializationContext context) throws Exception;

        @JsonInput
        SealedInterface readPolymorphism(JsonParser parser, DeserializationContext context) throws Exception;

        @JsonInput
        RecordOne readOne(JsonParser parser, DeserializationContext context) throws Exception;
    }

    interface JsonTypeInfoAndJsonSubTypesSerde {
        @JsonOutput
        void writeUseClass(JsonTypeInfoUseClass obj, JsonGenerator generator) throws Exception;

        @JsonInput
        JsonTypeInfoUseClass readUseClass(JsonParser parser) throws Exception;

        @JsonOutput
        void writeUseName(JsonTypeInfoUseName obj, JsonGenerator generator) throws Exception;

        @JsonInput
        JsonTypeInfoUseName readUseName(JsonParser parser) throws Exception;

        @JsonOutput
        void writeUseSimpleName(JsonTypeInfoUseSimpleName obj, JsonGenerator generator) throws Exception;

        @JsonInput
        JsonTypeInfoUseSimpleName readUseSimpleName(JsonParser parser) throws Exception;
    }
}
