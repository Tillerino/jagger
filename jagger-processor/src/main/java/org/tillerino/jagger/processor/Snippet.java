package org.tillerino.jagger.processor;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.util.Named;

/**
 * The purpose of this interface is to provide a way to carry both the format and the arguments for
 * {@link CodeBlock.Builder} methods. We add a few convenience feature for the format on top: E.g. you can use $C to
 * insert a nested snippet. Using {@link Element} or {@link Named} as arguments will automatically extract the name.
 */
public interface Snippet {
    Flattened flatten();

    static Snippet of(String format, Object... args) {
        record A(String f, Object a) {}
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
                    Object o = remainingArgs.remove();
                    if (!(o instanceof Snippet s)) {
                        throw new IllegalArgumentException();
                    }
                    deconstructed.add(s);
                }
                default -> {
                    deconstructed.add(new A(format.substring(i, j + 2), remainingArgs.remove()));
                }
            }
            i = j + 2;
        }

        return () -> {
            StringBuilder builder = new StringBuilder();
            List<Object> flatArgs = new ArrayList<>();
            for (Object o : deconstructed) {
                if (o instanceof String s) {
                    builder.append(s);
                } else if (o instanceof A a) {
                    builder.append(a.f);
                    if (a.a instanceof Named e) {
                        flatArgs.add(e.name());
                    } else {
                        flatArgs.add(a.a);
                    }
                } else {
                    Snippet s = (Snippet) o;
                    Flattened f = s.flatten();
                    builder.append(f.format);
                    flatArgs.addAll(Arrays.asList(f.args));
                }
            }
            return new Flattened(builder.toString(), flatArgs.toArray());
        };
    }

    static Snippet join(Collection<? extends Snippet> snippets, String delimiter) {
        return join(snippets, delimiter, "", "");
    }

    static Snippet join(Collection<? extends Snippet> snippets, String delimiter, String before, String after) {
        String format = snippets.stream().map(__ -> "$C").collect(Collectors.joining(delimiter, before, after));
        Object[] args = snippets.toArray();
        return Snippet.of(format, args);
    }

    static Snippet joinPrependingCommaToEach(Collection<? extends Snippet> snippets) {
        String format = snippets.stream().map(s -> ", $C").collect(Collectors.joining());
        Object[] args = snippets.toArray();
        return Snippet.of(format, args);
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

    interface TypedSnippet extends Snippet {
        TypeMirror type();

        static TypedSnippet of(TypeMirror type, Snippet nested) {
            return new TypedSnippet() {
                @Override
                public TypeMirror type() {
                    return type;
                }

                @Override
                public Flattened flatten() {
                    return nested.flatten();
                }
            };
        }

        static TypedSnippet of(TypeMirror type, String format, Object... args) {
            return of(type, Snippet.of(format, args));
        }
    }

    interface PerfectSnippet extends TypedSnippet {
        PerfectSnippet replaceVar(String name, PerfectSnippet replacement);

        static <T extends PerfectSnippet> List<T> modAll(List<T> os, UnaryOperator<T> op) {
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

        record TypedVariable(TypeMirror type, String name) implements PerfectSnippet {
            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                return this.name.equals(name) ? replacement : this;
            }

            @Override
            public Flattened flatten() {
                return Flattened.of("$L", name);
            }
        }

        record ReadAccessorInvocation(TypeMirror type, PerfectSnippet object, String invocation)
                implements PerfectSnippet {
            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                PerfectSnippet replaced = object.replaceVar(name, replacement);
                return replaced != object ? new ReadAccessorInvocation(type, replaced, invocation) : this;
            }

            @Override
            public Flattened flatten() {
                return Snippet.of("$C.$L", object, invocation).flatten();
            }
        }

        record ConstructorCall(TypeMirror type, TypeMirror classType, String diamond, List<PerfectSnippet> arguments)
                implements PerfectSnippet {
            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                List<PerfectSnippet> replacedArgs = modAll(arguments, a -> a.replaceVar(name, replacement));
                return replacedArgs != null ? new ConstructorCall(type, classType, diamond, replacedArgs) : this;
            }

            @Override
            public Flattened flatten() {
                String cs = arguments.stream().map(__ -> "$C").collect(Collectors.joining(", "));
                return Snippet.of("new $T" + diamond + "(" + cs + ")", classType, arguments.toArray())
                        .flatten();
            }
        }

        record StaticMethodInvocation(
                TypeMirror type, TypeMirror classType, String methodName, List<PerfectSnippet> arguments)
                implements PerfectSnippet {
            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                List<PerfectSnippet> replacedArgs = modAll(arguments, a -> a.replaceVar(name, replacement));
                return replacedArgs != null
                        ? new StaticMethodInvocation(type, classType, methodName, replacedArgs)
                        : this;
            }

            @Override
            public Flattened flatten() {
                return Snippet.of("$T.$L($L)", classType, methodName, arguments.toArray())
                        .flatten();
            }
        }

        record InstanceMethodInvocation(
                TypeMirror type, PerfectSnippet object, String methodName, List<PerfectSnippet> arguments)
                implements PerfectSnippet {
            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                List<PerfectSnippet> replacedArgs = modAll(arguments, a -> a.replaceVar(name, replacement));
                PerfectSnippet replacedObject = object.replaceVar(name, replacement);
                return replacedArgs != null || replacedObject != object
                        ? new InstanceMethodInvocation(type, replacedObject, methodName, replacedArgs)
                        : this;
            }

            @Override
            public Flattened flatten() {
                return Snippet.of("$C.$L($L)", object, methodName, arguments.toArray())
                        .flatten();
            }
        }

        record StaticMethodReference(TypeMirror type, TypeMirror classType, String methodName)
                implements PerfectSnippet {

            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                return this;
            }

            @Override
            public Flattened flatten() {
                return Snippet.of("$T::$L", classType, methodName).flatten();
            }
        }

        record InstanceMethodReference(TypeMirror type, PerfectSnippet object, String methodName)
                implements PerfectSnippet {

            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                PerfectSnippet replaced = object.replaceVar(name, replacement);
                return replaced != object ? new InstanceMethodReference(type, replaced, methodName) : this;
            }

            @Override
            public Flattened flatten() {
                return Snippet.of("$C::$L", object, methodName).flatten();
            }
        }

        record Literal(TypeMirror type, String value) implements PerfectSnippet {

            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                return this;
            }

            @Override
            public Flattened flatten() {
                return Flattened.of("$L", value);
            }
        }

        record ClassExpr(TypeMirror type) implements PerfectSnippet {

            @Override
            public PerfectSnippet replaceVar(String name, PerfectSnippet replacement) {
                return this;
            }

            @Override
            public Flattened flatten() {
                return Flattened.of("$T.class", type);
            }
        }
    }

    record Flattened(String format, Object[] args) implements Snippet {
        @Override
        public Flattened flatten() {
            return this;
        }

        public static Flattened of(String format, Object... args) {
            return new Flattened(format, args);
        }
    }
}
