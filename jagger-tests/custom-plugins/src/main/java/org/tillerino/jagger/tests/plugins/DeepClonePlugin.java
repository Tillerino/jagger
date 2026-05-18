package org.tillerino.jagger.tests.plugins;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.*;
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
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PlainTypeName;

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

        ctx.register(new PrototypeDetector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m, AnnotationMirrorWrapper annotation) {
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
            public Collection<String> supportedAnnotationTypes() {
                return List.of(Clone.class.getCanonicalName());
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Clone.class.getCanonicalName());
    }

    @Target(ElementType.METHOD)
    public @interface Clone {}

    record CloneKind(TypeMirror type) implements TemplatablePrototypeKind {

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return new DeepCloneGenerator(context).build();
        }

        @Override
        public List<TypeMirror> types() {
            return List.of(type);
        }

        @Override
        public String defaultMethodName() {
            return "clone" + PlainTypeName.of(type);
        }

        @Override
        public TemplatablePrototypeKind withTypes(List<TypeMirror> newTypes) {
            return new CloneKind(newTypes.get(0));
        }
    }

    private static class DeepCloneGenerator extends AbstractCodeGenerator<DeepCloneGenerator> {

        public DeepCloneGenerator(CodeGeneratorContext context) {
            super(context);
        }

        public CodeBlock.Builder build() {
            CloneKind kind = (CloneKind) prototype.kind();
            TypeMirror type = kind.types().get(0);

            TypedVariable result = createVariable(type, "result");

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

        private void cloneProperty(WriteAccessor writeAccessor, ReadAccessor readAccessor, TypedVariable result) {
            TypeMirror fieldType = writeAccessor.type();
            Expr valueToWrite =
                    readAccessor.read(prototype.method().parameters().get(0));

            if (!isDirectlyAssignable(readAccessor.type(), writeAccessor.type())) {
                Delegation.Delegatee delegatee = ctx.delegation
                        .findDelegatee(
                                ((TemplatablePrototypeKind) prototype.kind()).withTypes(List.of(fieldType)),
                                prototype,
                                false,
                                true,
                                prototype.config(),
                                generatedClass)
                        .orElseThrow(() -> new ContextedRuntimeException("Cannot clone property")
                                .addContextValue("property", writeAccessor.name())
                                .addContextValue("type", fieldType));
                valueToWrite = delegatee.call(prototype, List.of(valueToWrite), generatedClass);
            }

            addStatement(writeAccessor.write(result, valueToWrite));
        }

        private boolean isDirectlyAssignable(TypeMirror source, TypeMirror target) {
            if (!ctx.types.isSameType(source, target)) {
                return false;
            }
            return source.getKind().isPrimitive()
                    || ctx.commonTypes.isBoxed(source)
                    || ctx.commonTypes.isEnum(source)
                    || ctx.commonTypes.isString(source);
        }
    }
}
