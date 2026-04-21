package org.tillerino.jagger.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface JdbcUpdate {
    String value() default "";
}
