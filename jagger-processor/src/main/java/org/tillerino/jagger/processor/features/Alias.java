package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.config.JacksonAnnotationsPlugin;
import org.tillerino.jagger.processor.databind.AbstractCodeGeneratorStack;
import org.tillerino.jagger.processor.util.Annotations;
import org.tillerino.jagger.processor.util.Snippet;

public class Alias {
    public static ConfigProperty<Set<String>> ALIASES = ConfigProperty.createConfigProperty(
            "ALIASES", List.of(LocationKind.PROPERTY), Set.of(), MergeFunction.mergeSets(), PropagationKind.none());

    protected final JaggerContext ctx;

    public Alias(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public static Snippet caseSnippet(AbstractCodeGeneratorStack.Property property) {
        Stream<String> allValues = Stream.concat(
                        Stream.of(property.serializedName()),
                        property.config().resolveProperty(ALIASES).value().stream())
                .distinct();
        return Snippet.join(allValues.map(name -> Snippet.of("case $S:", name)).toList(), "\n");
    }

    public Map<String, List<String>> getEnumConstantJsonAliases(TypeMirror enumType) {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        TypeElement typeElement = (TypeElement) ctx.types.asElement(enumType);
        for (VariableElement constant : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
            if (constant.getKind() == ElementKind.ENUM_CONSTANT) {
                ctx.annotations
                        .findAnnotation(constant, JacksonAnnotationsPlugin.CFJA + ".JsonAlias")
                        .flatMap(ann -> ann.method("value", true))
                        .filter(wrapper -> !wrapper.asArray().isEmpty())
                        .map(wrapper -> wrapper.asArray().stream()
                                .map(Annotations.AnnotationValueWrapper::asString)
                                .toList())
                        .filter(list -> !list.isEmpty())
                        .ifPresent(aliasList ->
                                aliases.put(constant.getSimpleName().toString(), aliasList));
            }
        }
        return aliases;
    }
}
