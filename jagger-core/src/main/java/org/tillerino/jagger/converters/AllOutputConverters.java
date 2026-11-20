package org.tillerino.jagger.converters;

import org.tillerino.jagger.annotations.JsonConfig;

/**
 * All output converters provided by Jagger.
 *
 * <p>Can be reused with {@code @JsonConfig(uses = AllOutputConverters.class)}.
 */
@JsonConfig(uses = {JavaTimeOutputConverters.class, OptionalOutputConverters.class, UuidOutputConverters.class})
public class AllOutputConverters {}
