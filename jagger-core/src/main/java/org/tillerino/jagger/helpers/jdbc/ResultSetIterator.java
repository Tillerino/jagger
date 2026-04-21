package org.tillerino.jagger.helpers.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import org.apache.commons.lang3.function.FailableFunction;

public class ResultSetIterator<T> implements Iterator<T> {
    public final ResultSet resultSet;
    public final FailableFunction<ResultSet, T, SQLException> mapping;
    public final PreparedStatement parent;
    private State state = State.UNKNOWN;

    public ResultSetIterator(
            ResultSet resultSet, PreparedStatement parent, FailableFunction<ResultSet, T, SQLException> mapping) {
        this.resultSet = resultSet;
        this.parent = parent;
        this.mapping = mapping;
    }

    @Override
    public boolean hasNext() {
        if (state == State.CLOSED) {
            return false;
        }
        if (state == State.UNKNOWN) {
            try {
                if (resultSet.next()) {
                    state = State.HAS_NEXT;
                } else {
                    state = State.CLOSED;
                    try {
                        resultSet.close();
                    } finally {
                        if (parent != null) {
                            parent.close();
                        }
                    }
                }
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
        return state == State.HAS_NEXT;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        state = State.UNKNOWN;
        try {
            return mapping.apply(resultSet);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    enum State {
        UNKNOWN,
        HAS_NEXT,
        CLOSED
    }
}
