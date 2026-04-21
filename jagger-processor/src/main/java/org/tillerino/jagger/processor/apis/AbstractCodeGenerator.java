package org.tillerino.jagger.processor.apis;

import com.squareup.javapoet.CodeBlock;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.Snippet;
import org.tillerino.jagger.processor.Snippet.Flattened;
import org.tillerino.jagger.processor.Snippet.PerfectSnippet.TypedVariable;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

public class AbstractCodeGenerator<SELF extends AbstractCodeGenerator<SELF>> {
    protected final CodeBlock.Builder code;
    protected final Stack<Set<String>> variables;

    public AbstractCodeGenerator(InstantiatedMethod method) {
        this.code = CodeBlock.builder();
        this.variables = new Stack<>();
        LinkedHashSet<String> rootVariables = new LinkedHashSet<>();
        for (InstantiatedVariable parameter : method.parameters()) {
            rootVariables.add(parameter.name());
        }
        this.variables.push(rootVariables);
    }

    public AbstractCodeGenerator(AbstractCodeGenerator<SELF> parent) {
        this.code = parent.code;
        this.variables = parent.variables;
    }

    protected AbstractCodeGenerator<SELF> addStatement(Snippet s) {
        Flattened f = s.flatten();
        code.addStatement(f.format(), f.args());
        return this;
    }

    protected AbstractCodeGenerator<SELF> addStatement(String format, Object... args) {
        return addStatement(Snippet.of(format, args));
    }

    protected NullaryControlFlowScope beginControlFlow(Snippet s) {
        Flattened f = s.flatten();
        code.beginControlFlow(f.format(), f.args());
        variables.push(new LinkedHashSet<>(variables.peek()));
        return new NullaryControlFlowScope(this);
    }

    protected NullaryControlFlowScope beginControlFlow(String controlFlow, Object... args) {
        return beginControlFlow(Snippet.of(controlFlow, args));
    }

    protected NullaryControlFlowScope nextControlFlow(Snippet s) {
        Flattened f = s.flatten();
        popVariablesStack();
        pushVariablesStack(f);
        return new NullaryControlFlowScope(this);
    }

    protected NullaryControlFlowScope nextControlFlow(String controlFlow, Object... args) {
        return nextControlFlow(Snippet.of(controlFlow, args));
    }

    protected AbstractCodeGenerator<SELF> endControlFlow() {
        popVariablesStack();
        code.endControlFlow();
        return this;
    }

    protected void pushVariablesStack(Flattened f) {
        code.nextControlFlow(f.format(), f.args());
        variables.push(new LinkedHashSet<>(variables.peek()));
    }

    protected void popVariablesStack() {
        variables.pop();
        assert !variables.isEmpty();
    }

    protected ScopedVar createVariable(String name) {
        if (variables.peek().add(name)) {
            return new ScopedVar(name);
        }
        int suf = 2;
        while (!variables.peek().add(name + suf)) {
            suf++;
        }
        return new ScopedVar(name + suf);
    }

    protected record ScopedVar(String name) implements Snippet {
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
