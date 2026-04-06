package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IgnorePropertyModel {
    class JsonIgnoreOnFieldWithGetter {
        @JsonIgnore
        private String s;

        public JsonIgnoreOnFieldWithGetter(String s) {
            this.s = s;
        }

        public String getS() {
            return s;
        }
    }

    /** Field annotation takes precedence. */
    class JsonIgnoreOnFieldAndGetter {
        @JsonIgnore
        private String s;

        public JsonIgnoreOnFieldAndGetter(String s) {
            this.s = s;
        }

        @JsonIgnore(false)
        public String getS() {
            return s;
        }
    }

    class JsonIgnoreOnFieldWithoutGetter {
        @JsonIgnore
        public String s;

        public JsonIgnoreOnFieldWithoutGetter(String s) {
            this.s = s;
        }
    }

    class JsonIgnoreOnGetter {
        private String s;

        @JsonIgnore
        public String getS() {
            return s;
        }

        public JsonIgnoreOnGetter(String s) {
            this.s = s;
        }
    }

    record JsonIgnoreOnRecordComponent(@JsonIgnore String s) {}

    /** Test JsonIgnore inheritance from parent to child class */
    class ParentWithIgnoredField {
        @JsonIgnore
        private String parentField;

        public ParentWithIgnoredField(String parentField) {
            this.parentField = parentField;
        }

        public String getParentField() {
            return parentField;
        }
    }

    class ChildInheritsParentIgnore extends ParentWithIgnoredField {
        private String childField;

        public ChildInheritsParentIgnore(String parentField, String childField) {
            super(parentField);
            this.childField = childField;
        }

        public String getChildField() {
            return childField;
        }
    }

    /** Test JsonIgnore inheritance from parent interface */
    interface ParentInterfaceWithIgnoredField {
        @JsonIgnore
        String getParentField();
    }

    class ChildImplementsParentInterface implements ParentInterfaceWithIgnoredField {
        private String parentField;
        private String childField;

        public ChildImplementsParentInterface(String parentField, String childField) {
            this.parentField = parentField;
            this.childField = childField;
        }

        @Override
        public String getParentField() {
            return parentField;
        }

        public String getChildField() {
            return childField;
        }
    }

    interface GrandparentWithIgnoredProperty {
        @JsonIgnore
        String getGrandparentField();
    }

    abstract class ParentInheritsGrandparentIgnore implements GrandparentWithIgnoredProperty {}

    class ChildInheritsGrandparentIgnore extends ParentInheritsGrandparentIgnore {
        String field;

        public ChildInheritsGrandparentIgnore(String field) {
            this.field = field;
        }

        @Override
        public String getGrandparentField() {
            return field;
        }
    }
}
