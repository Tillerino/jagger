package org.tillerino.jagger.converters;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.tillerino.jagger.annotations.JsonInputConverter;
import org.tillerino.jagger.annotations.JsonInputDefaultValue;
import org.tillerino.jagger.annotations.Nullable;

/** Input converters for the optional types. */
public class OptionalInputConverters {
    @JsonInputConverter
    public static <T> Optional<T> nullableToOptional(@Nullable T nullable) {
        return Optional.ofNullable(nullable);
    }

    @JsonInputConverter
    public static OptionalInt boxedToOptionalInt(@Nullable Integer boxed) {
        return boxed == null ? OptionalInt.empty() : OptionalInt.of(boxed);
    }

    @JsonInputConverter
    public static OptionalLong boxedToOptionalLong(@Nullable Long boxed) {
        return boxed == null ? OptionalLong.empty() : OptionalLong.of(boxed);
    }

    @JsonInputConverter
    public static OptionalDouble boxedToOptionalDouble(@Nullable Double boxed) {
        return boxed == null ? OptionalDouble.empty() : OptionalDouble.of(boxed);
    }

    @JsonInputDefaultValue
    public static <T> Optional<T> defaultOptional() {
        return Optional.empty();
    }

    @JsonInputDefaultValue
    public static OptionalInt defaultOptionalInt() {
        return OptionalInt.empty();
    }

    @JsonInputDefaultValue
    public static OptionalLong defaultOptionalLong() {
        return OptionalLong.empty();
    }

    @JsonInputDefaultValue
    public static OptionalDouble defaultOptionalDouble() {
        return OptionalDouble.empty();
    }
}
