package org.tillerino.jagger.processor.features;

import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.*;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public class IgnoreProperty {
    public static ConfigProperty<Boolean> IGNORE_PROPERTY = ConfigProperty.createConfigProperty(
            List.of(LocationKind.PROPERTY),
            List.of(
                    new AnnotationConfigPropertyRetriever<>(
                            "com.fasterxml.jackson.annotation.JsonIgnore",
                            (ann, utils) -> ann.method("value", true).map(AnnotationValueWrapper::asBoolean)),
                    new AnnotationConfigPropertyRetriever<>(
                            "jakarta.persistence.Transient", (ann, utils) -> Optional.of(true)),
                    new AnnotationConfigPropertyRetriever<>(
                            "javax.persistence.Transient", (ann, utils) -> Optional.of(true))),
            false,
            MergeFunction.notDefault(false),
            PropagationKind.none());

    public static ConfigProperty<Boolean> TRANSIENT_FIELD = ConfigProperty.createConfigProperty(
            List.of(LocationKind.PROPERTY),
            List.of(((element, utils) ->
                    element instanceof VariableElement ve && ve.getModifiers().contains(Modifier.TRANSIENT)
                            ? Optional.of(new PropertyOccurrence<>(true, element + " is transient"))
                            : Optional.empty())),
            false,
            MergeFunction.notDefault(false),
            PropagationKind.none());

    public static Boolean isIgnoredForJson(AnyConfig propertyConfig) {
        return propertyConfig.resolveProperty(IGNORE_PROPERTY).value();
    }

    public static boolean isIgnoredForJdbc(AnyConfig config) {
        return config.resolveProperty(IGNORE_PROPERTY).value()
                || config.resolveProperty(TRANSIENT_FIELD).value();
    }
}
