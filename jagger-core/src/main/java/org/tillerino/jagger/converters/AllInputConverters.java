package org.tillerino.jagger.converters;

import org.tillerino.jagger.annotations.JsonConfig;

/**
 * All input converters provided by Jagger.
 *
 * <p>Can be reused with {@code @JsonConfig(uses = AllInputConverters.class)}.
 */
@JsonConfig(
        uses = {
            BigMathInputConverters.class,
            JavaTimeInputConverters.class,
            OptionalInputConverters.class,
            UuidInputConverters.class,
        })
public class AllInputConverters {}
