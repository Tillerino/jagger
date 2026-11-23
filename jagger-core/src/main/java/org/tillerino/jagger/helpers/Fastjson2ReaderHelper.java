package org.tillerino.jagger.helpers;

import com.alibaba.fastjson2.JSONReader;
import java.io.IOException;
import org.tillerino.jagger.api.JaggerDatabindException;

public class Fastjson2ReaderHelper {
    public static String readDiscriminator(String discriminatorName, JSONReader parser) throws IOException {
        String fieldName = parser.readFieldName();
        if (fieldName == null) {
            throw new JaggerDatabindException("Expected field name, got " + parser.current());
        }
        if (!fieldName.equals(discriminatorName)) {
            throw new JaggerDatabindException("Expected field name " + discriminatorName + ", got " + fieldName);
        } else if (!parser.isString()) {
            throw new JaggerDatabindException("Expected string, got " + parser.current());
        } else {
            return parser.readString();
        }
    }
}
