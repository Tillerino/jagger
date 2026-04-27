package org.tillerino.jagger.processor.ext;

import java.util.Set;
import org.tillerino.jagger.processor.JaggerContext;

public interface JaggerPlugin {
    /**
     * If this plugin introduces new annotations to be processed, they must be returned here.
     *
     * @return The fully qualified names of the annotation types.
     */
    default Set<String> getSupportedAnnotationTypes() {
        return Set.of();
    }

    /**
     * Jagger's context may be modified in any way - however sensible. Sensible modifications include:
     *
     * <ul>
     *   <li>Registering code generators via {@link JaggerContext#detectors}.
     *   <li>Adding support for annotations. {@link org.tillerino.jagger.processor.config.JacksonAnnotationsPlugin} is a
     *       good example.
     * </ul>
     *
     * @param ctx the partially configured context. The order of configurations depends on the annotation processor
     *     path.
     */
    void configure(JaggerContext ctx);
}
