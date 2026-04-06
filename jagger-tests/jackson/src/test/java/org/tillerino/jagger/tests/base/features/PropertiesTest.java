package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.PropertiesModel.Child;

public class PropertiesTest extends ReferenceTest {
    PropertiesSerde serde = SerdeUtil.impl(PropertiesSerde.class);

    @Test
    public void inheritanceRoundtrip() throws Exception {
        Child child = new Child();
        child.childField = "cf";
        child.setChildProp("cp");
        child.parentField = "pf";
        child.setParentProp("pp");

        outputUtils.roundTrip(child, serde::writeChild, serde::readChild, new TypeReference<>() {});
    }
}
