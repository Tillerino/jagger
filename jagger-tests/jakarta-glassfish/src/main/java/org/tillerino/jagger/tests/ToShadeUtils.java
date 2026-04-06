package org.tillerino.jagger.tests;

import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.io.StringReader;
import java.io.StringWriter;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.glassfish.json.JsonProviderImpl;
import org.tillerino.jagger.helpers.JakartaJsonParserHelper.JsonParserWrapper;

/** This class is in the main class path since we base our shading on this. */
public class ToShadeUtils {

    public static <T> T withJsonReader(String json, FailableFunction<JsonParserWrapper, T, Exception> consumer)
            throws Exception {
        JsonParser parser = new JsonProviderImpl().createParser(new StringReader(json));
        return consumer.apply(new JsonParserWrapper(parser));
    }

    public static String withGsonJsonWriter(FailableConsumer<JsonGenerator, Exception> output) throws Exception {
        StringWriter out = new StringWriter();
        JsonGenerator generator = new JsonProviderImpl().createGenerator(out);
        output.accept(generator);
        generator.flush();
        return out.toString();
    }
}
