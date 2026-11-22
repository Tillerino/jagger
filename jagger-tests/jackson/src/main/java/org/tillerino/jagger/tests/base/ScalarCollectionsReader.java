package org.tillerino.jagger.tests.base;

import java.util.Set;
import java.util.TreeSet;
import org.tillerino.jagger.annotations.JsonInput;
import tools.jackson.core.JsonParser;

/**
 * All scalar component types are tested in {@link ScalarListsReader}, we test other collections with the Boolean
 * component type here.
 */
interface ScalarCollectionsReader {
    @JsonInput
    Set<Boolean> readBooleanSet(JsonParser parser) throws Exception;

    @JsonInput
    TreeSet<Boolean> readBooleanTreeSet(JsonParser parser) throws Exception;
}
