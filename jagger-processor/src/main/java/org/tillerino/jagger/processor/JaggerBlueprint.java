package org.tillerino.jagger.processor;

import java.util.*;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.tillerino.jagger.processor.config.AnyConfig;
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
    public final AnyConfig config;
    public final Map<TypeVar, TypeMirror> typeBindings;
    public final JaggerContext ctx;

    private JaggerBlueprint(
            FullyQualifiedClassName className,
            TypeElement typeElement,
            AnyConfig config,
            Map<TypeVar, TypeMirror> typeBindings,
            JaggerContext ctx) {
        this.className = className;
        this.typeElement = typeElement;
        this.config = config;
        this.typeBindings = typeBindings;
        this.ctx = ctx;
    }

    public static JaggerBlueprint of(TypeMirror type, JaggerContext ctx) {
        TypeElement element = (TypeElement) ctx.types.asElement(type);

        Map<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
        Polymorphism.typeHierarchyBfs(type, ctx, t -> {
            typeBindings.putAll(ctx.generics.recordTypeBindings(t));
            return Optional.empty();
        });

        JaggerBlueprint blueprint = new JaggerBlueprint(
                FullyQualifiedClassName.of(element),
                element,
                AnyConfig.create(element, LocationKind.BLUEPRINT, ctx),
                typeBindings,
                ctx);

        for (InstantiatedMethod method :
                ctx.generics.instantiateAllMethodsFromTypeHierarchy(type, LocationKind.PROTOTYPE)) {
            ctx.detectPrototype(method)
                    .ifPresent(kind -> blueprint.prototypes.add(JaggerPrototype.of(
                            blueprint, method, kind, ctx, true, new JaggerProcessor.Trigger(method.element()))));
        }

        return blueprint;
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
