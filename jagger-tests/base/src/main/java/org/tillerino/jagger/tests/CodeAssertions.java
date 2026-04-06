package org.tillerino.jagger.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.function.Failable;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

public class CodeAssertions {
    private static final Map<String, CompileUnitAssert> compilationUnitCache = new LinkedHashMap<>();

    public static CompileUnitAssert assertThatCode(Class<?> clazz) throws Exception {
        if (!compilationUnitCache.containsKey(clazz.getCanonicalName())) {
            compilationUnitCache.put(clazz.getCanonicalName(), parseClass(clazz));
        }
        return compilationUnitCache.get(clazz.getCanonicalName());
    }

    public static CompileUnitAssert assertThatImpl(Class<?> clazz) throws Exception {
        return assertThatCode(SerdeUtil.implClass(clazz));
    }

    private static CompileUnitAssert parseClass(Class<?> clazz) throws Exception {
        SourceRoot sourceRoot = new SourceRoot(
                CodeGenerationUtils.mavenModuleRoot(clazz).resolve("target/generated-sources/annotations"));
        sourceRoot.setParserConfiguration(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8));
        ParseResult<CompilationUnit> result =
                sourceRoot.tryToParse(clazz.getPackageName(), clazz.getSimpleName() + ".java");
        CompilationUnit compilationUnit = result.getResult().orElseGet(() -> {
            throw Failable.rethrow(result.getProblem(0).getCause().get());
        });
        CompilationUnit.Storage storage = compilationUnit
                .getStorage()
                .orElseThrow(() -> new AssertionError("No storage in " + clazz.getCanonicalName()));
        TypeDeclaration<?> primaryType = compilationUnit
                .getPrimaryType()
                .orElseThrow(() -> new AssertionError("No primary type in " + storage.getFileName()));
        return new CompileUnitAssert(compilationUnit, primaryType, storage);
    }

    public record CompileUnitAssert(
            CompilationUnit cu, TypeDeclaration<?> primaryType, CompilationUnit.Storage storage) {
        public MethodAssert method(String methodName) {
            List<MethodDeclaration> methods = primaryType.getMethodsByName(methodName);
            Assertions.assertThat(methods)
                    .as("Methods named %s in %s", methodName, storage.getFileName())
                    .hasSize(1);
            return new MethodAssert(this, methods.get(0));
        }

        public ListAssert<MethodDeclaration> methods() {
            return assertThat(primaryType.getMethods());
        }
    }

    public record MethodAssert(CompileUnitAssert cu, MethodDeclaration decl) {
        public MethodAssert calls(String name) {
            allCalls().contains(name);
            return this;
        }

        private AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> allCalls() {
            BlockStmt blockStmt =
                    decl.getBody().orElseThrow(() -> new AssertionError("No body in " + decl.getNameAsString()));
            return Assertions.assertThat(blockStmt.findAll(MethodCallExpr.class))
                    .extracting(MethodCallExpr::getNameAsString);
        }

        public MethodAssert doesNotCall(String name) {
            allCalls().doesNotContain(name);
            return this;
        }

        public MethodAssert bodyContains(String code) {
            assertThat(decl.getBody()
                            .orElseThrow(() -> new AssertionError("No body in " + decl.getNameAsString()))
                            .toString())
                    .contains(code);
            return this;
        }

        public MethodAssert references(String name) {
            allReferences().contains(name);
            return this;
        }

        private AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> allReferences() {
            BlockStmt blockStmt =
                    decl.getBody().orElseThrow(() -> new AssertionError("No body in " + decl.getNameAsString()));
            return assertThat(blockStmt.findAll(MethodReferenceExpr.class))
                    .extracting(MethodReferenceExpr::getIdentifier);
        }
    }
}
