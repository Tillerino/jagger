package org.tillerino.jagger.tests.crossmodule;

import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.sibling.SiblingSerde;
import org.tillerino.jagger.tests.sibling.SiblingSerde.SiblingRecord;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

/**
 * Uses a blueprint whose implementation is generated in a different module ({@code jagger-tests-jackson-sibling}).
 *
 * <p>This package is deliberately outside of {@code org.tillerino.jagger.tests.base}, so that {@code CopyTests} does
 * not copy it into the alternate backend modules.
 */
public interface CrossModuleSerde {
    @JsonConfig(uses = SiblingSerde.class)
    interface UsesSiblingSerde {
        @JsonOutput
        void writeWrapper(Wrapper wrapper, JsonGenerator generator) throws Exception;

        @JsonInput
        Wrapper readWrapper(JsonParser parser) throws Exception;

        record Wrapper(SiblingRecord sibling, String name) {}
    }
}
