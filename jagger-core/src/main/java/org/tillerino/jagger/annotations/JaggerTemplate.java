package org.tillerino.jagger.annotations;

import java.lang.annotation.*;
import org.tillerino.jagger.annotations.JaggerTemplate.JaggerTemplates;

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
 * @JaggerTemplate(
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
@Repeatable(JaggerTemplates.class)
public @interface JaggerTemplate {
    /**
     * Templates to generate prototypes from.
     *
     * @return functional interfaces. Each interfaces must have one or more type parameters either on the interface
     *     itself or on the single method.
     */
    Class[] templates();

    /**
     * If the template prototypes have a single type parameter, one prototype is instantiated for each template and each
     * type specified here.
     */
    Class[] types() default {};

    /**
     * If the template prototypes have multiple type parameters, one prototype is instantiated for each template and
     * each type array specified here.
     */
    TypeArray[] typeArrays() default {};

    /** If true, the templates are instantiated automatically whenever a delegator is searched. */
    boolean auto() default false;

    /** Container annotation for repeatable {@link JaggerTemplate} annotations. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @interface JaggerTemplates {
        JaggerTemplate[] value();
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface TypeArray {
        Class[] value();
    }
}
