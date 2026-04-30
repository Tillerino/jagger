package org.tillerino.jagger.tests.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.plugins.RequiredFieldPlugin.RequireField;

class RequiredFieldPluginTest {
    @RequireField(ClassLoader.class)
    interface RequireFieldOn {
        @RequireField
        void foo();
    }

    @Test
    void hasRequiredField() {
        RequireFieldOn impl =
                new RequiredFieldPluginTest$RequireFieldOnImpl(getClass().getClassLoader());
        assertThat(impl).hasFieldOrPropertyWithValue("classLoader", getClass().getClassLoader());
    }
}
