package dk.dma.ais.coverage.persistence;

import java.io.IOException;

/**
 * Indicates an exception while marshalling coverage data.
 */
public class MarshallingException extends RuntimeException {

    public MarshallingException(String message, IOException cause) {
        super(message, cause);
    }
}
