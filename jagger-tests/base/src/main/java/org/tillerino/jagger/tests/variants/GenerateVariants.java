package org.tillerino.jagger.tests.variants;

import com.github.javaparser.*;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.SourceRoot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.tillerino.jagger.tests.variants.GenerateVariants.VariantValue.ReplacementValue;

/**
 * We only write all tests for {@link com.fasterxml.jackson.core.JsonGenerator} and
 * {@link com.fasterxml.jackson.core.JsonParser}. These tests are then translated for other libraries.
 */
public class GenerateVariants {
    private static final LexicalPreservingPrinter printer = new LexicalPreservingPrinter();

    public static void main(String[] args) throws Exception {
        copy();
    }

    /** Copies all the original tests to a new package, replacing the writer and reader classes. */
    public static void copy() throws IOException {
        for (String p : List.of("src/main/java", "src/test/java")) {
            if (!Files.exists(Path.of(p))) {
                continue;
            }
            SourceRoot sourceRoot = new SourceRoot(Path.of(p));
            sourceRoot.setParserConfiguration(
                    new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));

            for (ParseResult<CompilationUnit> parseResult : sourceRoot.tryToParse("org.tillerino.jagger.tests")) {
                CompilationUnit cu = parseResult
                        .getResult()
                        .orElseThrow(() -> new RuntimeException("failed to parse" + parseResult.getProblems()));
                CompilationUnit lpp = LexicalPreservingPrinter.setup(cu);

                Boolean modified = cu.accept(
                        new GenericVisitorAdapter<Boolean, List<VariantValue>>() {
                            boolean top = true;

                            @Override
                            public Boolean visit(ClassOrInterfaceDeclaration n, List<VariantValue> arg) {
                                top = false;
                                return super.visit(n, arg);
                            }

                            @Override
                            public Boolean visit(NodeList n, List<VariantValue> arg) {
                                n.removeIf(node -> node instanceof NodeWithAnnotations<?> a
                                        && a.isAnnotationPresent(GeneratedVariant.class));
                                ArrayList originalChildren = new ArrayList<>(n);
                                Boolean modified = null;
                                Map<Node, Modification> append = new LinkedHashMap<>();

                                // we have variants -> modify children
                                if (arg != null && !arg.isEmpty()) {
                                    for (Object child : originalChildren) {
                                        if (child instanceof NodeWithAnnotations<?> nwa
                                                && nwa.isAnnotationPresent(ApplyVariantsToChildren.class)) {
                                            if (((Node) child).accept(this, arg) == Boolean.TRUE) {
                                                modified = true;
                                            }
                                        }
                                    }
                                    for (VariantValue variant : arg) {
                                        for (Object child : originalChildren) {
                                            if (child instanceof NodeWithAnnotations<?> nwa
                                                    && (nwa.isAnnotationPresent(ApplyVariantsToChildren.class)
                                                            || nwa.isAnnotationPresent(NoVariants.class))) {
                                                continue;
                                            }

                                            Optional<Modification> modifiedChild = modifyCode(variant, child);
                                            modifiedChild.ifPresent(m -> {
                                                if (!append.containsKey(m.modified)) {
                                                    append.put(m.modified, m);
                                                }
                                            });
                                            if (!modifiedChild.isPresent()) {
                                                ((Node) child).accept(this, arg);
                                            }
                                        }
                                    }

                                } else {

                                    for (Object child : originalChildren) {
                                        if (child instanceof NodeWithAnnotations<?> nwa) {
                                            List<VariantValue> variants = VariantValue.from(nwa);
                                            if (variants != null) {
                                                if (top
                                                        || (nwa.isAnnotationPresent(ApplyVariantsToChildren.class)
                                                                || nwa.isAnnotationPresent(NoVariants.class))) {
                                                    if (((Node) nwa).accept(this, variants) == Boolean.TRUE) {
                                                        modified = true;
                                                    }
                                                    continue;
                                                }

                                                for (VariantValue variant : variants) {
                                                    modifyCode(variant, child).ifPresent(m -> {
                                                        if (!append.containsKey(m.modified)) {
                                                            append.put(m.modified, m);
                                                        }
                                                    });
                                                }
                                            } else {
                                                Boolean result = ((Node) child).accept(this, arg);
                                                if (Boolean.TRUE == result) {
                                                    modified = true;
                                                }
                                            }
                                        }
                                    }
                                }

                                for (Modification modification : append.values()) {
                                    Node node = modification.withDecorations();
                                    if (n.size() == originalChildren.size()) {
                                        node.setBlockComment(" GENERATED CODE. DO NOT MODIFY BELOW! %s"
                                                .formatted(node.getComment()
                                                        .map(Comment::getContent)
                                                        .orElse("")));
                                    }
                                    n.add(node);
                                    modified = true;
                                }

                                return modified;
                            }
                        },
                        null);

                System.out.println(cu.getStorage().get().getFileName());
                System.out.println(modified);
                if (modified == Boolean.TRUE) {
                    cu.getStorage().get().save(LexicalPreservingPrinter::print);
                }
            }
        }
    }

    private static Optional<Modification> modifyCode(VariantValue variant, Object v) {
        if (!(v instanceof TypeDeclaration<?>) && !(v instanceof MethodDeclaration)) {
            return Optional.empty();
        }
        String original = LexicalPreservingPrinter.print((Node) v);
        String code = original;
        for (ReplacementValue replacement : variant.replacements) {
            code = code.replaceAll(replacement.regex, replacement.replacement);
        }
        if (code.equals(original)) {
            return Optional.empty();
        }
        Node parsed = parse(code, v);

        return Optional.of(new Modification(parsed, variant, (Node) v));
    }

    private static Node parse(String code, Object reference) {
        ParseStart result;
        if (reference instanceof RecordDeclaration) {
            result = ParseStart.COMPILATION_UNIT;
        } else if (reference instanceof MethodDeclaration) {
            result = ParseStart.METHOD_DECLARATION;
        } else {
            throw new UnsupportedOperationException("Unknown kind of element: " + reference);
        }
        ParseResult parse = new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_17))
                .parse(result, Providers.provider(code));
        if (!parse.isSuccessful()) {
            throw new RuntimeException(parse.toString());
        }
        Node node = (Node) parse.getResult().get();
        LexicalPreservingPrinter.setup(node);
        if (node instanceof CompilationUnit cu) {
            return cu.getChildNodes().get(0);
        }
        return node;
    }

    record VariantValue(List<ReplacementValue> replacements) {
        static List<VariantValue> from(NodeWithAnnotations<?> node) {
            for (AnnotationExpr annotation : node.getAnnotations()) {
                VariantsExtractor extractor = new VariantsExtractor();
                annotation.accept(extractor, null);
                if (extractor.karth != null) {
                    return karthesianProduct(extractor.karth);
                }
            }
            return null;
        }

        static List<VariantValue> karthesianProduct(List<List<VariantValue>> base) {
            if (base.size() == 1) {
                return base.get(0);
            }
            List<VariantValue> acc = new ArrayList<>();
            accumulate(base, new ArrayList<>(), 0, acc);
            return acc;
        }

        private static void accumulate(
                List<List<VariantValue>> base,
                ArrayList<ReplacementValue> replacements,
                int idx,
                List<VariantValue> acc) {
            if (idx == base.size()) {
                if (!replacements.isEmpty()) {
                    acc.add(new VariantValue(new ArrayList<>(replacements)));
                }
                return;
            }
            accumulate(base, replacements, idx + 1, acc);
            for (VariantValue variant : base.get(idx)) {
                int prefix = replacements.size();
                replacements.addAll(variant.replacements());
                accumulate(base, replacements, idx + 1, acc);
                while (replacements.size() > prefix) {
                    replacements.remove(replacements.size() - 1);
                }
            }
        }

        @Override
        public String toString() {
            return replacements.stream().map(ReplacementValue::toString).collect(Collectors.joining(", "));
        }

        record ReplacementValue(String regex, String replacement) {
            @Override
            public String toString() {
                return "%s -> %s".formatted(regex, replacement);
            }
        }
    }

    record Modification(Node modified, VariantValue variant, Node original) {
        Node withDecorations() {
            if (modified instanceof NodeWithAnnotations<?> nwa) {
                nwa.getAnnotations()
                        .removeIf(expr -> expr.getName().toString().endsWith(Variants.class.getSimpleName())
                                || expr.getName().toString().endsWith(Variant.class.getSimpleName())
                                || expr.getName().toString().endsWith(VariantsKarthesianProduct.class.getSimpleName()));
                nwa.addAnnotation(new SingleMemberAnnotationExpr(
                        new Name(GeneratedVariant.class.getSimpleName()),
                        new StringLiteralExpr()
                                .setString("Variant of %s".formatted(((NodeWithSimpleName<?>) original).getName()))));
            }
            modified.setBlockComment(" %s ".formatted(variant.toString()));
            return modified;
        }
    }
}
