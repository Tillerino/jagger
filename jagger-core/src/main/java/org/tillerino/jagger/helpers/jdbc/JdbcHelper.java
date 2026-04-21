package org.tillerino.jagger.helpers.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcHelper {
    /**
     * Since the primitive getters of {@link ResultSet} will return a default value if the database returns NULL, we
     * check for NULL values manually.
     */
    public static void throwOnNull(ResultSet rs, String propertyName) throws SQLException {
        if (rs.wasNull()) {
            throw new NullPointerException(propertyName);
        }
    }
}
