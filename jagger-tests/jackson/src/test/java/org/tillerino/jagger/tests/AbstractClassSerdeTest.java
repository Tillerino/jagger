package org.tillerino.jagger.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractClassSerdeTest {
    @Test
    void implementsSuper() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        AbstractClassSerde s = new AbstractClassSerdeImpl(cl);
        assertThat(s.cl).isSameAs(cl);
    }
}
