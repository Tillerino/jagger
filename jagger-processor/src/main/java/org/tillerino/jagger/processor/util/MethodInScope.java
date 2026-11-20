package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerBlueprint;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

/** A method that is available to a prototype and knows how to obtain the instance it is called on. */
public sealed interface MethodInScope {
    InstantiatedMethod method();

    default MethodInScope applyTypeBindings(Map<TypeVar, TypeMirror> typeBindings) {
        InstantiatedMethod method = method();
        InstantiatedMethod bound = method.ctx().generics.applyTypeBindings(method, typeBindings);
        if (this instanceof FromParameter fromParameter) {
            return new FromParameter(fromParameter.parameter(), bound);
        }
        if (this instanceof FromPrototype fromPrototype) {
            JaggerPrototype prototype = fromPrototype.prototype();
            return new FromPrototype(new JaggerPrototype(
                    prototype.blueprint(),
                    bound,
                    prototype.kind(),
                    prototype.ctx(),
                    prototype.config(),
                    prototype.overrides(),
                    prototype.trigger()));
        }
        if (this instanceof FromBlueprint fromBlueprint) {
            return new FromBlueprint(fromBlueprint.blueprint(), bound);
        }
        throw new IllegalStateException();
    }

    default Expr call(JaggerPrototype caller, List<Expr> arguments, GeneratedClass generatedClass) {
        InstantiatedMethod method = method();
        if (method.element().getModifiers().contains(Modifier.STATIC)) {
            return method.callStaticFindingArguments(caller, arguments, generatedClass);
        }
        return method.callFindingArguments(instance(caller, generatedClass), caller, arguments, generatedClass);
    }

    Expr instance(JaggerPrototype caller, GeneratedClass generatedClass);

    record FromParameter(InstantiatedVariable parameter, InstantiatedMethod method) implements MethodInScope {
        @Override
        public Expr instance(JaggerPrototype caller, GeneratedClass generatedClass) {
            return parameter;
        }
    }

    record FromPrototype(JaggerPrototype prototype) implements MethodInScope {
        @Override
        public InstantiatedMethod method() {
            return prototype.method();
        }

        @Override
        public Expr instance(JaggerPrototype caller, GeneratedClass generatedClass) {
            return generatedClass.getOrCreateDelegateeField(
                    caller.blueprint(), prototype.blueprint(), !prototype.overrides());
        }
    }

    /** A general method that can be reached from the blueprint that is not an actual {@link JaggerPrototype}. */
    record FromBlueprint(JaggerBlueprint blueprint, InstantiatedMethod method) implements MethodInScope {
        @Override
        public Expr instance(JaggerPrototype caller, GeneratedClass generatedClass) {
            if (blueprint == caller.blueprint()) {
                return generatedClass.getOrCreateDelegateeField(caller.blueprint(), blueprint, false);
            }
            TypeMirror declaringType = blueprint.typeElement.asType();
            for (InstantiatedVariable parameter : caller.parameters()) {
                if (caller.blueprint().ctx.commonTypes.isAssignable(parameter.type(), declaringType)) {
                    return parameter;
                }
            }
            return generatedClass.getOrCreateDelegateeField(caller.blueprint(), blueprint, false);
        }
    }
}
