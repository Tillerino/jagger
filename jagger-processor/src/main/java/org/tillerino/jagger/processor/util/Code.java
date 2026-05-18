package org.tillerino.jagger.processor.util;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.tillerino.jagger.processor.util.Code.DeconstructedCode.A;

/**
 * The purpose of this interface is to provide a way to carry both the format and the arguments for
 * {@link CodeBlock.Builder} methods. Whereas {@link Code} is any piece of code, {@link Expr} is a piece of code with a
 * type.
 */
public interface Code {
    Flattened flatten();

    Code subst(Expr needle, Expr replacement);

    static Code c(String format, Object... args) {

        List<Object> deconstructed = new ArrayList<>();

        LinkedList<Object> remainingArgs = new LinkedList<>();
        collectInto(args, remainingArgs);
        for (int i = 0; i < format.length(); ) {
            int j = format.indexOf('$', i);
            if (j == -1 || j == format.length() - 1) {
                deconstructed.add(format.substring(i));
                break;
            }
            switch (format.charAt(j + 1)) {
                case '$' -> {
                    deconstructed.add(format.substring(i, j + 2));
                }
                case 'C' -> {
                    deconstructed.add(format.substring(i, j));
                    if (remainingArgs.isEmpty()) {
                        throw new IllegalArgumentException("Too few snippet arguments");
                    }
                    Object o = remainingArgs.remove();
                    if (!(o instanceof Code s)) {
                        throw new IllegalArgumentException("Not code: " + o);
                    }
                    deconstructed.add(s);
                }
                default -> {
                    deconstructed.add(new A(format.substring(i, j + 2), remainingArgs.remove()));
                }
            }
            i = j + 2;
        }

        return new DeconstructedCode(deconstructed);
    }

    static Code join(Collection<? extends Code> snippets, String delimiter) {
        return join(snippets, delimiter, "", "");
    }

    static Code join(Collection<? extends Code> snippets, String delimiter, String before, String after) {
        String format = snippets.stream().map(__ -> "$C").collect(Collectors.joining(delimiter, before, after));
        Object[] args = snippets.toArray();
        return c(format, args);
    }

    static void collectInto(Object o, List<Object> aggregator) {
        if (o instanceof Object[] oa) {
            for (Object o2 : oa) {
                collectInto(o2, aggregator);
            }
        } else {
            aggregator.add(o);
        }
    }

    static <T> List<T> modAll(List<T> os, UnaryOperator<T> op) {
        List<T> ms = null;
        for (int i = 0; i < os.size(); i++) {
            if (ms != null) {
                ms.set(i, op.apply(os.get(i)));
            } else {
                T o = os.get(i);
                T m = op.apply(o);
                if (o != m) {
                    ms = new ArrayList<>(os);
                    ms.set(i, m);
                }
            }
        }
        return ms != null ? ms : os;
    }

    class DeconstructedCode implements Code {
        record A(String f, Object a) {}

        private final List<Object> deconstructed;

        public DeconstructedCode(List<Object> deconstructed) {
            this.deconstructed = deconstructed;
        }

        @Override
        public Flattened flatten() {
            StringBuilder builder = new StringBuilder();
            List<Object> flatArgs = new ArrayList<>();
            for (Object o : deconstructed) {
                if (o instanceof String s) {
                    builder.append(s);
                } else if (o instanceof A a) {
                    builder.append(a.f);
                    flatArgs.add(a.a);
                } else {
                    Code s = (Code) o;
                    Flattened f = s.flatten();
                    builder.append(f.format);
                    flatArgs.addAll(Arrays.asList(f.args));
                }
            }
            return new Flattened(builder.toString(), flatArgs.toArray());
        }

        @Override
        public Code subst(Expr needle, Expr replacement) {
            List<Object> replaced = modAll(deconstructed, d -> d instanceof Code s ? s.subst(needle, replacement) : d);
            return replaced != deconstructed ? new DeconstructedCode(replaced) : this;
        }
    }

    class CodeWrapper<SELF extends Code> implements Code {
        final Code wrapped;
        final Function<Code, SELF> reconstructor;

        protected CodeWrapper(Code wrapped, Function<Code, SELF> reconstructor) {
            this.wrapped = wrapped;
            this.reconstructor = reconstructor;
        }

        @Override
        public Flattened flatten() {
            return wrapped.flatten();
        }

        @Override
        public SELF subst(Expr needle, Expr replacement) {
            Code subst = wrapped.subst(needle, replacement);
            return subst != wrapped ? reconstructor.apply(subst) : (SELF) this;
        }
    }

    interface AnyVariable extends Expr {
        String variableName();
    }

    record Flattened(String format, Object[] args) {
        public static Flattened of(String format, Object... args) {
            return new Flattened(format, args);
        }
    }
}
