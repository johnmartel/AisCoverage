package dk.dma.ais.coverage;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.configuration.bus.AisBusConfiguration;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;

import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builder class that builds test instances of {@link AisCoverage}.
 */
public class AisCoverageBuilder {
    private AisCoverageConfiguration aisCoverageConfiguration;

    public AisCoverageBuilder withMockAisCoverageConfiguration() {
        aisCoverageConfiguration = mock(AisCoverageConfiguration.class);

        return this;
    }

    public AisCoverageBuilder withMockAisBusConfiguration() {
        Objects.requireNonNull(aisCoverageConfiguration, "An AisCoverageConfiguration instance is required. Consider invoking #withMockAisCoverageConfiguration");

        AisBus aisBus = mock(AisBus.class);
        AisBusConfiguration aisBusConfiguration = mock(AisBusConfiguration.class);
        when(aisBusConfiguration.getInstance()).thenReturn(aisBus);
        when(aisCoverageConfiguration.getAisbusConfiguration()).thenReturn(aisBusConfiguration);

        return this;
    }

    public AisCoverage build() {
        if (aisCoverageConfiguration == null) {
            withMockAisCoverageConfiguration();
            withMockAisBusConfiguration();
        }

        return AisCoverage.create(aisCoverageConfiguration);
    }
}
