package org.tillerino.jagger.processor.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tillerino.jagger.processor.util.Code.c;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.processor.util.Code.Flattened;

class CodeTest {
    @Test
    void empty() {
        assertEqual(c(""), "");
    }

    @Test
    void simple() {
        assertEqual(c("a$Sb", ""), "a$Sb", "");
    }

    @Test
    void nested() {
        assertEqual(c("a$Cb", c("$S", "y")), "a$Sb", "y");
    }

    @Test
    void dollar() {
        assertEqual(c("a$S$$$Sb", "a", "b"), "a$S$$$Sb", "a", "b");
    }

    @Test
    void startsWith() {
        assertEqual(c("$Cbbbb$Sc", c("$S", "y"), "z"), "$Sbbbb$Sc", "y", "z");
    }

    static void assertEqual(Code s, String format, Object... args) {
        Flattened flattened = s.flatten();
        assertThat(flattened.format()).isEqualTo(format);
        assertThat(flattened.args()).containsExactly(args);
    }
}
