package org.tillerino.jagger.converters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.tillerino.jagger.annotations.JsonOutputConverter;

/** Output converters for the java.time types. */
public class JavaTimeOutputConverters {
    @JsonOutputConverter
    public static String localDateToString(LocalDate localDate) {
        return localDate.toString();
    }

    @JsonOutputConverter
    public static String localDateTimeToString(LocalDateTime localDateTime) {
        return localDateTime.toString();
    }

    @JsonOutputConverter
    public static String localTimeToString(LocalTime localTime) {
        return localTime.toString();
    }

    @JsonOutputConverter
    public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toString();
    }

    @JsonOutputConverter
    public static String zonedDateTimeToString(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toString();
    }
}
