package org.tillerino.jagger.converters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.tillerino.jagger.annotations.JsonInputConverter;

/** Input converters for the java.time types. */
public class JavaTimeInputConverters {
    @JsonInputConverter
    public static LocalDate stringToLocalDate(String string) {
        return LocalDate.parse(string);
    }

    @JsonInputConverter
    public static LocalDateTime stringToLocalDateTime(String string) {
        return LocalDateTime.parse(string);
    }

    @JsonInputConverter
    public static LocalTime stringToLocalTime(String string) {
        return LocalTime.parse(string);
    }

    @JsonInputConverter
    public static OffsetDateTime stringToOffsetDateTime(String string) {
        return OffsetDateTime.parse(string);
    }

    @JsonInputConverter
    public static ZonedDateTime stringToZonedDateTime(String string) {
        return ZonedDateTime.parse(string);
    }
}
