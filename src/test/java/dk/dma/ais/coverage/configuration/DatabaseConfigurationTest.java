package dk.dma.ais.coverage.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DatabaseConfigurationTest {

    @Test
    public void whenNewInstance_thenPersistenceIntervalInSecondsDefaultsTo60() {
        DatabaseConfiguration configuration = new DatabaseConfiguration();

        assertThat(configuration.getPersistenceIntervalInSeconds(), is(equalTo(60)));
    }
}
