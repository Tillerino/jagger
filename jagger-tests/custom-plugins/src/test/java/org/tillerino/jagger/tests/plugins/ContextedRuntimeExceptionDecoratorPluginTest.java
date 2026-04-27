package org.tillerino.jagger.tests.plugins;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.plugins.ContextedRuntimeExceptionDecoratorPlugin.AddContext;

class ContextedRuntimeExceptionDecoratorPluginTest {
    static class ToDecorate {
        @AddContext
        public String iCall(String shortParameter) {
            return iFall("foo: " + shortParameter);
        }

        @AddContext
        public String iFall(String fullParameter) {
            throw new ContextedRuntimeException("Oh no! I fell :(");
        }
    }

    ToDecorate impl = SerdeUtil.impl(ToDecorate.class);

    @Test
    void decorationWorks() {
        Assertions.assertThatThrownBy(() -> impl.iCall("bar")).hasMessage("""
                    Oh no! I fell :(
                    Exception Context:
                    \t[1:fullParameter=foo: bar]
                    \t[2:method=iFall]
                    \t[3:shortParameter=bar]
                    \t[4:method=iCall]
                    ---------------------------------""");
    }
}
