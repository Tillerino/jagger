package org.tillerino.jagger.processor.features;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.AnnotationProcessorUtils;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.AnyConfig.ResolvedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.ConfigPropertyRetriever;
import org.tillerino.jagger.processor.config.ConfigProperty.InstantiatedProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Accessor.ReadAccessor;
import org.tillerino.jagger.processor.util.Annotations.AnnotationValueWrapper;
import org.tillerino.jagger.processor.util.Exceptions;

public record References(AnnotationProcessorUtils utils) {
    public static ConfigProperty<Config> REFERENCES = ConfigProperty.createConfigProperty(
            List.of(LocationKind.DTO),
            List.of(new ConfigPropertyRetriever<>(
                    "com.fasterxml.jackson.annotation.JsonIdentityInfo",
                    ((annotation, utils) -> Optional.of(new Config(
                            annotation.method("property", false).map(AnnotationValueWrapper::asString),
                            annotation
                                    .method("generator", false)
                                    .map(AnnotationValueWrapper::asTypeMirror)
                                    .orElseThrow(Exceptions::unexpected),
                            annotation.method("resolver", false).map(AnnotationValueWrapper::asTypeMirror),
                            annotation.method("scope", false).map(AnnotationValueWrapper::asTypeMirror)))))),
            null,
            (strong, weak) -> new InstantiatedProperty<>(
                    strong.property(),
                    strong.locationKind(),
                    new Config(
                            strong.value().property().or(weak.value()::property),
                            strong.value().generator,
                            strong.value().resolver.or(weak.value()::resolver),
                            strong.value().scope.or(weak.value()::scope)),
                    "Merged " + strong.sourceLocation() + " and " + weak.sourceLocation()),
            PropagationKind.none());

    public Optional<Setup> resolveSetup(AnyConfig anyConfig, JaggerPrototype prototype, TypeMirror dto) {
        ResolvedProperty<Config> configResolvedProperty = anyConfig.resolveProperty(REFERENCES);
        Config config = configResolvedProperty.value();
        if (config == null) {
            return Optional.empty();
        }
        if (!(config.generator instanceof DeclaredType dt)) {
            throw Exceptions.unexpected();
        }
        Map<TypeVar, TypeMirror> generatorTypeBindings = utils.generics.recordTypeBindingsFor(
                dt, utils.elements.getTypeElement("com.fasterxml.jackson.annotation.ObjectIdGenerator"));
        if (generatorTypeBindings == null || generatorTypeBindings.isEmpty()) {
            throw Exceptions.unexpected();
        }
        TypeMirror idType = generatorTypeBindings.values().iterator().next();
        VariableElement context = prototype
                .contextParameter()
                .orElseThrow(
                        () -> new ContextedRuntimeException(
                                "Need context to use references. Declare a SerializerContext or DeserializerContext parameter."));

        return Optional.of(new Setup(
                config.property.orElse("@id"),
                config.generator,
                idType,
                config.resolver.orElseGet(() -> utils.elements
                        .getTypeElement("com.fasterxml.jackson.annotation.SimpleObjectIdResolver")
                        .asType()),
                config.resolver.orElseGet(
                        () -> utils.elements.getTypeElement("java.lang.Object").asType()),
                Snippet.of("$L", context)));
    }

    record Config(
            Optional<String> property,
            TypeMirror generator,
            Optional<TypeMirror> resolver,
            Optional<TypeMirror> scope) {}

    public record Setup(
            String property,
            TypeMirror generator,
            TypeMirror idType,
            TypeMirror resolver,
            TypeMirror scope,
            Snippet context) {
        public Snippet bindItem(Snippet idVar, Snippet objectVar) {
            return Snippet.of(
                    "$C.bindItem($T.class, $T.class, $T::new, $C, $C)",
                    context(),
                    resolver(),
                    scope(),
                    resolver(),
                    idVar,
                    objectVar);
        }

        public Snippet resolveId(Snippet idVar) {
            return Snippet.of("$C.resolveId($T.class, $T.class, $C)", context(), resolver(), scope(), idVar);
        }

        public Snippet previouslyWritten(Snippet rhs) {
            return Snippet.of("$C.previouslyWrittenId($C)", context(), rhs);
        }

        public Optional<Snippet> generateId(Snippet rhs) {
            if (isPropertyBased()) {
                return Optional.empty();
            }
            return Optional.of(Snippet.of(
                    "$C.generateId($T.class, $T.class, $T::new, $C)",
                    context(),
                    generator(),
                    scope(),
                    generator(),
                    rhs));
        }

        public Optional<Snippet> rememberId(Snippet rhs, Snippet property) {
            if (!isPropertyBased()) {
                return Optional.empty();
            }
            return Optional.of(Snippet.of("$C.rememberId($C, $C)", context(), rhs, property));
        }

        public boolean isPropertyBased() {
            return generator.toString().equals("com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator");
        }

        public TypeMirror finalIdType(TypeMirror type, AnnotationProcessorUtils utils) {
            if (!isPropertyBased()) {
                return idType;
            }
            return utils.properties.listReadAccessors(type).entrySet().stream()
                    .flatMap(entry -> {
                        String canonicalName = entry.getKey();
                        ReadAccessor accessor = entry.getValue();
                        AnyConfig propertyConfig = AnyConfig.fromAccessorConsideringField(
                                accessor, accessor.name(), type, canonicalName, utils);
                        String externalName = PropertyName.resolvePropertyName(propertyConfig, canonicalName);
                        if (!externalName.equals(property())) {
                            return Stream.empty();
                        }
                        return Stream.of(accessor.type());
                    })
                    .findFirst()
                    .orElseThrow(() -> new ContextedRuntimeException(
                            "Configured property-based ID but property not found: " + property()));
        }
    }
}
