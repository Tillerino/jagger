package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.features.PolymorphismSerde.WithDelegate;
import org.tillerino.jagger.tests.model.features.PolymorphismModel.*;

class PolymorphismTest extends ReferenceTest {
    PolymorphismSerde serde = SerdeUtil.impl(PolymorphismSerde.class);

    WithDelegate withDelegate = SerdeUtil.impl(WithDelegate.class);

    @Test
    void sealedInterfaceDefaultsOutput() throws Exception {
        outputUtils.assertIsEqualToDatabind(new RecordOne("abc"), serde::writePolymorphism);
        outputUtils.assertIsEqualToDatabind(new RecordTwo(123), serde::writePolymorphism);
    }

    @Test
    void sealedInterfaceWithDelegatorDefaultsOutput() throws Exception {
        outputUtils.assertIsEqualToDatabind(new RecordOne("abc"), withDelegate::writePolymorphism);
        outputUtils.assertIsEqualToDatabind(new RecordTwo(123), withDelegate::writePolymorphism);
    }

    @Test
    void sealedInterfaceDefaultsInput() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{\"@c\": \".PolymorphismModel$RecordOne\", \"s\":\"abc\"}",
                serde::readPolymorphism,
                new TypeReference<>() {});
        inputUtils.assertIsEqualToDatabind(
                "{\"@c\": \".PolymorphismModel$RecordTwo\", \"i\": 123}",
                serde::readPolymorphism,
                new TypeReference<>() {});
    }

    @Test
    void sealedInterfaceWithDelefatorDefaultsInput() throws Exception {
        inputUtils.assertIsEqualToDatabind(
                "{\"@c\": \".PolymorphismModel$RecordOne\", \"s\":\"abc\"}",
                withDelegate::readPolymorphism,
                new TypeReference<>() {});
        inputUtils.assertIsEqualToDatabind(
                "{\"@c\": \".PolymorphismModel$RecordTwo\", \"i\": 123}",
                withDelegate::readPolymorphism,
                new TypeReference<>() {});
    }

    @Nested
    class JsonTypeInfoAndJsonSubTypesTest {
        PolymorphismSerde.JsonTypeInfoAndJsonSubTypesSerde serde =
                SerdeUtil.impl(PolymorphismSerde.JsonTypeInfoAndJsonSubTypesSerde.class);

        // minimal class is used in our basic polymorphism tests

        @Test
        void useClass() throws Exception {
            for (var record : List.of(new JsonTypeInfoUseClass.RecordOne("x"), new JsonTypeInfoUseClass.RecordTwo(1))) {
                outputUtils.roundTrip(record, serde::writeUseClass, serde::readUseClass, new TypeReference<>() {});
            }
        }

        @Test
        void useName() throws Exception {
            for (var record : List.of(new JsonTypeInfoUseName.RecordOne("x"), new JsonTypeInfoUseName.RecordTwo(1))) {
                outputUtils.roundTrip(record, serde::writeUseName, serde::readUseName, new TypeReference<>() {});
            }
        }

        @Test
        void useSimpleName() throws Exception {
            for (var record :
                    List.of(new JsonTypeInfoUseSimpleName.RecordOne("x"), new JsonTypeInfoUseSimpleName.RecordTwo(1))) {
                outputUtils.roundTrip(
                        record, serde::writeUseSimpleName, serde::readUseSimpleName, new TypeReference<>() {});
            }
        }
    }
}
