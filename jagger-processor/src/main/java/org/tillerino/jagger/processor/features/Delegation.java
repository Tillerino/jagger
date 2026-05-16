package org.tillerino.jagger.processor.features;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.ShortName;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet.ClassExpr;

public record Delegation(JaggerContext ctx) {
    public static ConfigProperty<JsonConfig.DelegateeMode> DELEGATE_TO = ConfigProperty.createConfigProperty(
            "DELEGATE_TO",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE),
            JsonConfig.DelegateeMode.DEFAULT,
            MergeFunction.notDefault(),
            List.of());

    public static ConfigProperty<Boolean> DELEGATE_FROM = ConfigProperty.createConfigProperty(
            "DELEGATE_FROM", List.of(LocationKind.PROPERTY), true, (x, y) -> x, List.of(PropagationKind.SUBSTITUTE));

    public Optional<Delegatee> findDelegatee(
            TemplatablePrototypeKind target,
            JaggerPrototype caller,
            boolean allowRecursion,
            boolean allowExact,
            AnyConfig config,
            GeneratedClass generatedClass) {
        return findPrototype(target, caller, allowRecursion, allowExact, config)
                .map(d -> new Delegatee(
                        generatedClass.getOrCreateDelegateeField(
                                caller.blueprint(),
                                d.blueprint(),
                                !d.prototype().overrides()),
                        d.method()))
                .or(() -> ctx.delegation.findDelegateeInMethodParameters(caller, target));
    }

    private Optional<InstantiatedPrototype> findPrototype(
            TemplatablePrototypeKind target,
            JaggerPrototype caller,
            boolean allowRecursion,
            boolean allowExact,
            AnyConfig config) {
        if (!config.resolveProperty(DELEGATE_FROM).value()) {
            return Optional.empty();
        }
        JaggerBlueprint blueprint = caller.blueprint();
        for (JaggerPrototype callee : blueprint.prototypes) {
            if (canBeDelegatedTo(callee) && (callee != caller || allowRecursion)) {
                InstantiatedMethod match = callee.matches(target, allowExact);
                if (match != null) {
                    return Optional.of(new InstantiatedPrototype(blueprint, callee, match));
                }
            }
        }
        for (JaggerBlueprint use : config.reversedUses()) {
            for (JaggerPrototype callee : use.prototypes) {
                if (canBeDelegatedTo(callee)) {
                    InstantiatedMethod match = callee.matches(target, allowExact);
                    if (match != null) {
                        return Optional.of(new InstantiatedPrototype(use, callee, match));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean canBeDelegatedTo(JaggerPrototype callee) {
        return callee.config().resolveProperty(DELEGATE_TO).value().canBeDelegatedTo();
    }

    private Optional<Delegatee> findDelegateeInMethodParameters(
            JaggerPrototype prototype, TemplatablePrototypeKind target) {
        if (!(prototype.kind() instanceof TemplatablePrototypeKind t)) {
            return Optional.empty();
        }
        for (InstantiatedVariable parameter : prototype.parameters()) {
            for (InstantiatedMethod method :
                    ctx.generics.instantiateMethods(parameter.type(), LocationKind.PROTOTYPE)) {
                Optional<TemplatablePrototypeKind> prototypeKind = ctx.detectPrototype(method)
                        .filter(kind -> kind instanceof TemplatablePrototypeKind)
                        .map(TemplatablePrototypeKind.class::cast)
                        .filter(kind -> kind.matches(target, ctx, new LinkedHashMap<>(), method.freeTypeVars()));
                if (prototypeKind.isPresent()) {
                    return Optional.of(new Delegatee(parameter, method));
                }
            }
        }
        return Optional.empty();
    }

    public List<PerfectSnippet> findArguments(
            JaggerPrototype caller, InstantiatedMethod callee, int firstArgument, GeneratedClass generatedClass) {
        return IntStream.range(firstArgument, callee.parameters().size())
                .mapToObj(i -> {
                    InstantiatedVariable targetParameter = callee.parameters().get(i);
                    return findArgument(caller, generatedClass, targetParameter)
                            .orElseThrow(() -> new ContextedRuntimeException(
                                            ("Could not find a value of type %s to pass in method call. Consider declaring a parameter of this type on the caller.")
                                                    .formatted(ShortName.of(targetParameter.type())))
                                    .addContextValue("parameter", targetParameter)
                                    .addContextValue("callee", callee)
                                    .addContextValue("caller", caller.method()));
                })
                .collect(Collectors.toList());
    }

    private Optional<PerfectSnippet> findArgument(
            JaggerPrototype caller, GeneratedClass generatedClass, InstantiatedVariable targetArgument) {
        // search in caller's own parameters
        for (InstantiatedVariable instantiatedParameter : caller.parameters()) {
            if (ctx.commonTypes.isAssignable(instantiatedParameter.type(), targetArgument.type())) {
                return Optional.of(instantiatedParameter);
            }
        }
        // see if we can instantiate an instance from our list of used blueprints
        PerfectSnippet delegateeInField =
                generatedClass.getOrCreateUsedBlueprintWithTypeField(targetArgument.type(), caller.config());
        if (delegateeInField != null) {
            return Optional.of(delegateeInField);
        }
        if (targetArgument.type() instanceof DeclaredType t
                && t.asElement().equals(ctx.commonTypes.classElement)
                && !t.getTypeArguments().isEmpty()) {
            TypeMirror typeOfClass = t.getTypeArguments().get(0);
            if (Generics.canBeClass(typeOfClass)) {
                return Optional.of(new ClassExpr(typeOfClass));
            }
        }
        // see if we can instantiate a lambda from our list of used blueprints
        return ctx.generics.getOrCreateLambda(generatedClass, targetArgument.type(), caller.parameters(), 0);
    }

    public record Delegatee(PerfectSnippet fieldOrParameter, InstantiatedMethod method) {}

    public record InstantiatedPrototype(
            JaggerBlueprint blueprint, JaggerPrototype prototype, InstantiatedMethod method) {}
}
