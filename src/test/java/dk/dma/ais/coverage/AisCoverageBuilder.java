package dk.dma.ais.coverage;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Objects;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.configuration.bus.AisBusConfiguration;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;
import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.persistence.DatabaseInstance;
import dk.dma.ais.coverage.persistence.DatabaseInstanceFactory;
import dk.dma.ais.coverage.persistence.PersisterService;

/**
 * Builder class that builds test instances of {@link AisCoverage}.
 */
public class AisCoverageBuilder {
    private static final String MISSING_COVERAGE_CONFIGURATION_ERROR_MESSAGE = "An AisCoverageConfiguration instance is required. Consider invoking #withMockAisCoverageConfiguration";

    private AisCoverageConfiguration aisCoverageConfiguration;
    private DatabaseInstanceFactory mockingDatabaseInstanceFactory;
    private PersisterService persisterService;

    public AisCoverageBuilder withMockAisCoverageConfiguration() {
        aisCoverageConfiguration = mock(AisCoverageConfiguration.class);

        return this;
    }

    public AisCoverageBuilder withMockAisBusConfiguration() {
        Objects.requireNonNull(aisCoverageConfiguration, MISSING_COVERAGE_CONFIGURATION_ERROR_MESSAGE);

        AisBus aisBus = mock(AisBus.class);
        AisBusConfiguration aisBusConfiguration = mock(AisBusConfiguration.class);
        when(aisBusConfiguration.getInstance()).thenReturn(aisBus);
        when(aisCoverageConfiguration.getAisbusConfiguration()).thenReturn(aisBusConfiguration);

        return this;
    }

    public AisCoverageBuilder withDatabaseConfiguration(DatabaseConfiguration databaseConfiguration) {
        Objects.requireNonNull(aisCoverageConfiguration, MISSING_COVERAGE_CONFIGURATION_ERROR_MESSAGE);

        when(aisCoverageConfiguration.getDatabaseConfiguration()).thenReturn(databaseConfiguration);

        return this;
    }

    public AisCoverageBuilder withMockDatabaseConfiguration() {
        Objects.requireNonNull(aisCoverageConfiguration, MISSING_COVERAGE_CONFIGURATION_ERROR_MESSAGE);

        DatabaseConfiguration mockDatabaseConfiguration = new DatabaseConfiguration();
        when(aisCoverageConfiguration.getDatabaseConfiguration()).thenReturn(mockDatabaseConfiguration);

        return this;
    }

    public AisCoverageBuilder withDatabaseInstanceForType(DatabaseInstance databaseInstance, String databaseType) {
        mockingDatabaseInstanceFactory = mock(DatabaseInstanceFactory.class);
        when(mockingDatabaseInstanceFactory.createDatabaseInstance(eq(databaseType))).thenReturn(databaseInstance);

        return this;
    }

    public AisCoverageBuilder withMockDatabaseInstanceForAnyType() {
        mockingDatabaseInstanceFactory = mock(DatabaseInstanceFactory.class);
        DatabaseInstance mockDatabaseInstance = mock(DatabaseInstance.class);
        when(mockingDatabaseInstanceFactory.createDatabaseInstance(anyString())).thenReturn(mockDatabaseInstance);

        return this;
    }

    public AisCoverageBuilder withPersisterService(PersisterService persisterService) {
        this.persisterService = persisterService;

        return this;
    }

    public AisCoverage build() {
        if (aisCoverageConfiguration == null) {
            withMockAisCoverageConfiguration();
            withMockAisBusConfiguration();
        }

        AisCoverage aisCoverage = AisCoverage.create(aisCoverageConfiguration, mockingDatabaseInstanceFactory);

        if (persisterService != null) {
            aisCoverage.setPersisterService(persisterService);
        }

        return aisCoverage;
    }
}
