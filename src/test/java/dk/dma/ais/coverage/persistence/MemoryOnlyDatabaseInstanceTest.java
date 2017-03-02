package dk.dma.ais.coverage.persistence;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import dk.dma.ais.coverage.data.Cell;

public class MemoryOnlyDatabaseInstanceTest {

    @Test
    public void whenSave_thenNullPersistenceResultIsReturned() {
        DatabaseInstance databaseInstance = new MemoryOnlyDatabaseInstance();

        PersistenceResult persistenceResult = databaseInstance.save(emptyList());

        assertThat(persistenceResult, is(nullValue()));
    }

    @Test
    public void whenLoadLatestSavedCoverageData_thenEmptyListIsReturned() {
        DatabaseInstance databaseInstance = new MemoryOnlyDatabaseInstance();

        List<Cell> coverageData = databaseInstance.loadLatestSavedCoverageData();

        assertThat(coverageData.isEmpty(), is(true));
    }
}
