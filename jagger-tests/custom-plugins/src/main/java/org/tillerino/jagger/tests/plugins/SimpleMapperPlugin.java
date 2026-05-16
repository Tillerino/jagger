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
import org.tillerino.jagger.processor.features.Delegation.Delegatee;
import org.tillerino.jagger.processor.util.Accessor.ReadAccessor;
import org.tillerino.jagger.processor.util.Accessor.WriteAccessor;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.CollectionUtil;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.PlainTypeName;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;

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

        ctx.register(new PrototypeDetector() {
            @Override
            public Optional<PrototypeKind> detect(InstantiatedMethod m, AnnotationMirrorWrapper annotation) {
                if (m.parameters().size() != 1) {
                    return Optional.empty();
                }
                TypeMirror sourceType = m.parameters().get(0).type();
                TypeMirror targetType = m.returnType();
                return Optional.of(new MapperKind(List.of(targetType, sourceType)));
            }

            @Override
            public Collection<String> supportedAnnotationTypes() {
                return List.of(Mapper.class.getCanonicalName());
            }
        });
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Mapper.class.getCanonicalName());
    }

    @Target(ElementType.METHOD)
    public @interface Mapper {}

    record MapperKind(List<TypeMirror> types) implements TemplatablePrototypeKind {
        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return new SimpleMapperGenerator(context).build();
        }

        @Override
        public String defaultMethodName() {
            return "map" + PlainTypeName.of(types().get(1)) + "To" + PlainTypeName.of(types().get(0));
        }

        @Override
        public TemplatablePrototypeKind withTypes(List<TypeMirror> newTypes) {
            return new MapperKind(newTypes);
        }
    }

    private static class SimpleMapperGenerator extends AbstractCodeGenerator<SimpleMapperGenerator> {

        public SimpleMapperGenerator(CodeGeneratorContext context) {
            super(context);
        }

        public CodeBlock.Builder build() {
            MapperKind kind = (MapperKind) prototype.kind();
            TypeMirror targetType = kind.types().get(0);
            TypeMirror sourceType = kind.types().get(1);

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

            PerfectSnippet value =
                    readAccessor.readSnippet(prototype.method().parameters().get(0));
            if (!ctx.types.isAssignable(readAccessor.type(), writeAccessor.type())) {
                TemplatablePrototypeKind types = ((TemplatablePrototypeKind) prototype.kind())
                        .withTypes(List.of(writeAccessor.type(), readAccessor.type()));
                Delegatee delegatee = ctx.delegation
                        .findDelegatee(types, prototype, false, true, prototype.config(), generatedClass)
                        .orElseThrow(() -> new ContextedRuntimeException("Types are incompatible")
                                .addContextValue("source", readAccessor.type())
                                .addContextValue("target", writeAccessor.type()));

                List<PerfectSnippet> arguments = CollectionUtil.append(
                        value, ctx.delegation.findArguments(prototype, delegatee.method(), 1, generatedClass));
                value = delegatee.method().invokeInstance(delegatee.fieldOrParameter(), arguments);
            }

            addStatement(writeAccessor.writeSnippet(result, value));
        }
    }
}
