package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonConfig.VerificationMode;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;
import org.tillerino.jagger.tests.model.features.VerificationModel.DuplicatedProperty;
import org.tillerino.jagger.tests.model.features.VerificationModel.MoreGettersThanSetters;
import org.tillerino.jagger.tests.model.features.VerificationModel.MoreSettersThanGetters;
import org.tillerino.jagger.tests.model.features.VerificationModel.TwoPropertiesOfTheSameType;

/** There are currently no tests for this, since I haven't figured out a good way to test this. */
public interface VerificationSerde {
    @JsonConfig(verifySymmetry = VerificationMode.WARN)
    interface WriterButNoReader {
        @JsonOutput
        void write(ScalarFieldsRecord s, JsonGenerator gen) throws Exception;
    }

    @JsonConfig(verifySymmetry = VerificationMode.WARN)
    interface ReaderButNoWriter {
        @JsonInput
        ScalarFieldsRecord read(JsonParser parser) throws Exception;
    }

    @JsonConfig(verifySymmetry = VerificationMode.WARN)
    interface IntraDto {
        @JsonOutput
        void writeDuplicatedProperty(DuplicatedProperty duplicatedProperty, JsonGenerator gen) throws Exception;

        // duplicated properties cause duplicated labels in switch -> no need to verify read of DuplicatedProperty

        @JsonOutput
        void writeMoreSettersThanGetters(MoreSettersThanGetters moreSettersThanGetters, JsonGenerator gen)
                throws Exception;

        @JsonInput
        MoreSettersThanGetters readMoreSettersThanGetters(JsonParser parser) throws Exception;

        @JsonOutput
        void writeMoreGettersThanSetters(MoreGettersThanSetters moreGettersThanSetters, JsonGenerator gen)
                throws Exception;

        @JsonInput
        MoreGettersThanSetters readMoreGettersThanSetters(JsonParser parser) throws Exception;

        @JsonOutput
        void writeTwoPropertiesOfTheSameType(TwoPropertiesOfTheSameType twoPropertiesOfTheSameType, JsonGenerator gen)
                throws Exception;

        @JsonInput
        TwoPropertiesOfTheSameType readTwoPropertiesOfTheSameType(JsonParser parser) throws Exception;
    }
}
