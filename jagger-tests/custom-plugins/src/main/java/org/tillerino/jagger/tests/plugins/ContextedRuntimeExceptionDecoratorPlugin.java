package org.tillerino.jagger.tests.plugins;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.*;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeKind;
import org.tillerino.jagger.processor.util.PrototypeKind.CodeGeneratorContext;

@AutoService(JaggerPlugin.class)
public class ContextedRuntimeExceptionDecoratorPlugin implements JaggerPlugin {

    public @interface AddContext {}

    @Override
    public void configure(JaggerContext ctx) {
        TypeElement type = ctx.elements.getTypeElement(AddContext.class.getCanonicalName());

        ctx.detectors.add(new Detector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m) {
                return Optional.of(new DecorateKind(m.returnType(), m.parameters()));
            }

            @Override
            public List<TypeElement> supportedAnnotationTypes() {
                return List.of(type);
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(AddContext.class.getCanonicalName());
    }

    record DecorateKind(TypeMirror internalType, List<InstantiatedVariable> otherParameters) implements PrototypeKind {
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
            if (prototype.instantiatedReturnType().getKind() != TypeKind.VOID) {
                code.add("return ");
            }
            addStatement("super.$L($C)", this.prototype.name(), Snippet.join(prototype.instantiatedParameters(), ", "));
            nextControlFlow("catch ($T $C)", ContextedRuntimeException.class, e);
            for (InstantiatedVariable parameter : prototype.instantiatedParameters()) {
                addStatement("e.addContextValue($S, $C)", parameter.name(), parameter);
            }
            addStatement("e.addContextValue($S, $S)", "method", this.prototype.name());
            addStatement("throw $C", e);
            endControlFlow();
            return code;
        }
    }
}
