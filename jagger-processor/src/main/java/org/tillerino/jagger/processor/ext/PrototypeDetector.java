package org.tillerino.jagger.processor.ext;

import java.util.List;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

public interface PrototypeDetector {
    /**
     * Any methods annotated with any of the returned types are passed to {@link #detect(InstantiatedMethod)}.
     *
     * @return may contain nulls for convenience
     */
    List<TypeElement> supportedAnnotationTypes();

    /**
     * Any method annotated with types from {@link #supportedAnnotationTypes()} is passed to this method.
     *
     * @param m the signature of the method to be processed. Type bindings from child types will have been applied.
     * @return the detected prototype kind. If empty, an error will be reported.
     */
    default Optional<PrototypeKind> detect(InstantiatedMethod m) {
        return Optional.empty();
    }
}
