package org.tillerino.jagger.tests.base.edgecases;

import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;

public interface VariableNaming {
    record Outer(Inner sameName) {}

    record Inner(String sameName) {}

    interface Serde {
        @JsonInput
        Outer read(JsonParser sameName) throws Exception;
    }
}
