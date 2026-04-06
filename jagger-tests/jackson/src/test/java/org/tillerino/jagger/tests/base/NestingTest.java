package org.tillerino.jagger.tests.base;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.NestingSerde.*;
import org.tillerino.jagger.tests.model.NestingModel;

/**
 * We test ways of nesting JSON objects (Java records, plain Java objects, and Java Maps) and JSON arrays (Java arrays,
 * Java Lists, and Java Sets). These tests do not need to be tested with multiple backends, and we test everything with
 * Jackson streaming.
 *
 * <p>"Outer" objects are meant to contain all sorts of fields to test the nesting. "Inner" objects are contained by
 * outer objects and do not need to be complex. Both inner and outer objects are tested with records, field-based access
 * and accessor-based access.
 *
 * <p>Since all collections are treated identically internally, we can just test Java Lists. Since the JSON of Java
 * lists and Java arrays is identical, we can combine these tests.
 *
 * <p>Checks: (record, fields, accessors, array, list, map)
 *
 * <ul>
 *   <li>Outer (record, fields, accessors), inner (record, fields, accessors, array list, map): testOuterObject
 *   <li>Outer (array, list), inner (record, fields, accessors): testInnerObjectArray
 *   <li>Outer (array, list), inner (array, list): testDoubleArrayArray
 *   <li>Outer (array, list), inner map: testStringDoubleMapArray
 *   <li>Outer map, inner (record, fields, accessors): testStringInnerObjectMap
 *   <li>Outer map, inner (array, list): testStringDoubleArrayMap
 *   <li>Outer map, inner map: testStringStringDoubleMapMap
 * </ul>
 */
class NestingTest extends ReferenceTest {
    @Test
    void testOuterObject() throws Exception {
        OuterRecordSerde recordImpl = SerdeUtil.impl(OuterRecordSerde.class);
        OuterFieldsSerde fieldsImpl = SerdeUtil.impl(OuterFieldsSerde.class);
        OuterAccessorsSerde accessorsImpl = SerdeUtil.impl(OuterAccessorsSerde.class);

        String[] jsons = {
            "null",
            "{}",
            "{\"doubleArray\":null, \"doubleList\":null, \"doubleMap\":null, \"innerRecord\":null, \"innerFields\":null, \"innerAccessors\":null}",
            "{\"doubleArray\":[], \"doubleList\":[], \"doubleMap\":{}, \"innerRecord\":{}, \"innerFields\":{}, \"innerAccessors\":{}}",
            "{\"doubleArray\":[1.0], \"doubleList\":[1.0], \"doubleMap\":{\"a\":1.0}, \"innerRecord\":{\"d\":1.0}, \"innerFields\":{\"d\":1.0}, \"innerAccessors\":{\"d\":1.0}}",
            """
				{
					"doubleArray":[1.0,null,"NaN","Infinity","-Infinity",2.0],
					"doubleList":[1.0,null,"NaN","Infinity","-Infinity",2.0],
					"doubleMap":{"a":1.0,"b":null,"c":"NaN","d":"Infinity","e":"-Infinity","f":2.0},
					"innerRecord":{"d":"NaN"}, "innerFields":{"d":"Infinity"}, "innerAccessors":{"d":"-Infinity"}
				}
			""",
        };

        for (String json : jsons) {
            NestingModel.OuterRecord outerRecord = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, recordImpl::read, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(outerRecord, recordImpl::write);

            NestingModel.OuterFields outerFields = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, fieldsImpl::read, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(outerFields, fieldsImpl::write);

            NestingModel.OuterAccessors outerAccessors = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, accessorsImpl::read, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(outerAccessors, accessorsImpl::write);
        }
    }

    @Test
    void testDoubleArrayArray() throws Exception {
        ArraySerde arrayImpl = SerdeUtil.impl(ArraySerde.class);
        ListSerde listImpl = SerdeUtil.impl(ListSerde.class);

        String[] jsons = {
            "null",
            "[]",
            "[null]",
            "[[]]",
            "[[1.0]]",
            """
				[
					null,
					[1.0,null,"NaN","Infinity","-Infinity",2.0]
				]
			""",
        };

        for (String json : jsons) {
            Double[][] doubleArrayArray = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, arrayImpl::readDoubleArrayArray, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(doubleArrayArray, arrayImpl::writeDoubleArrayArray);

            List<Double>[] doubleListArray = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, arrayImpl::readDoubleListArray, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(doubleListArray, arrayImpl::writeDoubleListArray);

            List<Double[]> doubleArrayList = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, listImpl::readDoubleArrayList, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(doubleArrayList, listImpl::writeDoubleArrayList);

            List<List<Double>> doubleListList = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, listImpl::readDoubleListList, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(doubleListList, listImpl::writeDoubleListList);
        }
    }

