package org.tillerino.jagger.helpers.jdbc;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;

public class UncheckedSQLException extends PersistenceException {
    public UncheckedSQLException(SQLException cause) {
        super(cause);
    }

    @Override
    public synchronized SQLException getCause() {
        return (SQLException) super.getCause();
    }
}
