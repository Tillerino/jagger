package org.tillerino.jagger.processor;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.Validate;
import org.tillerino.jagger.processor.util.Named;

/**
 * The purpose of this interface is to provide a way to carry both the format and the arguments for
 * {@link CodeBlock.Builder} methods. We add a few convenience feature for the format on top: E.g. you can use $C to
 * insert a nested snippet. Using {@link Element} or {@link Named} as arguments will automatically extract the name.
 */
public interface Snippet {
    String format();

    Object[] args();

    static Snippet of(String format, Object... args) {
        Queue<Object> remainingArgs = new LinkedList<>(Arrays.asList(args));
        StringBuilder sb = new StringBuilder();
        List<Object> flatArgs = new ArrayList<>();
        for (int i = 0; i < format.length(); ) {
            int j = format.indexOf('$', i);
            if (j == -1 || j == format.length() - 1) {
                sb.append(format, i, format.length());
                break;
            }
            switch (format.charAt(j + 1)) {
                case '$' -> {
                    sb.append(format, i, j + 2);
                }
                case 'C' -> {
                    sb.append(format, i, j);
                    Snippet nested = (Snippet) remainingArgs.remove();
                    sb.append(nested.format());
                    collectInto(nested.args(), flatArgs);
                }
                default -> {
                    sb.append(format, i, j + 2);
                    collectInto(remainingArgs.remove(), flatArgs);
                }
            }
            i = j + 2;
        }
        Validate.isTrue(remainingArgs.isEmpty(), "Too many arguments");
        String flatFormat = sb.toString();
        Object[] flatArgsArray = flatArgs.toArray();
        return new Snippet() {
            @Override
            public String format() {
                return flatFormat;
            }

            @Override
            public Object[] args() {
                return flatArgsArray;
            }
        };
    }

    static Snippet join(Collection<? extends Snippet> snippets, String delimiter) {
        return join(snippets, delimiter, "", "");
    }

    static Snippet join(Collection<? extends Snippet> snippets, String delimiter, String before, String after) {
        String format = snippets.stream().map(Snippet::format).collect(Collectors.joining(delimiter, before, after));
        Object[] args = snippets.stream().flatMap(s -> Arrays.stream(s.args())).toArray();
        return Snippet.of(format, args);
    }

    static Snippet joinPrependingCommaToEach(Collection<? extends Snippet> snippets) {
        String format = snippets.stream().map(s -> ", " + s.format()).collect(Collectors.joining());
        Object[] args = snippets.stream().flatMap(s -> Arrays.stream(s.args())).toArray();
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
                public String format() {
                    return nested.format();
                }

                @Override
                public Object[] args() {
                    return nested.args();
                }
            };
        }

        static TypedSnippet of(TypeMirror type, String format, Object... args) {
            return of(type, Snippet.of(format, args));
        }
    }
}
