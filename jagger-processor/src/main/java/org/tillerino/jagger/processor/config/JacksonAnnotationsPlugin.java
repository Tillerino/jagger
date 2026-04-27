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
    @Override
    public void configure(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                UnknownProperties.UNKNOWN_PROPERTIES,
                "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
                ann -> ann.method("ignoreUnknown", true)
                        .map(AnnotationValueWrapper::asBoolean)
                        .map(i ->
                                i ? JsonConfig.UnknownPropertiesMode.IGNORE : JsonConfig.UnknownPropertiesMode.THROW));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                RequiredProperty.REQUIRED_PROPERTY,
                "com.fasterxml.jackson.annotation.JsonProperty",
                ann -> ann.method("required", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                IgnoreProperty.IGNORE_PROPERTY,
                "com.fasterxml.jackson.annotation.JsonIgnore",
                ann -> ann.method("value", true).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                PropertyName.PROPERTY_NAME,
                "com.fasterxml.jackson.annotation.JsonProperty",
                ann -> ann.method("value", true).map(AnnotationValueWrapper::asString));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                IgnoreProperties.IGNORED_PROPERTIES,
                "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
                ann -> ann.method("value", true)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(AnnotationValueWrapper::asString)
                                .collect(ConfigProperty.toUnmodifiableSet())));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                References.REFERENCES,
                "com.fasterxml.jackson.annotation.JsonIdentityInfo",
                ann -> Optional.of(new References.Config(
                        ann.method("property", false).map(AnnotationValueWrapper::asString),
                        ann.method("generator", false)
                                .map(AnnotationValueWrapper::asTypeMirror)
                                .orElseThrow(Exceptions::unexpected),
                        ann.method("resolver", false).map(AnnotationValueWrapper::asTypeMirror),
                        ann.method("scope", false).map(AnnotationValueWrapper::asTypeMirror))));
    }
}
