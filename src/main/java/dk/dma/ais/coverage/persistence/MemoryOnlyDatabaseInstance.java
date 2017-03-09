package dk.dma.ais.coverage.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.data.Cell;

/**
 * {@link DatabaseInstance} implementation that keeps data in memory. Data is never persisted anywhere.
 */
class MemoryOnlyDatabaseInstance implements DatabaseInstance {

    @Override
    public void open(DatabaseConfiguration configuration) {
    }

    @Override
    public void createDatabase() {
    }

    @Override
    public PersistenceResult save(Map<String, Collection<Cell>> coverageData) {
        return null;
    }

    @Override
    public Map<String, Collection<Cell>> loadLatestSavedCoverageData() {
        return Collections.emptyMap();
    }

    @Override
    public void close() throws Exception {
    }
}
