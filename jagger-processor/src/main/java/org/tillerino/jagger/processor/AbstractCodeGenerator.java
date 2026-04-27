package org.tillerino.jagger.processor;

import com.squareup.javapoet.CodeBlock;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.Snippet.Flattened;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeKind.CodeGeneratorContext;

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
        for (InstantiatedVariable parameter : generatorContext.prototype().instantiatedParameters()) {
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

    public AbstractCodeGenerator<SELF> addStatement(Snippet s) {
        Flattened f = s.flatten();
        code.addStatement(f.format(), f.args());
        return this;
    }

    public AbstractCodeGenerator<SELF> addStatement(String format, Object... args) {
        return addStatement(Snippet.of(format, args));
    }

    public NullaryControlFlowScope beginControlFlow(Snippet s) {
        Flattened f = s.flatten();
        code.beginControlFlow(f.format(), f.args());
        variables.push(new LinkedHashSet<>(variables.peek()));
        return new NullaryControlFlowScope(this);
    }

    public NullaryControlFlowScope beginControlFlow(String controlFlow, Object... args) {
        return beginControlFlow(Snippet.of(controlFlow, args));
    }

    public NullaryControlFlowScope nextControlFlow(Snippet s) {
        Flattened f = s.flatten();
        popVariablesStack();
        pushVariablesStack(f);
        return new NullaryControlFlowScope(this);
    }

    public NullaryControlFlowScope nextControlFlow(String controlFlow, Object... args) {
        return nextControlFlow(Snippet.of(controlFlow, args));
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

    public ScopedVar createVariable(String name) {
        if (variables.peek().add(name)) {
            return new ScopedVar(name);
        }
        int suf = 2;
        while (!variables.peek().add(name + suf)) {
            suf++;
        }
        return new ScopedVar(name + suf);
    }

    /**
     * @param parameters not including the arrow, must include parentheses if multiple args.
     * @param body body-generating code - indented with a nested variable scope
     * @param afterBody e.g. {@code ;\n}
     */
    public void lambda(Snippet parameters, Runnable body, Snippet afterBody) {
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

    public record ScopedVar(String name) implements Snippet {
        @Override
        public Flattened flatten() {
            return Flattened.of("$L", name());
        }

        public TypedVariable withType(TypeMirror type) {
            return new TypedVariable(type, name);
        }
    }

    public static class NullaryControlFlowScope {
        final AbstractCodeGenerator<?> generator;

        public NullaryControlFlowScope(AbstractCodeGenerator<?> generator) {
            this.generator = generator;
        }

        public void withBody(Runnable r) {
            r.run();
            generator.endControlFlow();
        }

        public <T> UnaryControlFlowScope<T> withPayload(T payload) {
            return new UnaryControlFlowScope<>(generator, payload);
        }
    }

    public static class UnaryControlFlowScope<T> {
        final AbstractCodeGenerator<?> generator;
        final T argument;

        public UnaryControlFlowScope(AbstractCodeGenerator<?> generator, T argument) {
            this.generator = generator;
            this.argument = argument;
        }

        public void withBody(Consumer<T> c) {
            c.accept(argument);
            generator.endControlFlow();
        }
    }
}
