package dk.dma.ais.coverage.persistence;

/**
 * Indicates that an unknown or unsupported database type has been specified.
 */
public class UnknownDatabaseTypeException extends RuntimeException {

    public UnknownDatabaseTypeException(String message) {
        super(message);
    }
}
