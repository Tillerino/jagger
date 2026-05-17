package org.tillerino.jagger.processor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Supplier;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.lang.model.util.Types;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.config.ConfigProperties;
import org.tillerino.jagger.processor.config.JaggerAnnotations;
import org.tillerino.jagger.processor.ext.BlueprintConfigurator;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.features.*;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.features.Properties;
import org.tillerino.jagger.processor.util.Annotations;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.ShortName;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet.Literal;

public class JaggerContext {
    public final Elements elements;
    public final Types types;
    public final Messager messager;

    public CommonTypes commonTypes;
    public Delegation delegation;
    public Generics generics;
    public Converters converters;
    public DefaultValues defaultValues;
    public Annotations annotations;
    public Verification verification;
    public Creators creators;
    public References references;
    public Properties properties;
    public Alias alias;
    public Enums enums;
    public CodeGeneration codeGeneration;
    public ConfigProperties configProperties;

    public final Map<String, JaggerBlueprint> blueprints = new LinkedHashMap<>();
    public final Map<String, PrototypeDetector> prototypeDetectors = new LinkedHashMap<>();
    public final Map<String, BlueprintConfigurator> blueprintConfigurators = new LinkedHashMap<>();

    public JaggerContext(ProcessingEnvironment processingEnv) {
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();

        commonTypes = new CommonTypes();
        delegation = new Delegation(this);
        generics = new Generics(this);
        annotations = new Annotations(this);
        converters = new Converters(this);
        defaultValues = new DefaultValues(this);
        verification = new Verification(this);
        creators = new Creators(this);
        references = new References(this);
        properties = new Properties(this);
        alias = new Alias(this);
        enums = new Enums(this);
        codeGeneration = new CodeGeneration(this);
        configProperties = new ConfigProperties(this);

        JaggerAnnotations.configureJaggerAnnotations(this);
    }

    /**
     * Registers a prototype detector. For each supported annotation type, later registrations overwrite earlier ones.
     */
    public void register(PrototypeDetector prototypeDetector) {
        for (String supportedAnnotationType : prototypeDetector.supportedAnnotationTypes()) {
            prototypeDetectors.put(supportedAnnotationType, prototypeDetector);
        }
    }

    /**
     * Registers a blueprint configurator. For each supported annotation type, later registrations overwrite earlier
     * ones.
     */
    public void register(BlueprintConfigurator blueprintConfigurator) {
        for (String supportedAnnotationType : blueprintConfigurator.supportedAnnotationTypes()) {
            blueprintConfigurators.put(supportedAnnotationType, blueprintConfigurator);
        }
    }

    public static class GetAnnotationValues<R, P> extends SimpleAnnotationValueVisitor14<R, P> {
        @Override
        public R visitArray(List<? extends AnnotationValue> vals, P o) {
            vals.forEach(val -> val.accept(this, o));
            return null;
        }
    }

    public JaggerBlueprint blueprint(TypeElement element) {
        // cannot use computeIfAbsent because this can recurse
        JaggerBlueprint blueprint = blueprints.get(element.getQualifiedName().toString());
        if (blueprint == null) {
            blueprints.put(element.getQualifiedName().toString(), blueprint = JaggerBlueprint.of(element, this));
        }
        return blueprint;
    }

