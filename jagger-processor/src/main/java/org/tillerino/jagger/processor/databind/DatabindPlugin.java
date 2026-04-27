package org.tillerino.jagger.processor.databind;

import com.google.auto.service.AutoService;
import java.util.Set;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPlugin;
import org.tillerino.jagger.processor.config.JaggerAnnotations;

@AutoService(JaggerPlugin.class)
public class DatabindPlugin implements JaggerPlugin {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(DatabindDetector.JSON_INPUT, DatabindDetector.JSON_OUTPUT, JaggerAnnotations.JSON_CONFIG);
    }

    @Override
    public void configure(JaggerContext ctx) {
        ctx.detectors.add(new DatabindDetector(ctx));
    }
}
