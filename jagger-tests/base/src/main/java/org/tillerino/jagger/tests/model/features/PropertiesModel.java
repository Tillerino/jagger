package org.tillerino.jagger.tests.model.features;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

public interface PropertiesModel {
    @EqualsAndHashCode
    class Parent {
        public String parentField;

        @Getter
        @Setter
        private String parentProp;
    }

    @EqualsAndHashCode(callSuper = true)
    class Child extends Parent {
        public String childField;

        @Getter
        @Setter
        private String childProp;
    }
}