    public Optional<PrototypeKind> detectPrototype(InstantiatedMethod m) {
        for (AnnotationMirror annotationMirror : m.element().getAnnotationMirrors()) {
            PrototypeDetector prototypeDetector =
                    prototypeDetectors.get(annotationMirror.getAnnotationType().toString());
            if (prototypeDetector == null) {
                continue;
            }
            Optional<PrototypeKind> detected =
                    prototypeDetector.detect(m, new AnnotationMirrorWrapper(annotationMirror, this));
            if (detected.isEmpty()) {
                throw new ContextedRuntimeException("Signature unknown. Please see @"
                                + ShortName.of(annotationMirror.getAnnotationType()) + " for hints.")
                        .addContextValue("Method signature", m);
            }
            return detected;
        }
        return Optional.empty();
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

        public final Set<String> boxedTypes = Set.of(
                boxedBoolean.toString(),
                boxedByte.toString(),
                boxedShort.toString(),
                boxedInt.toString(),
                boxedLong.toString(),
                boxedFloat.toString(),
                boxedDouble.toString(),
                boxedChar.toString());

        public TypeMirror connection =
                elements.getTypeElement(Connection.class.getName()).asType();

        public TypeMirror preparedStatement =
                elements.getTypeElement(PreparedStatement.class.getName()).asType();

        public TypeMirror resultSet =
                elements.getTypeElement(ResultSet.class.getName()).asType();

        public TypeMirror supplier =
                elements.getTypeElement(Supplier.class.getName()).asType();

        public TypeMirror nullableTypeMirror(String name) {
            TypeElement typeElement = elements.getTypeElement(name);
            if (typeElement == null) {
                return null;
            }
            return typeElement.asType();
        }

        public boolean isBoxed(TypeMirror type) {
            return boxedTypes.contains(type.toString());
        }

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

        public boolean isIterableOrArray(TypeMirror type) {
            return isErasureAssignableTo(type, Iterable.class) || type.getKind() == TypeKind.ARRAY;
        }

        public boolean isErasureAssignableTo(TypeMirror type, Class<?> cls) {
            return isAssignable(types.erasure(type), cls);
        }

        public TypeMirror getComponentType(TypeMirror type, Class<?> parameterizedClass) {
            Map<TypeVar, TypeMirror> typeBindings = generics.recordTypeBindingsFor(
                    (DeclaredType) type, elements.getTypeElement(parameterizedClass.getName()));
            return typeBindings.values().iterator().next();
        }

        public TypeMirror getArrayComponentType(TypeMirror type) {
            if (type.getKind() == TypeKind.ARRAY) {
                return ((ArrayType) type).getComponentType();
            }
            return null;
        }

        public TypeMirror unwrapContainer(TypeMirror type) {
            if (type.getKind() == TypeKind.ARRAY) {
                return getArrayComponentType(type);
            } else if (isErasureAssignableTo(type, Iterable.class)) {
                return getComponentType(type, Iterable.class);
            } else if (isErasureAssignableTo(type, Optional.class)) {
                return getComponentType(type, Optional.class);
            }
            return type;
        }

        public TypeElement elem(Class<?> cls) {
            return elements.getTypeElement(cls.getName());
        }

        public TypeMirror type(Class<?> cls) {
            return elem(cls).asType();
        }

        /** Checks if the first type is assignable to the second. */
        public boolean isAssignable(TypeMirror type1, Class<?> cls) {
            return isAssignable(type1, type(cls));
        }

        /**
         * Checks if the first type is assignable to the second. There is some extra checking, so use this instead of
         * {@link Types#isAssignable(TypeMirror, TypeMirror)}!
         */
        public boolean isAssignable(TypeMirror type1, TypeMirror type2) {
            // If type1 is an error type, it becomes assignable for some reason.
            // Since that leads to all kinds of bonkers follow-up errors, we specifically filter it out here.
            return type1.getKind() != TypeKind.ERROR && types.isAssignable(type1, type2);
        }

        public PerfectSnippet getNullValueRaw(TypeMirror type) {
            return switch (type.getKind()) {
                case BOOLEAN -> new Literal(type, "false");
                case BYTE, SHORT, INT, CHAR -> new Literal(type, "0");
                case LONG -> new Literal(type, "0L");
                case FLOAT -> new Literal(type, "0.0f");
                case DOUBLE -> new Literal(type, "0.0d");
                case ARRAY, DECLARED, TYPEVAR, WILDCARD, UNION, INTERSECTION -> new Literal(type, "null");
                default -> throw Exceptions.unexpected();
            };
        }

        /** Use instead of {@link Types#asElement(TypeMirror)} - will throw instead of returning null. */
        public Element asElement(TypeMirror type) {
            Element element = types.asElement(type);
            if (element == null) {
                throw new ContextedRuntimeException("Not as much of a type as expected: " + type);
            }
            return element;
        }

        public PerfectSnippet stringLiteral(String s) {
            return new PerfectSnippet() {
                @Override
                public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                    return this;
                }

                @Override
                public TypeMirror type() {
                    return string;
                }

                @Override
                public Flattened flatten() {
                    return new Flattened("$S", new Object[] {s});
                }
            };
        }
    }
}
