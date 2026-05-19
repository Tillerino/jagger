package org.tillerino.jagger.processor.util;

import static org.tillerino.jagger.processor.util.Code.c;

import java.util.List;
import java.util.function.Function;
import javax.lang.model.type.TypeMirror;

public interface Expr extends Code {
    TypeMirror type();

    @Override
    Expr subst(Expr needle, Expr replacement);

    default boolean isQuick() {
        return false;
    }

    default Expr field(TypeMirror fieldType, String fieldName) {
        return e(fieldType, "$C.$L", this, fieldName);
    }

    default Expr call(TypeMirror returnType, String methodName) {
        return e(returnType, "$C.$L()", this, methodName);
    }

    default Expr call(TypeMirror returnType, String methodName, List<Expr> arguments) {
        return e(returnType, "$C.$L($C)", this, methodName, Code.join(arguments, ", "));
    }

    static Expr e(TypeMirror type, String format, Object... args) {
        return e(type, c(format, args));
    }

    static Expr e(TypeMirror type, Code nested) {
        return new Expr() {
            @Override
            public TypeMirror type() {
                return type;
            }

            @Override
            public Flattened flatten() {
                return nested.flatten();
            }

            @Override
            public Expr subst(Expr needle, Expr replacement) {
                Code subst = nested.subst(needle, replacement);
                if (subst != nested) {
                    return e(type, subst);
                }
                return this;
            }
        };
    }

    record TypedVariable(TypeMirror type, String name) implements AnyVariable {
        @Override
        public Expr subst(Expr needle, Expr replacement) {
            return needle instanceof AnyVariable v && v.variableName().equals(name) ? replacement : this;
        }

        @Override
        public Flattened flatten() {
            return Flattened.of("$L", name);
        }

        @Override
        public boolean isQuick() {
            return true;
        }

        @Override
        public String variableName() {
            return name;
        }
    }

    class ExprWrapper<SELF extends Expr> implements Expr {
        protected final Expr wrapped;
        private final Function<Expr, SELF> reconstructor;

        protected ExprWrapper(Expr wrapped, Function<Expr, SELF> reconstructor) {
            this.wrapped = wrapped;
            this.reconstructor = reconstructor;
        }

        @Override
        public TypeMirror type() {
            return wrapped.type();
        }

        @Override
        public Flattened flatten() {
            return wrapped.flatten();
        }

        @Override
        public SELF subst(Expr needle, Expr replacement) {
            Expr subst = wrapped.subst(needle, replacement);
            return subst != wrapped ? reconstructor.apply(subst) : (SELF) this;
        }

        @Override
        public boolean isQuick() {
            return wrapped.isQuick();
        }
    }
}
