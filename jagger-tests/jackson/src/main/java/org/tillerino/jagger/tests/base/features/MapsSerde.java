package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.Map;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.converters.AllOutputConverters;
import org.tillerino.jagger.tests.model.features.CreatorsModel.ConvertersInheritance.JsonValueChild;
import org.tillerino.jagger.tests.model.features.MapsModel.Keys.OffsetDateTimeMapRecord;

interface MapsSerde {
    @JsonConfig(uses = AllOutputConverters.class)
    interface Keys {
        @JsonOutput
        void writeOffsetDateTimeMapRecord(OffsetDateTimeMapRecord record, JsonGenerator generator) throws Exception;

        @JsonOutput
        void writeJsonValueMap(Map<JsonValueChild, String> map, JsonGenerator generator) throws Exception;
    }
}
