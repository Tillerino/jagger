package org.tillerino.jagger.tests;

import java.io.IOException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * This is here so that we reference all needed classes for the shaded jar. Jackson 3 no longer requires an ObjectCodec
 * to read a JsonNode, but reading a tree still requires the full ObjectMapper.
 */
public class ToShadeHelper {
    public static JsonNode readNode(String json) throws IOException {
        return new ObjectMapper().readTree(json);
    }

    public static JsonNodeFactory jsonNodeFactory() {
        return new ObjectMapper().getNodeFactory();
    }
}
