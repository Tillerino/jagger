package org.tillerino.jagger.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.processor.JaggerProcessor.Trigger;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeInScope;
import org.tillerino.jagger.processor.util.PrototypeInScope.FromParameter;
import org.tillerino.jagger.processor.util.PrototypeInScope.FromPrototype;

/**
 * Accessor object for a method which is annotated with {@link org.tillerino.jagger.annotations.JsonInput} or
 * {@link org.tillerino.jagger.annotations.JsonOutput}.
 */
public record JaggerPrototype(
        JaggerBlueprint blueprint,
        InstantiatedMethod method,
        PrototypeKind kind,
        JaggerContext ctx,
        AnyConfig config,
        boolean overrides,
        Trigger trigger) {

    public static JaggerPrototype of(
            JaggerBlueprint blueprint,
            InstantiatedMethod instantiated,
            PrototypeKind kind,
            JaggerContext ctx,
            boolean overrides,
            Trigger trigger) {
        AnyConfig config = AnyConfig.create(instantiated.element(), ConfigProperty.LocationKind.PROTOTYPE, ctx)
                .merge(blueprint.config);

        return new JaggerPrototype(blueprint, instantiated, kind, ctx, config, overrides, trigger);
    }

    public List<PrototypeInScope> listPrototypesInScope(AnyConfig config) {
        List<PrototypeInScope> prototypes = new ArrayList<>();
        for (InstantiatedVariable parameter : method.parameters()) {
            if (!(parameter.type() instanceof DeclaredType dt)) {
                continue;
            }
            JaggerBlueprint.of(dt, ctx).prototypes.forEach(pt -> prototypes.add(new FromParameter(parameter, pt)));
        }
        blueprint.prototypes.forEach(pt -> prototypes.add(new FromPrototype(pt)));
        config.reversedUses().forEach(use -> use.prototypes.forEach(pt -> prototypes.add(new FromPrototype(pt))));
        return prototypes;
    }

    @Override
    public String toString() {
        return blueprint + "." + name()
                + parameters().stream().map(InstantiatedVariable::toString).collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int hashCode() {
        throw new ContextedRuntimeException("hashCode");
    }

    @Override
    public boolean equals(Object obj) {
        throw new ContextedRuntimeException("equals");
    }

    public String name() {
        return method.name();
    }

    public JaggerPrototype withMethod(InstantiatedMethod newMethod) {
        return new JaggerPrototype(blueprint, newMethod, kind, ctx, config, overrides, trigger);
    }

    public ExecutableElement element() {
        return method.element();
    }

    public List<InstantiatedVariable> parameters() {
        return method.parameters();
    }

    public TypeMirror returnType() {
        return method.returnType();
    }

    public Set<TypeVar> freeTypeVars() {
        return method.freeTypeVars();
    }
}
