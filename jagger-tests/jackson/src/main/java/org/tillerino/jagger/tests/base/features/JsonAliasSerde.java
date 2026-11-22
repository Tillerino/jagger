package org.tillerino.jagger.tests.base.features;

import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.JsonAliasModel;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.InnerJsonAlias;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.JsonAliasValue;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.NestedJsonAlias;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;

public interface JsonAliasSerde {
    @JsonInput
    JsonAliasValue readJsonAliasValue(JsonParser in) throws Exception;

    @JsonOutput
    void writeJsonAliasValue(JsonAliasValue value, JsonGenerator out) throws Exception;

    @JsonInput
    NestedJsonAlias readNestedJsonAlias(JsonParser in) throws Exception;

    @JsonOutput
    void writeNestedJsonAlias(NestedJsonAlias value, JsonGenerator out) throws Exception;

    @JsonInput
    InnerJsonAlias readInnerJsonAlias(JsonParser in) throws Exception;

    @JsonOutput
    void writeInnerJsonAlias(InnerJsonAlias value, JsonGenerator out) throws Exception;

    interface Enums {
        @JsonInput
        JsonAliasModel.Enums.OnlyAliases readOnlyAliases(JsonParser in) throws Exception;

        @JsonOutput
        void writeOnlyAliases(JsonAliasModel.Enums.OnlyAliases value, JsonGenerator out) throws Exception;

        @JsonInput
        JsonAliasModel.Enums.AliasAndJsonProperty readAliasAndJsonProperty(JsonParser in) throws Exception;

        @JsonOutput
        void writeAliasAndJsonProperty(JsonAliasModel.Enums.AliasAndJsonProperty value, JsonGenerator out)
                throws Exception;
    }
}
