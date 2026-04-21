package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.jdbc.IgnoreProperty.*;
import org.tillerino.jagger.tests.variants.GeneratedVariant;
import org.tillerino.jagger.tests.variants.Variant;
import org.tillerino.jagger.tests.variants.Variants;

public class IgnorePropertyTest extends JdbcTest {
    IgnoreProperty.Serde serde = SerdeUtil.impl(IgnoreProperty.Serde.class);

    @Variants({
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JavaxTransientFieldPojo")}),
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JakartaTransientFieldPojo")}),
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JavaxTransientComponentRecord")}),
        @Variant({@Replacement(regex = "JavaTransientFieldPojo", replacement = "JakartaTransientComponentRecord")}),
    })
    @Test
    void roundTripJavaTransientFieldPojo() throws SQLException {
        execute(IgnoreProperty.Serde.SCHEMA);

        JavaTransientFieldPojo original = new JavaTransientFieldPojo(1, "test", "bar");
        serde.insertAllJavaTransientFieldPojo(connection, List.of(original));

        List<JavaTransientFieldPojo> selected = serde.selectAllJavaTransientFieldPojo(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(original);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(original);

        JavaTransientFieldPojo update = new JavaTransientFieldPojo(1, "update", "bar");
        serde.updateAllJavaTransientFieldPojo(connection, List.of(update));

        selected = serde.selectAllJavaTransientFieldPojo(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(update);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(update);
    }

    /* GENERATED CODE. DO NOT MODIFY BELOW!  JavaTransientFieldPojo -> JavaxTransientFieldPojo */
    @Test
    @GeneratedVariant("Variant of roundTripJavaTransientFieldPojo")
    void roundTripJavaxTransientFieldPojo() throws SQLException {
        execute(IgnoreProperty.Serde.SCHEMA);

        JavaxTransientFieldPojo original = new JavaxTransientFieldPojo(1, "test", "bar");
        serde.insertAllJavaxTransientFieldPojo(connection, List.of(original));

        List<JavaxTransientFieldPojo> selected = serde.selectAllJavaxTransientFieldPojo(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(original);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(original);

        JavaxTransientFieldPojo update = new JavaxTransientFieldPojo(1, "update", "bar");
        serde.updateAllJavaxTransientFieldPojo(connection, List.of(update));

        selected = serde.selectAllJavaxTransientFieldPojo(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(update);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(update);
    }

    /* JavaTransientFieldPojo -> JakartaTransientFieldPojo */
    @Test
    @GeneratedVariant("Variant of roundTripJavaTransientFieldPojo")
    void roundTripJakartaTransientFieldPojo() throws SQLException {
        execute(IgnoreProperty.Serde.SCHEMA);

        JakartaTransientFieldPojo original = new JakartaTransientFieldPojo(1, "test", "bar");
        serde.insertAllJakartaTransientFieldPojo(connection, List.of(original));

        List<JakartaTransientFieldPojo> selected = serde.selectAllJakartaTransientFieldPojo(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(original);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(original);

        JakartaTransientFieldPojo update = new JakartaTransientFieldPojo(1, "update", "bar");
        serde.updateAllJakartaTransientFieldPojo(connection, List.of(update));

        selected = serde.selectAllJakartaTransientFieldPojo(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(update);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(update);
    }

    /* JavaTransientFieldPojo -> JavaxTransientComponentRecord */
    @Test
    @GeneratedVariant("Variant of roundTripJavaTransientFieldPojo")
    void roundTripJavaxTransientComponentRecord() throws SQLException {
        execute(IgnoreProperty.Serde.SCHEMA);

        JavaxTransientComponentRecord original = new JavaxTransientComponentRecord(1, "test", "bar");
        serde.insertAllJavaxTransientComponentRecord(connection, List.of(original));

        List<JavaxTransientComponentRecord> selected = serde.selectAllJavaxTransientComponentRecord(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(original);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(original);

        JavaxTransientComponentRecord update = new JavaxTransientComponentRecord(1, "update", "bar");
        serde.updateAllJavaxTransientComponentRecord(connection, List.of(update));

        selected = serde.selectAllJavaxTransientComponentRecord(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(update);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(update);
    }

    /* JavaTransientFieldPojo -> JakartaTransientComponentRecord */
    @Test
    @GeneratedVariant("Variant of roundTripJavaTransientFieldPojo")
    void roundTripJakartaTransientComponentRecord() throws SQLException {
        execute(IgnoreProperty.Serde.SCHEMA);

        JakartaTransientComponentRecord original = new JakartaTransientComponentRecord(1, "test", "bar");
        serde.insertAllJakartaTransientComponentRecord(connection, List.of(original));

        List<JakartaTransientComponentRecord> selected = serde.selectAllJakartaTransientComponentRecord(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(original);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(original);

        JakartaTransientComponentRecord update = new JakartaTransientComponentRecord(1, "update", "bar");
        serde.updateAllJakartaTransientComponentRecord(connection, List.of(update));

        selected = serde.selectAllJakartaTransientComponentRecord(connection);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0)).isNotEqualTo(update);
        assertThat(selected.get(0))
                .usingRecursiveComparison()
                .ignoringFields("foo")
                .isEqualTo(update);
    }
}
