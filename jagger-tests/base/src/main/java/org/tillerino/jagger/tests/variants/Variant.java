package org.tillerino.jagger.tests.variants;

import org.tillerino.jagger.tests.Replacement;

/** Determines one variant to be produced by {@link GenerateVariants}. */
public @interface Variant {
    Replacement[] value();
}