    @Test
    void testStringDoubleMapArray() throws Exception {
        ArraySerde arrayImpl = SerdeUtil.impl(ArraySerde.class);
        ListSerde listImpl = SerdeUtil.impl(ListSerde.class);

        String[] jsons = {
            "null",
            "[]",
            "[null]",
            "[{}]",
            "[{\"a\":1.0}]",
            """
				[
					null,
					{"a":1.0,"b":null,"c":"NaN","d":"Infinity","e":"-Infinity","f":2.0}
				]
			""",
        };

        for (String json : jsons) {
            Map<String, Double>[] stringDoubleMapArray = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, arrayImpl::readStringDoubleMapArray, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringDoubleMapArray, arrayImpl::writeStringDoubleMapArray);

            List<Map<String, Double>> stringDoubleMapList = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, listImpl::readStringDoubleMapList, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringDoubleMapList, listImpl::writeStringDoubleMapList);
        }
    }

    @Test
    void testStringInnerObjectMap() throws Exception {
        MapSerde mapImpl = SerdeUtil.impl(MapSerde.class);

        String[] jsons = {
            "null",
            "{}",
            "{\"a\":null}",
            "{\"a\":{}}",
            "{\"a\":{\"d\":1.0}}",
            """
				{
					"a":null,
					"b":{"d":"NaN"},
					"c":{"d":"Infinity"},
					"d":{"d":"-Infinity"}
				}
			""",
        };

        for (String json : jsons) {
            Map<String, NestingModel.InnerRecord> stringRecordMap =
                    inputUtils.assertIsEqualToDatabindComparingRecursively(
                            json, mapImpl::readStringRecordMap, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringRecordMap, mapImpl::writeStringRecordMap);

            Map<String, NestingModel.InnerAccessors> stringAccessorsMap =
                    inputUtils.assertIsEqualToDatabindComparingRecursively(
                            json, mapImpl::readStringAccessorsMap, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringAccessorsMap, mapImpl::writeStringAccessorsMap);

            Map<String, NestingModel.InnerFields> stringFieldsMap =
                    inputUtils.assertIsEqualToDatabindComparingRecursively(
                            json, mapImpl::readStringFieldsMap, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringFieldsMap, mapImpl::writeStringFieldsMap);
        }
    }

    @Test
    void testInnerObjectArray() throws Exception {
        ArraySerde arrayImpl = SerdeUtil.impl(ArraySerde.class);
        ListSerde listImpl = SerdeUtil.impl(ListSerde.class);

        String[] jsons = {
            "null",
            "[]",
            "[null]",
            "[{}]",
            "[{\"d\":1.0}]",
            """
				[
					null,
					{"d":"NaN"},
					{"d":"Infinity"},
					{"d":"-Infinity"}
				]
			""",
        };

        for (String json : jsons) {
            NestingModel.InnerRecord[] innerRecordArray = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, arrayImpl::readInnerRecordArray, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(innerRecordArray, arrayImpl::writeInnerRecordArray);

            NestingModel.InnerFields[] innerFieldsArray = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, arrayImpl::readInnerFieldsArray, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(innerFieldsArray, arrayImpl::writeInnerFieldsArray);

            NestingModel.InnerAccessors[] innerAccessorsArray = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, arrayImpl::readInnerAccessorsArray, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(innerAccessorsArray, arrayImpl::writeInnerAccessorsArray);

            List<NestingModel.InnerRecord> innerRecordList = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, listImpl::readInnerRecordList, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(innerRecordList, listImpl::writeInnerRecordList);

            List<NestingModel.InnerFields> innerFieldsList = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, listImpl::readInnerFieldsList, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(innerFieldsList, listImpl::writeInnerFieldsList);

            List<NestingModel.InnerAccessors> innerAccessorsList =
                    inputUtils.assertIsEqualToDatabindComparingRecursively(
                            json, listImpl::readInnerAccessorsList, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(innerAccessorsList, listImpl::writeInnerAccessorsList);
        }
    }

    @Test
    void testStringStringDoubleMapMap() throws Exception {
        MapSerde mapImpl = SerdeUtil.impl(MapSerde.class);

        String[] jsons = {
            "null",
            "{}",
            "{\"a\":null}",
            "{\"a\":{}}",
            "{\"a\":{\"b\":1.0}}",
            """
				{
					"a":null,
					"b":{"a": 1.0, "b":"NaN", "c":"Infinity", "d":"-Infinity", "e": null, "f": 2.0}
				}
			""",
        };

        for (String json : jsons) {
            Map<String, Map<String, Double>> stringStringDoubleMapMap =
                    inputUtils.assertIsEqualToDatabindComparingRecursively(
                            json, mapImpl::readStringStringDoubleMapMap, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringStringDoubleMapMap, mapImpl::writeStringStringDoubleMapMap);
        }
    }

    @Test
    void testStringDoubleArrayMap() throws Exception {
        MapSerde mapImpl = SerdeUtil.impl(MapSerde.class);

        String[] jsons = {
            "null",
            "{}",
            "{\"a\":null}",
            "{\"a\":[]}",
            "{\"a\":[1.0]}",
            """
				{
					"a":null,
					"b":[1.0,null,"NaN","Infinity","-Infinity",2.0]
				}
			""",
        };

        for (String json : jsons) {
            Map<String, Double[]> stringDoubleArrayMap = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, mapImpl::readStringDoubleArrayMap, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringDoubleArrayMap, mapImpl::writeStringDoubleArrayMap);

            Map<String, List<Double>> stringDoubleListMap = inputUtils.assertIsEqualToDatabindComparingRecursively(
                    json, mapImpl::readStringDoubleListMap, new TypeReference<>() {});
            outputUtils.assertIsEqualToDatabind(stringDoubleListMap, mapImpl::writeStringDoubleListMap);
        }
    }

    /** Adders are hard to support. For now, we check that if there is an adder and a setter, deserialization works. */
    @Test
    void adderIsIgnoredIfSetterIsPresent() throws Exception {
        ListSerde listSerde = SerdeUtil.impl(ListSerde.class);

        String[] jsons = {
            "null",
            "{}",
            "{ \"things\": [] }",
            "{ \"things\": [ \"thing\" ] }",
            "{ \"things\": [ \"thing\", \"another\" ] }",
        };
        for (String json : jsons) {
            inputUtils.assertIsEqualToDatabind(json, listSerde::readListFieldWithAdder, new TypeReference<>() {});
        }
    }
}
