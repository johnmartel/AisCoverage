package dk.dma.ais.coverage.persistence;

/**
 * Indicates that the system could not connect to a given database.
 */
public class DatabaseConnectionException extends RuntimeException {

    public DatabaseConnectionException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
