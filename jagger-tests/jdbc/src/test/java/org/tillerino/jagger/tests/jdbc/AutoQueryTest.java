package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.jdbc.AutoQuerySerde.JaggerOnDtoTable;
import org.tillerino.jagger.tests.jdbc.AutoQuerySerde.JakartaTable;
import org.tillerino.jagger.tests.jdbc.AutoQuerySerde.JavaxTable;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.NoVariants;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

@Variants({
    @Variant({@Replacement(regex = "Jakarta", replacement = "Javax")}),
    @Variant({
        @Replacement(regex = "Jakarta", replacement = "Explicit"),
        @Replacement(regex = "ExplicitTable\\.SCHEMA", replacement = "AutoQuerySerde.EXPLICIT_SCHEMA"),
        @Replacement(regex = "ExplicitTable", replacement = "JavaxTable"),
    }),
    @Variant({@Replacement(regex = "Jakarta", replacement = "JaggerOnDto")})
})
public class AutoQueryTest extends AbstractJdbcTest {
    AutoQuerySerde serde = SerdeUtil.impl(AutoQuerySerde.class);

    @Test
    void testInsertSelectUpdateJakarta() throws SQLException {
        execute(JakartaTable.SCHEMA);

        serde.insertJakarta(connection, new JakartaTable(1, "test", ""));
        assertThat(serde.selectJakarta(connection)).isEqualTo(new JakartaTable(1, "test", ""));

        serde.updateJakarta(connection, new JakartaTable(1, "updated", ""));
        assertThat(serde.selectJakarta(connection)).isEqualTo(new JakartaTable(1, "updated", ""));
    }

    @Test
    void testInsertSelectUpdateMultiJakarta() throws SQLException {
        execute(JakartaTable.SCHEMA);

        serde.insertJakartaMulti(
                connection, List.of(new JakartaTable(1, "test1", ""), new JakartaTable(2, "test2", "")));
        assertThat(serde.selectJakartaMulti(connection))
                .containsExactlyInAnyOrder(new JakartaTable(1, "test1", ""), new JakartaTable(2, "test2", ""));

        serde.updateJakartaMulti(
                connection, List.of(new JakartaTable(1, "updated1", ""), new JakartaTable(2, "updated2", "")));
        assertThat(serde.selectJakartaMulti(connection))
                .containsExactlyInAnyOrder(new JakartaTable(1, "updated1", ""), new JakartaTable(2, "updated2", ""));
    }

    @Test
    void testSelectByIdJakarta() throws SQLException {
        execute(JakartaTable.SCHEMA);

        serde.insertJakarta(connection, new JakartaTable(1, "test1", ""));
        serde.insertJakarta(connection, new JakartaTable(2, "test2", ""));
        JakartaTable result = serde.selectJakartaById(connection, 1);
        assertThat(result).isEqualTo(new JakartaTable(1, "test1", ""));
    }

    @Test
    void testSelectByIdJakartaMulti() throws SQLException {
        execute(JakartaTable.SCHEMA);

        serde.insertJakartaMulti(
                connection, List.of(new JakartaTable(1, "test1", ""), new JakartaTable(2, "test2", "")));
        List<JakartaTable> result = serde.selectJakartaByIdMulti(connection, 1);
        assertThat(result).containsExactly(new JakartaTable(1, "test1", ""));
    }

    @Test
    void testSelectByPayloadJakarta() throws SQLException {
        execute(JakartaTable.SCHEMA);

        serde.insertJakarta(connection, new JakartaTable(1, "test1", ""));
        serde.insertJakarta(connection, new JakartaTable(2, "test2", ""));
        JakartaTable results = serde.selectJakartaByPayload(connection, "test1");
        assertThat(results).isEqualTo(new JakartaTable(1, "test1", ""));
    }

