package org.tillerino.jagger.tests.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.jdbc.GeneratedValues.IdentityJakartaPojo;

class GeneratedValuesTest extends AbstractJdbcTest {
    GeneratedValues.Serde serde = SerdeUtil.impl(GeneratedValues.Serde.class);

    @Test
    void roundTripIdentityJakartaPojo() throws SQLException {
        execute(GeneratedValues.Serde.SCHEMA);
        serde.insertAllIdentityJakartaPojo(
                connection, List.of(new IdentityJakartaPojo(3, "test1"), new IdentityJakartaPojo(3, "test2")));

        assertThat(serde.selectAllIdentityJakartaPojo(connection))
                .containsExactly(new IdentityJakartaPojo(1, "test1"), new IdentityJakartaPojo(2, "test2"));
    }
}
