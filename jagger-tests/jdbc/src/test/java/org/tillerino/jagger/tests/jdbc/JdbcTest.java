package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.helpers.jdbc.ResultSetIterator;
import org.tillerino.jagger.tests.CodeAssertions;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.jdbc.Serde.MultiIdEntityRecord;
import org.tillerino.jagger.tests.jdbc.Serde.SimpleEntityClass;
import org.tillerino.jagger.tests.jdbc.Serde.SimpleEntityRecord;

public class JdbcTest extends AbstractJdbcTest {
    Serde serde = SerdeUtil.impl(Serde.class);

    @Test
    void testSelect() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'selected'), (2, 'other')");

        SimpleEntityRecord result = serde.selectSingleRecordBySomeId(connection, 1);
        assertThat(result).isEqualTo(new SimpleEntityRecord(1, "selected"));
    }

    @Test
    void testDelete() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'deleted'), (2, 'kept')");

        serde.deleteById(connection, 1);

        assertThatThrownBy(() -> serde.selectSingleRecordBySomeId(connection, 1))
                .isInstanceOf(NoResultException.class);

        SimpleEntityRecord kept = serde.selectSingleRecordBySomeId(connection, 2);
        assertThat(kept).isEqualTo(new SimpleEntityRecord(2, "kept"));
    }

    @Test
    void testDeleteAll() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'one'), (2, 'two')");

        serde.deleteAll(connection);

        List<SimpleEntityRecord> result = serde.selectMultipleRecordByPayload(connection, "one");
        assertThat(result).isEmpty();
        result = serde.selectMultipleRecordByPayload(connection, "two");
        assertThat(result).isEmpty();
    }

    @Test
    void testNoResult() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);

        assertThatThrownBy(() -> serde.selectSingleRecordBySomeId(connection, 999))
                .isInstanceOf(NoResultException.class);
    }

    @Test
    void testNonUniqueResult() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'foo'), (2, 'foo')");

        assertThatThrownBy(() -> serde.selectSingleRecordByPayload(connection, "foo"))
                .isInstanceOf(NonUniqueResultException.class);
    }

    @Test
    void testSelectMultiple() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'multi'), (2, 'multi'), (3, 'other')");

        List<SimpleEntityRecord> result = serde.selectMultipleRecordByPayload(connection, "multi");
        assertThat(result)
                .containsExactlyInAnyOrder(new SimpleEntityRecord(1, "multi"), new SimpleEntityRecord(2, "multi"));
    }

    @Test
    void testSelectMultipleIterator() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'multi'), (2, 'multi'), (3, 'other')");

        ResultSetIterator<SimpleEntityRecord> result =
                (ResultSetIterator<SimpleEntityRecord>) serde.selectIteratorRecordByPayload(connection, "multi");
        assertThat(result)
                .toIterable()
                .containsExactlyInAnyOrder(new SimpleEntityRecord(1, "multi"), new SimpleEntityRecord(2, "multi"));
        assertThat(result.parent.isClosed()).isTrue();
    }

    @Test
    void testSelectMultipleIteratorWithFetchSize() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'multi'), (2, 'multi'), (3, 'other')");

        ResultSetIterator<SimpleEntityRecord> result =
                (ResultSetIterator<SimpleEntityRecord>) serde.selectIteratorRecordWithFetchSize(connection, "multi");
        assertThat(result)
                .toIterable()
                .containsExactlyInAnyOrder(new SimpleEntityRecord(1, "multi"), new SimpleEntityRecord(2, "multi"));
        assertThat(result.parent.isClosed()).isTrue();
    }

    @Test
    void testSelectMultipleIterable() throws Exception {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'multi'), (2, 'multi'), (3, 'other')");

        Iterable<SimpleEntityRecord> result = serde.selectIterableRecordByPayload(connection, "multi");
        assertThat(result)
                .containsExactlyInAnyOrder(new SimpleEntityRecord(1, "multi"), new SimpleEntityRecord(2, "multi"));

        CodeAssertions.assertThatImpl(Serde.class)
                .method("selectIterableRecordByPayload")
                .callsConstructor("ResultSetIterator");
    }

    @Test
    void testSelectOptionalEmpty() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);

        assertThat(serde.selectOptionalRecordByPayload(connection, "notfound")).isEmpty();
        assertThat(serde.selectOptionalClassByPayload(connection, "notfound")).isEmpty();
    }

    @Test
    void testSelectOptionalOne() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'optional'), (2, 'other')");

        assertThat(serde.selectOptionalRecordByPayload(connection, "optional"))
                .contains(new SimpleEntityRecord(1, "optional"));
        assertThat(serde.selectOptionalClassByPayload(connection, "optional"))
                .contains(new SimpleEntityClass(1, "optional"));
    }

    @Test
    void testSelectOptionalMultiple() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'optional'), (2, 'optional')");

        assertThatThrownBy(() -> serde.selectOptionalRecordByPayload(connection, "optional"))
                .isInstanceOf(NonUniqueResultException.class);
        assertThatThrownBy(() -> serde.selectOptionalClassByPayload(connection, "optional"))
                .isInstanceOf(NonUniqueResultException.class);
    }

    @Test
    void testInsertSingle() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);

        SimpleEntityRecord entity = new SimpleEntityRecord(1, "single-inserted");
        serde.insertSingle(connection, entity);

        SimpleEntityRecord result = serde.selectSingleRecordBySomeId(connection, 1);
        assertThat(result).isNotSameAs(entity).isEqualTo(entity);
    }

    @Test
    void testInsertMultiple() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);

        SimpleEntityRecord entity1 = new SimpleEntityRecord(1, "inserted");
        SimpleEntityRecord entity2 = new SimpleEntityRecord(2, "inserted");
        serde.insertMultiple(connection, List.of(entity1, entity2));

        assertThat(serde.selectSingleRecordBySomeId(connection, 1))
                .isNotSameAs(entity1)
                .isEqualTo(entity1);
        assertThat(serde.selectSingleRecordBySomeId(connection, 2))
                .isNotSameAs(entity2)
                .isEqualTo(entity2);
    }

    @Test
    void testUpdate() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'original'), (2, 'unchanged')");

        serde.updateSimpleSingle(connection, new SimpleEntityRecord(1, "updated"));

        SimpleEntityRecord updated = serde.selectSingleRecordBySomeId(connection, 1);
        assertThat(updated).isEqualTo(new SimpleEntityRecord(1, "updated"));

        SimpleEntityRecord unchanged = serde.selectSingleRecordBySomeId(connection, 2);
        assertThat(unchanged).isEqualTo(new SimpleEntityRecord(2, "unchanged"));
    }

    @Test
    void testUpdateMultiple() throws SQLException {
        execute(SimpleEntityRecord.SCHEMA);
        execute("INSERT INTO \"simple\" VALUES (1, 'original'), (2, 'original'), (3, 'unchanged')");

        serde.updateSimpleMultiple(
                connection, List.of(new SimpleEntityRecord(1, "updated"), new SimpleEntityRecord(2, "updated")));

        SimpleEntityRecord updated1 = serde.selectSingleRecordBySomeId(connection, 1);
        assertThat(updated1).isEqualTo(new SimpleEntityRecord(1, "updated"));

        SimpleEntityRecord updated2 = serde.selectSingleRecordBySomeId(connection, 2);
        assertThat(updated2).isEqualTo(new SimpleEntityRecord(2, "updated"));

        SimpleEntityRecord unchanged = serde.selectSingleRecordBySomeId(connection, 3);
        assertThat(unchanged).isEqualTo(new SimpleEntityRecord(3, "unchanged"));
    }

    @Test
    void testMultiIdSelect() throws SQLException {
        execute(MultiIdEntityRecord.SCHEMA);
        execute("INSERT INTO \"multi\" VALUES (1, 1, 'first'), (1, 2, 'second'), (2, 2, 'third')");

        MultiIdEntityRecord result = serde.selectByIds(connection, 1, 2);
        assertThat(result).isEqualTo(new MultiIdEntityRecord(1, 2, "second"));
    }

    @Test
    void testMultiIdUpdate() throws SQLException {
        execute(MultiIdEntityRecord.SCHEMA);
        execute("INSERT INTO \"multi\" VALUES (1, 1, 'unchanged'), (2, 1, 'original'), (2, 2, 'unchanged')");

        serde.updateMulti(connection, new MultiIdEntityRecord(2, 1, "updated"));

        MultiIdEntityRecord updated = serde.selectByIds(connection, 2, 1);
        assertThat(updated).isEqualTo(new MultiIdEntityRecord(2, 1, "updated"));

        MultiIdEntityRecord unchanged = serde.selectByIds(connection, 1, 1);
        assertThat(unchanged).isEqualTo(new MultiIdEntityRecord(1, 1, "unchanged"));

        unchanged = serde.selectByIds(connection, 2, 2);
        assertThat(unchanged).isEqualTo(new MultiIdEntityRecord(2, 2, "unchanged"));
    }

    @Test
    void testMultiIdUpdateWithAnd() throws SQLException {
        execute(MultiIdEntityRecord.SCHEMA);
        execute("INSERT INTO \"multi\" VALUES (1, 1, 'original'), (2, 1, 'unchanged')");

        serde.updateMultiConj(connection, new MultiIdEntityRecord(1, 1, "updated"));

        MultiIdEntityRecord updated = serde.selectByIds(connection, 1, 1);
        assertThat(updated).isEqualTo(new MultiIdEntityRecord(1, 1, "updated"));

        MultiIdEntityRecord unchanged = serde.selectByIds(connection, 2, 1);
        assertThat(unchanged).isEqualTo(new MultiIdEntityRecord(2, 1, "unchanged"));
    }

    @Test
    void testFetchSize() throws Exception {
        assertThatImpl(Serde.class).method("selectIteratorRecordWithFetchSize").bodyContains("setFetchSize(100)");
    }

    @Test
    void testNoFetchSize() throws Exception {
        assertThatImpl(Serde.class).method("selectIteratorRecordByPayload").doesNotCall("setFetchSize");
    }
}
