package org.tillerino.jagger.processor.databind;

import com.google.auto.service.AutoService;
import java.util.Set;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.config.JaggerAnnotations;
import org.tillerino.jagger.processor.ext.JaggerPlugin;

@AutoService(JaggerPlugin.class)
public class DatabindPlugin implements JaggerPlugin {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                DatabindPrototypeDetector.JSON_INPUT,
                DatabindPrototypeDetector.JSON_OUTPUT,
                JaggerAnnotations.JSON_CONFIG);
    }

    @Override
    public void configure(JaggerContext ctx) {
        ctx.register(new DatabindPrototypeDetector(ctx));
    }
}
