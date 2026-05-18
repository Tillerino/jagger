package org.tillerino.jagger.processor.util;

import static org.tillerino.jagger.processor.util.Code.c;
import static org.tillerino.jagger.processor.util.Expr.e;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

public sealed interface Accessor {
    TypeMirror type();

    Element element();

    String name();

    AccessorKind kind();

    sealed interface ReadAccessor extends Accessor {
        default Expr read(Expr instance) {
            return kind() == AccessorKind.GETTER
                    ? e(type(), "$C.$L()", instance, name())
                    : e(type(), "$C.$L", instance, name());
        }
    }

    sealed interface WriteAccessor extends Accessor {
        default Code write(Expr instance, Expr value) {
            return kind() == AccessorKind.SETTER
                    ? c("$C.$L($C)", instance, name(), value)
                    : c("$C.$L = $C", instance, name(), value);
        }
    }

    record ElementAccessor(TypeMirror type, Element element, AccessorKind kind) implements ReadAccessor, WriteAccessor {
        public String name() {
            return element.getSimpleName().toString();
        }
    }

    enum AccessorKind {
        PARAMETER,
        FIELD,
        /** Also includes a record component for reading. */
        GETTER,
        SETTER;
    }
}
