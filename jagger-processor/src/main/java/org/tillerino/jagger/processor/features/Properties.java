package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Accessor;
import org.tillerino.jagger.processor.util.Accessor.AccessorKind;
import org.tillerino.jagger.processor.util.Accessor.ElementAccessor;
import org.tillerino.jagger.processor.util.Exceptions;

public record Properties(AnnotationProcessorUtils utils) {
    public Map<String, Accessor.ReadAccessor> listReadAccessors(TypeMirror type) {
        Map<String, Accessor.ReadAccessor> accessors = new LinkedHashMap<>();

        if (!(type instanceof DeclaredType declaredType)) {
            throw Exceptions.unexpected();
        }
        Map<TypeVar, TypeMirror> typeBindings = utils.generics.recordTypeBindings(declaredType);

        TypeElement typeElement = (TypeElement) declaredType.asElement();
        if (typeElement.getKind() == ElementKind.RECORD) {
            for (RecordComponentElement recordComponent : typeElement.getRecordComponents()) {
                accessors.put(
                        recordComponent.getSimpleName().toString(),
                        new ElementAccessor(
                                utils.generics.applyTypeBindings(recordComponent.asType(), typeBindings),
                                recordComponent.getAccessor(),
                                AccessorKind.GETTER));
            }
        }

        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) element;
                if (field.getModifiers().contains(Modifier.STATIC)
                        || field.getModifiers().contains(Modifier.TRANSIENT)
                        || !field.getModifiers().contains(Modifier.PUBLIC)) {
                    continue;
                }
                TypeMirror fieldType = utils.generics.applyTypeBindings(field.asType(), typeBindings);
                accessors.put(
                        element.getSimpleName().toString(), new ElementAccessor(fieldType, field, AccessorKind.FIELD));
            } else if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                if (isGetter(method)) {
                    String propertyName =
                            getPropertyNameFromGetter(method.getSimpleName().toString());
                    accessors.put(
                            propertyName,
                            new Accessor.ElementAccessor(
                                    utils.generics.applyTypeBindings(method.getReturnType(), typeBindings),
                                    method,
                                    Accessor.AccessorKind.GETTER));
                }
            }
        }

        return accessors;
    }

    public Map<String, Accessor.WriteAccessor> listWriteAccessors(TypeMirror type) {
        Map<String, Accessor.WriteAccessor> accessors = new LinkedHashMap<>();

        if (!(type instanceof DeclaredType declaredType)) {
            return accessors;
        }
        Map<TypeVar, TypeMirror> typeBindings = utils.generics.recordTypeBindings(declaredType);

        for (Element element : declaredType.asElement().getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) element;
                if (field.getModifiers().contains(Modifier.STATIC)
                        || field.getModifiers().contains(Modifier.TRANSIENT)
                        || !field.getModifiers().contains(Modifier.PUBLIC)) {
                    continue;
                }
                accessors.put(
                        element.getSimpleName().toString(),
                        new Accessor.ElementAccessor(
                                utils.generics.applyTypeBindings(field.asType(), typeBindings),
                                field,
                                Accessor.AccessorKind.FIELD));
            } else if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                if (isSetter(method)) {
                    String propertyName =
                            getPropertyNameFromSetter(method.getSimpleName().toString());
                    accessors.put(
                            propertyName,
                            new Accessor.ElementAccessor(
                                    utils.generics.applyTypeBindings(
                                            method.getParameters().get(0).asType(), typeBindings),
                                    method,
                                    Accessor.AccessorKind.SETTER));
                }
            }
        }

        return accessors;
    }

    private boolean isGetter(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        return method.getParameters().isEmpty()
                && method.getReturnType().getKind() != javax.lang.model.type.TypeKind.VOID
                && (name.startsWith("get") && name.length() > 3 || name.startsWith("is") && name.length() > 2);
    }

    private boolean isSetter(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        return name.startsWith("set")
                && name.length() > 3
                && method.getParameters().size() == 1
                && method.getReturnType().getKind() == javax.lang.model.type.TypeKind.VOID;
    }

    private String getPropertyNameFromGetter(String getterName) {
        if (getterName.startsWith("get")) {
            return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
        } else if (getterName.startsWith("is")) {
            return Character.toLowerCase(getterName.charAt(2)) + getterName.substring(3);
        }
        return getterName;
    }

    private String getPropertyNameFromSetter(String setterName) {
        return Character.toLowerCase(setterName.charAt(3)) + setterName.substring(4);
    }
}
