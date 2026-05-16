package org.tillerino.jagger.tests.plugins;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AbstractCodeGenerator;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.Snippet;

/**
 * This is an example for a plugin that decorates methods filling the context of a {@link ContextedRuntimeException}
 * with method name and parameters. A child class is generated that calls the annotated method via {@code super} in a
 * try-catch block.
 */
@AutoService(JaggerPlugin.class)
public class ContextedRuntimeExceptionDecoratorPlugin implements JaggerPlugin {

    /** This annotation triggers the decoration. */
    @Target(ElementType.METHOD)
    public @interface AddContext {}

    @Override
    public void configure(JaggerContext ctx) {
        ctx.register(new PrototypeDetector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m, AnnotationMirrorWrapper annotation) {
                return Optional.of(new DecorateKind());
            }

            @Override
            public Collection<String> supportedAnnotationTypes() {
                return List.of(AddContext.class.getCanonicalName());
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(AddContext.class.getCanonicalName());
    }

    record DecorateKind() implements PrototypeKind {
        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return new ContextedRuntimeExceptionDecoratorGenerator(context).build();
        }

        @Override
        public boolean decorates(AnyConfig config) {
            return true;
        }
    }

    private static class ContextedRuntimeExceptionDecoratorGenerator
            extends AbstractCodeGenerator<ContextedRuntimeExceptionDecoratorGenerator> {

        public ContextedRuntimeExceptionDecoratorGenerator(CodeGeneratorContext context) {
            super(context);
        }

        public CodeBlock.Builder build() {
            ScopedVar e = createVariable("e");

            beginControlFlow("try");
            if (prototype.returnType().getKind() != TypeKind.VOID) {
                code.add("return ");
            }
            addStatement("super.$L($C)", this.prototype.name(), Snippet.join(prototype.parameters(), ", "));

            nextControlFlow("catch ($T $C)", ContextedRuntimeException.class, e);
            for (InstantiatedVariable parameter : prototype.parameters()) {
                addStatement("e.addContextValue($S, $C)", parameter.name(), parameter);
            }
            addStatement("e.addContextValue($S, $S)", "method", this.prototype.name());
            addStatement("throw $C", e);
            endControlFlow();

            return code;
        }
    }
}
