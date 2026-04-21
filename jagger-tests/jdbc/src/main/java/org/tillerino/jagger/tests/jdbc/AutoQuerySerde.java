package org.tillerino.jagger.tests.jdbc;

import jakarta.persistence.Id;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.tillerino.jagger.annotations.JdbcConfig;
import org.tillerino.jagger.annotations.JdbcInsert;
import org.tillerino.jagger.annotations.JdbcSelect;
import org.tillerino.jagger.annotations.JdbcUpdate;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.variants.*;

@VariantsKarthesianProduct({
    @Variants({
        @Variant({
            @Replacement(regex = "\\(Connection", replacement = "Multi(Connection"),
            @Replacement(regex = "(\\w+) entity", replacement = "List<$1> entities"),
            @Replacement(regex = "(\\w+) select", replacement = "List<$1> select"),
        })
    }),
    @Variants({
        @Variant({@Replacement(regex = "Jakarta", replacement = "Javax")}),
        @Variant({@Replacement(regex = "Jakarta", replacement = "JaggerOnDto")}),
    })
})
public interface AutoQuerySerde {
    String EXPLICIT_SCHEMA =
            """
        CREATE TABLE "explicit_auto" ("id" INT PRIMARY KEY, "payload" VARCHAR(100), "payload2" VARCHAR(100))""";

    @JdbcSelect
    JakartaTable selectJakarta(Connection c) throws SQLException;

    @JdbcInsert
    void insertJakarta(Connection c, JakartaTable entity) throws SQLException;

    @JdbcUpdate
    void updateJakarta(Connection c, JakartaTable entity) throws SQLException;

    @JdbcSelect(where = "\"id\" = :someId")
    JakartaTable selectJakartaById(Connection c, int someId) throws SQLException;

    @JdbcSelect(where = "\"payload\" = :somePayload")
    JakartaTable selectJakartaByPayload(Connection c, String somePayload) throws SQLException;

    @JdbcConfig(table = "explicit_auto")
    @JdbcSelect
    JavaxTable selectExplicit(Connection c) throws SQLException;

    @JdbcConfig(table = "explicit_auto")
    @JdbcInsert
    void insertExplicit(Connection c, JavaxTable entity) throws SQLException;

    @JdbcConfig(table = "explicit_auto")
    @JdbcUpdate
    void updateExplicit(Connection c, JavaxTable entity) throws SQLException;

    @JdbcConfig(table = "explicit_auto")
    @JdbcSelect(where = "\"id\" = :someId")
    JavaxTable selectExplicitById(Connection c, int someId) throws SQLException;

    @JdbcConfig(table = "explicit_auto")
    @JdbcSelect(where = "\"payload\" = :somePayload")
    JavaxTable selectExplicitByPayload(Connection c, String somePayload) throws SQLException;

    // test quote char
    @NoVariants
    @JdbcConfig(quoteChar = "`")
    @JdbcSelect
    Optional<JakartaTable> selectJakartaWithQuoteChar(Connection c) throws SQLException;

    @NoVariants
    @JdbcConfig(quoteChar = "`")
    @JdbcInsert
    void insertJakartaWithQuoteChar(Connection c, JakartaTable entity) throws SQLException;

    @NoVariants
    @JdbcConfig(quoteChar = "`")
    @JdbcUpdate
    void updateJakartaWithQuoteChar(Connection c, JakartaTable entity) throws SQLException;

    @ApplyVariantsToChildren
    @jakarta.persistence.Table(name = "jakarta_auto")
    record JakartaTable(@jakarta.persistence.Id int id, String payload, String payload2) {
        public static final String SCHEMA =
                """
            CREATE TABLE "jakarta_auto" ("id" INT PRIMARY KEY, "payload" VARCHAR(100), "payload2" VARCHAR(100))""";
    }

    @javax.persistence.Table(name = "javax_auto")
    record JavaxTable(@javax.persistence.Id int id, String payload, String payload2) {
        public static final String SCHEMA =
                """
            CREATE TABLE "javax_auto" ("id" INT PRIMARY KEY, "payload" VARCHAR(100), "payload2" VARCHAR(100))""";
    }

    @JdbcConfig(table = "jagger_on_dto")
    record JaggerOnDtoTable(@Id int id, String payload, String payload2) {
        public static final String SCHEMA =
                """
            CREATE TABLE "jagger_on_dto" ("id" INT PRIMARY KEY, "payload" VARCHAR(100), "payload2" VARCHAR(100))""";
    }

    /* GENERATED CODE. DO NOT MODIFY BELOW!  Jakarta -> Javax */
    @JdbcSelect
    @GeneratedVariant("Variant of selectJakarta")
    JavaxTable selectJavax(Connection c) throws SQLException;

    /* Jakarta -> Javax */
    @JdbcInsert
    @GeneratedVariant("Variant of insertJakarta")
    void insertJavax(Connection c, JavaxTable entity) throws SQLException;

    /* Jakarta -> Javax */
    @JdbcUpdate
    @GeneratedVariant("Variant of updateJakarta")
    void updateJavax(Connection c, JavaxTable entity) throws SQLException;

    /* Jakarta -> Javax */
    @JdbcSelect(where = "\"id\" = :someId")
    @GeneratedVariant("Variant of selectJakartaById")
    JavaxTable selectJavaxById(Connection c, int someId) throws SQLException;

    /* Jakarta -> Javax */
    @JdbcSelect(where = "\"payload\" = :somePayload")
    @GeneratedVariant("Variant of selectJakartaByPayload")
    JavaxTable selectJavaxByPayload(Connection c, String somePayload) throws SQLException;

