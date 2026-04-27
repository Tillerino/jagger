package org.tillerino.jagger.processor;

import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.features.Polymorphism;
import org.tillerino.jagger.processor.util.FullyQualifiedName.FullyQualifiedClassName;
import org.tillerino.jagger.processor.util.InstantiatedMethod;

/** Accessor object for the interface which contains Jagger methods. */
public final class JaggerBlueprint {
    public final FullyQualifiedClassName className;
    public final TypeElement typeElement;
    public final List<JaggerPrototype> prototypes = new ArrayList<>();
    public final List<InstantiatedMethod> declaredMethods;
    public final AnyConfig config;
    public final Map<TypeVar, TypeMirror> typeBindings;

    private JaggerBlueprint(
            FullyQualifiedClassName className,
            TypeElement typeElement,
            List<InstantiatedMethod> declaredMethods,
            AnyConfig config,
            Map<TypeVar, TypeMirror> typeBindings) {
        this.className = className;
        this.typeElement = typeElement;
        this.declaredMethods = declaredMethods;
        this.config = config;
        this.typeBindings = typeBindings;
    }

    static JaggerBlueprint of(TypeElement element, JaggerContext ctx) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        for (DeclaredType dt : Polymorphism.directSupertypes(element.asType(), ctx)) {
            typeBindings.putAll(ctx.generics.recordTypeBindings(dt));
        }

        List<InstantiatedMethod> declaredMethods = ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                .map(method -> ctx.generics.instantiateMethod(method, typeBindings, LocationKind.PROTOTYPE))
                .toList();

        return new JaggerBlueprint(
                FullyQualifiedClassName.of(element),
                element,
                declaredMethods,
                AnyConfig.create(element, ConfigProperty.LocationKind.BLUEPRINT, ctx),
                typeBindings);
    }

    String generatedClassName() {
        return className.fileName().replace("/", ".") + "Impl";
    }

    public Stream<JaggerBlueprint> includeUses() {
        return Stream.concat(config.resolveProperty(AnyConfig.USES).value().stream(), Stream.of(this));
    }

    @Override
    public String toString() {
        return className.className();
    }
}
