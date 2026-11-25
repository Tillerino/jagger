package org.tillerino.jagger.tests.model.features;

import java.time.OffsetDateTime;
import java.util.*;

public interface ConvertersModel {
    record OffsetDateTimeRecord(OffsetDateTime timestamp) {}

    record UuidRecord(UUID value) {
        public static final List<UuidRecord> RECORDS = Arrays.asList(
                null, new UuidRecord(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")), new UuidRecord(null));
    }

    record OptionalComponentsRecord(
            Optional<String> optionalString,
            OptionalInt optionalInt,
            OptionalLong optionalLong,
            OptionalDouble optionalDouble,
            Optional<Optional<String>> nestedOptionalBecauseWeCan) {}
}
