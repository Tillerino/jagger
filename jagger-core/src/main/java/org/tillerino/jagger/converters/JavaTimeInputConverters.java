package org.tillerino.jagger.converters;

import java.time.OffsetDateTime;
import org.tillerino.jagger.annotations.JsonInputConverter;

/** Input converters for the java.time types. */
public class JavaTimeInputConverters {
    @JsonInputConverter
    public static OffsetDateTime stringToOffsetDateTime(String string) {
        return OffsetDateTime.parse(string);
    }
}
