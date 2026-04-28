package org.tillerino.jagger.tests.plugins;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonConfig.ImplementationMode;
import org.tillerino.jagger.annotations.JsonTemplate;
import org.tillerino.jagger.tests.plugins.DeepClonePlugin.Clone;

class DeepClonePluginTest {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Person {
        private String name;
        private int age;
        private Address address;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {
        private String street;
        private Integer zipCode;
        private City city;
        private boolean isValid;
    }

    public enum City {
        JAGGINSTON,
    }

    @JsonConfig(implement = ImplementationMode.DO_IMPLEMENT)
    @JsonTemplate(
            templates = {ClonerTemplate.class},
            types = {Person.class, Address.class})
    public interface Cloner {}

    @JsonConfig(implement = ImplementationMode.DO_NOT_IMPLEMENT)
    public interface ClonerTemplate<T> {
        @Clone
        T clone(T t);
    }

    DeepClonePluginTest$ClonerImpl cloner = new DeepClonePluginTest$ClonerImpl();

    @Test
    void deepCloneCreatesEqualButIndependentCopy() {
        Address originalAddress = new DeepClonePluginTest.Address("123 Main St", 45467, City.JAGGINSTON, true);

        Person original = new Person("Alice", 30, originalAddress);

        Person cloned = cloner.clonePerson(original);

        Assertions.assertThat(cloned).isEqualTo(original);
        Assertions.assertThat(cloned).isNotSameAs(original);
        Assertions.assertThat(cloned.getAddress()).isNotSameAs(original.getAddress());
    }
}
