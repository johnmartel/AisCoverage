package dk.dma.ais.coverage.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import dk.dma.ais.coverage.data.ICoverageData;

public class PersisterServiceTest {

    @Test
    public void whenStart_thenSaveIsInvokedOnDatabaseInstanceAtInterval() throws InterruptedException {
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        when(databaseInstance.save(anyMap())).thenReturn(PersistenceResult.success(1));
        ICoverageData coverageData = mock(ICoverageData.class);
        PersisterService persisterService = new PersisterService(databaseInstance, coverageData);
        persisterService.intervalInSeconds(1L);

        persisterService.start();
        Thread.sleep(5 * 1000);
        persisterService.stop();

        verify(databaseInstance, atLeast(4)).save(anyMap());
    }

    @Test
    public void whenNewInstance_thenIntervalInSecondsDefaultsTo60() {
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        ICoverageData coverageData = mock(ICoverageData.class);
        PersisterService persisterService = new PersisterService(databaseInstance, coverageData);

        assertThat(persisterService.getIntervalInSeconds(), is(equalTo(60L)));
    }
}
