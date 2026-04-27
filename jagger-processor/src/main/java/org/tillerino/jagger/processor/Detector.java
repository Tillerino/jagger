package org.tillerino.jagger.processor;

import java.util.List;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PrototypeKind;

public interface Detector {
    /** @return may contain nulls for convenience */
    List<TypeElement> supportedAnnotationTypes();

    default Optional<PrototypeKind> detect(InstantiatedMethod m) {
        return Optional.empty();
    }
}
