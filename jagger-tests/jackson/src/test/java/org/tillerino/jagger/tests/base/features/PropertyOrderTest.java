package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.AlphabeticOrderedProperties;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.ExplicitZThenAlphabetic;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.ExplicitZThenDeclaration;
import org.tillerino.jagger.tests.model.features.PropertyOrderModel.OrderedProperties;

class PropertyOrderTest extends ReferenceTest {
    PropertyOrderSerde serde = SerdeUtil.impl(PropertyOrderSerde.class);

    @Test
    void orderedProperties() throws Exception {
        outputUtils.roundTrip(
                new OrderedProperties("a", 1, "b"),
                serde::writeOrderedProperties,
                serde::readOrderedProperties,
                new TypeReference<>() {});
    }

    @Test
    void alphabeticOrderedProperties() throws Exception {
        outputUtils.roundTrip(
                new AlphabeticOrderedProperties("z", "a", "b"),
                serde::writeAlphabeticOrderedProperties,
                serde::readAlphabeticOrderedProperties,
                new TypeReference<>() {});
    }

    @Test
    void explicitZThenAlphabetic() throws Exception {
        outputUtils.roundTrip(
                new ExplicitZThenAlphabetic("c", "z", "b"),
                serde::writeExplicitZThenAlphabetic,
                serde::readExplicitZThenAlphabetic,
                new TypeReference<>() {});
    }

    @Test
    void explicitZThenDeclaration() throws Exception {
        outputUtils.roundTrip(
                new ExplicitZThenDeclaration("c", "z", "b"),
                serde::writeExplicitZThenDeclaration,
                serde::readExplicitZThenDeclaration,
                new TypeReference<>() {});
    }
}