    @Test
    void testSelectByPayloadJakartaMulti() throws SQLException {
        execute(JakartaTable.SCHEMA);

        serde.insertJakartaMulti(
                connection,
                List.of(
                        new JakartaTable(1, "test1", ""),
                        new JakartaTable(2, "test2", ""),
                        new JakartaTable(3, "test1", "")));
        List<JakartaTable> results = serde.selectJakartaByPayloadMulti(connection, "test1");
        assertThat(results)
                .containsExactlyInAnyOrder(new JakartaTable(1, "test1", ""), new JakartaTable(3, "test1", ""));
    }

    @Test
    @NoVariants
    void testInsertQuoteChar() throws SQLException {
        JdbcMock jdbcMock = JdbcMock.create();

        serde.insertJakartaWithQuoteChar(jdbcMock.c(), new JakartaTable(1, "test1", ""));
        Mockito.verify(jdbcMock.c())
                .prepareStatement("INSERT INTO `jakarta_auto` (`id`, `payload`, `payload2`) VALUES (?, ?, ?)");
    }

    @Test
    @NoVariants
    void testSelectQuoteChar() throws SQLException {
        JdbcMock jdbcMock = JdbcMock.create();

        serde.selectJakartaWithQuoteChar(jdbcMock.c());
        Mockito.verify(jdbcMock.c()).prepareStatement("SELECT * FROM `jakarta_auto`");
    }

    @Test
    @NoVariants
    void testUpdateQuoteChar() throws SQLException {
        JdbcMock jdbcMock = JdbcMock.create();

        serde.updateJakartaWithQuoteChar(jdbcMock.c(), new JakartaTable(1, "test1", ""));
        Mockito.verify(jdbcMock.c())
                .prepareStatement("UPDATE `jakarta_auto` SET (`payload`, `payload2`) = (?, ?) WHERE (`id`) = (?)");
    }

    /* GENERATED CODE. DO NOT MODIFY BELOW!  Jakarta -> Javax */
    @Test
    @GeneratedVariant("Variant of testInsertSelectUpdateJakarta")
    void testInsertSelectUpdateJavax() throws SQLException {
        execute(JavaxTable.SCHEMA);

        serde.insertJavax(connection, new JavaxTable(1, "test", ""));
        assertThat(serde.selectJavax(connection)).isEqualTo(new JavaxTable(1, "test", ""));

        serde.updateJavax(connection, new JavaxTable(1, "updated", ""));
        assertThat(serde.selectJavax(connection)).isEqualTo(new JavaxTable(1, "updated", ""));
    }

    /* Jakarta -> Javax */
    @Test
    @GeneratedVariant("Variant of testInsertSelectUpdateMultiJakarta")
    void testInsertSelectUpdateMultiJavax() throws SQLException {
        execute(JavaxTable.SCHEMA);

        serde.insertJavaxMulti(connection, List.of(new JavaxTable(1, "test1", ""), new JavaxTable(2, "test2", "")));
        assertThat(serde.selectJavaxMulti(connection))
                .containsExactlyInAnyOrder(new JavaxTable(1, "test1", ""), new JavaxTable(2, "test2", ""));

        serde.updateJavaxMulti(
                connection, List.of(new JavaxTable(1, "updated1", ""), new JavaxTable(2, "updated2", "")));
        assertThat(serde.selectJavaxMulti(connection))
                .containsExactlyInAnyOrder(new JavaxTable(1, "updated1", ""), new JavaxTable(2, "updated2", ""));
    }

    /* Jakarta -> Javax */
    @Test
    @GeneratedVariant("Variant of testSelectByIdJakarta")
    void testSelectByIdJavax() throws SQLException {
        execute(JavaxTable.SCHEMA);

        serde.insertJavax(connection, new JavaxTable(1, "test1", ""));
        serde.insertJavax(connection, new JavaxTable(2, "test2", ""));
        JavaxTable result = serde.selectJavaxById(connection, 1);
        assertThat(result).isEqualTo(new JavaxTable(1, "test1", ""));
    }

