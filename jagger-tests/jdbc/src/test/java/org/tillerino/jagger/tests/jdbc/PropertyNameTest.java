package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.model.features.PropertyNameModel.JdbcCustomColumnPojo;
import org.tillerino.jagger.tests.model.features.PropertyNameModel.JdbcCustomColumnRecord;

public class PropertyNameTest extends AbstractJdbcTest {
    PropertyNameSerde serde = SerdeUtil.impl(PropertyNameSerde.class);

    @Test
    void testRecordWithCustomColumnNames() throws SQLException {
        execute("CREATE TABLE \"custom_columns\" (\"custom_id\" INT PRIMARY KEY, \"custom_payload\" VARCHAR(100))");
        serde.insertRecord(connection, new JdbcCustomColumnRecord(1, "test"));
        List<JdbcCustomColumnRecord> results = serde.selectRecord(connection);
        assertThat(results).containsExactly(new JdbcCustomColumnRecord(1, "test"));
    }

    @Test
    void testPojoWithCustomColumnNames() throws SQLException {
        execute(
                "CREATE TABLE \"custom_columns_pojo\" (\"custom_id\" INT PRIMARY KEY, \"custom_payload\" VARCHAR(100))");
        serde.insertPojo(connection, new JdbcCustomColumnPojo(1, "test"));
        List<JdbcCustomColumnPojo> results = serde.selectPojo(connection);
        assertThat(results).containsExactly(new JdbcCustomColumnPojo(1, "test"));
    }
}
