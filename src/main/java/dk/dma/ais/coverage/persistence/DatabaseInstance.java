package dk.dma.ais.coverage.persistence;

import java.util.Collection;
import java.util.Map;

import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.data.Cell;

/**
 * Data layer abstraction. Implementations care for the details of each operation for a specific database type.
 */
public interface DatabaseInstance extends AutoCloseable {

    /**
     * Opens the connection to the database based on the provided configuration.
     * Any opened instance should be closed using {@link #close()}.
     *
     * @param configuration
     *      the database configuration parameters
     * @throws DatabaseConnectionException
     *      when connection to the database server is impossible for any reason
     */
    void open(DatabaseConfiguration configuration);

    /**
     * Creates the database where coverage data is collected if required.
     *
     * @throws DatabaseConnectionException
     *      when connection to the database server is impossible for any reason
     */
    void createDatabase();

    /**
     * Saves the coverage data stored in in-memory cells to the underlying database.
     *
     * @param coverageData
     *      the coverage data stored as a {@link Collection} of {@link Cell} by source identifier
     * @return
     *      the result of the save operation
     * @throws DatabaseConnectionException
     *      when connection to the database server is impossible for any reason
     */
    PersistenceResult save(Map<String, Collection<Cell>> coverageData);

    Map<String, Collection<Cell>> loadLatestSavedCoverageData();
}
