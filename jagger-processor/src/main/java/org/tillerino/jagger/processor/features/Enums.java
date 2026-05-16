package org.tillerino.jagger.processor.features;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.FieldSpec.Builder;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.*;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.helpers.EnumHelper;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.JacksonAnnotationsPlugin;
import org.tillerino.jagger.processor.util.Annotations;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.Snippet;
import org.tillerino.jagger.processor.util.Snippet.Flattened;

public class Enums {
    protected JaggerContext ctx;

    public Enums(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public static Snippet serializationSnippet(
            JaggerContext ctx, GeneratedClass generatedClass, TypeMirror enumType, Snippet enumValue) {
        Optional<InstantiatedMethod> jsonValueMethod =
                ctx.converters.findJsonValueMethod(enumType, ctx.commonTypes::isString);
        if (jsonValueMethod.isPresent()) {
            return Snippet.of("$C.$L()", enumValue, jsonValueMethod.get().name());
        }

        EnumValuesField enumField = generatedClass.getOrCreateEnumField(enumType);
        if (!enumField.explicitMappings().isEmpty()) {
            return Snippet.of("$L.get($C)", enumField.serializedFormField(), enumValue);
        }

        return Snippet.of("$C.name()", enumValue);
    }

    public EnumValuesField createEnumField(TypeMirror enumType, int nextIndex) {
        String valueFunction = ctx.converters
                .findJsonValueMethod(enumType, ctx.commonTypes::isString)
                .map(InstantiatedMethod::name)
                .orElse("name");

        Map<String, String> explicitMappings = getEnumConstantJsonPropertyNames(enumType);
        Map<String, List<String>> aliases = ctx.alias.getEnumConstantJsonAliases(enumType);
        String enumName = StringUtils.uncapitalize(
                ((DeclaredType) enumType).asElement().getSimpleName().toString());

        return new EnumValuesField(
                enumName + "$" + nextIndex + "$lookup",
                enumType,
                valueFunction,
                enumName + "$" + nextIndex + "$values",
                explicitMappings,
                aliases);
    }

    Map<String, String> getEnumConstantJsonPropertyNames(TypeMirror enumType) {
        Map<String, String> mappings = new LinkedHashMap<>();
        TypeElement typeElement = (TypeElement) ctx.types.asElement(enumType);
        for (VariableElement constant : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
            if (constant.getKind() == ElementKind.ENUM_CONSTANT) {
                ctx.annotations
                        .findAnnotation(constant, JacksonAnnotationsPlugin.CFJA + ".JsonProperty")
                        .flatMap(ann -> ann.method("value", true))
                        .map(Annotations.AnnotationValueWrapper::asString)
                        .filter(s -> !s.isEmpty())
                        .ifPresent(customName ->
                                mappings.put(constant.getSimpleName().toString(), customName));
            }
        }
        return mappings;
    }

    public record EnumValuesField(
            String name,
            TypeMirror type,
            String valueMethod,
            String serializedFormField,
            Map<String, String> explicitMappings,
            Map<String, List<String>> aliases) {
        public List<Builder> createFields() {
            // We only need the explicit mappings for serialization if we are using the default "name" method.
            // A custom value method will always override explicit mappings and will be called by the converter
            // mechanism.
            Map<String, String> mappings = valueMethod.equals("name") ? explicitMappings : Map.of();

            Snippet values = valuePairs(mappings, type);

            Snippet aliases = aliasPairs(aliases(), type);

            Flattened deserInitializer = Snippet.of(
                            "$T.deserializationMap($T.class, $T::$L, $C, $C)",
                            EnumHelper.class,
                            type,
                            type,
                            valueMethod,
                            values,
                            aliases)
                    .flatten();

            ParameterizedTypeName stringMapType =
                    ParameterizedTypeName.get(ClassName.get(Map.class), TypeName.get(String.class), TypeName.get(type));
            Builder deserField = FieldSpec.builder(stringMapType, name)
                    .initializer(deserInitializer.format(), deserInitializer.args());

            if (mappings.isEmpty()) {
                return List.of(deserField);
            }

            Flattened serInit = Snippet.of(
                            "$T.serializationMap($T.class, $T::$L, $C)",
                            EnumHelper.class,
                            type,
                            type,
                            valueMethod,
                            values)
                    .flatten();

            ParameterizedTypeName enumMapType = ParameterizedTypeName.get(
                    ClassName.get(EnumMap.class), TypeName.get(type), TypeName.get(String.class));
            Builder serField = FieldSpec.builder(enumMapType, serializedFormField);
            serField.initializer(serInit.format(), serInit.args());

            return List.of(deserField, serField);
        }

        private static Snippet valuePairs(Map<String, String> explicitMappings, TypeMirror type) {
            List<Snippet> valuePairs = explicitMappings.entrySet().stream()
                    .map(e -> Snippet.of("\t$T.$L, $S", type, e.getKey(), e.getValue()))
                    .toList();

            return Snippet.join(valuePairs, ",\n", "new Object[] {\n", "}");
        }

        private static Snippet aliasPairs(Map<String, List<String>> aliasesByConstant, TypeMirror type) {
            List<Snippet> valuePairs = aliasesByConstant.entrySet().stream()
                    .map(e -> {
                        Snippet aliases = Snippet.join(
                                e.getValue().stream()
                                        .map(a -> Snippet.of("$S", a))
                                        .toList(),
                                ",");
                        return Snippet.of("\t$T.$L, $C", type, e.getKey(), aliases);
                    })
                    .toList();

            return Snippet.join(valuePairs, ",\n", "new Object[] {\n", "}");
        }
    }
}
