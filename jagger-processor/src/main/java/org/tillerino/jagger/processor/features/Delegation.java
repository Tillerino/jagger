package org.tillerino.jagger.processor.features;

import static org.tillerino.jagger.processor.util.Expr.e;

import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.config.ConfigProperty;
import org.tillerino.jagger.processor.config.ConfigProperty.LocationKind;
import org.tillerino.jagger.processor.config.ConfigProperty.MergeFunction;
import org.tillerino.jagger.processor.config.ConfigProperty.PropagationKind;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind.MatchingOptions;
import org.tillerino.jagger.processor.features.Converters.ConverterPrototypeKind;
import org.tillerino.jagger.processor.features.Generics.FunctionalInterface;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Expr;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeInScope;
import org.tillerino.jagger.processor.util.ShortName;

public class Delegation {
    protected final JaggerContext ctx;

    public Delegation(JaggerContext ctx) {
        this.ctx = ctx;
    }

    public static ConfigProperty<JsonConfig.DelegateeMode> DELEGATE_TO = ConfigProperty.createConfigProperty(
            "DELEGATE_TO",
            List.of(LocationKind.BLUEPRINT, LocationKind.PROTOTYPE),
            JsonConfig.DelegateeMode.DEFAULT,
            MergeFunction.notDefault(),
            PropagationKind.none());

    public static ConfigProperty<Boolean> DELEGATE_FROM = ConfigProperty.createConfigProperty(
            "DELEGATE_FROM", List.of(LocationKind.PROPERTY), true, (x, y) -> x, List.of(PropagationKind.SUBSTITUTE));

    public Optional<Delegatee> findDelegatee(
            TemplatablePrototypeKind target,
            JaggerPrototype caller,
            boolean allowRecursion,
            boolean allowExact,
            AnyConfig config,
            GeneratedClass generatedClass) {
        if (!config.resolveProperty(DELEGATE_FROM).value()) {
            return Optional.empty();
        }
        return findDelegateeInScope(target, caller, allowRecursion, allowExact, config, generatedClass)
                .or(() -> allowExact
                        ? Templates.delegateToAutoTemplate(ctx, target, caller).map(prototype -> {
                            PrototypeInScope template = new PrototypeInScope.FromPrototype(prototype);
                            return new Delegatee(
                                    template.instance(caller, generatedClass),
                                    template.prototype().method());
                        })
                        : Optional.empty());
    }

    private Optional<Delegatee> findDelegateeInScope(
            TemplatablePrototypeKind target,
            JaggerPrototype caller,
            boolean allowRecursion,
            boolean allowExact,
            AnyConfig config,
            GeneratedClass generatedClass) {
        for (PrototypeInScope prototypeInScope : caller.listPrototypesInScope(config)) {
            JaggerPrototype callee = prototypeInScope.prototype();
            if (prototypeInScope instanceof PrototypeInScope.FromPrototype
                    && (!canBeDelegatedTo(callee) || (callee == caller && !allowRecursion))) {
                continue;
            }
            if (!(callee.kind() instanceof TemplatablePrototypeKind kind)) {
                continue;
            }
            LinkedHashMap<TypeVar, TypeMirror> typeBindings = new LinkedHashMap<>();
            if (!kind.matches(target, new MatchingOptions(ctx, typeBindings, callee.freeTypeVars(), false))) {
                continue;
            }
            if (prototypeInScope instanceof PrototypeInScope.FromPrototype && !allowExact && typeBindings.isEmpty()) {
                continue;
            }
            PrototypeInScope bound = prototypeInScope.applyTypeBindings(typeBindings);
            return Optional.of(new Delegatee(
                    bound.instance(caller, generatedClass), bound.prototype().method()));
        }
        return Optional.empty();
    }

    private static boolean canBeDelegatedTo(JaggerPrototype callee) {
        return callee.config().resolveProperty(DELEGATE_TO).value().canBeDelegatedTo();
    }

