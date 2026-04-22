package org.tillerino.jagger.processor.databind;

import com.google.auto.service.AutoService;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPlugin;

@AutoService(JaggerPlugin.class)
public class DatabindPlugin implements JaggerPlugin {
    @Override
    public void configure(JaggerContext ctx) {
        ctx.detectors.add(new DatabindDetector(ctx));
    }
}
