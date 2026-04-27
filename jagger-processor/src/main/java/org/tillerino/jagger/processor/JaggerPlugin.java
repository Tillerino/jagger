package org.tillerino.jagger.processor;

import java.util.Set;

public interface JaggerPlugin {
    default Set<String> getSupportedAnnotationTypes() {
        return Set.of();
    }

    void configure(JaggerContext ctx);
}
