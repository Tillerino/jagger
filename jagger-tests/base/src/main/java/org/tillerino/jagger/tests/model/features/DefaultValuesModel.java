package org.tillerino.jagger.tests.model.features;

import java.util.List;
import org.tillerino.jagger.tests.model.AnEnum;

public interface DefaultValuesModel {
    record Mixed(String s, int i, double[] ds, AnEnum e, List<String> l, Mixed m) {}
}
