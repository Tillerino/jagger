package org.tillerino.jagger.processor;

import java.util.*;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
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
    public final List<InstantiatedMethod> allMethods;
    public final AnyConfig config;
    public final Map<TypeVar, TypeMirror> typeBindings;
    public final JaggerContext ctx;

    private JaggerBlueprint(
            FullyQualifiedClassName className,
            TypeElement typeElement,
            List<InstantiatedMethod> allMethods,
            AnyConfig config,
            Map<TypeVar, TypeMirror> typeBindings,
            JaggerContext ctx) {
        this.className = className;
        this.typeElement = typeElement;
        this.allMethods = allMethods;
        this.config = config;
        this.typeBindings = typeBindings;
        this.ctx = ctx;
    }

    static JaggerBlueprint of(TypeElement element, JaggerContext ctx) {
        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        Polymorphism.typeHierarchyBfs(element.asType(), ctx, type -> {
            typeBindings.putAll(ctx.generics.recordTypeBindings(type));
            return Optional.empty();
        });

        return new JaggerBlueprint(
                FullyQualifiedClassName.of(element),
                element,
                ctx.generics.instantiateAllMethodsFromTypeHierarchy(element.asType(), LocationKind.PROTOTYPE),
                AnyConfig.create(element, ConfigProperty.LocationKind.BLUEPRINT, ctx),
                typeBindings,
                ctx);
    }

    String generatedClassName() {
        return className.fileName().replace("/", ".") + "Impl";
    }

    public boolean hasNoArgSuperConstructor() {
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(typeElement.getEnclosedElements());
        return constructors.isEmpty()
                || constructors.stream().anyMatch(c -> c.getParameters().isEmpty());
    }

    @Override
    public String toString() {
        return className.className();
    }
}
