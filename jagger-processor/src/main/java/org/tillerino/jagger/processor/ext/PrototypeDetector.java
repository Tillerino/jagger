package org.tillerino.jagger.processor.ext;

import java.util.Collection;
import java.util.Optional;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public interface PrototypeDetector {
    /**
     * Any methods annotated with any of the returned types are passed to {@link #detect(InstantiatedMethod,
     * AnnotationMirrorWrapper)}.
     */
    Collection<String> supportedAnnotationTypes();

    /**
     * Any method annotated with types from {@link #supportedAnnotationTypes()} is passed to this method.
     *
     * @param m the signature of the method to be processed. Type bindings from child types will have been applied.
     * @param annotation the annotation which triggered this detector
     * @return the detected prototype kind. If empty, an error will be reported.
     */
    default Optional<PrototypeKind> detect(InstantiatedMethod m, AnnotationMirrorWrapper annotation) {
        return Optional.empty();
    }
}
