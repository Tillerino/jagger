package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"z", "a", "b"})
public interface PropertyOrderModel {
    record OrderedProperties(String a, int z, String b) {}

    @JsonPropertyOrder(alphabetic = true)
    record AlphabeticOrderedProperties(String z, String a, String b) {}

    @JsonPropertyOrder(
            value = {"z"},
            alphabetic = true)
    record ExplicitZThenAlphabetic(String c, String z, String b) {}

    @JsonPropertyOrder(value = {"z"})
    record ExplicitZThenDeclaration(String c, String z, String b) {}
}
