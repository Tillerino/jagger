package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.AnnotationProcessorUtils.GetAnnotationValues;

public record Annotations(AnnotationProcessorUtils utils) {
    public Optional<AnnotationMirrorWrapper> findAnnotation(Element element, String annotationType) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationType)) {
                return Optional.of(new AnnotationMirrorWrapper(annotationMirror, utils));
            }
        }
        return Optional.empty();
    }

    public record AnnotationMirrorWrapper(AnnotationMirror mirror, AnnotationProcessorUtils utils) {
        public Optional<AnnotationValueWrapper> method(String name, boolean withDefaults) {
            return filterMethod(
                            name,
                            withDefaults
                                    ? utils.elements.getElementValuesWithDefaults(mirror)
                                    : mirror.getElementValues())
                    .map(Map.Entry::getValue)
                    .map(AnnotationValueWrapper::new);
        }

        private static Optional<? extends Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>>
                filterMethod(String name, Map<? extends ExecutableElement, ? extends AnnotationValue> baseValues) {
            return baseValues.entrySet().stream()
                    .filter(entry -> entry.getKey().getSimpleName().toString().equals(name))
                    .findFirst();
        }
    }

    public record AnnotationValueWrapper(AnnotationValue value) {
        public List<AnnotationValueWrapper> asArray() {
            return Exceptions.notNull(
                    value.accept(
                            new GetAnnotationValues<List<AnnotationValueWrapper>, Void>() {
                                @Override
                                public List<AnnotationValueWrapper> visitArray(
                                        List<? extends AnnotationValue> vals, Void o) {
                                    return vals.stream()
                                            .map(AnnotationValueWrapper::new)
                                            .toList();
                                }
                            },
                            null),
                    "not an array: %s",
                    value);
        }

        public AnnotationMirrorWrapper asAnnotation() {
            return new AnnotationMirrorWrapper(
                    Exceptions.notNull(
                            value.accept(
                                    new GetAnnotationValues<AnnotationMirror, Void>() {
                                        @Override
                                        public AnnotationMirror visitAnnotation(AnnotationMirror a, Void o) {
                                            return a;
                                        }
                                    },
                                    null),
                            "not an annotation: %s",
                            value),
                    null);
        }

        public String asString() {
            return Exceptions.notNull(
                    value.accept(
                            new GetAnnotationValues<String, Void>() {
                                @Override
                                public String visitString(String s, Void o) {
                                    return s;
                                }
                            },
                            null),
                    "not a string: %s",
                    value);
        }

        public TypeMirror asTypeMirror() {
            return Exceptions.notNull(
                    value.accept(
                            new GetAnnotationValues<TypeMirror, Void>() {
                                @Override
                                public TypeMirror visitType(TypeMirror t, Void o) {
                                    return t;
                                }
                            },
                            null),
                    "not a type: %s",
                    value);
        }

        public <T extends Enum<T>> T asEnum(Class<T> cls) {
            return Exceptions.notNull(
                    value.accept(
                            new GetAnnotationValues<T, Void>() {
                                @Override
                                public T visitEnumConstant(VariableElement c, Void o) {
                                    try {
                                        return Enum.valueOf(
                                                cls, c.getSimpleName().toString());
                                    } catch (IllegalArgumentException e) {
                                        throw new ContextedRuntimeException("Unknown value %s for type %s"
                                                .formatted(c.getSimpleName().toString(), cls.getSimpleName()));
                                    }
                                }
                            },
                            null),
                    "not an enum: %s",
                    value);
        }

        public boolean asBoolean() {
            return Exceptions.notNull(
                    value.accept(
                            new GetAnnotationValues<Boolean, Void>() {
                                @Override
                                public Boolean visitBoolean(boolean b, Void o) {
                                    return b;
                                }
                            },
                            null),
                    "not a boolean: %s",
                    value);
        }

        public int asInt() {
            return Exceptions.notNull(
                    value.accept(
                            new GetAnnotationValues<Integer, Void>() {
                                @Override
                                public Integer visitInt(int i, Void o) {
                                    return i;
                                }
                            },
                            null),
                    "not an int: %s",
                    value);
        }
    }
}
