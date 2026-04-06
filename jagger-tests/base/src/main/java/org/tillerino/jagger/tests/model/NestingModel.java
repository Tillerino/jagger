package org.tillerino.jagger.tests.model;

import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;

public class NestingModel {
    public record OuterRecord(
            Double[] doubleArray,
            List<Double> doubleList,
            Map<String, Double> doubleMap,
            InnerRecord innerRecord,
            InnerFields innerFields,
            InnerAccessors innerAccessors) {}

    public record InnerRecord(Double d) {}

    @EqualsAndHashCode
    public static class OuterFields {
        public Double[] doubleArray;
        public List<Double> doubleList;
        public Map<String, Double> doubleMap;
        public InnerRecord innerRecord;
        public InnerFields innerFields;
        public InnerAccessors innerAccessors;
    }

    @EqualsAndHashCode
    public static class InnerFields {
        public Double d;
    }

    @EqualsAndHashCode
    public static class OuterAccessors {
        private Double[] doubleArray;
        private List<Double> doubleList;
        private Map<String, Double> doubleMap;
        private InnerRecord innerRecord;
        private InnerFields innerFields;
        private InnerAccessors innerAccessors;

        public Double[] getDoubleArray() {
            return doubleArray;
        }

        public void setDoubleArray(Double[] doubleArray) {
            this.doubleArray = doubleArray;
        }

        public List<Double> getDoubleList() {
            return doubleList;
        }

        public void setDoubleList(List<Double> doubleList) {
            this.doubleList = doubleList;
        }

        public Map<String, Double> getDoubleMap() {
            return doubleMap;
        }

        public void setDoubleMap(Map<String, Double> doubleMap) {
            this.doubleMap = doubleMap;
        }

        public InnerRecord getInnerRecord() {
            return innerRecord;
        }

        public void setInnerRecord(InnerRecord innerRecord) {
            this.innerRecord = innerRecord;
        }

        public InnerFields getInnerFields() {
            return innerFields;
        }

        public void setInnerFields(InnerFields innerFields) {
            this.innerFields = innerFields;
        }

        public InnerAccessors getInnerAccessors() {
            return innerAccessors;
        }

        public void setInnerAccessors(InnerAccessors innerAccessors) {
            this.innerAccessors = innerAccessors;
        }
    }

    @EqualsAndHashCode
    public static class InnerAccessors {
        private Double d;

        public Double getD() {
            return d;
        }

        public void setD(Double d) {
            this.d = d;
        }
    }
}
