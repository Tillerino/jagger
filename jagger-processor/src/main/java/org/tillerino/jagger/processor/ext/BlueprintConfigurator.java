package org.tillerino.jagger.processor.ext;

import java.util.List;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;

public interface BlueprintConfigurator {
    /**
     * Any type declarations annotated with any of the returned types are passed to {@link #configure(JaggerBlueprint,
     * AnnotationMirrorWrapper)}.
     */
    List<String> supportedAnnotationTypes();

    void configure(JaggerBlueprint blueprint, AnnotationMirrorWrapper annotation);
}
