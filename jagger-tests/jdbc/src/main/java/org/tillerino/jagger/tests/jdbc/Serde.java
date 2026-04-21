package org.tillerino.jagger.tests.jdbc;

import jakarta.persistence.Id;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.annotations.JdbcUpdate;

public interface Serde {
    @JdbcSelect("""
        SELECT * from "simple" where "someId" = :param""")
    SimpleEntityRecord selectSingleRecordBySomeId(Connection c, int param) throws SQLException;

    @JdbcSelect("""
        SELECT * from "simple" where "payload" = :param""")
    SimpleEntityRecord selectSingleRecordByPayload(Connection c, String param) throws SQLException;

    @JdbcSelect("""
        SELECT * from "simple" where "payload" = :param""")
    List<SimpleEntityRecord> selectMultipleRecordByPayload(Connection c, String param) throws SQLException;

    @JdbcSelect(value = """
        SELECT * from "simple" where "payload" = :param""", fetchSize = 100)
    Iterator<SimpleEntityRecord> selectIteratorRecordWithFetchSize(Connection c, String param) throws SQLException;

    @JdbcSelect("""
        SELECT * from "simple" where "payload" = :param""")
    Iterator<SimpleEntityRecord> selectIteratorRecordByPayload(Connection c, String param) throws SQLException;

    @JdbcSelect("""
        SELECT * from "simple" where "payload" = :param""")
    Iterable<SimpleEntityRecord> selectIterableRecordByPayload(Connection c, String param) throws SQLException;

    @JdbcSelect("""
        SELECT * from "simple" where "payload" = :param""")
    Optional<SimpleEntityRecord> selectOptionalRecordByPayload(Connection c, String param) throws SQLException;

    @JdbcSelect("""
        SELECT * from "simple" where "payload" = :param""")
    Optional<SimpleEntityClass> selectOptionalClassByPayload(Connection c, String param) throws SQLException;

    @JdbcInsert("""
        INSERT into "simple" ("entity.#columns") values (:entity.#values)""")
    void insertSingle(Connection c, SimpleEntityRecord entity) throws SQLException;

    @JdbcInsert("""
        INSERT into "simple" ("entities.#columns") values (:entities.#values)""")
    void insertMultiple(Connection c, List<SimpleEntityRecord> entities) throws SQLException;

    @JdbcUpdate(
            """
            UPDATE "simple" set ("entity.#updateColumns") = (:entity.#updateValues) where ("entity.#keyColumns") = (:entity.#keyValues)""")
    void updateSimpleSingle(Connection c, SimpleEntityRecord entity) throws SQLException;

    @JdbcUpdate(
            """
            UPDATE "simple" set ("entities.#updateColumns") = (:entities.#updateValues) where ("entities.#keyColumns") = (:entities.#keyValues)""")
    void updateSimpleMultiple(Connection c, List<SimpleEntityRecord> entities) throws SQLException;

    @JdbcSelect("""
        SELECT * from "multi" where "id1" = :param1 and "id2" = :param2""")
    MultiIdEntityRecord selectByIds(Connection c, int param1, int param2) throws SQLException;

    @JdbcUpdate(
            """
            UPDATE "multi" set ("entity.#updateColumns") = (:entity.#updateValues) where ("entity.#keyColumns") = (:entity.#keyValues)""")
    void updateMulti(Connection c, MultiIdEntityRecord entity) throws SQLException;

    @JdbcUpdate(
            """
            UPDATE "multi" set ("entity.#updateColumns") = (:entity.#updateValues) where ("entity.#keyColumns") = (:entity.#keyValues) and 1""")
    void updateMultiConj(Connection c, MultiIdEntityRecord entity) throws SQLException;

    @JdbcUpdate("delete from \"simple\" where \"someId\" = :param")
    void deleteById(Connection c, int param) throws SQLException;

    @JdbcUpdate("delete from \"simple\"")
    void deleteAll(Connection c) throws SQLException;

    record SimpleEntityRecord(@Id int someId, String payload) {
        public static final String SCHEMA =
                """
            CREATE TABLE "simple" ("someId" INT PRIMARY KEY, "payload" VARCHAR(100))""";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class SimpleEntityClass {
        @Id
        int someId;

        String payload;
    }

    record MultiIdEntityRecord(@Id int id1, @Id int id2, String payload) {
        public static final String SCHEMA =
                """
                CREATE TABLE "multi" ("id1" INT, "id2" INT, "payload" VARCHAR(100), PRIMARY KEY ("id1", "id2"))""";
    }
}
