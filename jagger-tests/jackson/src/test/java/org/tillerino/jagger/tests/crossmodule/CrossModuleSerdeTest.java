package org.tillerino.jagger.tests.crossmodule;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.crossmodule.CrossModuleSerde.UsesSiblingSerde;
import org.tillerino.jagger.tests.sibling.SiblingSerde.SiblingRecord;
import tools.jackson.core.type.TypeReference;

class CrossModuleSerdeTest extends ReferenceTest {
    @Test
    void roundTrip() throws Exception {
        UsesSiblingSerde serde = SerdeUtil.impl(UsesSiblingSerde.class);

        UsesSiblingSerde.Wrapper wrapper = new UsesSiblingSerde.Wrapper(new SiblingRecord("value"), "name");

        outputUtils.roundTrip(
                wrapper, serde::writeWrapper, serde::readWrapper, new TypeReference<UsesSiblingSerde.Wrapper>() {});
    }
}
