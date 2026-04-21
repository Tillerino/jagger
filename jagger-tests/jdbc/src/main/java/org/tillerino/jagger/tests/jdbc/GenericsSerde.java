package org.tillerino.jagger.tests.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.annotations.JdbcUpdate;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonConfig.ImplementationMode;
import org.tillerino.jagger.tests.jdbc.AutoQuerySerde.JakartaTable;

public interface GenericsSerde {
    @JsonConfig(implement = ImplementationMode.DO_NOT_IMPLEMENT)
    interface GenericJdbcSerde<T> {
        @JdbcSelect
        Iterable<T> selectAll(Connection c) throws SQLException;

        @JdbcInsert
        void insertAll(Connection c, Iterable<T> entities) throws SQLException;

        @JdbcUpdate
        void updateAll(Connection c, Iterable<T> entities) throws SQLException;
    }

    @JsonConfig(implement = ImplementationMode.DO_IMPLEMENT)
    interface ConcreteJdbcSerde extends GenericJdbcSerde<JakartaTable> {}
}
