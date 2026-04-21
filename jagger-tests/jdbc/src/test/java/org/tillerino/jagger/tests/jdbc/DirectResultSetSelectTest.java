package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.helpers.jdbc.ResultSetIterator;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.jdbc.Serde.SimpleEntityRecord;

public class DirectResultSetSelectTest extends AbstractJdbcTest {
    DirectResultSetSelectSerde rsSerde = SerdeUtil.impl(DirectResultSetSelectSerde.class);
    Serde serde = SerdeUtil.impl(Serde.class);

    @Test
    void testSelectSingleNoResult() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        executeQuery("SELECT * from \"simple\"", rs -> {
            assertThatThrownBy(() -> rsSerde.singleFromResultSet(rs)).isInstanceOf(NoResultException.class);
        });
    }

    @Test
    void testSelectSingleOneResult() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        serde.insertSingle(connection, new SimpleEntityRecord(1, "test"));
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            SimpleEntityRecord result = rsSerde.singleFromResultSet(rs);
            assertThat(result).isEqualTo(new SimpleEntityRecord(1, "test"));
        });
    }

    @Test
    void testSelectListEmpty() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            List<SimpleEntityRecord> result = rsSerde.listFromResultSet(rs);
            assertThat(result).isEmpty();
        });
    }

    @Test
    void testSelectListMultiple() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        serde.insertMultiple(connection, List.of(new SimpleEntityRecord(1, "a"), new SimpleEntityRecord(2, "b")));
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            List<SimpleEntityRecord> result = rsSerde.listFromResultSet(rs);
            assertThat(result).hasSize(2);
        });
    }

    @Test
    void testSelectOptionalEmpty() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            var result = rsSerde.optionalFromResultSet(rs);
            assertThat(result).isEmpty();
        });
    }

    @Test
    void testSelectOptionalOne() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        serde.insertSingle(connection, new SimpleEntityRecord(1, "test"));
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            var result = rsSerde.optionalFromResultSet(rs);
            assertThat(result).contains(new SimpleEntityRecord(1, "test"));
        });
    }

    @Test
    void testSelectOptionalMultiple() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        serde.insertMultiple(connection, List.of(new SimpleEntityRecord(1, "a"), new SimpleEntityRecord(2, "b")));
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            assertThatThrownBy(() -> rsSerde.optionalFromResultSet(rs)).isInstanceOf(NonUniqueResultException.class);
        });
    }

    @Test
    void testSelectIterator() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        serde.insertMultiple(connection, List.of(new SimpleEntityRecord(1, "a"), new SimpleEntityRecord(2, "b")));
        executeQuery("SELECT * FROM \"simple\"", rs -> {
            ResultSetIterator<SimpleEntityRecord> result =
                    (ResultSetIterator<SimpleEntityRecord>) rsSerde.iteratorFromResultSet(rs);
            assertThat(result)
                    .toIterable()
                    .containsExactly(new SimpleEntityRecord(1, "a"), new SimpleEntityRecord(2, "b"));
        });
    }
}
