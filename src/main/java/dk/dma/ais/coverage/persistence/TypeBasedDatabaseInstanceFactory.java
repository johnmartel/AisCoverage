package dk.dma.ais.coverage.persistence;

/**
 * This {@link DatabaseInstanceFactory} implementation supports creating {@link DatabaseInstance} instances for MongoDB
 * or MemoryOnly.
 */
public class TypeBasedDatabaseInstanceFactory implements DatabaseInstanceFactory {

    @Override
    public DatabaseInstance createDatabaseInstance(String databaseType) {
        if ("MemoryOnly".equalsIgnoreCase(databaseType)) {
            return new MemoryOnlyDatabaseInstance();
        } else if ("MongoDB".equalsIgnoreCase(databaseType)) {
            return new MongoDatabaseInstance();
        } else {
            throw new UnknownDatabaseTypeException(String.format("Unsupported database type: [%s]", databaseType));
        }
    }
}
