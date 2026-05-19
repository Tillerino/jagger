package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerContext;

public class Annotations {
    protected final JaggerContext ctx;

    public Annotations(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public Optional<AnnotationMirrorWrapper> findAnnotation(Element element, String annotationType) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationType)) {
                return Optional.of(new AnnotationMirrorWrapper(annotationMirror, ctx));
            }
        }
        return Optional.empty();
    }

    public record AnnotationMirrorWrapper(AnnotationMirror mirror, JaggerContext ctx) {
        public AnnotationValueWrapper requiredMethod(String name) {
            return method(name, false)
                    .orElseThrow(() -> new ContextedRuntimeException("Assumed method required, but was not present")
                            .addContextValue("Annotation", mirror.getAnnotationType())
                            .addContextValue("Method", name));
        }

        public AnnotationValueWrapper defaultMethod(String name) {
            return method(name, true)
                    .orElseThrow(
                            () -> new ContextedRuntimeException("Assumed method has default value, but was not present")
                                    .addContextValue("Annotation", mirror.getAnnotationType())
                                    .addContextValue("Method", name));
        }

        public Optional<AnnotationValueWrapper> method(String name, boolean withDefaults) {
            return filterMethod(
                            name,
                            withDefaults
                                    ? ctx.elements.getElementValuesWithDefaults(mirror)
                                    : mirror.getElementValues())
                    .map(Map.Entry::getValue)
                    .map(v -> new AnnotationValueWrapper(v, ctx));
        }

        private static Optional<? extends Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>>
                filterMethod(String name, Map<? extends ExecutableElement, ? extends AnnotationValue> baseValues) {
            return baseValues.entrySet().stream()
                    .filter(entry -> entry.getKey().getSimpleName().toString().equals(name))
                    .findFirst();
        }
    }

    public record AnnotationValueWrapper(AnnotationValue value, JaggerContext ctx) {
        public List<AnnotationValueWrapper> asArray() {
            return value.accept(
                    new GetAnnotationValues<List<AnnotationValueWrapper>, Void>("array") {
                        @Override
                        public List<AnnotationValueWrapper> visitArray(List<? extends AnnotationValue> vals, Void o) {
                            return vals.stream()
                                    .map(v -> new AnnotationValueWrapper(v, ctx))
                                    .toList();
                        }
                    },
                    null);
        }

        public AnnotationMirrorWrapper asAnnotation() {
            return new AnnotationMirrorWrapper(
                    value.accept(
                            new GetAnnotationValues<AnnotationMirror, Void>("annotation") {
                                @Override
                                public AnnotationMirror visitAnnotation(AnnotationMirror a, Void o) {
                                    return a;
                                }
                            },
                            null),
                    ctx);
        }

        public String asString() {
            return value.accept(
                    new GetAnnotationValues<String, Void>("string") {
                        @Override
                        public String visitString(String s, Void o) {
                            return s;
                        }
                    },
                    null);
        }

        public TypeMirror asTypeMirror() {
            return value.accept(
                    new GetAnnotationValues<TypeMirror, Void>("type") {
                        @Override
                        public TypeMirror visitType(TypeMirror t, Void o) {
                            return t;
                        }
                    },
                    null);
        }

        public <T extends Enum<T>> T asEnum(Class<T> cls) {
            return value.accept(
                    new GetAnnotationValues<T, Void>("enum") {
                        @Override
                        public T visitEnumConstant(VariableElement c, Void o) {
                            try {
                                return Enum.valueOf(cls, c.getSimpleName().toString());
                            } catch (IllegalArgumentException e) {
                                throw new ContextedRuntimeException("Unknown value %s for type %s"
                                        .formatted(c.getSimpleName().toString(), cls.getSimpleName()));
                            }
                        }
                    },
                    null);
        }

        public boolean asBoolean() {
            return value.accept(
                    new GetAnnotationValues<Boolean, Void>("boolean") {
                        @Override
                        public Boolean visitBoolean(boolean b, Void o) {
                            return b;
                        }
                    },
                    null);
        }

        public int asInt() {
            return value.accept(
                    new GetAnnotationValues<Integer, Void>("int") {
                        @Override
                        public Integer visitInt(int i, Void o) {
                            return i;
                        }
                    },
                    null);
        }
    }

    @RequiredArgsConstructor
    static class GetAnnotationValues<R, P> extends SimpleAnnotationValueVisitor14<R, P> {
        private final String requestedType;

        @Override
        protected R defaultAction(Object o, P p) {
            throw new ContextedRuntimeException("Not a " + requestedType).addContextValue("Annotation value", o);
        }
    }
}
