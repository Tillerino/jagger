package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.CreatorsModel.*;
import org.tillerino.jagger.tests.model.features.CreatorsModel.Priority.*;

public interface CreatorsSerde {

    @JsonInput
    JsonCreatorConstructorFactoryClass<Integer> readConstructorFactory(JsonParser parser) throws Exception;

    @JsonInput
    JsonCreatorConstructorFactoryMultiplePropertiesClass<Integer> readConstructorFactoryMultipleProperties(
            JsonParser parser, Thread t) throws Exception;

    @JsonInput
    JsonCreatorMethodFactoryRecord<Integer> readMethodFactory(JsonParser parser) throws Exception;

    @JsonInput
    JsonCreatorMethodFactoryMultiplePropertiesRecord<Integer> readMethodFactoryMultipleProperties(
            JsonParser parser, Thread t) throws Exception;

    @JsonInput
    JsonCreatorConstructorCreatorClass<Integer> readConstructorCreator(JsonParser parser) throws Exception;

    @JsonInput
    JsonCreatorMethodCreatorRecord<Integer> readMethodCreator(JsonParser parser) throws Exception;

    @JsonInput
    JsonCreatorMethodCreatorSinglePropertyRecord<Integer> readMethodCreatorSingleProperty(JsonParser parser)
            throws Exception;

    @JsonInput
    JsonCreatorConstructorCreatorSinglePropertyClass<Integer> readConstructorCreatorSingleProperty(JsonParser parser)
            throws Exception;

    @JsonOutput
    void write(JsonValueRecord<Integer> obj, JsonGenerator generator) throws Exception;

    @JsonInput
    PolyInterface readPolyInterfaceNestedWithCreator(JsonParser parser) throws Exception;

    interface Priority {
        @JsonOutput
        void writeJsonValueEnum(JsonValueEnum enumValue, JsonGenerator generator) throws Exception;

        @JsonOutput
        void writeJsonValueIterable(JsonValueIterable iterable, JsonGenerator generator) throws Exception;

        @JsonOutput
        void writeJsonValueMap(JsonValueMap map, JsonGenerator generator) throws Exception;

        @JsonInput
        JsonCreatorMethodEnum readJsonCreatorMethodEnum(JsonParser parser) throws Exception;

        @JsonInput
        JsonCreatorMethodCollection readJsonCreatorMethodCollection(JsonParser parser) throws Exception;

        @JsonInput
        JsonCreatorMethodMap readJsonCreatorMethodMap(JsonParser parser) throws Exception;

        @JsonInput
        JsonCreatorMethodMultipleParamsEnum readJsonCreatorMethodMultipleParamsEnum(JsonParser parser) throws Exception;

        @JsonInput
        JsonCreatorMethodMultipleParamsCollection readJsonCreatorMethodMultipleParamsCollection(JsonParser parser)
                throws Exception;

        @JsonInput
        JsonCreatorMethodMultipleParamsMap readJsonCreatorMethodMultipleParamsMap(JsonParser parser) throws Exception;
    }
}
