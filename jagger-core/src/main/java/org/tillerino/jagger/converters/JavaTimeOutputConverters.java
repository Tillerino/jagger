package org.tillerino.jagger.converters;

import java.time.OffsetDateTime;
import org.tillerino.jagger.annotations.JsonOutputConverter;

/** Output converters for the java.time types. */
public class JavaTimeOutputConverters {
    @JsonOutputConverter
    public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toString();
    }
}
