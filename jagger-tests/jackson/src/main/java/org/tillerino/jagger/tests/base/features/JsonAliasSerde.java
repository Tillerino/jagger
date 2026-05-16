package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.JsonAliasModel;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.InnerJsonAlias;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.JsonAliasValue;
import org.tillerino.jagger.tests.model.features.JsonAliasModel.NestedJsonAlias;

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
