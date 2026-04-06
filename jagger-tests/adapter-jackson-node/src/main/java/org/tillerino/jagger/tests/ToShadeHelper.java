package org.tillerino.jagger.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;

/**
 * This is here so that we reference all needed classes for the shaded jar. JsonParser requires an ObjectCodec to read
 * JsonNode, so it looks like we actually need the full ObjectMapper.
 */
public class ToShadeHelper {
    public static JsonNode readNode(String json) throws IOException {
        return new ObjectMapper().readTree(json);
    }

    public static JsonNodeFactory jsonNodeFactory() {
        return new ObjectMapper().getNodeFactory();
    }
}
