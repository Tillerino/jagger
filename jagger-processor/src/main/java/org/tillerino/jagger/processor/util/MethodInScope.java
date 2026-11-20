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
        if (this instanceof FromBlueprint fromBlueprint) {
            return new FromBlueprint(fromBlueprint.blueprint(), bound);
        }
        throw new IllegalStateException();
    }

    default Expr call(JaggerPrototype prototype, List<Expr> arguments, GeneratedClass generatedClass) {
        InstantiatedMethod method = method();
        if (method.element().getModifiers().contains(Modifier.STATIC)) {
            return method.callStaticFindingArguments(prototype, arguments, generatedClass);
        }
        return method.callFindingArguments(instance(prototype, generatedClass), prototype, arguments, generatedClass);
    }

    Expr instance(JaggerPrototype prototype, GeneratedClass generatedClass);

    record FromParameter(InstantiatedVariable parameter, InstantiatedMethod method) implements MethodInScope {
        @Override
        public Expr instance(JaggerPrototype prototype, GeneratedClass generatedClass) {
            return parameter;
        }
    }

    record FromBlueprint(JaggerBlueprint blueprint, InstantiatedMethod method) implements MethodInScope {
        @Override
        public Expr instance(JaggerPrototype prototype, GeneratedClass generatedClass) {
            if (blueprint == prototype.blueprint()) {
                return generatedClass.getOrCreateDelegateeField(prototype.blueprint(), blueprint, false);
            }
            TypeMirror declaringType = blueprint.typeElement.asType();
            for (InstantiatedVariable parameter : prototype.parameters()) {
                if (prototype.blueprint().ctx.commonTypes.isAssignable(parameter.type(), declaringType)) {
                    return parameter;
                }
            }
            return generatedClass.getOrCreateDelegateeField(prototype.blueprint(), blueprint, false);
        }
    }
}
