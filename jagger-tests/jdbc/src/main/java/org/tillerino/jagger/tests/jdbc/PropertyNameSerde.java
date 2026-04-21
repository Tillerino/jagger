package org.tillerino.jagger.tests.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.tillerino.jagger.annotations.JdbcConfig;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.tests.model.features.PropertyNameModel.JdbcCustomColumnPojo;
import org.tillerino.jagger.tests.model.features.PropertyNameModel.JdbcCustomColumnRecord;

public interface PropertyNameSerde {
    @JdbcConfig(table = "custom_columns")
    @JdbcInsert
    void insertRecord(Connection c, JdbcCustomColumnRecord entity) throws SQLException;

    @JdbcConfig(table = "custom_columns")
    @JdbcSelect
    List<JdbcCustomColumnRecord> selectRecord(Connection c) throws SQLException;

    @JdbcConfig(table = "custom_columns_pojo")
    @JdbcInsert
    void insertPojo(Connection c, JdbcCustomColumnPojo entity) throws SQLException;

    @JdbcConfig(table = "custom_columns_pojo")
    @JdbcSelect
    List<JdbcCustomColumnPojo> selectPojo(Connection c) throws SQLException;
}