    /* Jakarta -> Javax */
    @Test
    @GeneratedVariant("Variant of testSelectByIdJakartaMulti")
    void testSelectByIdJavaxMulti() throws SQLException {
        execute(JavaxTable.SCHEMA);

        serde.insertJavaxMulti(connection, List.of(new JavaxTable(1, "test1", ""), new JavaxTable(2, "test2", "")));
        List<JavaxTable> result = serde.selectJavaxByIdMulti(connection, 1);
        assertThat(result).containsExactly(new JavaxTable(1, "test1", ""));
    }

    /* Jakarta -> Javax */
    @Test
    @GeneratedVariant("Variant of testSelectByPayloadJakarta")
    void testSelectByPayloadJavax() throws SQLException {
        execute(JavaxTable.SCHEMA);

        serde.insertJavax(connection, new JavaxTable(1, "test1", ""));
        serde.insertJavax(connection, new JavaxTable(2, "test2", ""));
        JavaxTable results = serde.selectJavaxByPayload(connection, "test1");
        assertThat(results).isEqualTo(new JavaxTable(1, "test1", ""));
    }

    /* Jakarta -> Javax */
    @Test
    @GeneratedVariant("Variant of testSelectByPayloadJakartaMulti")
    void testSelectByPayloadJavaxMulti() throws SQLException {
        execute(JavaxTable.SCHEMA);

        serde.insertJavaxMulti(
                connection,
                List.of(
                        new JavaxTable(1, "test1", ""),
                        new JavaxTable(2, "test2", ""),
                        new JavaxTable(3, "test1", "")));
        List<JavaxTable> results = serde.selectJavaxByPayloadMulti(connection, "test1");
        assertThat(results).containsExactlyInAnyOrder(new JavaxTable(1, "test1", ""), new JavaxTable(3, "test1", ""));
    }

    /* Jakarta -> Explicit, ExplicitTable\.SCHEMA -> AutoQuerySerde.EXPLICIT_SCHEMA, ExplicitTable -> JavaxTable */
    @Test
    @GeneratedVariant("Variant of testInsertSelectUpdateJakarta")
    void testInsertSelectUpdateExplicit() throws SQLException {
        execute(AutoQuerySerde.EXPLICIT_SCHEMA);

        serde.insertExplicit(connection, new JavaxTable(1, "test", ""));
        assertThat(serde.selectExplicit(connection)).isEqualTo(new JavaxTable(1, "test", ""));

        serde.updateExplicit(connection, new JavaxTable(1, "updated", ""));
        assertThat(serde.selectExplicit(connection)).isEqualTo(new JavaxTable(1, "updated", ""));
    }

    /* Jakarta -> Explicit, ExplicitTable\.SCHEMA -> AutoQuerySerde.EXPLICIT_SCHEMA, ExplicitTable -> JavaxTable */
    @Test
    @GeneratedVariant("Variant of testInsertSelectUpdateMultiJakarta")
    void testInsertSelectUpdateMultiExplicit() throws SQLException {
        execute(AutoQuerySerde.EXPLICIT_SCHEMA);

        serde.insertExplicitMulti(connection, List.of(new JavaxTable(1, "test1", ""), new JavaxTable(2, "test2", "")));
        assertThat(serde.selectExplicitMulti(connection))
                .containsExactlyInAnyOrder(new JavaxTable(1, "test1", ""), new JavaxTable(2, "test2", ""));

        serde.updateExplicitMulti(
                connection, List.of(new JavaxTable(1, "updated1", ""), new JavaxTable(2, "updated2", "")));
        assertThat(serde.selectExplicitMulti(connection))
                .containsExactlyInAnyOrder(new JavaxTable(1, "updated1", ""), new JavaxTable(2, "updated2", ""));
    }

