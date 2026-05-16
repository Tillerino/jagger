package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface JsonAliasModel {
    record JsonAliasValue(
            @JsonProperty("displayName") @JsonAlias({"name", "fullName"})
            String name,

            @JsonAlias({"val", "number"}) int value) {}

    record NestedJsonAlias(@JsonAlias("outer") String outer, InnerJsonAlias inner) {}

    record InnerJsonAlias(
            @JsonAlias({"innerAlias1", "innerAlias2"}) String value) {}

    interface Enums {
        enum OnlyAliases {
            @JsonAlias({"alias-one", "alias1"})
            VALUE1,
            @JsonAlias("alias-two")
            VALUE2,
            VALUE3;
        }

        enum AliasAndJsonProperty {
            @JsonProperty("custom-value")
            @JsonAlias({"customAlias", "cv"})
            VALUE1,
            VALUE2;
        }
    }
}
