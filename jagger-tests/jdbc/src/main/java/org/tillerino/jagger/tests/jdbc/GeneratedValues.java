package org.tillerino.jagger.tests.jdbc;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;

public interface GeneratedValues {
    interface Serde {
        String SCHEMA =
                """
      CREATE TABLE "identity_id" ("id" INT AUTO_INCREMENT PRIMARY KEY, "payload" VARCHAR(100))""";

        @JdbcInsert
        void insertAllIdentityJakartaPojo(Connection c, Iterable<IdentityJakartaPojo> entities) throws SQLException;

        @JdbcSelect
        Iterable<IdentityJakartaPojo> selectAllIdentityJakartaPojo(Connection c) throws SQLException;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Table(name = "identity_id")
    class IdentityJakartaPojo {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        int id;

        String payload;
    }
}
