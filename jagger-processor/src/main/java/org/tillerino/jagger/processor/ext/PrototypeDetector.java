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

    default Optional<PrototypeKind> detect(InstantiatedMethod m) {
        return Optional.empty();
    }
}
