package org.tillerino.jagger.processor.config;

import com.google.auto.service.AutoService;
import java.util.Optional;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.Exceptions;

@AutoService(JaggerPlugin.class)
public class JacksonAnnotationsPlugin implements JaggerPlugin {
    public static final String CFJA = "com.fasterxml.jackson.annotation";

    @Override
    public void configure(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                UnknownProperties.UNKNOWN_PROPERTIES,
                CFJA + ".JsonIgnoreProperties",
                ann -> ann.method("ignoreUnknown", true)
                        .map(AnnotationValueWrapper::asBoolean)
                        .map(i ->
                                i ? JsonConfig.UnknownPropertiesMode.IGNORE : JsonConfig.UnknownPropertiesMode.THROW));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                RequiredProperty.REQUIRED_PROPERTY,
                CFJA + ".JsonProperty",
                ann -> ann.method("required", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                IgnoreProperty.IGNORE_PROPERTY,
                CFJA + ".JsonIgnore",
                ann -> ann.method("value", true).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                PropertyName.PROPERTY_NAME,
                CFJA + ".JsonProperty",
                ann -> ann.method("value", true).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                IgnoreProperties.IGNORED_PROPERTIES,
                CFJA + ".JsonIgnoreProperties",
                ann -> ann.method("value", true)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(AnnotationValueWrapper::asString)
                                .collect(ConfigProperty.toUnmodifiableSet())));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                References.REFERENCES,
                CFJA + ".JsonIdentityInfo",
                ann -> Optional.of(new References.Config(
                        ann.method("property", false).map(AnnotationValueWrapper::asString),
                        ann.method("generator", false)
                                .map(AnnotationValueWrapper::asTypeMirror)
                                .orElseThrow(Exceptions::unexpected),
                        ann.method("resolver", false).map(AnnotationValueWrapper::asTypeMirror),
                        ann.method("scope", false).map(AnnotationValueWrapper::asTypeMirror))));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                PropertyOrder.PROPERTY_ORDER,
                CFJA + ".JsonPropertyOrder",
                ann -> ann.method("value", false)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(AnnotationValueWrapper::asString)
                                .collect(java.util.stream.Collectors.toUnmodifiableList())));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                PropertyOrder.PROPERTY_ORDER_ALPHABETIC,
                CFJA + ".JsonPropertyOrder",
                ann -> ann.method("alphabetic", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                Alias.ALIASES,
                CFJA + ".JsonAlias",
                ann -> ann.method("value", true)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(AnnotationValueWrapper::asString)
                                .collect(ConfigProperty.toUnmodifiableSet())));
    }
}
