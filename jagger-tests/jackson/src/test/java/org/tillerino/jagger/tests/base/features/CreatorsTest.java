package org.tillerino.jagger.tests.base.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.TestSettings;
import org.tillerino.jagger.tests.model.features.CreatorsModel;
import org.tillerino.jagger.tests.model.features.CreatorsModel.JsonCreatorConstructorFactoryMultiplePropertiesClass;
import org.tillerino.jagger.tests.model.features.CreatorsModel.JsonCreatorMethodFactoryMultiplePropertiesRecord;
import org.tillerino.jagger.tests.model.features.CreatorsModel.PolyInterface;
import org.tillerino.jagger.tests.model.features.CreatorsModel.Priority;
import org.tillerino.jagger.tests.model.features.CreatorsModel.Priority.*;

public class CreatorsTest extends ReferenceTest {
    CreatorsSerde serde = SerdeUtil.impl(CreatorsSerde.class);

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorConstructorFactory() throws Exception {
        CreatorsModel.JsonCreatorConstructorFactoryClass<Integer> o = inputUtils.assertIsEqualToDatabind(
                "{ \"notprop\": 1 }", serde::readConstructorFactory, new TypeReference<>() {});
        assertThat(o.getProp()).isEqualTo(1);
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorRecordFactory() throws Exception {
        CreatorsModel.JsonCreatorMethodFactoryRecord<Integer> o = inputUtils.assertIsEqualToDatabind(
                "{ \"notprop\": 1 }", serde::readMethodFactory, new TypeReference<>() {});
        assertThat(o.prop()).isEqualTo(1);
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorConstructorFactoryMultipleProperties() throws Exception {
        JsonCreatorConstructorFactoryMultiplePropertiesClass<Integer> o =
                inputUtils.deserialize2("1", Thread.currentThread(), serde::readConstructorFactoryMultipleProperties);
        assertThat(o.getProp()).isEqualTo(1);
        assertThat(o.getT()).isSameAs(Thread.currentThread());
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorRecordFactoryMultipleProperties() throws Exception {
        JsonCreatorMethodFactoryMultiplePropertiesRecord<Integer> o =
                inputUtils.deserialize2("1", Thread.currentThread(), serde::readMethodFactoryMultipleProperties);
        assertThat(o.prop()).isEqualTo(1);
        assertThat(o.t()).isSameAs(Thread.currentThread());
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorConstructorCreator() throws Exception {
        CreatorsModel.JsonCreatorConstructorCreatorClass<Integer> o = inputUtils.assertIsEqualToDatabind(
                "{ \"notprop\": 1, \"nots\": \"x\" }", serde::readConstructorCreator, new TypeReference<>() {});
        assertThat(o.getProp()).isEqualTo(1);
        assertThat(o.getS()).isEqualTo("x");
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorMethodCreator() throws Exception {
        CreatorsModel.JsonCreatorMethodCreatorRecord<Integer> o = inputUtils.assertIsEqualToDatabind(
                "{ \"notprop\": 1, \"nots\": \"x\" }", serde::readMethodCreator, new TypeReference<>() {});
        assertThat(o.prop()).isEqualTo(1);
        assertThat(o.s()).isEqualTo("x");
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorMethodCreatorSinglePropertyRecord() throws Exception {
        CreatorsModel.JsonCreatorMethodCreatorSinglePropertyRecord<Integer> o = inputUtils.assertIsEqualToDatabind(
                "{ \"notprop\": 1 }", serde::readMethodCreatorSingleProperty, new TypeReference<>() {});
        assertThat(o.prop()).containsExactly(1);
    }

    /** FEATURE-JSON */
    @Test
    public void jsonCreatorConstructorCreatorSinglePropertyRecord() throws Exception {
        CreatorsModel.JsonCreatorConstructorCreatorSinglePropertyClass<Integer> o = inputUtils.assertIsEqualToDatabind(
                "{ \"notprop\": 1 }", serde::readConstructorCreatorSingleProperty, new TypeReference<>() {});
        assertThat(o.getProp()).containsExactly(1);
    }

    @Test
    public void jsonValueOutput() throws Exception {
        for (Integer boxedInt : TestSettings.SETTINGS.javaData().BOXED_INTS) {
            CreatorsModel.JsonValueRecord<Integer> obj = new CreatorsModel.JsonValueRecord<>(boxedInt);
            outputUtils.assertIsEqualToDatabind(obj, serde::write);
        }
    }

    @Test
    public void nestedPolyWithChildCreator() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{ \"@c\": \".CreatorsModel$PolyInterface$PolyChildWithCreator\" }",
                serde::readPolyInterfaceNestedWithCreator,
                new TypeReference<PolyInterface>() {});
    }

    @Nested
    class PriorityTest {
        CreatorsSerde.Priority prioritySerde = SerdeUtil.impl(CreatorsSerde.Priority.class);

        @Test
        public void jsonValueEnum() throws Exception {
            JsonValueEnum enumValue = Priority.JsonValueEnum.VALUE1;
            outputUtils.assertIsEqualToDatabind(enumValue, prioritySerde::writeJsonValueEnum);
        }

        @Test
        public void jsonValueIterable() throws Exception {
            JsonValueIterable iterable = new JsonValueIterable();
            outputUtils.assertIsEqualToDatabind(iterable, prioritySerde::writeJsonValueIterable);
        }

        @Test
        public void jsonValueMap() throws Exception {
            JsonValueMap map = new JsonValueMap();
            map.put("key1", 1);
            map.put("key2", 2);
            outputUtils.assertIsEqualToDatabind(map, prioritySerde::writeJsonValueMap);
        }

        @Test
        public void jsonCreatorMethodEnum() throws Exception {
            JsonCreatorMethodEnum enumValue = inputUtils.assertIsEqualToDatabind(
                    "[7, 8, 9]", prioritySerde::readJsonCreatorMethodEnum, new TypeReference<>() {});
            assertThat(enumValue).isEqualTo(Priority.JsonCreatorMethodEnum.VALUE1);
        }

        @Test
        public void jsonCreatorMethodCollection() throws Exception {
            JsonCreatorMethodCollection collection = inputUtils.assertIsEqualToDatabind(
                    "[7, 8, 9]", prioritySerde::readJsonCreatorMethodCollection, new TypeReference<>() {});
            assertThat(collection.s).isEqualTo(3);
        }

        @Test
        public void jsonCreatorMethodMap() throws Exception {
            JsonCreatorMethodMap map = inputUtils.assertIsEqualToDatabind(
                    "[7, 8, 9]", prioritySerde::readJsonCreatorMethodMap, new TypeReference<>() {});
            assertThat(map.s).isEqualTo(3);
        }

        @Test
        public void jsonCreatorMethodMultipleParamsEnum() throws Exception {
            JsonCreatorMethodMultipleParamsEnum enumValue = inputUtils.assertIsEqualToDatabind(
                    "{}", prioritySerde::readJsonCreatorMethodMultipleParamsEnum, new TypeReference<>() {});
            assertThat(enumValue).isEqualTo(Priority.JsonCreatorMethodMultipleParamsEnum.VALUE1);
        }

        @Test
        public void jsonCreatorMethodMultipleParamsCollection() throws Exception {
            // Jackson can't do this
            JsonCreatorMethodMultipleParamsCollection collection = inputUtils.deserialize(
                    "{ \"i\": [7, 8, 9], \"name\": \"foo\" }",
                    prioritySerde::readJsonCreatorMethodMultipleParamsCollection);
            assertThat(collection.name).isEqualTo("foo");
            assertThat(collection.count).isEqualTo(3);
        }

        @Test
        public void jsonCreatorMethodMultipleParamsMap() throws Exception {
            JsonCreatorMethodMultipleParamsMap map = inputUtils.assertIsEqualToDatabind(
                    "{ \"i\": [7, 8, 9], \"name\": \"foo\" }",
                    prioritySerde::readJsonCreatorMethodMultipleParamsMap,
                    new TypeReference<>() {});
            assertThat(map.name).isEqualTo("foo");
            assertThat(map.count).isEqualTo(3);
        }
    }
}
