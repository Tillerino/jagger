package org.tillerino.jagger.processor.ext;

import java.util.Collection;
import java.util.List;
import org.tillerino.jagger.processor.JaggerContext;

public interface JaggerPlugin {
    /**
     * If this plugin introduces new annotations to be processed, they must be returned here.
     *
     * @return The fully qualified names of the annotation types.
     */
    default Collection<String> getSupportedAnnotationTypes() {
        return List.of();
    }

    /**
     * Jagger's context may be modified in any way - however sensible. Sensible modifications include:
     *
     * <ul>
     *   <li>Registering code generators via {@link JaggerContext#register(PrototypeDetector)}.
     *   <li>Registering method-level generators via {@link JaggerContext#register(PrototypeDetector)}.
     *   <li>Registering class-level code via {@link JaggerContext#register(BlueprintConfigurator)}.
     *   <li>Adding support for annotations. {@link org.tillerino.jagger.processor.config.JacksonAnnotationsPlugin} is a
     *       good example.
     * </ul>
     *
     * Less sensible modifications include:
     *
     * <ul>
     *   <li>Overwriting fields of {@link JaggerContext} with your own implementations. These fields and their
     *       implementations are intentionally public and non-final. Nonetheless, that code is considered internal and
     *       its API can change quickly and without warning. This is intended for temporary fixes when upstream changes
     *       are required.
     * </ul>
     *
     * @param ctx the partially configured context. The order of configurations depends on the annotation processor
     *     path.
     */
    void configure(JaggerContext ctx);
}
