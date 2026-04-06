package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.List;
import java.util.Map;
import org.tillerino.jagger.annotations.JsonOutput;

interface NestedScalarsWriter {
    @JsonOutput
    void writeDoubleArrayArray(Double[][] array, JsonGenerator output) throws Exception;

    @JsonOutput
    void writeStringDoubleArrayMap(Map<String, Double[]> map, JsonGenerator output) throws Exception;

    @JsonOutput
    void writeStringDoubleMapList(List<Map<String, Double>> list, JsonGenerator output) throws Exception;

    @JsonOutput
    void writeStringDoubleMapMap(Map<String, Map<String, Double>> map, JsonGenerator output) throws Exception;
}
