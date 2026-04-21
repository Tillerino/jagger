package org.tillerino.jagger.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Causes the annotated method to be generated treating it as a JDBC select.
 *
 * <p>The method may return a list or iterator, an optional, or an object. Accordingly, the query must return any number
 * of results, at most one result, or exactly one result. Otherwise, {@link jakarta.persistence.NoResultException} or
 * {@link jakarta.persistence.NonUniqueResultException} are thrown.
 *
 * <p>If an iterator is returned, the underlying {@link java.sql.PreparedStatement} is closed when the iterator is
 * exhausted.
 */
@Target(ElementType.METHOD)
public @interface JdbcSelect {
    String value() default "";

    /** TODO implement */
    int fetchSize() default 0;

    String where() default "";
}
