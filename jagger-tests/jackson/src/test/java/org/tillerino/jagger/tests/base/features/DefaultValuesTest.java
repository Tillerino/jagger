package org.tillerino.jagger.tests.base.features;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.features.DefaultValuesModel.Mixed;

public class DefaultValuesTest extends ReferenceTest {
    DefaultValuesSerde serde = SerdeUtil.impl(DefaultValuesSerde.class);

    @Test
    void testAllMissing() throws Exception {
        Mixed deserialized = inputUtils.deserialize("{}", serde::read);
        Assertions.assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(new Mixed(
                        "foo",
                        42,
                        new double[] {1.0, 2.0},
                        AnEnum.ANOTHER_VALUE,
                        Arrays.asList("foo", "bar"),
                        new Mixed(null, 0, null, null, null, null)));
    }
}
