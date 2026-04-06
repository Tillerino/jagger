package org.tillerino.jagger.tests.model.features;

import org.tillerino.jagger.tests.model.AnEnum;

public interface TemplatesModel {
    record HasAnEnumArrayProperty(AnEnum[] a, double[][] d) {}
}
