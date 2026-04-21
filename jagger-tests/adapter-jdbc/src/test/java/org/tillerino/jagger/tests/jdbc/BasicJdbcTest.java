package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.*;
import java.util.List;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.adapters.ResultSetAdapter;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.api.JaggerReader.Advance;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

public class BasicJdbcTest {
    private final Serde serde = new BasicJdbcTest$SerdeImpl();

    private final String stringsOnlyTableDef =
            """
        CREATE TABLE users (
            "username" VARCHAR(50) NULL UNIQUE,
            "email" VARCHAR(100) NULL
        )""";

    private final String scalarFieldsTableDef =
            """
        CREATE TABLE scalars (
            "bo" BOOLEAN NOT NULL,
            "by" TINYINT NOT NULL,
            "s" SMALLINT NOT NULL,
            "i" INT NOT NULL,
            "l" BIGINT NOT NULL,
            "c" CHAR(1) NOT NULL,
            "f" REAL NOT NULL,
            "d" DOUBLE NOT NULL,

            "bbo" BOOLEAN NULL,
            "bby" TINYINT NULL,
            "ss" SMALLINT NULL,
            "ii" INT NULL,
            "ll" BIGINT NULL,
            "cc" CHAR(1) NULL,
            "ff" REAL NULL,
            "dd" DOUBLE NULL,

            "str" VARCHAR(255),
            "en" VARCHAR
        )""";

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        // Connect to H2 in-memory database
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

        execute("DROP ALL OBJECTS");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void basicAdapterTest() throws Exception {
        execute(stringsOnlyTableDef);

        execute("INSERT INTO users values ('foo', 'foo@example.com')");

        executeQuery("SELECT * FROM users", resultSet -> {
            ResultSetAdapter adapter = new ResultSetAdapter(resultSet);

            assertThat(adapter.isArrayStart(Advance.CONSUME)).isTrue();
            assertThat(adapter.isObjectStart(Advance.CONSUME)).isTrue();

            assertThat(adapter.isFieldName()).isTrue();
            assertThat(adapter.getFieldName(Advance.CONSUME)).isEqualTo("username");
            assertThat(adapter.isText()).isTrue();
            assertThat(adapter.getText(Advance.CONSUME)).isEqualTo("foo");

            assertThat(adapter.isFieldName()).isTrue();
            assertThat(adapter.getFieldName(Advance.CONSUME)).isEqualTo("email");
            assertThat(adapter.isText()).isTrue();
            assertThat(adapter.getText(Advance.CONSUME)).isEqualTo("foo@example.com");

            assertThat(adapter.isObjectEnd(Advance.CONSUME)).isTrue();
            assertThat(adapter.isArrayEnd(Advance.CONSUME)).isTrue();
        });
    }

    @Test
    void stringsOnly() throws Exception {
        execute(stringsOnlyTableDef);

        execute("INSERT INTO users values ('foo', 'foo@example.com'), (NULL, 'bar@example.com'), ('bar', NULL)");

        executeQuery(
                "SELECT * FROM users", resultSet -> assertThat(serde.readStringsOnly(new ResultSetAdapter(resultSet)))
                        .containsExactly(
                                new StringsOnly("foo", "foo@example.com"),
                                new StringsOnly(null, "bar@example.com"),
                                new StringsOnly("bar", null)));
    }

    @Test
    void scalarFields() throws Exception {
        execute(scalarFieldsTableDef);

        execute(
                """
                INSERT INTO scalars VALUES (TRUE, 1, 2, 3, 4, '5', 6.6, 7.7, FALSE, -1, -2, -3, -4, 'x', -6.6, -7.7, 'string', 'SOME_VALUE')""");

        executeQuery("SELECT * FROM scalars", resultSet -> assertThat(
                        serde.readScalarFields(new ResultSetAdapter(resultSet)))
                .containsExactly(new ScalarFieldsRecord(
                        true,
                        (byte) 1,
                        (short) 2,
                        3,
                        4L,
                        '5',
                        6.6f,
                        7.7,
                        false,
                        (byte) -1,
                        (short) -2,
                        -3,
                        -4L,
                        'x',
                        -6.6f,
                        -7.7,
                        "string",
                        AnEnum.SOME_VALUE)));
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void executeQuery(String sql, FailableConsumer<ResultSet, SQLException> consumer) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            consumer.accept(statement.executeQuery(sql));
        }
    }

    record StringsOnly(String username, String email) {}

    interface Serde {
        @JsonInput
        List<StringsOnly> readStringsOnly(ResultSetAdapter adapter) throws SQLException;

        @JsonInput
        List<ScalarFieldsRecord> readScalarFields(ResultSetAdapter adapter) throws SQLException;
    }
}
