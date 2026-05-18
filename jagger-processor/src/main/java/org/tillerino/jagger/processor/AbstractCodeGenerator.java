package org.tillerino.jagger.processor;

import static org.tillerino.jagger.processor.util.Code.c;

import com.squareup.javapoet.CodeBlock;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.ext.PrototypeKind.CodeGeneratorContext;
import org.tillerino.jagger.processor.util.Code;
import org.tillerino.jagger.processor.util.Code.Flattened;
import org.tillerino.jagger.processor.util.Expr.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class AbstractCodeGenerator<SELF extends AbstractCodeGenerator<SELF>> {
    protected final CodeBlock.Builder code;
    protected final Stack<Set<String>> variables;
    protected final JaggerContext ctx;
    protected final GeneratedClass generatedClass;
    protected final JaggerPrototype prototype;

    public AbstractCodeGenerator(CodeGeneratorContext generatorContext) {
        this.ctx = generatorContext.ctx();
        this.generatedClass = Objects.requireNonNull(generatorContext.generatedClass());
        this.prototype = generatorContext.prototype();
        this.variables = new Stack<>();
        LinkedHashSet<String> rootVariables = new LinkedHashSet<>();
        for (InstantiatedVariable parameter : generatorContext.prototype().parameters()) {
            rootVariables.add(parameter.name());
        }
        this.variables.push(rootVariables);
        this.code = CodeBlock.builder();
    }

    public AbstractCodeGenerator(AbstractCodeGenerator<SELF> parent) {
        this.code = parent.code;
        this.variables = parent.variables;
        this.ctx = parent.ctx;
        this.generatedClass = Objects.requireNonNull(parent.generatedClass);
        this.prototype = parent.prototype;
    }

    public AbstractCodeGenerator<SELF> addStatement(Code s) {
        Flattened f = s.flatten();
        code.addStatement(f.format(), f.args());
        return this;
    }

    public AbstractCodeGenerator<SELF> addStatement(String format, Object... args) {
        return addStatement(c(format, args));
    }

    public NullaryControlFlowScope beginControlFlow(Code s) {
        Flattened f = s.flatten();
        code.beginControlFlow(f.format(), f.args());
        variables.push(new LinkedHashSet<>(variables.peek()));
        return new NullaryControlFlowScope(this::endControlFlow);
    }

    public NullaryControlFlowScope beginControlFlow(String controlFlow, Object... args) {
        return beginControlFlow(c(controlFlow, args));
    }

    public NullaryControlFlowScope nextControlFlow(Code s) {
        Flattened f = s.flatten();
        popVariablesStack();
        pushVariablesStack(f);
        return new NullaryControlFlowScope(this::endControlFlow);
    }

    public NullaryControlFlowScope nextControlFlow(String controlFlow, Object... args) {
        return nextControlFlow(c(controlFlow, args));
    }

    public AbstractCodeGenerator<SELF> endControlFlow() {
        popVariablesStack();
        code.endControlFlow();
        return this;
    }

    public void pushVariablesStack(Flattened f) {
        code.nextControlFlow(f.format(), f.args());
        variables.push(new LinkedHashSet<>(variables.peek()));
    }

    public void popVariablesStack() {
        variables.pop();
        assert !variables.isEmpty();
    }

    public TypedVariable createVariable(TypeMirror t, String name) {
        if (variables.peek().add(name)) {
            return new TypedVariable(t, name);
        }
        int suf = 2;
        while (!variables.peek().add(name + suf)) {
            suf++;
        }
        return new TypedVariable(t, name + suf);
    }

    /**
     * @param parameters not including the arrow, must include parentheses if multiple args.
     * @param body body-generating code - indented with a nested variable scope
     * @param afterBody e.g. {@code ;\n}
     */
    public void lambda(Code parameters, Runnable body, Code afterBody) {
        Flattened f = parameters.flatten();
        code.add(f.format() + " -> {\n", f.args());

        code.indent();
        variables.push(new LinkedHashSet<>(variables.peek()));
        body.run();
        code.unindent();
        popVariablesStack();

        Flattened g = afterBody.flatten();
        code.add("}" + g.format(), g.args());
    }

    public static class NullaryControlFlowScope {
        private final Runnable afterBody;

        public NullaryControlFlowScope(Runnable afterBody) {
            this.afterBody = afterBody;
        }

        public void withBody(Runnable r) {
            r.run();
            afterBody.run();
        }

        public <T> UnaryControlFlowScope<T> withPayload(T payload) {
            return new UnaryControlFlowScope<>(afterBody, payload);
        }
    }

    public static class UnaryControlFlowScope<T> {
        private final Runnable afterBody;
        final T argument;

        public UnaryControlFlowScope(Runnable afterBody, T argument) {
            this.afterBody = afterBody;
            this.argument = argument;
        }

        public void withBody(Consumer<T> c) {
            c.accept(argument);
            afterBody.run();
        }
    }
}
