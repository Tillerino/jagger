package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.converters.AllInputConverters;
import org.tillerino.jagger.converters.AllOutputConverters;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OffsetDateTimeRecord;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OptionalComponentsRecord;

@JsonConfig(uses = {AllInputConverters.class, AllOutputConverters.class})
interface ConvertersSerde {
    @JsonOutput
    void writeOffsetDateTimeRecord(OffsetDateTimeRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    OffsetDateTimeRecord readOffsetDateTimeRecord(JsonParser parser) throws Exception;

    @JsonOutput
    void writeOptionalComponentsRecord(OptionalComponentsRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    OptionalComponentsRecord readOptionalComponentsRecord(JsonParser parser) throws Exception;
}
