package org.tillerino.jagger.tests.sibling;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

/**
 * A blueprint that is generated in a different module than the blueprints that use it.
 *
 * <p>This package is deliberately outside of {@code org.tillerino.jagger.tests.base}, so that {@code CopyTests} does
 * not copy it into the alternate backend modules.
 */
public interface SiblingSerde {
    @JsonOutput
    void writeSiblingRecord(SiblingRecord record, JsonGenerator generator) throws Exception;

    @JsonInput
    SiblingRecord readSiblingRecord(JsonParser parser) throws Exception;

    record SiblingRecord(String value) {}
}
