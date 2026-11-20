package org.tillerino.jagger.tests.model.features;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public interface ConvertersModel {
    record OffsetDateTimeRecord(OffsetDateTime timestamp) {}

    record UuidRecord(UUID value) {}

    record OptionalComponentsRecord(
            Optional<String> optionalString,
            OptionalInt optionalInt,
            OptionalLong optionalLong,
            OptionalDouble optionalDouble,
            Optional<Optional<String>> nestedOptionalBecauseWeCan) {}
}
