package org.tillerino.jagger.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ConstructorCases.DelegatesToAbstractClassSerde;

class ConstructorCasesTest {
    @Test
    void implementsSuper() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ConstructorCases.AbstractClassSerde layer1 = new ConstructorCases$AbstractClassSerdeImpl(cl);
        assertThat(layer1.cl).isSameAs(cl);

        // This layer2 only generates an arg-constructor because layer1 needs one
        DelegatesToAbstractClassSerde layer2 = new ConstructorCases$DelegatesToAbstractClassSerdeImpl(layer1);
        assertThat(layer2).extracting("abstractClassSerde$0$delegate").isSameAs(layer1);
    }

    @Test
    void dependOnEachOtherNoargs() {
        new ConstructorCases$MutualNoArgAImpl(null);
    }
}
