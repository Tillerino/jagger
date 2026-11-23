package org.tillerino.jagger.processor.util;

import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

/** A method that is available to a prototype and knows how to obtain the instance it is called on. */
public sealed interface PrototypeInScope {
    JaggerPrototype prototype();

    default PrototypeInScope applyTypeBindings(Map<TypeVar, TypeMirror> typeBindings) {
        JaggerPrototype prototype = prototype();
        InstantiatedMethod bound = prototype.ctx().generics.applyTypeBindings(prototype.method(), typeBindings);
        JaggerPrototype boundPrototype = prototype.withMethod(bound);
        if (this instanceof FromParameter fromParameter) {
            return new FromParameter(fromParameter.parameter(), boundPrototype);
        }
        return new FromPrototype(boundPrototype);
    }

    default Expr call(JaggerPrototype caller, List<Expr> arguments, GeneratedClass generatedClass) {
        InstantiatedMethod method = prototype().method();
        if (method.element().getModifiers().contains(Modifier.STATIC)) {
            return method.callStaticFindingArguments(caller, arguments, generatedClass);
        }
        return method.callFindingArguments(instance(caller, generatedClass), caller, arguments, generatedClass);
    }

    Expr instance(JaggerPrototype caller, GeneratedClass generatedClass);

    record FromParameter(InstantiatedVariable parameter, JaggerPrototype prototype) implements PrototypeInScope {
        @Override
        public Expr instance(JaggerPrototype caller, GeneratedClass generatedClass) {
            return parameter;
        }
    }

    record FromPrototype(JaggerPrototype prototype) implements PrototypeInScope {
        @Override
        public Expr instance(JaggerPrototype caller, GeneratedClass generatedClass) {
            return generatedClass.getOrCreateDelegateeField(
                    caller.blueprint(), prototype.blueprint(), !prototype.overrides());
        }
    }
}
