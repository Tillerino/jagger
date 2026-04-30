package org.tillerino.jagger.tests;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.List;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.ConstructorCases.MutualNoArgA.MutualRecordA;
import org.tillerino.jagger.tests.ConstructorCases.MutualNoArgB.MutualRecordB;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

public interface ConstructorCases {
    /** An implementation of this class must receive a matching constructor. */
    abstract class AbstractClassSerde {
        final ClassLoader cl;

        protected AbstractClassSerde(ClassLoader cl) {
            this.cl = cl;
        }

        @JsonOutput
        abstract void writeScalarFieldsRecord(ScalarFieldsRecord scalarFieldsRecord, JsonGenerator gen)
                throws Exception;

        @JsonInput
        abstract ScalarFieldsRecord readScalarFieldsRecord(JsonParser parser) throws Exception;
    }

    /** An implementation of this class must receive a constructor with an {@link AbstractClassSerde} argument. */
    @JsonConfig(uses = AbstractClassSerde.class)
    interface DelegatesToAbstractClassSerde {
        @JsonOutput
        void writeList(List<ScalarFieldsRecord> records, JsonGenerator gen) throws Exception;
    }

    /**
     * An implementation of this class must receive a constructor with a {@link DelegatesToAbstractClassSerde} argument,
     * but this is discovered lazily.
     */
    @JsonConfig(uses = DelegatesToAbstractClassSerde.class)
    interface DelegatesToLatentNoNoArgConstructor {
        @JsonOutput
        void write(HasListOfScalarFieldsRecord record, JsonGenerator gen) throws Exception;

        record HasListOfScalarFieldsRecord(List<ScalarFieldsRecord> l) {}
    }

    @JsonConfig(uses = MutualNoArgB.class)
    interface MutualNoArgA {
        @JsonOutput
        void writeA(MutualRecordA a, JsonGenerator gen) throws Exception;

        record MutualRecordA(MutualRecordB b) {}
    }

    @JsonConfig(uses = MutualNoArgA.class)
    interface MutualNoArgB {
        @JsonOutput
        void writeB(MutualRecordB b, JsonGenerator gen) throws Exception;

        record MutualRecordB(MutualRecordA a) {}
    }
}
