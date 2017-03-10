package dk.dma.ais.coverage.configuration;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DatabaseConfigurationTest {

    @Test
    public void whenNewInstance_thenPersistenceIntervalInMinutesDefaultsTo60() {
        DatabaseConfiguration configuration = new DatabaseConfiguration();

        assertThat(configuration.getPersistenceIntervalInMinutes(), is(equalTo(60)));
    }
}
