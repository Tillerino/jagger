package org.tillerino.jagger.processor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.lang.model.util.Types;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.util.Annotations;
import org.tillerino.jagger.processor.util.Exceptions;

public class AnnotationProcessorUtils {
    public final Elements elements;
    public final Types types;
    public final CommonTypes commonTypes;
    public final Delegation delegation;
    public final Generics generics;
    public final Converters converters;
    public final DefaultValues defaultValues;
    public final Templates templates;
    public final Map<String, JaggerBlueprint> blueprints = new LinkedHashMap<>();
    public final Annotations annotations;
    public final Verification verification;
    public final Creators creators;
    public final References references;
    public final Properties properties;

    public final Messager messager;

    public AnnotationProcessorUtils(ProcessingEnvironment processingEnv) {
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        commonTypes = new CommonTypes();
        delegation = new Delegation(this);
        generics = new Generics(this);
        annotations = new Annotations(this);
        converters = new Converters(this);
        defaultValues = new DefaultValues(this);
        templates = new Templates(this);
        verification = new Verification(this);
        creators = new Creators(this);
        references = new References(this);
        properties = new Properties(this);
        messager = processingEnv.getMessager();
    }

    public static class GetAnnotationValues<R, P> extends SimpleAnnotationValueVisitor14<R, P> {
        @Override
        public R visitArray(List<? extends AnnotationValue> vals, P o) {
            vals.forEach(val -> val.accept(this, o));
            return null;
        }
    }

    public boolean isBoxed(TypeMirror type) {
        return commonTypes.boxedTypes.contains(type.toString());
    }

    public JaggerBlueprint blueprint(TypeElement element) {
        // cannot use computeIfAbsent because this can recurse
        JaggerBlueprint blueprint = blueprints.get(element.getQualifiedName().toString());
        if (blueprint == null) {
            blueprints.put(element.getQualifiedName().toString(), blueprint = JaggerBlueprint.of(element, this));
        }
        return blueprint;
    }

    public class CommonTypes {
        public final TypeMirror string =
                elements.getTypeElement(String.class.getName()).asType();

        public final TypeMirror boxedBoolean =
                elements.getTypeElement(Boolean.class.getName()).asType();
        public final TypeMirror boxedByte =
                elements.getTypeElement(Byte.class.getName()).asType();
        public final TypeMirror boxedShort =
                elements.getTypeElement(Short.class.getName()).asType();
        public final TypeMirror boxedInt =
                elements.getTypeElement(Integer.class.getName()).asType();
        public final TypeMirror boxedLong =
                elements.getTypeElement(Long.class.getName()).asType();
        public final TypeMirror boxedFloat =
                elements.getTypeElement(Float.class.getName()).asType();
        public final TypeMirror boxedDouble =
                elements.getTypeElement(Double.class.getName()).asType();
        public final TypeMirror boxedChar =
                elements.getTypeElement(Character.class.getName()).asType();
        public final TypeMirror object =
                elements.getTypeElement(Object.class.getName()).asType();

        public final TypeElement classElement = elements.getTypeElement(Class.class.getName());

        public final TypeMirror serializationContext =
                elements.getTypeElement(SerializationContext.class.getName()).asType();
        public final TypeMirror deserializationContext =
                elements.getTypeElement(DeserializationContext.class.getName()).asType();

        public final Set<String> boxedTypes = Set.of(
                boxedBoolean.toString(),
                boxedByte.toString(),
                boxedShort.toString(),
                boxedInt.toString(),
                boxedLong.toString(),
                boxedFloat.toString(),
                boxedDouble.toString(),
                boxedChar.toString());

        public boolean isString(TypeMirror type) {
            return types.isSameType(type, string);
        }

        public boolean isArrayOf(TypeMirror type, TypeKind componentKind) {
            return type.getKind() == TypeKind.ARRAY
                    && ((ArrayType) type).getComponentType().getKind() == componentKind;
        }

        public boolean isEnum(TypeMirror type) {
            Element element = types.asElement(type);
            return element != null && element.getKind() == ElementKind.ENUM;
        }

        public boolean isIterable(TypeMirror type) {
            return isAssignableTo(types.erasure(type), Iterable.class) || type.getKind() == TypeKind.ARRAY;
        }

        public boolean isMap(TypeMirror type) {
            return isAssignableTo(types.erasure(type), Map.class);
        }

        public TypeMirror getArrayComponentType(TypeMirror type) {
            if (type.getKind() == TypeKind.ARRAY) {
                return ((ArrayType) type).getComponentType();
            }
            return null;
        }

        public TypeElement elem(Class<?> cls) {
            return elements.getTypeElement(cls.getName());
        }

        public TypeMirror type(Class<?> cls) {
            return elem(cls).asType();
        }

        public boolean isAssignableTo(TypeMirror type1, Class<?> cls) {
            return types.isAssignable(type1, type(cls));
        }

        public String getNullValueRaw(TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN -> "false";
                case BYTE, SHORT, INT, CHAR -> "0";
                case LONG -> "0L";
                case FLOAT -> "0.0f";
                case DOUBLE -> "0.0d";
                case ARRAY, DECLARED, TYPEVAR, WILDCARD, UNION, INTERSECTION -> "null";
                default -> throw Exceptions.unexpected();
            };
        }
    }
}
