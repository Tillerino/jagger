package org.tillerino.jagger.processor.config;

import java.util.stream.Collectors;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.features.CodeGeneration;
import org.tillerino.jagger.processor.features.Delegation;
import org.tillerino.jagger.processor.features.UnknownProperties;
import org.tillerino.jagger.processor.features.Verification;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;

public class JaggerAnnotations {
    public static void configureJaggerAnnotations(JaggerContext ctx) {
        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                AnyConfig.USES,
                "org.tillerino.jagger.annotations.JsonConfig",
                ann -> ann.method("uses", true)
                        .map(AnnotationValueWrapper::asArray)
                        .map(classNames -> classNames.stream()
                                .map(className -> ctx.blueprint(ctx.elements.getTypeElement(
                                        className.asTypeMirror().toString())))
                                .flatMap(JaggerBlueprint::includeUses)
                                .collect(Collectors.toUnmodifiableSet())));

        ctx.configProperties.addRetriever(
                UnknownProperties.UNKNOWN_PROPERTIES,
                jsonConfigPropertyRetriever("unknownProperties", JsonConfig.UnknownPropertiesMode.class));

        ctx.configProperties.addRetriever(
                CodeGeneration.IMPLEMENT,
                jsonConfigPropertyRetriever("implement", JsonConfig.ImplementationMode.class));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                CodeGeneration.ON_GENERATED_CLASS,
                "org.tillerino.jagger.annotations.JsonConfig",
                ann -> ann.method("onGeneratedClass", false)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(classValue -> ctx.elements.getTypeElement(
                                        classValue.asTypeMirror().toString()))
                                .collect(ConfigProperty.toUnmodifiableSet())));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                CodeGeneration.ON_GENERATED_CONSTRUCTOR,
                "org.tillerino.jagger.annotations.JsonConfig",
                ann -> ann.method("onGeneratedConstructors", false)
                        .map(AnnotationValueWrapper::asArray)
                        .map(arr -> arr.stream()
                                .map(classValue -> ctx.elements.getTypeElement(
                                        classValue.asTypeMirror().toString()))
                                .collect(ConfigProperty.toUnmodifiableSet())));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                CodeGeneration.ADD_GENERATED_ANNOTATION_TO_CLASS,
                "org.tillerino.jagger.annotations.JsonConfig",
                ann -> ann.method("addGeneratedAnnotationToClass", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addAnnotationPropertyConfigRetriever(
                CodeGeneration.ADD_GENERATED_ANNOTATION_TO_METHODS,
                "org.tillerino.jagger.annotations.JsonConfig",
                ann -> ann.method("addGeneratedAnnotationToMethods", false).map(AnnotationValueWrapper::asBoolean));

        ctx.configProperties.addRetriever(
                Delegation.DELEGATE_TO, jsonConfigPropertyRetriever("delegateTo", JsonConfig.DelegateeMode.class));

        ctx.configProperties.addRetriever(
                Verification.VERIFY_SYMMETRY,
                jsonConfigPropertyRetriever("verifySymmetry", JsonConfig.VerificationMode.class));
    }

    public static <T extends Enum<T>> AnnotationConfigPropertyRetriever<T> jsonConfigPropertyRetriever(
            String method, Class<T> enumClass) {
        return new AnnotationConfigPropertyRetriever<>(
                "org.tillerino.jagger.annotations.JsonConfig",
                ann -> ann.method(method, false)
                        .map(annotationValueWrapper -> annotationValueWrapper.asEnum(enumClass)));
    }
}