    public List<Expr> findArguments(
            JaggerPrototype caller,
            InstantiatedMethod callee,
            List<? extends Expr> additionalParameters,
            GeneratedClass generatedClass,
            Set<String> avn) {
        List<Expr> exprs = new ArrayList<>();
        for (InstantiatedVariable targetParameter : callee.parameters()) {
            Optional<Expr> argument = findArgument(caller, generatedClass, additionalParameters, targetParameter, avn);
            if (argument.isEmpty()) {
                throw new ContextedRuntimeException(
                                ("Could not find a value of type %s to pass in method call. Consider declaring a parameter of this type on the caller.")
                                        .formatted(ShortName.of(targetParameter.type())))
                        .addContextValue("parameter", targetParameter)
                        .addContextValue("callee", callee)
                        .addContextValue("caller", caller != null ? caller.method() : "(anonymous)");
            }
            exprs.add(argument.get());
        }
        return exprs;
    }

    public Optional<Expr> findArgument(
            JaggerPrototype caller,
            GeneratedClass generatedClass,
            List<? extends Expr> additionalParameters,
            InstantiatedVariable targetArgument,
            Set<String> avn) {
        // search in caller's own parameters
        Iterable<Expr> locallyAvailable = () -> Stream.concat(
                        additionalParameters.stream(), caller != null ? caller.parameters().stream() : Stream.of())
                .iterator();
        for (Expr instantiatedParameter : locallyAvailable) {
            if (ctx.commonTypes.isAssignable(instantiatedParameter.type(), targetArgument.type())) {
                return Optional.of(instantiatedParameter);
            }
        }

        {
            // see if we can instantiate an instance from our list of used blueprints
            Expr delegateeInField =
                    generatedClass.getOrCreateUsedBlueprintWithTypeField(targetArgument.type(), caller.config());
            if (delegateeInField != null) {
                return Optional.of(delegateeInField);
            }
        }
        if (targetArgument.type() instanceof DeclaredType t
                && t.asElement().equals(ctx.commonTypes.classElement)
                && !t.getTypeArguments().isEmpty()) {
            TypeMirror typeOfClass = t.getTypeArguments().get(0);
            if (Generics.canBeClass(typeOfClass)) {
                return Optional.of(e(null, "$T.class", typeOfClass));
            }
        }

        Optional<FunctionalInterface> functionalInterface = ctx.generics.isFunctionalInterface(targetArgument.type());
        if (functionalInterface.isEmpty()) {
            return Optional.empty();
        }

        Optional<PrototypeKind> prototypeKind =
                ctx.detectPrototype(functionalInterface.get().method());
        if (prototypeKind.isEmpty()) {
            return Optional.empty();
        }

        if (prototypeKind.get() instanceof TemplatablePrototypeKind tpk) {
            Optional<Expr> lambda = ctx.delegation
                    .findDelegatee(tpk, caller, false, true, caller.config(), generatedClass)
                    .flatMap(delegatee -> ctx.generics.getOrCreateLambda(
                            caller, generatedClass, targetArgument.type(), delegatee, avn));
            if (lambda.isPresent()) {
                return lambda;
            }
        }

        if (!(prototypeKind.get() instanceof ConverterPrototypeKind cpk)) {
            return Optional.empty();
        }

        Optional<Expr> jsonValueOrJsonCreator = ctx.creators.findConverterLambda(cpk, caller, generatedClass, avn);
        if (jsonValueOrJsonCreator.isPresent()) {
            return jsonValueOrJsonCreator;
        }

        return ctx.converters.instantiateStringConverter(
                targetArgument.type(), functionalInterface.get().method(), cpk, generatedClass);
    }

    public record Delegatee(Expr fieldOrParameter, InstantiatedMethod method) {
        public Expr call(
                JaggerPrototype caller,
                List<Expr> additionalParameters,
                GeneratedClass generatedClass,
                Set<String> avn) {
            return method.callFindingArguments(fieldOrParameter, caller, additionalParameters, generatedClass, avn);
        }
    }
}
