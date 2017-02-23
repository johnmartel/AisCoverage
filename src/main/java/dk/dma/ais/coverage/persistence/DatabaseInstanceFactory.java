package dk.dma.ais.coverage.persistence;

/**
 * Implementations of this factory type create new {@link DatabaseInstance} implementations instances.
 */
public interface DatabaseInstanceFactory {

    DatabaseInstance createDatabaseInstance(String databaseType);
}
