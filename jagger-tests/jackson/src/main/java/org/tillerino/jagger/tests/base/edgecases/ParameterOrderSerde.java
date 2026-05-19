package org.tillerino.jagger.tests.base.edgecases;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;

/**
 * We're testing if we can declare parameters of databind prototypes in any order. The only restriction is that for
 * writers, the type-to-be-serialized must be the first type of the type-to-serialize-to parameters.
 */
public interface ParameterOrderSerde {
    @JsonOutput
    void writeStringIntMap(JsonGenerator out, Map<String, Integer> obj, SerializationContext ctx, List<Integer> l)
            throws Exception;

    @JsonOutput
    void writeInt(JsonGenerator out, Integer obj, List<Integer> l, SerializationContext ctx) throws Exception;

    @JsonInput
    Map<String, Integer> readStringIntMap(Queue<Integer> q, JsonParser in, DeserializationContext ctx) throws Exception;

    @JsonInput
    Integer readInt(Queue<Integer> q, DeserializationContext ctx, JsonParser in) throws Exception;
}
