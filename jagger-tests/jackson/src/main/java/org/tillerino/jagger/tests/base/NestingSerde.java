package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.List;
import java.util.Map;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.ListFieldWithAdder;
import org.tillerino.jagger.tests.model.NestingModel;

public class NestingSerde {
    interface OuterRecordSerde {
        @JsonOutput
        void write(NestingModel.OuterRecord obj, JsonGenerator out) throws Exception;

        @JsonInput
        NestingModel.OuterRecord read(JsonParser parser) throws Exception;
    }

    interface OuterFieldsSerde {
        @JsonOutput
        void write(NestingModel.OuterFields obj, JsonGenerator out) throws Exception;

        @JsonInput
        NestingModel.OuterFields read(JsonParser parser) throws Exception;
    }

    interface OuterAccessorsSerde {
        @JsonOutput
        void write(NestingModel.OuterAccessors obj, JsonGenerator out) throws Exception;

        @JsonInput
        NestingModel.OuterAccessors read(JsonParser parser) throws Exception;
    }

    interface ArraySerde {
        @JsonOutput
        void writeDoubleArrayArray(Double[][] obj, JsonGenerator out) throws Exception;

        @JsonInput
        Double[][] readDoubleArrayArray(JsonParser parser) throws Exception;

        @JsonOutput
        void writeDoubleListArray(List<Double>[] obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<Double>[] readDoubleListArray(JsonParser parser) throws Exception;

        @JsonOutput
        void writeStringDoubleMapArray(Map<String, Double>[] obj, JsonGenerator out) throws Exception;

        @JsonInput
        Map<String, Double>[] readStringDoubleMapArray(JsonParser parser) throws Exception;

        @JsonOutput
        void writeInnerRecordArray(NestingModel.InnerRecord[] obj, JsonGenerator out) throws Exception;

        @JsonInput
        NestingModel.InnerRecord[] readInnerRecordArray(JsonParser parser) throws Exception;

        @JsonOutput
        void writeInnerFieldsArray(NestingModel.InnerFields[] obj, JsonGenerator out) throws Exception;

        @JsonInput
        NestingModel.InnerFields[] readInnerFieldsArray(JsonParser parser) throws Exception;

        @JsonOutput
        void writeInnerAccessorsArray(NestingModel.InnerAccessors[] obj, JsonGenerator out) throws Exception;

        @JsonInput
        NestingModel.InnerAccessors[] readInnerAccessorsArray(JsonParser parser) throws Exception;
    }

    interface ListSerde {
        @JsonOutput
        void writeDoubleArrayList(List<Double[]> obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<Double[]> readDoubleArrayList(JsonParser parser) throws Exception;

        @JsonOutput
        void writeDoubleListList(List<List<Double>> obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<List<Double>> readDoubleListList(JsonParser parser) throws Exception;

        @JsonOutput
        void writeStringDoubleMapList(List<Map<String, Double>> obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<Map<String, Double>> readStringDoubleMapList(JsonParser parser) throws Exception;

        @JsonOutput
        void writeInnerRecordList(List<NestingModel.InnerRecord> obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<NestingModel.InnerRecord> readInnerRecordList(JsonParser parser) throws Exception;

        @JsonOutput
        void writeInnerFieldsList(List<NestingModel.InnerFields> obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<NestingModel.InnerFields> readInnerFieldsList(JsonParser parser) throws Exception;

        @JsonOutput
        void writeInnerAccessorsList(List<NestingModel.InnerAccessors> obj, JsonGenerator out) throws Exception;

        @JsonInput
        List<NestingModel.InnerAccessors> readInnerAccessorsList(JsonParser parser) throws Exception;

        @JsonOutput
        void writeListFieldWithAdder(ListFieldWithAdder listFieldWithAdder, JsonGenerator gen) throws Exception;

        @JsonInput
        ListFieldWithAdder readListFieldWithAdder(JsonParser parser) throws Exception;
    }

    interface MapSerde {

        @JsonInput
        Map<String, Map<String, Double>> readStringStringDoubleMapMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writeStringStringDoubleMapMap(Map<String, Map<String, Double>> obj, JsonGenerator out) throws Exception;

        @JsonOutput
        void writeStringDoubleArrayMap(Map<String, Double[]> obj, JsonGenerator out) throws Exception;

        @JsonOutput
        void writeStringDoubleListMap(Map<String, List<Double>> obj, JsonGenerator out) throws Exception;

        @JsonInput
        Map<String, List<Double>> readStringDoubleListMap(JsonParser parser) throws Exception;

        @JsonInput
        Map<String, Double[]> readStringDoubleArrayMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writeStringRecordMap(Map<String, NestingModel.InnerRecord> obj, JsonGenerator out) throws Exception;

        @JsonInput
        Map<String, NestingModel.InnerRecord> readStringRecordMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writeStringAccessorsMap(Map<String, NestingModel.InnerAccessors> obj, JsonGenerator out) throws Exception;

        @JsonInput
        Map<String, NestingModel.InnerAccessors> readStringAccessorsMap(JsonParser parser) throws Exception;

        @JsonInput
        Map<String, NestingModel.InnerFields> readStringFieldsMap(JsonParser parser) throws Exception;

        @JsonOutput
        void writeStringFieldsMap(Map<String, NestingModel.InnerFields> obj, JsonGenerator out) throws Exception;
    }
}
