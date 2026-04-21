package org.tillerino.jagger.tests.jdbc;

import java.sql.*;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class AbstractJdbcTest {

    protected Connection connection;

    @BeforeEach
    protected void setUp() throws Exception {
        // Connect to H2 in-memory database
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

        execute("DROP ALL OBJECTS");
    }

    @AfterEach
    protected void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    protected void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    protected void executeQuery(String sql, FailableConsumer<ResultSet, SQLException> consumer) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            consumer.accept(statement.executeQuery(sql));
        }
    }
}
