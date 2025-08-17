package org.tillerino.scruse.tests.base.features;

import com.fasterxml.jackson.core.JsonParser;
import java.util.Arrays;
import java.util.List;
import org.tillerino.scruse.annotations.JsonConfig;
import org.tillerino.scruse.annotations.JsonInput;
import org.tillerino.scruse.annotations.JsonInputDefaultValue;
import org.tillerino.scruse.tests.base.features.DefaultValuesSerde.SomeDefaultValues;
import org.tillerino.scruse.tests.model.AnEnum;
import org.tillerino.scruse.tests.model.features.DefaultValuesModel.Mixed;

@JsonConfig(uses = SomeDefaultValues.class)
public interface DefaultValuesSerde {
    @JsonInput
    Mixed read(JsonParser parser) throws Exception;

    interface SomeDefaultValues {
        @JsonInputDefaultValue
        static String defaultString() {
            return "foo";
        }

        @JsonInputDefaultValue
        static int defaultInt() {
            return 42;
        }

        @JsonInputDefaultValue
        static double[] defaultDoubleArray() {
            return new double[] {1.0, 2.0};
        }

        @JsonInputDefaultValue
        static AnEnum defaultAnEnum() {
            return AnEnum.ANOTHER_VALUE;
        }

        @JsonInputDefaultValue
        static List<String> defaultList() {
            return Arrays.asList("foo", "bar");
        }

        @JsonInputDefaultValue
        static Mixed defaultMixed() {
            return new Mixed(null, 0, null, null, null, null);
        }
    }
}
