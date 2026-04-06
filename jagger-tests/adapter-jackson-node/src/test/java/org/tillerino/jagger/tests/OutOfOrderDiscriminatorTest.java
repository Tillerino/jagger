package org.tillerino.jagger.tests;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.alt.adapter.jsonnode.features.PolymorphismSerde;
import org.tillerino.jagger.tests.model.features.PolymorphismModel.RecordOne;

class OutOfOrderDiscriminatorTest extends ReferenceTest {
    PolymorphismSerde serde = SerdeUtil.impl(PolymorphismSerde.class);

    /**
     * This tests a special case that we can only handle with {@link com.fasterxml.jackson.databind.JsonNode}. The
     * discriminator is NOT the first field in the object.
     */
    @Test
    void outOfOrderDiscriminator() throws Exception {
        Assertions.assertThat(inputUtils.deserialize(
                        "{\"s\":\"abc\", \"@c\": \".PolymorphismModel$RecordOne\"}", serde::readPolymorphism))
                .isInstanceOf(RecordOne.class);
    }
}
