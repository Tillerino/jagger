package org.tillerino.jagger.tests.jdbc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import lombok.SneakyThrows;
import org.mockito.Mockito;

public record JdbcMock(Connection c, PreparedStatement ps, ResultSet rs) {
    @SneakyThrows
    public static JdbcMock create() {
        Connection c = Mockito.mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        return new JdbcMock(c, ps, rs);
    }
}
