package org.tillerino.jagger.annotations;

import java.lang.annotation.*;
import org.tillerino.jagger.annotations.JsonTemplate.JsonTemplates;

/**
 * Generate prototypes with this shorthand.
 *
 * <p>Assuming these templates:
 *
 * <pre>{@code
 * @JsonConfig(implement = JsonConfig.ImplementationMode.DO_NOT_IMPLEMENT)
 * interface GenericInputSerde<V> {
 *     @JsonInput
 *     V readOnGenericInterface(JsonParser parser) throws IOException;
 * }
 *
 * @JsonConfig(implement = JsonConfig.ImplementationMode.DO_NOT_IMPLEMENT)
 * interface GenericOutputSerde<U> {
 *     @JsonOutput
 *     void writeOnGenericInterface(U obj, JsonGenerator gen) throws IOException;
 * }
 * }</pre>
 *
 * <p>They can be instantiated like:
 *
 * <pre>{@code
 * @JsonTemplate(
 *     templates = {GenericInputSerde.class, GenericOutputSerde.class},
 *     types = {Float.class, String.class})
 * interface MySerde {}
 * }</pre>
 *
 * <p>The implementation of {@code MySerde} will then contain methods
 *
 * <pre>{@code
 * MySerdeImpl {
 *     Float readFloat(JsonParser parser) throws IOException {...}
 *     void writeFloat(Float obj, JsonGenerator gen) throws IOException {...}
 *     String readString(JsonParser parser) throws IOException {...}
 *     void writeString(String obj, JsonGenerator gen) throws IOException {...}
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(JsonTemplates.class)
public @interface JsonTemplate {
    /**
     * A template for the methods to generate.
     *
     * @return functional interfaces. Each interfaces must have exactly one type parameter either on the interface or
     *     method.
     */
    Class[] templates();

    Class[] types();

    /** Container annotation for repeatable @JsonTemplate annotations. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface JsonTemplates {
        JsonTemplate[] value();
    }
}
