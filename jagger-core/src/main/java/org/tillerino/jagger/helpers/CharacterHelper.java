package org.tillerino.jagger.helpers;

public class CharacterHelper {

    public static Character toCharacter(String string) {
        if (string == null) {
            return null;
        }
        if (string.length() != 1) {
            throw new IllegalArgumentException("Expected a single character, got " + string);
        }
        return string.charAt(0);
    }
}