    /* Jakarta -> Explicit, ExplicitTable\.SCHEMA -> AutoQuerySerde.EXPLICIT_SCHEMA, ExplicitTable -> JavaxTable */
    @Test
    @GeneratedVariant("Variant of testSelectByIdJakarta")
    void testSelectByIdExplicit() throws SQLException {
        execute(AutoQuerySerde.EXPLICIT_SCHEMA);

        serde.insertExplicit(connection, new JavaxTable(1, "test1", ""));
        serde.insertExplicit(connection, new JavaxTable(2, "test2", ""));
        JavaxTable result = serde.selectExplicitById(connection, 1);
        assertThat(result).isEqualTo(new JavaxTable(1, "test1", ""));
    }

    /* Jakarta -> Explicit, ExplicitTable\.SCHEMA -> AutoQuerySerde.EXPLICIT_SCHEMA, ExplicitTable -> JavaxTable */
    @Test
    @GeneratedVariant("Variant of testSelectByIdJakartaMulti")
    void testSelectByIdExplicitMulti() throws SQLException {
        execute(AutoQuerySerde.EXPLICIT_SCHEMA);

        serde.insertExplicitMulti(connection, List.of(new JavaxTable(1, "test1", ""), new JavaxTable(2, "test2", "")));
        List<JavaxTable> result = serde.selectExplicitByIdMulti(connection, 1);
        assertThat(result).containsExactly(new JavaxTable(1, "test1", ""));
    }

    /* Jakarta -> Explicit, ExplicitTable\.SCHEMA -> AutoQuerySerde.EXPLICIT_SCHEMA, ExplicitTable -> JavaxTable */
    @Test
    @GeneratedVariant("Variant of testSelectByPayloadJakarta")
    void testSelectByPayloadExplicit() throws SQLException {
        execute(AutoQuerySerde.EXPLICIT_SCHEMA);

        serde.insertExplicit(connection, new JavaxTable(1, "test1", ""));
        serde.insertExplicit(connection, new JavaxTable(2, "test2", ""));
        JavaxTable results = serde.selectExplicitByPayload(connection, "test1");
        assertThat(results).isEqualTo(new JavaxTable(1, "test1", ""));
    }

    /* Jakarta -> Explicit, ExplicitTable\.SCHEMA -> AutoQuerySerde.EXPLICIT_SCHEMA, ExplicitTable -> JavaxTable */
    @Test
    @GeneratedVariant("Variant of testSelectByPayloadJakartaMulti")
    void testSelectByPayloadExplicitMulti() throws SQLException {
        execute(AutoQuerySerde.EXPLICIT_SCHEMA);

        serde.insertExplicitMulti(
                connection,
                List.of(
                        new JavaxTable(1, "test1", ""),
                        new JavaxTable(2, "test2", ""),
                        new JavaxTable(3, "test1", "")));
        List<JavaxTable> results = serde.selectExplicitByPayloadMulti(connection, "test1");
        assertThat(results).containsExactlyInAnyOrder(new JavaxTable(1, "test1", ""), new JavaxTable(3, "test1", ""));
    }

    /* Jakarta -> JaggerOnDto */
    @Test
    @GeneratedVariant("Variant of testInsertSelectUpdateJakarta")
    void testInsertSelectUpdateJaggerOnDto() throws SQLException {
        execute(JaggerOnDtoTable.SCHEMA);

        serde.insertJaggerOnDto(connection, new JaggerOnDtoTable(1, "test", ""));
        assertThat(serde.selectJaggerOnDto(connection)).isEqualTo(new JaggerOnDtoTable(1, "test", ""));

        serde.updateJaggerOnDto(connection, new JaggerOnDtoTable(1, "updated", ""));
        assertThat(serde.selectJaggerOnDto(connection)).isEqualTo(new JaggerOnDtoTable(1, "updated", ""));
    }

