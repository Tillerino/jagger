package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.converters.OptionalInputConverters;
import org.tillerino.jagger.converters.OptionalOutputConverters;
import org.tillerino.jagger.tests.model.features.ConvertersModel.OptionalComponentsRecord;

@JsonConfig(uses = {OptionalInputConverters.class, OptionalOutputConverters.class})
interface ConvertersSerde {
    @JsonOutput
    void writeOptionalComponentsRecord(OptionalComponentsRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    OptionalComponentsRecord readOptionalComponentsRecord(JsonParser parser) throws Exception;
}
