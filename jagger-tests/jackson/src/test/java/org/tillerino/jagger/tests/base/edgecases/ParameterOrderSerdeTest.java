package org.tillerino.jagger.tests.base.edgecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.tests.CodeAssertions.CompileUnitAssert;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;

class ParameterOrderSerdeTest extends ReferenceTest {
    ParameterOrderSerde serde = SerdeUtil.impl(ParameterOrderSerde.class);

    @Test
    void roundtrip() throws Exception {
        Map<String, Integer> map = Map.of("one", 1, "two", 2);

        String json =
                outputUtils.withWriter(gen -> serde.writeStringIntMap(gen, map, new SerializationContext(), List.of()));

        Map<String, Integer> re = inputUtils.withReader(
                json, parser -> serde.readStringIntMap(new ArrayDeque<>(), parser, new DeserializationContext()));

        assertThat(re).isEqualTo(map);
    }

    @Test
    void assertCalls() throws Exception {
        CompileUnitAssert impl = assertThatImpl(ParameterOrderSerde.class);

        impl.method("writeStringIntMap").calls("writeInt");
        impl.method("readStringIntMap").calls("readInt");
    }
}
