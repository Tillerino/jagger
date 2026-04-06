package org.tillerino.jagger.tests.base.features;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.IgnorePropertyModel.*;

class IgnorePropertyTest extends ReferenceTest {
    IgnorePropertySerde serde = SerdeUtil.impl(IgnorePropertySerde.class);

    @Test
    void fieldWithGetter() throws Exception {
        outputUtils.assertIsEqualToDatabind(new JsonIgnoreOnFieldWithGetter("x"), serde::writeFieldWithGetter);
    }

    @Test
    void fieldAndGetter() throws Exception {
        outputUtils.assertIsEqualToDatabind(new JsonIgnoreOnFieldAndGetter("x"), serde::writeFieldAndGetter);
    }

    @Test
    void fieldWithoutGetter() throws Exception {
        outputUtils.assertIsEqualToDatabind(new JsonIgnoreOnFieldWithoutGetter("x"), serde::writeFieldWithoutGetter);
    }

    @Test
    void getter() throws Exception {
        outputUtils.assertIsEqualToDatabind(new JsonIgnoreOnGetter("x"), serde::writeGetter);
    }

    @Test
    void recordComponent() throws Exception {
        outputUtils.assertIsEqualToDatabind(new JsonIgnoreOnRecordComponent("x"), serde::writeRecordComponent);
    }

    @Test
    void childInheritsParentIgnore() throws Exception {
        // The parentField should be ignored because @JsonIgnore is inherited from parent
        outputUtils.assertIsEqualToDatabind(
                new ChildInheritsParentIgnore("parentValue", "childValue"), serde::writeChildInheritsParentIgnore);
    }

    @Test
    void childImplementsParentInterface() throws Exception {
        // The parentField should be ignored because @JsonIgnore is inherited from parent interface
        outputUtils.assertIsEqualToDatabind(
                new ChildImplementsParentInterface("parentValue", "childValue"),
                serde::writeChildImplementsParentInterface);
    }

    @Test
    void childInheritsGrandparentIgnore() throws Exception {
        outputUtils.assertIsEqualToDatabind(
                new ChildInheritsGrandparentIgnore("foo"), serde::writeChildInheritsGrandparentIgnore);
    }
}
