package org.mule.extension.db.unit;

import org.junit.Test;
import org.mockito.Mock;
import org.mule.extension.db.internal.result.resultset.ResultSetIterator;
import org.mule.extension.db.internal.result.row.RowHandler;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultSetIteratorTestCase {

    @Mock
    private final ResultSet resultSetMock = mock(ResultSet.class);

    @Mock
    private final RowHandler rowHandlerMock = mock(RowHandler.class);

    private final ResultSetIterator resultSetIterator = new ResultSetIterator(this.resultSetMock, this.rowHandlerMock);

    @Test(expected = ModuleException.class)
    public void next_WhenProcessingNextRowThrowsSQLException_ThenModuleExceptionIsCreated() throws SQLException {
        when(this.rowHandlerMock.process(any(ResultSet.class))).thenThrow(new SQLException("Some SQL Exception"));

        this.resultSetIterator.next();
    }
}
