package org.tillerino.jagger.tests.base.edgecases;

import org.tillerino.jagger.annotations.JsonInput;
import tools.jackson.core.JsonParser;

public interface VariableNaming {
    record Outer(Inner sameName) {}

    record Inner(String sameName) {}

    /**
     * In this serde, all variables are named the same. We're testing if we evade that issue by renaming our local
     * variables.
     */
    interface Serde {
        @JsonInput
        Outer read(JsonParser sameName) throws Exception;
    }
}
