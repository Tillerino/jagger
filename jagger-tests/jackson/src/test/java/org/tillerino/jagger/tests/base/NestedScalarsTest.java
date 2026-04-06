package org.tillerino.jagger.tests.base;

import static org.tillerino.jagger.tests.TestSettingsBase.JavaData.map;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;

class NestedScalarsTest extends ReferenceTest {
    NestedScalarsWriter impl = SerdeUtil.impl(NestedScalarsWriter.class);

    @Test
    void testDoubleArrayArray() throws Exception {
        Double[][][] values = {null, {null}, {{null}}, {null, {null}}, {null, {1D, null, 3D}}};
        for (Double[][] value : values) {
            outputUtils.assertIsEqualToDatabind(value, impl::writeDoubleArrayArray);
        }
    }

    @Test
    void testStringDoubleArrayMap() throws Exception {
        List<Map<String, Double[]>> values = Arrays.asList(
                null, map(), map("a", null), map("a", new Double[] {1D, null, 3D}, "b", null, "c", new Double[] {
                    4D, 5D, null
                }));
        for (Map<String, Double[]> value : values) {
            outputUtils.assertIsEqualToDatabind(value, impl::writeStringDoubleArrayMap);
        }
    }

    @Test
    void testStringDoubleMapList() throws Exception {
        List<List<Map<String, Double>>> values = Arrays.<List<Map<String, Double>>>asList(
                null,
                List.of(),
                Arrays.<Map<String, Double>>asList(new Map[] {null}),
                List.of(map()),
                List.of(map("a", 1D, "b", null, "c", 3D), map("d", 4D, "e", 5D, "f", null)));
        for (List<Map<String, Double>> value : values) {
            outputUtils.assertIsEqualToDatabind(value, impl::writeStringDoubleMapList);
        }
    }

    @Test
    void testStringDoubleMapMap() throws Exception {
        List<Map<String, Map<String, Double>>> values = Arrays.asList(
                null,
                map(),
                map("a", null),
                map("a", map("a", 1D, "b", null, "c", 3D), "b", null, "c", map("d", 4D, "e", 5D, "f", null)));
        for (Map<String, Map<String, Double>> value : values) {
            outputUtils.assertIsEqualToDatabind(value, impl::writeStringDoubleMapMap);
        }
    }
}
