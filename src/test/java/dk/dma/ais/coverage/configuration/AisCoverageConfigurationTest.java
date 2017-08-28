package dk.dma.ais.coverage.configuration;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AisCoverageConfigurationTest {

    @Test
    public void whenNewInstance_thenMessageBufferSizeDefaultsTo10000() {
        AisCoverageConfiguration configuration = new AisCoverageConfiguration();

        assertThat(configuration.getMessageBufferSize(), is(equalTo(10000)));
    }

    @Test
    public void whenNewInstance_thenReceivedPacketsBufferSizeDefaultsTo10000() {
        AisCoverageConfiguration configuration = new AisCoverageConfiguration();

        assertThat(configuration.getReceivedPacketsBufferSize(), is(equalTo(10000)));
    }
}
