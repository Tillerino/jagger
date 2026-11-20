package org.tillerino.jagger.tests.base.features;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.CreatorsModel.ConvertersInheritance.JsonValueChild;
import org.tillerino.jagger.tests.model.features.MapsModel.Keys.OffsetDateTimeMapRecord;

class MapsTest extends ReferenceTest {
    @Nested
    class KeysTest {
        MapsSerde.Keys serde = SerdeUtil.impl(MapsSerde.Keys.class);

        @Test
        void offsetDateTimeMapKey() throws Exception {
            Map<OffsetDateTime, String> map = new LinkedHashMap<>();
            map.put(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "first");
            map.put(OffsetDateTime.of(2020, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), "second");
            map.put(OffsetDateTime.of(2019, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC), null);
            outputUtils.assertIsEqualToDatabind(new OffsetDateTimeMapRecord(map), serde::writeOffsetDateTimeMapRecord);
        }

        @Test
        void jsonValueMapKey() throws Exception {
            Map<JsonValueChild, String> map = new LinkedHashMap<>();
            map.put(new JsonValueChild(), "value");
            outputUtils.assertIsEqualToDatabind(map, serde::writeJsonValueMap);
        }
    }
}
