package org.tillerino.jagger.tests;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.Indentation.IndentType;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.javaparser.utils.SourceRoot;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;

/**
 * We only write all tests for {@link com.fasterxml.jackson.core.JsonGenerator} and
 * {@link com.fasterxml.jackson.core.JsonParser}. These tests are then translated for other libraries.
 */
public class CopyTests {
    private static final String originalPackage = "org.tillerino.jagger.tests.base";
    private static final PrinterConfiguration configuration = new DefaultPrinterConfiguration()
            .addOption(new DefaultConfigurationOption(ConfigOption.INDENTATION, new Indentation(IndentType.TABS, 1)));
    private static final DefaultPrettyPrinter printer = new DefaultPrettyPrinter(configuration);

    public static void main(String[] args) throws Exception {
        Path targetRoot = Paths.get(args[0]);
        String targetPackage = args[1];
        String writer = args[2];
        String reader = args[3];
        WriterMode writerMode = WriterMode.valueOf(args[4]);
        Set<String> features =
                Stream.of(args[5].split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        copy(targetRoot, targetPackage, writer, reader, Map.of(), writerMode, features);
    }

    /**
     * Copies all the original tests to a new package, replacing the writer and reader classes.
     *
     * @param targetRoot if null, same as original
     * @param targetPackage e.g. "org.tillerino.jagger.tests.alt.gson"
     * @param writer class of the writer, e.g. {@link JsonWriter}
     * @param reader class of the reader, e.g. {@link JsonReader}
     * @param methodReplacements replacements for the methods in InputUtils and OutputUtils
     * @param writerMode see {@link WriterMode}
     * @param features features required by the implementation
     */
    public static void copy(
            Path targetRoot,
            String targetPackage,
            String writer,
            String reader,
            Map<String, String> methodReplacements,
            WriterMode writerMode,
            Set<String> features)
            throws IOException {
        for (String p : List.of("src/main/java", "src/test/java")) {
            SourceRoot sourceRoot = new SourceRoot(targetRoot.resolve("../jackson/" + p));
            sourceRoot.setParserConfiguration(
                    new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));

            for (ParseResult<CompilationUnit> parseResult : sourceRoot.tryToParse(originalPackage)) {
                CompilationUnit cu = parseResult
                        .getResult()
                        .orElseThrow(() -> new RuntimeException("failed to parse" + parseResult.getProblems()));

                AtomicReference<String> packageBefore = new AtomicReference<>();

                cu.accept(
                        new ModifierVisitor<Void>() {
                            @Override
                            public Visitable visit(ClassOrInterfaceDeclaration n, Void arg) {
                                if (packageBefore.get() != null
                                        && n.getParentNode()
                                                .filter(CompilationUnit.class::isInstance)
                                                .isPresent()) {
                                    n.setComment(new BlockComment(
                                            "\n" + StringUtils.repeat(StringUtils.repeat("COPY", " ", 15), "\n", 5)
                                                    + "\n" + "\n"
                                                    + "   This file was copied from "
                                                    + packageBefore.get() + ".\n" + "   This was performed by "
                                                    + CopyTests.class.getCanonicalName() + ".\n"
                                                    + "   Do not modify by hand. Run CopyTests to update this file.\n"
                                                    + "\n"
                                                    + StringUtils.repeat(StringUtils.repeat("COPY", " ", 15), "\n", 5)
                                                    + "\n"));
                                }
                                n.getChildNodes().stream()
                                        .filter(node -> node.getComment()
                                                .filter(c -> preventCopy(c, features))
                                                .isPresent())
                                        .toList()
                                        .forEach(n::remove);
                                return super.visit(n, arg);
                            }

                            private static boolean preventCopy(Comment c, Set<String> features) {
                                Matcher m = Pattern.compile("FEATURE-(\\w+)").matcher(c.getContent());
                                while (m.find()) {
                                    if (!features.contains(m.group(1))) {
                                        return true;
                                    }
                                }
                                return c.getContent().contains("NOCOPY");
                            }

                            @Override
                            public Visitable visit(PackageDeclaration n, Void arg) {
                                packageBefore.set(n.getNameAsString());
                                n.setName(n.getName().asString().replace(originalPackage, targetPackage));
                                String targetRel =
                                        switch (p) {
                                            case "src/main/java" -> "target/generated-sources/copied";
                                            case "src/test/java" -> "target/generated-test-sources/copied";
                                            default -> throw new AssertionError();
                                        };
                                cu.setStorage(fileName(
                                        targetRoot.resolve(targetRel),
                                        n.getNameAsString(),
                                        cu.getPrimaryTypeName().get()));
                                return super.visit(n, arg);
                            }

                            @Override
                            public Node visit(ImportDeclaration n, Void arg) {
                                if (n.getNameAsString().equals("com.fasterxml.jackson.core.JsonGenerator")) {
                                    n.setName(writer);
                                } else if (n.getNameAsString().equals("com.fasterxml.jackson.core.JsonParser")) {
                                    n.setName(reader);
                                } else if (p.endsWith("src/main/java")
                                        && n.getNameAsString().startsWith("com.fasterxml.jackson.core")
                                        && !writer.startsWith("com.fasterxml.jackson")) {
                                    return null;
                                } else {
                                    n.setName(n.getName().asString().replace(originalPackage, targetPackage));
                                    // for static imports
                                    methodReplacements.forEach((o, r) ->
                                            n.setName(n.getName().asString().replace(o, r)));
                                }
                                return super.visit(n, arg);
                            }

                            @Override
                            public Visitable visit(MethodCallExpr n, Void arg) {
                                String replacement = methodReplacements.get(n.getNameAsString());
                                if (replacement != null) {
                                    n.setName(replacement);
                                }
                                return super.visit(n, arg);
                            }

                            @Override
                            public Visitable visit(MethodDeclaration n, Void arg) {
                                if (n.getAnnotationByClass(JsonInput.class).isPresent()) {
                                    for (Parameter parameter : n.getParameters()) {
                                        if (parameter.getType().toString().equals("JsonParser")) {
                                            parameter.setType(reader);
                                        }
                                    }
                                }

                                if (n.getAnnotationByClass(JsonOutput.class).isPresent()) {
                                    if (writerMode == WriterMode.RETURN) {
                                        n.setType(writer);
                                        n.getBody().ifPresent(body -> {
                                            body.accept(
                                                    new ModifierVisitor<Void>() {
                                                        @Override
                                                        public Visitable visit(ExpressionStmt n, Void arg) {
                                                            return statementReplacement(n)
                                                                    .orElse(n);
                                                        }
                                                    },
                                                    null);
                                        });
                                        NodeList<Parameter> parameters = n.getParameters();
                                        for (int i = 0; i < parameters.size(); i++) {
                                            Parameter parameter = parameters.get(i);
                                            if (parameter.getType().toString().equals("JsonGenerator")) {
                                                parameters.remove(i);
                                                break;
                                            }
                                        }
                                    } else {
                                        for (Parameter parameter : n.getParameters()) {
                                            if (parameter.getType().toString().equals("JsonGenerator")) {
                                                parameter.setType(writer);
                                            }
                                        }
                                    }
                                }
                                return super.visit(n, arg);
                            }

                            private static Optional<Statement> statementReplacement(Statement statement) {
                                String prefix = "WriterMode.RETURN:";
                                Optional<Statement> replacement = statement
                                        .getComment()
                                        .map(c -> c.getContent().trim())
                                        .filter(c -> c.startsWith(prefix))
                                        .map(c -> c.substring(prefix.length()))
                                        .map(StaticJavaParser::parseStatement);
                                return replacement;
                            }
                        },
                        null);
                cu.getStorage().get().save(printer::print);
            }
        }
    }

    static Path fileName(Path targetRoot, String targetPackage, String className) {
        for (String s : targetPackage.split("\\.")) {
            targetRoot = targetRoot.resolve(s);
        }
        return targetRoot.resolve(className + ".java");
    }

    enum WriterMode {
        ARGUMENT,
        RETURN
    }
}
