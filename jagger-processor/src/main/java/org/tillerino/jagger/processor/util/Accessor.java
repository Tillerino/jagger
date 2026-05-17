package org.tillerino.jagger.processor.util;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.util.Snippet.PerfectSnippet;

public sealed interface Accessor {
    TypeMirror type();

    Element element();

    String name();

    AccessorKind kind();

    sealed interface ReadAccessor extends Accessor {
        default Snippet readSnippet(Snippet object) {
            return Snippet.of(
                    "$C.$L" + (kind() == AccessorKind.GETTER ? "()" : ""),
                    object,
                    element().getSimpleName().toString());
        }

        default PerfectSnippet readSnippet(PerfectSnippet object) {
            return kind() == AccessorKind.GETTER
                    ? object.invokeMethod(type(), element().getSimpleName().toString())
                    : object.readField(type(), element().getSimpleName().toString());
        }
    }

    sealed interface WriteAccessor extends Accessor {
        default Snippet writeSnippet(Snippet object, Snippet value) {
            return Snippet.of(
                    "$C.$L" + (kind() == AccessorKind.SETTER ? "($C)" : " = $C"),
                    object,
                    element().getSimpleName().toString(),
                    value);
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
