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
}
