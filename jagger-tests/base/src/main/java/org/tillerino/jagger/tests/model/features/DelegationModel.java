package org.tillerino.jagger.tests.model.features;

import java.util.Arrays;
import java.util.List;

public interface DelegationModel {
    List<SelfReferencingRecord> SELF_REFERENCING_INSTANCES = Arrays.asList(
            null,
            new SelfReferencingRecord("a", null),
            new SelfReferencingRecord("c", new SelfReferencingRecord("d", null)));

    record SelfReferencingRecord(String prop, SelfReferencingRecord self) {}

    interface GenericInterface<T> {}

    record UsesGenericInterfaceRaw(GenericInterface i) {}

    record UsesGenericInterfaceWildcard(GenericInterface<?> i) {}
}
