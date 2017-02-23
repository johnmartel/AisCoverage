package dk.dma.ais.coverage.persistence;

/**
 * Represents the result of saving a list of cells to a database.
 */
public class PersistenceResult {

    public enum Status {
        SUCCESS, FAILURE
    }

    public static PersistenceResult success(long writtenCells) {
        PersistenceResult result = new PersistenceResult();
        result.status = Status.SUCCESS;
        result.writtenCells = writtenCells;

        return result;
    }

    public static PersistenceResult failure() {
        PersistenceResult result = new PersistenceResult();
        result.status = Status.FAILURE;

        return result;
    }

    private Status status;
    private long writtenCells;

    private PersistenceResult() {
    }

    public Status getStatus() {
        return status;
    }

    public long getWrittenCells() {
        return writtenCells;
    }
}
