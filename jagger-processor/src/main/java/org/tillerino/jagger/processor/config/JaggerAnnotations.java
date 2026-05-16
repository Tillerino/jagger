package org.tillerino.jagger.processor.config;

import java.util.stream.Collectors;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public class JaggerAnnotations {
    public static final String JSON_CONFIG = "org.tillerino.jagger.annotations.JsonConfig";

    public static void configureJaggerAnnotations(JaggerContext ctx) {
        ctx.configProperties.addConfigAnnotation(
                AnyConfig.USES,
                JSON_CONFIG,
                ann -> ann.method("uses", true)
                        .map(AnnotationValueWrapper::asArray)
                        .map(classNames -> classNames.stream()
                                .map(className -> ctx.elements.getTypeElement(
                                        className.asTypeMirror().toString()))
                                .collect(Collectors.toUnmodifiableSet())));

        ctx.configProperties.addConfig(
                UnknownProperties.UNKNOWN_PROPERTIES,
                jsonConfigPropertyRetriever("unknownProperties", JsonConfig.UnknownPropertiesMode.class));

        ctx.configProperties.addConfig(
                CodeGeneration.IMPLEMENT,
                jsonConfigPropertyRetriever("implement", JsonConfig.ImplementationMode.class));

        ctx.configProperties.addConfigAnnotation(
                CodeGeneration.ON_GENERATED_CLASS,
                JSON_CONFIG,
                ann -> ann.method("onGeneratedClass", false)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(classValue -> ctx.elements.getTypeElement(
                                        classValue.asTypeMirror().toString()))
                                .collect(ConfigProperty.toUnmodifiableSet())));

        ctx.configProperties.addConfigAnnotation(
                CodeGeneration.ON_GENERATED_CONSTRUCTOR,
                JSON_CONFIG,
                ann -> ann.method("onGeneratedConstructors", false)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(classValue -> ctx.elements.getTypeElement(
                                        classValue.asTypeMirror().toString()))
                                .collect(ConfigProperty.toUnmodifiableSet())));

        ctx.configProperties.addConfigAnnotation(
                CodeGeneration.ADD_GENERATED_ANNOTATION_TO_CLASS,
                JSON_CONFIG,
                ann -> ann.method("addGeneratedAnnotationToClass", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addConfigAnnotation(
                CodeGeneration.ADD_GENERATED_ANNOTATION_TO_METHODS,
                JSON_CONFIG,
                ann -> ann.method("addGeneratedAnnotationToMethods", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addConfig(
                Delegation.DELEGATE_TO, jsonConfigPropertyRetriever("delegateTo", JsonConfig.DelegateeMode.class));

        ctx.configProperties.addConfig(
                Verification.VERIFY_SYMMETRY,
                jsonConfigPropertyRetriever("verifySymmetry", JsonConfig.VerificationMode.class));

        ctx.configProperties.addConfig(
                ctx.codeGeneration.provider,
                new AnnotationConfigPropertyRetriever<>(
                        JSON_CONFIG, ann -> ann.method("provider", false).map(AnnotationValueWrapper::asTypeMirror)));
    }

    public static <T extends Enum<T>> AnnotationConfigPropertyRetriever<T> jsonConfigPropertyRetriever(
            String method, Class<T> enumClass) {
        return new AnnotationConfigPropertyRetriever<>(
                JSON_CONFIG,
                ann -> ann.method(method, false)
                        .map(annotationValueWrapper -> annotationValueWrapper.asEnum(enumClass)));
    }
}
