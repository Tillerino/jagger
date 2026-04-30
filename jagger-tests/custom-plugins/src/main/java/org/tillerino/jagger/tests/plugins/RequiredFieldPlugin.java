package org.tillerino.jagger.tests.plugins;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass.RequiredField;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.AnnotationConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

/** This is a test plugin to check if requiring fields works. */
@AutoService(JaggerPlugin.class)
public class RequiredFieldPlugin implements JaggerPlugin {
    /** This annotation triggers the decoration. */
    public @interface RequireField {
        Class<?> value() default Object.class;
    }

    @Override
    public void configure(JaggerContext ctx) {
        TypeElement type = ctx.elements.getTypeElement(RequireField.class.getCanonicalName());

        ConfigProperty<TypeMirror> property = new ConfigProperty<>(
                "property",
                List.of(LocationKind.BLUEPRINT),
                ctx.commonTypes.object,
                MergeFunction.notDefault(),
                List.of());
        ctx.configProperties.addRetriever(
                property,
                new AnnotationConfigPropertyRetriever<>(
                        RequireField.class.getCanonicalName(),
                        ann -> ann.method("value", false).map(AnnotationValueWrapper::asTypeMirror)));

        ctx.detectors.add(new PrototypeDetector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m) {
                return Optional.of(new PrototypeKind() {
                    @Override
                    public Builder generateCode(CodeGeneratorContext context) {
                        context.generatedClass()
                                .requiredField(context.prototype()
                                        .config()
                                        .resolveProperty(property)
                                        .value());
                        return CodeBlock.builder();
                    }
                });
            }

            @Override
            public List<TypeElement> supportedAnnotationTypes() {
                return List.of(type);
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(RequiredField.class.getCanonicalName());
    }
}
