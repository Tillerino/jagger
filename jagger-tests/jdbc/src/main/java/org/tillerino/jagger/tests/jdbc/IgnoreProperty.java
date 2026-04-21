package org.tillerino.jagger.tests.jdbc;

import jakarta.persistence.Id;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tillerino.jagger.annotations.JdbcConfig;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.annotations.JdbcUpdate;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.variants.ApplyVariantsToChildren;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

public interface IgnoreProperty {
    @Variants({
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JavaxTransientFieldPojo")}),
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JakartaTransientFieldPojo")}),
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JavaxTransientComponentRecord")}),
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JakartaTransientComponentRecord")}),
    })
    @ApplyVariantsToChildren
    @JdbcConfig(table = "transient_field")
    interface Serde {
        String SCHEMA = """
      CREATE TABLE "transient_field" ("id" INT PRIMARY KEY, "payload" VARCHAR(100))""";

        @JdbcSelect
        List<JavaTransientFieldPojo> selectAllJavaTransientFieldPojo(Connection c) throws SQLException;

        @JdbcInsert
        void insertAllJavaTransientFieldPojo(Connection c, List<JavaTransientFieldPojo> entities) throws SQLException;

        @JdbcUpdate
        void updateAllJavaTransientFieldPojo(Connection c, List<JavaTransientFieldPojo> entities) throws SQLException;

        /* GENERATED CODE. DO NOT MODIFY BELOW!  JavaTransientFieldPojo -> JavaxTransientFieldPojo */
        @JdbcSelect
        @GeneratedVariant("Variant of selectAllJavaTransientFieldPojo")
        List<JavaxTransientFieldPojo> selectAllJavaxTransientFieldPojo(Connection c) throws SQLException;

        /* JavaTransientFieldPojo -> JavaxTransientFieldPojo */
        @JdbcInsert
        @GeneratedVariant("Variant of insertAllJavaTransientFieldPojo")
        void insertAllJavaxTransientFieldPojo(Connection c, List<JavaxTransientFieldPojo> entities) throws SQLException;

        /* JavaTransientFieldPojo -> JavaxTransientFieldPojo */
        @JdbcUpdate
        @GeneratedVariant("Variant of updateAllJavaTransientFieldPojo")
        void updateAllJavaxTransientFieldPojo(Connection c, List<JavaxTransientFieldPojo> entities) throws SQLException;

        /* JavaTransientFieldPojo -> JakartaTransientFieldPojo */
        @JdbcSelect
        @GeneratedVariant("Variant of selectAllJavaTransientFieldPojo")
        List<JakartaTransientFieldPojo> selectAllJakartaTransientFieldPojo(Connection c) throws SQLException;

        /* JavaTransientFieldPojo -> JakartaTransientFieldPojo */
        @JdbcInsert
        @GeneratedVariant("Variant of insertAllJavaTransientFieldPojo")
        void insertAllJakartaTransientFieldPojo(Connection c, List<JakartaTransientFieldPojo> entities)
                throws SQLException;

        /* JavaTransientFieldPojo -> JakartaTransientFieldPojo */
        @JdbcUpdate
        @GeneratedVariant("Variant of updateAllJavaTransientFieldPojo")
        void updateAllJakartaTransientFieldPojo(Connection c, List<JakartaTransientFieldPojo> entities)
                throws SQLException;

        /* JavaTransientFieldPojo -> JavaxTransientComponentRecord */
        @JdbcSelect
        @GeneratedVariant("Variant of selectAllJavaTransientFieldPojo")
        List<JavaxTransientComponentRecord> selectAllJavaxTransientComponentRecord(Connection c) throws SQLException;

        /* JavaTransientFieldPojo -> JavaxTransientComponentRecord */
        @JdbcInsert
        @GeneratedVariant("Variant of insertAllJavaTransientFieldPojo")
        void insertAllJavaxTransientComponentRecord(Connection c, List<JavaxTransientComponentRecord> entities)
                throws SQLException;

        /* JavaTransientFieldPojo -> JavaxTransientComponentRecord */
        @JdbcUpdate
        @GeneratedVariant("Variant of updateAllJavaTransientFieldPojo")
        void updateAllJavaxTransientComponentRecord(Connection c, List<JavaxTransientComponentRecord> entities)
                throws SQLException;

        /* JavaTransientFieldPojo -> JakartaTransientComponentRecord */
        @JdbcSelect
        @GeneratedVariant("Variant of selectAllJavaTransientFieldPojo")
        List<JakartaTransientComponentRecord> selectAllJakartaTransientComponentRecord(Connection c)
                throws SQLException;

        /* JavaTransientFieldPojo -> JakartaTransientComponentRecord */
        @JdbcInsert
        @GeneratedVariant("Variant of insertAllJavaTransientFieldPojo")
        void insertAllJakartaTransientComponentRecord(Connection c, List<JakartaTransientComponentRecord> entities)
                throws SQLException;

        /* JavaTransientFieldPojo -> JakartaTransientComponentRecord */
        @JdbcUpdate
        @GeneratedVariant("Variant of updateAllJavaTransientFieldPojo")
        void updateAllJakartaTransientComponentRecord(Connection c, List<JakartaTransientComponentRecord> entities)
                throws SQLException;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class JavaTransientFieldPojo {
        @Id
        int id;

        String payload;

        @EqualsAndHashCode.Include
        transient String foo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class JavaxTransientFieldPojo {
        @Id
        int id;

        String payload;

        @javax.persistence.Transient
        String foo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class JakartaTransientFieldPojo {
        @Id
        int id;

        String payload;

        @jakarta.persistence.Transient
        String foo;
    }

    record JavaxTransientComponentRecord(@Id int id, String payload, @javax.persistence.Transient String foo) {}

    record JakartaTransientComponentRecord(@Id int id, String payload, @jakarta.persistence.Transient String foo) {}
}
