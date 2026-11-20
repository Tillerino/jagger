package org.tillerino.jagger.tests.model.features;

import java.time.OffsetDateTime;
import java.util.Map;

public interface MapsModel {
    interface Keys {
        record OffsetDateTimeMapRecord(Map<OffsetDateTime, String> values) {}
    }
}
