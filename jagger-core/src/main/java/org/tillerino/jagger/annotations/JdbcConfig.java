package org.tillerino.jagger.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JdbcConfig {
    String table() default "";

    String quoteChar() default "\"";
}