    /* Jakarta -> JaggerOnDto */
    @Test
    @GeneratedVariant("Variant of testInsertSelectUpdateMultiJakarta")
    void testInsertSelectUpdateMultiJaggerOnDto() throws SQLException {
        execute(JaggerOnDtoTable.SCHEMA);

        serde.insertJaggerOnDtoMulti(
                connection, List.of(new JaggerOnDtoTable(1, "test1", ""), new JaggerOnDtoTable(2, "test2", "")));
        assertThat(serde.selectJaggerOnDtoMulti(connection))
                .containsExactlyInAnyOrder(new JaggerOnDtoTable(1, "test1", ""), new JaggerOnDtoTable(2, "test2", ""));

        serde.updateJaggerOnDtoMulti(
                connection, List.of(new JaggerOnDtoTable(1, "updated1", ""), new JaggerOnDtoTable(2, "updated2", "")));
        assertThat(serde.selectJaggerOnDtoMulti(connection))
                .containsExactlyInAnyOrder(
                        new JaggerOnDtoTable(1, "updated1", ""), new JaggerOnDtoTable(2, "updated2", ""));
    }

    /* Jakarta -> JaggerOnDto */
    @Test
    @GeneratedVariant("Variant of testSelectByIdJakarta")
    void testSelectByIdJaggerOnDto() throws SQLException {
        execute(JaggerOnDtoTable.SCHEMA);

        serde.insertJaggerOnDto(connection, new JaggerOnDtoTable(1, "test1", ""));
        serde.insertJaggerOnDto(connection, new JaggerOnDtoTable(2, "test2", ""));
        JaggerOnDtoTable result = serde.selectJaggerOnDtoById(connection, 1);
        assertThat(result).isEqualTo(new JaggerOnDtoTable(1, "test1", ""));
    }

    /* Jakarta -> JaggerOnDto */
    @Test
    @GeneratedVariant("Variant of testSelectByIdJakartaMulti")
    void testSelectByIdJaggerOnDtoMulti() throws SQLException {
        execute(JaggerOnDtoTable.SCHEMA);

        serde.insertJaggerOnDtoMulti(
                connection, List.of(new JaggerOnDtoTable(1, "test1", ""), new JaggerOnDtoTable(2, "test2", "")));
        List<JaggerOnDtoTable> result = serde.selectJaggerOnDtoByIdMulti(connection, 1);
        assertThat(result).containsExactly(new JaggerOnDtoTable(1, "test1", ""));
    }

    /* Jakarta -> JaggerOnDto */
    @Test
    @GeneratedVariant("Variant of testSelectByPayloadJakarta")
    void testSelectByPayloadJaggerOnDto() throws SQLException {
        execute(JaggerOnDtoTable.SCHEMA);

        serde.insertJaggerOnDto(connection, new JaggerOnDtoTable(1, "test1", ""));
        serde.insertJaggerOnDto(connection, new JaggerOnDtoTable(2, "test2", ""));
        JaggerOnDtoTable results = serde.selectJaggerOnDtoByPayload(connection, "test1");
        assertThat(results).isEqualTo(new JaggerOnDtoTable(1, "test1", ""));
    }

    /* Jakarta -> JaggerOnDto */
    @Test
    @GeneratedVariant("Variant of testSelectByPayloadJakartaMulti")
    void testSelectByPayloadJaggerOnDtoMulti() throws SQLException {
        execute(JaggerOnDtoTable.SCHEMA);

        serde.insertJaggerOnDtoMulti(
                connection,
                List.of(
                        new JaggerOnDtoTable(1, "test1", ""),
                        new JaggerOnDtoTable(2, "test2", ""),
                        new JaggerOnDtoTable(3, "test1", "")));
        List<JaggerOnDtoTable> results = serde.selectJaggerOnDtoByPayloadMulti(connection, "test1");
        assertThat(results)
                .containsExactlyInAnyOrder(new JaggerOnDtoTable(1, "test1", ""), new JaggerOnDtoTable(3, "test1", ""));
    }
}
