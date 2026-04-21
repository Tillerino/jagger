package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.jdbc.NativeTypes.AllTypesPojo;
import org.tillerino.jagger.tests.jdbc.NativeTypes.AllTypesRecord;

public class NativeTypesTest extends AbstractJdbcTest {
    NativeTypes.Serde serde = SerdeUtil.impl(NativeTypes.Serde.class);

    @Test
    void testRoundtripAllTypesRecord() throws SQLException {
        execute(NativeTypes.AllTypesRecord.SCHEMA);

        AllTypesRecord entity = new AllTypesRecord(
                1,
                true,
                (byte) 1,
                (short) 1,
                1,
                1L,
                1.0f,
                2.0,
                true,
                (byte) 2,
                (short) 2,
                2,
                2L,
                3.0f,
                4.0,
                "test",
                new BigDecimal("123.45"),
                Date.valueOf("2024-01-15"),
                new byte[] {1, 2, 3});
        serde.insertAllTypesRecord(connection, List.of(entity));

        AllTypesRecord result = serde.selectAllTypesRecord(connection, 1);
        assertThat(result).isNotSameAs(entity).usingRecursiveComparison().isEqualTo(entity);

        AllTypesRecord updated = new AllTypesRecord(
                1,
                false,
                (byte) 10,
                (short) 10,
                10,
                10L,
                10.0f,
                20.0,
                false,
                (byte) 20,
                (short) 20,
                20,
                20L,
                30.0f,
                40.0,
                "updated",
                new BigDecimal("999.99"),
                Date.valueOf("2025-06-30"),
                new byte[] {4, 5, 6});
        serde.updateAllTypesRecord(connection, updated);

        AllTypesRecord afterUpdate = serde.selectAllTypesRecord(connection, 1);
        assertThat(afterUpdate).isNotSameAs(updated).usingRecursiveComparison().isEqualTo(updated);
    }

    @Test
    void testRoundtripAllTypesRecordWithNulls() throws SQLException {
        execute(NativeTypes.AllTypesRecord.SCHEMA);

        AllTypesRecord entity = new AllTypesRecord(
                1, true, (byte) 1, (short) 1, 1, 1L, 1.0f, 2.0, null, null, null, null, null, null, null, null, null,
                null, null);
        serde.insertAllTypesRecord(connection, List.of(entity));

        AllTypesRecord result = serde.selectAllTypesRecord(connection, 1);
        assertThat(result).isNotSameAs(entity).isEqualTo(entity);
    }

    @Test
    void testSelectAllTypesRecordWithNullsThrowsNpe() throws Exception {
        for (String[] p : NativeTypes.AllTypesRecord.ILLEGAL_NULLS) {
            setUp();

            execute(NativeTypes.AllTypesRecord.SCHEMA);
            execute(p[1]);

            assertThatThrownBy(() -> serde.selectAllTypesRecord(connection, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage(p[0]);
        }
    }

    @Test
    void testRoundtripAllTypesPojo() throws SQLException {
        execute(NativeTypes.AllTypesRecord.SCHEMA);

        AllTypesPojo entity = new AllTypesPojo();
        entity.setId(1);
        entity.setBoolPrim(true);
        entity.setBytePrim((byte) 1);
        entity.setShortPrim((short) 1);
        entity.setIntPrim(1);
        entity.setLongPrim(1L);
        entity.setFloatPrim(1.0f);
        entity.setDoublePrim(2.0);
        entity.setBoolBox(true);
        entity.setByteBox((byte) 2);
        entity.setShortBox((short) 2);
        entity.setIntBox(2);
        entity.setLongBox(2L);
        entity.setFloatBox(3.0f);
        entity.setDoubleBox(4.0);
        entity.setStringVal("test");
        entity.setBigDecimalVal(new BigDecimal("123.45"));
        entity.setDateVal(Date.valueOf("2024-01-15"));
        entity.setBytesVal(new byte[] {1, 2, 3});
        serde.insertAllTypesPojo(connection, List.of(entity));

        AllTypesPojo result = serde.selectAllTypesPojo(connection, 1);
        assertThat(result).isNotSameAs(entity).isEqualTo(entity);

        AllTypesPojo updated = new AllTypesPojo();
        updated.setId(1);
        updated.setBoolPrim(false);
        updated.setBytePrim((byte) 10);
        updated.setShortPrim((short) 10);
        updated.setIntPrim(10);
        updated.setLongPrim(10L);
        updated.setFloatPrim(10.0f);
        updated.setDoublePrim(20.0);
        updated.setBoolBox(false);
        updated.setByteBox((byte) 20);
        updated.setShortBox((short) 20);
        updated.setIntBox(20);
        updated.setLongBox(20L);
        updated.setFloatBox(30.0f);
        updated.setDoubleBox(40.0);
        updated.setStringVal("updated");
        updated.setBigDecimalVal(new BigDecimal("999.99"));
        updated.setDateVal(Date.valueOf("2025-06-30"));
        updated.setBytesVal(new byte[] {4, 5, 6});
        serde.updateAllTypesPojo(connection, updated);

        AllTypesPojo afterUpdate = serde.selectAllTypesPojo(connection, 1);
        assertThat(afterUpdate).isNotSameAs(updated).isEqualTo(updated);
    }

    @Test
    void testRoundtripAllTypesPojoWithNulls() throws SQLException {
        execute(NativeTypes.AllTypesRecord.SCHEMA);

        AllTypesPojo entity = new AllTypesPojo();
        entity.setId(1);
        entity.setBoolPrim(true);
        entity.setBytePrim((byte) 1);
        entity.setShortPrim((short) 1);
        entity.setIntPrim(1);
        entity.setLongPrim(1L);
        entity.setFloatPrim(1.0f);
        entity.setDoublePrim(2.0);
        serde.insertAllTypesPojo(connection, List.of(entity));

        AllTypesPojo result = serde.selectAllTypesPojo(connection, 1);
        assertThat(result).isNotSameAs(entity).isEqualTo(entity);
    }

    @Test
    void testSelectAllTypesPojoWithNullsThrowsNpe() throws Exception {
        for (String[] p : NativeTypes.AllTypesRecord.ILLEGAL_NULLS) {
            setUp();

            execute(NativeTypes.AllTypesRecord.SCHEMA);
            execute(p[1]);

            assertThatThrownBy(() -> serde.selectAllTypesPojo(connection, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage(p[0]);
        }
    }
}
