package org.tillerino.jagger.tests.jdbc;

import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.annotations.JdbcUpdate;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.variants.ApplyVariantsToChildren;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

public interface NativeTypes {
    @Variants({@Variant({@Replacement(regex = "Record", replacement = "Pojo")})})
    @ApplyVariantsToChildren
    interface Serde {
        @JdbcSelect("""
        SELECT * from "allTypes" where "id" = :param""")
        AllTypesRecord selectAllTypesRecord(Connection c, int param) throws SQLException;

        @JdbcInsert("""
        INSERT into "allTypes" ("entities.#columns") values (:entities.#values)""")
        void insertAllTypesRecord(Connection c, List<AllTypesRecord> entities) throws SQLException;

        @JdbcUpdate(
                """
        UPDATE "allTypes" set ("entity.#updateColumns") = (:entity.#updateValues) where ("entity.#keyColumns") = (:entity.#keyValues)""")
        void updateAllTypesRecord(Connection c, AllTypesRecord entity) throws SQLException;

        /* GENERATED CODE. DO NOT MODIFY BELOW!  Record -> Pojo */
        @JdbcSelect("""
        SELECT * from "allTypes" where "id" = :param""")
        @GeneratedVariant("Variant of selectAllTypesRecord")
        AllTypesPojo selectAllTypesPojo(Connection c, int param) throws SQLException;

        /* Record -> Pojo */
        @JdbcInsert("""
        INSERT into "allTypes" ("entities.#columns") values (:entities.#values)""")
        @GeneratedVariant("Variant of insertAllTypesRecord")
        void insertAllTypesPojo(Connection c, List<AllTypesPojo> entities) throws SQLException;

        /* Record -> Pojo */
        @JdbcUpdate(
                """
        UPDATE "allTypes" set ("entity.#updateColumns") = (:entity.#updateValues) where ("entity.#keyColumns") = (:entity.#keyValues)""")
        @GeneratedVariant("Variant of updateAllTypesRecord")
        void updateAllTypesPojo(Connection c, AllTypesPojo entity) throws SQLException;
    }

    record AllTypesRecord(
            @Id int id,
            boolean boolPrim,
            byte bytePrim,
            short shortPrim,
            int intPrim,
            long longPrim,
            float floatPrim,
            double doublePrim,
            Boolean boolBox,
            Byte byteBox,
            Short shortBox,
            Integer intBox,
            Long longBox,
            Float floatBox,
            Double doubleBox,
            String stringVal,
            BigDecimal bigDecimalVal,
            Date dateVal,
            byte[] bytesVal) {
        public static final String SCHEMA =
                """
              CREATE TABLE "allTypes" (
                  "id" INT PRIMARY KEY,
                  "boolPrim" BOOLEAN,
                  "bytePrim" SMALLINT,
                  "shortPrim" SMALLINT,
                  "intPrim" INT,
                  "longPrim" BIGINT,
                  "floatPrim" REAL,
                  "doublePrim" DOUBLE PRECISION,
                  "boolBox" BOOLEAN,
                  "byteBox" SMALLINT,
                  "shortBox" SMALLINT,
                  "intBox" INT,
                  "longBox" BIGINT,
                  "floatBox" REAL,
                  "doubleBox" DOUBLE PRECISION,
                  "stringVal" VARCHAR(100),
                  "bigDecimalVal" DECIMAL(10,2),
                  "dateVal" DATE,
                  "bytesVal" BLOB
              )""";
        public static String[][] ILLEGAL_NULLS = {
            {
                "boolPrim",
                """
                  INSERT INTO "allTypes" VALUES (1, NULL, 1, 1, 1, 1, 1.0, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            },
            {
                "bytePrim",
                """
                  INSERT INTO "allTypes" VALUES (1, true, NULL, 1, 1, 1, 1.0, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            },
            {
                "shortPrim",
                """
                  INSERT INTO "allTypes" VALUES (1, true, 1, NULL, 1, 1, 1.0, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            },
            {
                "intPrim",
                """
                  INSERT INTO "allTypes" VALUES (1, true, 1, 1, NULL, 1, 1.0, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            },
            {
                "longPrim",
                """
                  INSERT INTO "allTypes" VALUES (1, true, 1, 1, 1, NULL, 1.0, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            },
            {
                "floatPrim",
                """
                  INSERT INTO "allTypes" VALUES (1, true, 1, 1, 1, 1, NULL, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            },
            {
                "doublePrim",
                """
                  INSERT INTO "allTypes" VALUES (1, true, 1, 1, 1, 1, 1.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'test', NULL, NULL, NULL)"""
            }
        };
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    class AllTypesPojo {
        @Id
        private int id;

        private boolean boolPrim;
        private byte bytePrim;
        private short shortPrim;
        private int intPrim;
        private long longPrim;
        private float floatPrim;
        private double doublePrim;
        private Boolean boolBox;
        private Byte byteBox;
        private Short shortBox;
        private Integer intBox;
        private Long longBox;
        private Float floatBox;
        private Double doubleBox;
        private String stringVal;
        private BigDecimal bigDecimalVal;
        private Date dateVal;
        private byte[] bytesVal;
    }
}
