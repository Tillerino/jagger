package org.tillerino.jagger.processor.databind;

import com.google.auto.service.AutoService;
import java.util.Set;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.JaggerAnnotations;
import org.tillerino.jagger.processor.ext.JaggerPlugin;
import org.tillerino.jagger.processor.features.Converters;
import org.tillerino.jagger.processor.features.DefaultValues;

@AutoService(JaggerPlugin.class)
public class DatabindPlugin implements JaggerPlugin {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                DatabindPrototypeDetector.JSON_INPUT,
                DatabindPrototypeDetector.JSON_OUTPUT,
                JaggerAnnotations.JSON_CONFIG,
                Converters.JSON_INPUT_CONVERTER,
                Converters.JSON_OUTPUT_CONVERTER,
                DefaultValues.JSON_INPUT_DEFAULT_VALUE);
    }

    @Override
    public void configure(JaggerContext ctx) {
        ctx.register(new DatabindPrototypeDetector(ctx));
        ctx.register(new Converters.ConverterPrototypeDetector());
        ctx.register(new DefaultValues.DefaultValuePrototypeDetector());
    }
}
