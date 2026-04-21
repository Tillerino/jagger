package org.tillerino.jagger.tests.jdbc;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import lombok.Data;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.annotations.JdbcUpdate;

public interface Docs {
    record BasicRecord(int id, String payload) {}

    interface Serde {
        @JdbcSelect("SELECT * FROM tablename WHERE id = :param")
        BasicRecord select(Connection c, int param) throws SQLException;
    }

    interface Serde2 {
        @JdbcUpdate("UPDATE tablename SET payload = :entity.payload WHERE id = :entity.id")
        void update(Connection c, BasicRecord entity) throws SQLException;

        @JdbcUpdate("INSERT INTO tablename (id, payload) VALUES (:entities.id, :entities.payload)")
        void insert(Connection c, List<BasicRecord> entities) throws SQLException;
    }

    @Data
    class MultiPropertyPojo {
        @Id
        int parentId;

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        int id;

        String payload1;

        String payload2;
    }

    interface Serde3 {
        @JdbcUpdate("INSERT INTO tablename (entity.#insertColumns) VALUES (:entity.#insertValues)")
        void insert(Connection c, MultiPropertyPojo entity) throws SQLException;

        @JdbcUpdate(
                """
                UPDATE tablename SET (entity.#updateColumns) = (:entity.#updateValues)
                  WHERE (entity.#keyColumns) = (:entity.#keyValues)""")
        void update(Connection c, MultiPropertyPojo entity) throws SQLException;
    }

    @Table(name = "tablename")
    record AutoRecord(@Id int id, String payload) {}

    interface Serde4 {
        @JdbcSelect
        AutoRecord select(Connection c) throws SQLException;

        @JdbcInsert
        void select(Connection c, AutoRecord e) throws SQLException;

        @JdbcUpdate
        void update(Connection c, AutoRecord e) throws SQLException;

        @JdbcSelect(where = "id = :id")
        AutoRecord selectById(Connection c, int id) throws SQLException;
    }
}
