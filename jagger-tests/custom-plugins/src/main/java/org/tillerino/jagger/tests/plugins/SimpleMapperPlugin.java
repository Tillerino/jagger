package org.tillerino.jagger.tests.plugins;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AbstractCodeGenerator;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.util.Accessor.ReadAccessor;
import org.tillerino.jagger.processor.util.Accessor.WriteAccessor;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

/**
 * Example plugin: the simplest POJO-to-POJO mapper. It does not support delegation, containers, arrays, or anything
 * interesting.
 *
 * <p>You should obviously use <a href="https://mapstruct.org/">MapStruct</a> instead :)
 */
@AutoService(JaggerPlugin.class)
public class SimpleMapperPlugin implements JaggerPlugin {

    @Override
    public void configure(JaggerContext ctx) {
        TypeElement type = ctx.elements.getTypeElement(Mapper.class.getCanonicalName());

        ctx.detectors.add(new PrototypeDetector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m) {
                if (m.parameters().size() != 1) {
                    return Optional.empty();
                }
                TypeMirror externalType = m.parameters().get(0).type();
                TypeMirror internalType = m.returnType();
                return Optional.of(new MapperKind(externalType, internalType));
            }

            @Override
            public List<TypeElement> supportedAnnotationTypes() {
                return List.of(type);
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Mapper.class.getCanonicalName());
    }

    @Target(ElementType.METHOD)
    public @interface Mapper {}

    record MapperKind(TypeMirror externalType, TypeMirror internalType) implements TemplatablePrototypeKind {

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return new SimpleMapperGenerator(context).build();
        }

        @Override
        public boolean decorates(org.tillerino.jagger.processor.config.AnyConfig config) {
            return true;
        }

        @Override
        public Direction direction() {
            return Direction.IRRELEVANT;
        }

        @Override
        public String defaultMethodName() {
            return "map";
        }

        @Override
        public TemplatablePrototypeKind withInternalType(TypeMirror newType) {
            return new MapperKind(externalType, newType);
        }
    }

    private static class SimpleMapperGenerator extends AbstractCodeGenerator<SimpleMapperGenerator> {

        public SimpleMapperGenerator(CodeGeneratorContext context) {
            super(context);
        }

        public CodeBlock.Builder build() {
            MapperKind kind = (MapperKind) prototype.kind();
            TypeMirror sourceType = kind.externalType();
            TypeMirror targetType = kind.internalType();

            ScopedVar result = createVariable("result");

            addStatement("$T $C = new $T()", targetType, result, targetType);

            Map<String, ReadAccessor> sourceAccessors = ctx.properties.listReadAccessors(sourceType);
            Map<String, WriteAccessor> targetAccessors = ctx.properties.listWriteAccessors(targetType);

            targetAccessors.forEach((fieldName, writeAccessor) ->
                    assignProperty(writeAccessor, sourceAccessors.get(fieldName), result));

            addStatement("return $C", result);

            return code;
        }

        private void assignProperty(WriteAccessor writeAccessor, ReadAccessor readAccessor, ScopedVar result) {
            if (readAccessor == null) {
                throw new ContextedRuntimeException("Unmatched target property")
                        .addContextValue("target", writeAccessor.name());
            }

            if (!ctx.types.isAssignable(readAccessor.type(), writeAccessor.type())) {
                throw new ContextedRuntimeException("Types are incompatible")
                        .addContextValue("source", readAccessor.type())
                        .addContextValue("target", writeAccessor.type());
            }

            addStatement(writeAccessor.writeSnippet(
                    result,
                    readAccessor.readSnippet(prototype.method().parameters().get(0))));
        }
    }

    enum Direction {
        IRRELEVANT
    }
}
