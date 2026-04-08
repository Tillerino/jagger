package org.tillerino.jagger.processor;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
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

        Queue<Object> remainingArgs = new LinkedList<>(Arrays.asList(args));
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
                    collectInto(a.a, flatArgs);
                } else {
                    Snippet s = (Snippet) o;
                    Flattened f = s.flatten();
                    builder.append(f.format);
                    collectInto(f.args, flatArgs);
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
        if (o instanceof Named e) {
            aggregator.add(e.name());
        } else if (o instanceof Object[] oa) {
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