    /* Jakarta -> JaggerOnDto */
    @JdbcSelect
    @GeneratedVariant("Variant of selectJakarta")
    JaggerOnDtoTable selectJaggerOnDto(Connection c) throws SQLException;

    /* Jakarta -> JaggerOnDto */
    @JdbcInsert
    @GeneratedVariant("Variant of insertJakarta")
    void insertJaggerOnDto(Connection c, JaggerOnDtoTable entity) throws SQLException;

    /* Jakarta -> JaggerOnDto */
    @JdbcUpdate
    @GeneratedVariant("Variant of updateJakarta")
    void updateJaggerOnDto(Connection c, JaggerOnDtoTable entity) throws SQLException;

    /* Jakarta -> JaggerOnDto */
    @JdbcSelect(where = "\"id\" = :someId")
    @GeneratedVariant("Variant of selectJakartaById")
    JaggerOnDtoTable selectJaggerOnDtoById(Connection c, int someId) throws SQLException;

    /* Jakarta -> JaggerOnDto */
    @JdbcSelect(where = "\"payload\" = :somePayload")
    @GeneratedVariant("Variant of selectJakartaByPayload")
    JaggerOnDtoTable selectJaggerOnDtoByPayload(Connection c, String somePayload) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcSelect
    @GeneratedVariant("Variant of selectJakarta")
    List<JakartaTable> selectJakartaMulti(Connection c) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcInsert
    @GeneratedVariant("Variant of insertJakarta")
    void insertJakartaMulti(Connection c, List<JakartaTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcUpdate
    @GeneratedVariant("Variant of updateJakarta")
    void updateJakartaMulti(Connection c, List<JakartaTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcSelect(where = "\"id\" = :someId")
    @GeneratedVariant("Variant of selectJakartaById")
    List<JakartaTable> selectJakartaByIdMulti(Connection c, int someId) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcSelect(where = "\"payload\" = :somePayload")
    @GeneratedVariant("Variant of selectJakartaByPayload")
    List<JakartaTable> selectJakartaByPayloadMulti(Connection c, String somePayload) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcConfig(table = "explicit_auto")
    @JdbcSelect
    @GeneratedVariant("Variant of selectExplicit")
    List<JavaxTable> selectExplicitMulti(Connection c) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcConfig(table = "explicit_auto")
    @JdbcInsert
    @GeneratedVariant("Variant of insertExplicit")
    void insertExplicitMulti(Connection c, List<JavaxTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcConfig(table = "explicit_auto")
    @JdbcUpdate
    @GeneratedVariant("Variant of updateExplicit")
    void updateExplicitMulti(Connection c, List<JavaxTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcConfig(table = "explicit_auto")
    @JdbcSelect(where = "\"id\" = :someId")
    @GeneratedVariant("Variant of selectExplicitById")
    List<JavaxTable> selectExplicitByIdMulti(Connection c, int someId) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select */
    @JdbcConfig(table = "explicit_auto")
    @JdbcSelect(where = "\"payload\" = :somePayload")
    @GeneratedVariant("Variant of selectExplicitByPayload")
    List<JavaxTable> selectExplicitByPayloadMulti(Connection c, String somePayload) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> Javax */
    @JdbcSelect
    @GeneratedVariant("Variant of selectJakarta")
    List<JavaxTable> selectJavaxMulti(Connection c) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> Javax */
    @JdbcInsert
    @GeneratedVariant("Variant of insertJakarta")
    void insertJavaxMulti(Connection c, List<JavaxTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> Javax */
    @JdbcUpdate
    @GeneratedVariant("Variant of updateJakarta")
    void updateJavaxMulti(Connection c, List<JavaxTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> Javax */
    @JdbcSelect(where = "\"id\" = :someId")
    @GeneratedVariant("Variant of selectJakartaById")
    List<JavaxTable> selectJavaxByIdMulti(Connection c, int someId) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> Javax */
    @JdbcSelect(where = "\"payload\" = :somePayload")
    @GeneratedVariant("Variant of selectJakartaByPayload")
    List<JavaxTable> selectJavaxByPayloadMulti(Connection c, String somePayload) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> JaggerOnDto */
    @JdbcSelect
    @GeneratedVariant("Variant of selectJakarta")
    List<JaggerOnDtoTable> selectJaggerOnDtoMulti(Connection c) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> JaggerOnDto */
    @JdbcInsert
    @GeneratedVariant("Variant of insertJakarta")
    void insertJaggerOnDtoMulti(Connection c, List<JaggerOnDtoTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> JaggerOnDto */
    @JdbcUpdate
    @GeneratedVariant("Variant of updateJakarta")
    void updateJaggerOnDtoMulti(Connection c, List<JaggerOnDtoTable> entities) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> JaggerOnDto */
    @JdbcSelect(where = "\"id\" = :someId")
    @GeneratedVariant("Variant of selectJakartaById")
    List<JaggerOnDtoTable> selectJaggerOnDtoByIdMulti(Connection c, int someId) throws SQLException;

    /* \(Connection -> Multi(Connection, (\w+) entity -> List<$1> entities, (\w+) select -> List<$1> select, Jakarta -> JaggerOnDto */
    @JdbcSelect(where = "\"payload\" = :somePayload")
    @GeneratedVariant("Variant of selectJakartaByPayload")
    List<JaggerOnDtoTable> selectJaggerOnDtoByPayloadMulti(Connection c, String somePayload) throws SQLException;
}
