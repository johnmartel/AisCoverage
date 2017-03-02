package dk.dma.ais.coverage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.persistence.DatabaseInstance;
import dk.dma.ais.coverage.persistence.PersisterService;

public class AisCoverageTest {

    @Test
    public void whenNewInstance_thenDatabaseIsCreatedAndExistingDataIsLoaded() {
        DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();
        databaseConfiguration.setType("MongoDB");

        DatabaseInstance mockDatabaseInstance = mock(DatabaseInstance.class);

        aisCoverageWithMockDatabaseInstance(databaseConfiguration, mockDatabaseInstance);

        verify(mockDatabaseInstance).open(databaseConfiguration);
        verify(mockDatabaseInstance).createDatabase();
        verify(mockDatabaseInstance).loadLatestSavedCoverageData();
    }

    private AisCoverage aisCoverageWithMockDatabaseInstance(DatabaseConfiguration databaseConfiguration, DatabaseInstance mockDatabaseInstance) {
        AisCoverageBuilder builder = new AisCoverageBuilder();
        builder.withMockAisCoverageConfiguration()
                .withDatabaseConfiguration(databaseConfiguration)
                .withMockAisBusConfiguration()
                .withDatabaseInstanceForType(mockDatabaseInstance, "MongoDB");

        return builder.build();
    }

    @Test
    public void whenStart_thenPersisterServiceIsStarted() {
        PersisterService persisterService = mock(PersisterService.class);
        AisCoverage aisCoverage = aisCoverageWithMockPersisterService(persisterService);

        aisCoverage.start();

        verify(persisterService).start();
    }

    private AisCoverage aisCoverageWithMockPersisterService(PersisterService persisterService) {
        AisCoverageBuilder builder = new AisCoverageBuilder();
        return builder
                .withMockAisCoverageConfiguration()
                .withMockAisBusConfiguration()
                .withMockDatabaseConfiguration()
                .withMockDatabaseInstanceForAnyType()
                .withPersisterService(persisterService).build();
    }

    @Test
    public void whenStop_thenPersisterServiceIsStopped() {
        PersisterService persisterService = mock(PersisterService.class);
        AisCoverage aisCoverage = aisCoverageWithMockPersisterService(persisterService);

        aisCoverage.stop();

        verify(persisterService).stop();
    }

    @Test
    public void whenStop_thenDatabaseInstanceIsClosed() throws Exception {
        DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();
        databaseConfiguration.setType("MongoDB");

        DatabaseInstance mockDatabaseInstance = mock(DatabaseInstance.class);

        PersisterService persisterService = mock(PersisterService.class);

        AisCoverageBuilder builder = new AisCoverageBuilder();
        AisCoverage aisCoverage = builder
                .withMockAisCoverageConfiguration()
                .withMockAisBusConfiguration()
                .withDatabaseConfiguration(databaseConfiguration)
                .withDatabaseInstanceForType(mockDatabaseInstance, "MongoDB")
                .withPersisterService(persisterService).build();

        aisCoverage.stop();

        verify(mockDatabaseInstance).close();
    }
}
