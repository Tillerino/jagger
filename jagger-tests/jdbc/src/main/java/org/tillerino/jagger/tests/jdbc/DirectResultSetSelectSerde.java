package org.tillerino.jagger.tests.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.tillerino.jagger.annotations.JdbcSelect;

public interface DirectResultSetSelectSerde {
    @JdbcSelect
    Serde.SimpleEntityRecord singleFromResultSet(ResultSet rs) throws SQLException;

    @JdbcSelect
    List<Serde.SimpleEntityRecord> listFromResultSet(ResultSet rs) throws SQLException;

    @JdbcSelect
    Optional<Serde.SimpleEntityRecord> optionalFromResultSet(ResultSet rs) throws SQLException;

    @JdbcSelect
    Iterator<Serde.SimpleEntityRecord> iteratorFromResultSet(ResultSet rs) throws SQLException;
}
