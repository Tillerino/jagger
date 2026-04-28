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
import org.tillerino.jagger.processor.features.Delegation;
import org.tillerino.jagger.processor.util.Accessor.ReadAccessor;
import org.tillerino.jagger.processor.util.Accessor.WriteAccessor;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PlainTypeName;
import org.tillerino.jagger.processor.util.Snippet;

/**
 * Example plugin: the simplest POJO cloner. It does support delegation and templating by using
 * {@link TemplatablePrototypeKind}.
 *
 * <p>Support for arrays containers, and nested types without delegation is left as an exercise to the reader.
 *
 * <p><a href="https://mapstruct.org/">MapStruct</a> can also clone objects, so please use that instead :)
 */
@AutoService(JaggerPlugin.class)
public class DeepClonePlugin implements JaggerPlugin {

    @Override
    public void configure(JaggerContext ctx) {
        TypeElement type = ctx.elements.getTypeElement(Clone.class.getCanonicalName());

        ctx.detectors.add(new PrototypeDetector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m) {
                if (m.parameters().size() != 1) {
                    return Optional.empty();
                }
                TypeMirror paramType = m.parameters().get(0).type();
                TypeMirror returnType = m.returnType();
                if (!ctx.types.isSameType(paramType, returnType)) {
                    return Optional.empty();
                }
                return Optional.of(new CloneKind(paramType));
            }

            @Override
            public List<TypeElement> supportedAnnotationTypes() {
                return List.of(type);
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Clone.class.getCanonicalName());
    }

    @Target(ElementType.METHOD)
    public @interface Clone {}

    enum Direction {
        CLONE
    }

    record CloneKind(TypeMirror type) implements TemplatablePrototypeKind {

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return new DeepCloneGenerator(context).build();
        }

        @Override
        public Direction direction() {
            return Direction.CLONE;
        }

        @Override
        public TypeMirror externalType() {
            return type;
        }

        @Override
        public TypeMirror internalType() {
            return type;
        }

        @Override
        public String defaultMethodName() {
            return "clone" + PlainTypeName.of(type);
        }

        @Override
        public TemplatablePrototypeKind withInternalType(TypeMirror newType) {
            return new CloneKind(newType);
        }
    }

    private static class DeepCloneGenerator extends AbstractCodeGenerator<DeepCloneGenerator> {

        public DeepCloneGenerator(CodeGeneratorContext context) {
            super(context);
        }

        public CodeBlock.Builder build() {
            CloneKind kind = (CloneKind) prototype.kind();
            TypeMirror type = kind.externalType();

            ScopedVar result = createVariable("result");

            addStatement("$T $C = new $T()", type, result, type);

            Map<String, ReadAccessor> readAccessors = ctx.properties.listReadAccessors(type);
            Map<String, WriteAccessor> writeAccessors = ctx.properties.listWriteAccessors(type);

            writeAccessors.forEach((fieldName, writeAccessor) -> {
                ReadAccessor readAccessor = readAccessors.get(fieldName);
                if (readAccessor == null) {
                    throw new ContextedRuntimeException("No read accessor for property")
                            .addContextValue("property", fieldName);
                }
                cloneProperty(writeAccessor, readAccessor, result);
            });

            addStatement("return $C", result);

            return code;
        }

        private void cloneProperty(WriteAccessor writeAccessor, ReadAccessor readAccessor, ScopedVar result) {
            TypeMirror fieldType = writeAccessor.type();
            Snippet sourceValue =
                    readAccessor.readSnippet(prototype.method().parameters().get(0));

            Delegation.Delegatee delegatee = ctx.delegation
                    .findDelegatee(
                            ((TemplatablePrototypeKind) prototype.kind()).withInternalType(fieldType),
                            prototype,
                            false,
                            true,
                            prototype.config(),
                            generatedClass)
                    .orElse(null);

            Snippet valueToWrite;
            if (delegatee != null) {
                valueToWrite = Snippet.of("this.$L($C)", delegatee.method().name(), sourceValue);
            } else if (isDirectlyAssignable(readAccessor.type(), writeAccessor.type())) {
                valueToWrite = sourceValue;
            } else {
                throw new ContextedRuntimeException("Cannot clone property")
                        .addContextValue("property", writeAccessor.name())
                        .addContextValue("type", fieldType);
            }

            addStatement(writeAccessor.writeSnippet(result, valueToWrite));
        }

        private boolean isDirectlyAssignable(TypeMirror source, TypeMirror target) {
            if (!ctx.types.isSameType(source, target)) {
                return false;
            }
            return source.getKind().isPrimitive()
                    || ctx.isBoxed(source)
                    || ctx.commonTypes.isEnum(source)
                    || ctx.commonTypes.isString(source);
        }
    }
}
