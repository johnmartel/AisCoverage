package dk.dma.ais.coverage.persistence;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import dk.dma.ais.coverage.data.Cell;

public class MemoryOnlyDatabaseInstanceTest {

    @Test
    public void whenSave_thenNullPersistenceResultIsReturned() {
        DatabaseInstance databaseInstance = new MemoryOnlyDatabaseInstance();

        PersistenceResult persistenceResult = databaseInstance.save(emptyMap());

        assertThat(persistenceResult, is(nullValue()));
    }

    @Test
    public void whenLoadLatestSavedCoverageData_thenEmptyListIsReturned() {
        DatabaseInstance databaseInstance = new MemoryOnlyDatabaseInstance();

        Map<String, Collection<Cell>> coverageData = databaseInstance.loadLatestSavedCoverageData();

        assertThat(coverageData.isEmpty(), is(true));
    }
}
