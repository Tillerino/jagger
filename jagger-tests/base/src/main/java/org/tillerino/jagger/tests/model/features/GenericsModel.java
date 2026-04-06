package org.tillerino.jagger.tests.model.features;

public interface GenericsModel {
    record GenericRecord<F>(F f) {}

    record PointlessGenericsRecord<F>(String prop) {}

    record UsesGenericRecord(GenericRecord<Integer> gi) {}
}
