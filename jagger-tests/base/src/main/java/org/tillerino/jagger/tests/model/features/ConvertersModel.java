package org.tillerino.jagger.tests.model.features;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public interface ConvertersModel {
    record OptionalComponentsRecord(
            Optional<String> optionalString,
            OptionalInt optionalInt,
            OptionalLong optionalLong,
            OptionalDouble optionalDouble,
            Optional<Optional<String>> nestedOptionalBecauseWeCan) {}
}
