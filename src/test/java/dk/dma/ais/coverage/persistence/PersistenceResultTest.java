package dk.dma.ais.coverage.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PersistenceResultTest {

    @Test
    public void whenSuccess_thenResultStatusIsSuccessAndWrittenNumberOfCellsIsAsProvided() {
        PersistenceResult result = PersistenceResult.success(3L);

        assertThat(result.getStatus(), is(equalTo(PersistenceResult.Status.SUCCESS)));
        assertThat(result.getWrittenCells(), is(3L));
    }

    @Test
    public void whenFailure_thenResultStatusIsFailureAndWrittenNumberOfCellsIsZero() {
        PersistenceResult result = PersistenceResult.failure();

        assertThat(result.getStatus(), is(equalTo(PersistenceResult.Status.FAILURE)));
        assertThat(result.getWrittenCells(), is(0L));
    }
}
