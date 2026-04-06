package org.tillerino.jagger.tests.base.cases;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.cases.InjectionSerde.InjectionSerializationContext;
import org.tillerino.jagger.tests.model.features.DelegationModel.SelfReferencingRecord;

/** This test demonstrates how to hack injection into the serialization process. */
class InjectionTest extends ReferenceTest {
    InjectionSerde serde = SerdeUtil.impl(InjectionSerde.class);

    SelfReferencingRecord b = new SelfReferencingRecord("b", null);
    SelfReferencingRecord a = new SelfReferencingRecord("a", b);

    List<SelfReferencingRecord> list = List.of(b, a, b, a);

    /** FEATURE-JSON */
    String json = """
		[{"prop":"b","self":null},{"prop":"a","self":"b"},"b","a"]
		""";

    /** FEATURE-JSON */
    @Test
    void testInjectionOutput() throws Exception {
        assertThatJson(outputUtils.serialize2(list, new InjectionSerializationContext(), serde::writeList))
                .isEqualTo(json);
    }

    /**
     * NOCOPY: The output code can be automatically copied, the input code cannot. Not much to prove anyway, since this
     * is just a demo.
     */
    @Test
    void testInjectionInput() throws Exception {
        List<SelfReferencingRecord> records = inputUtils.withJsonParser(
                json, parser -> serde.readList(parser, new InjectionSerde.InjectionDeserializationContext()));
        assertThat(records).isEqualTo(list);
        assertThat(records.get(1).self()).isSameAs(records.get(0));
        assertThat(records.get(2)).isSameAs(records.get(0));
        assertThat(records.get(3)).isSameAs(records.get(1));
    }
}
