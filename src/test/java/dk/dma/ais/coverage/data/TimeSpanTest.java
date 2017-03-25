package dk.dma.ais.coverage.data;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TimeSpanTest {
    private Date now;
    private TimeSpan aTimeSpan;
    private TimeSpan anotherTimeSpan;

    @Before
    public void setUp() throws Exception {
        now = new Date(ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli());
        aTimeSpan = new TimeSpan(now);
        anotherTimeSpan = new TimeSpan(now);
    }

    @Test
    public void whenAdd_thenVsiMessageCounterIsSumOfCurrentAndAddedTimeSpan() {
        aTimeSpan.setVsiMessageCounter(5);
        anotherTimeSpan.setVsiMessageCounter(5);

        aTimeSpan.add(anotherTimeSpan);

        assertThat(aTimeSpan.getVsiMessageCounter(), is(equalTo(10)));
    }

    @Test
    public void whenAdd_thenAverageSignalStrengthComputesAddedTimeSpan() {
        aTimeSpan.setVsiMessageCounter(9);
        aTimeSpan.setAverageSignalStrength(0);
        anotherTimeSpan.setVsiMessageCounter(1);
        anotherTimeSpan.setAverageSignalStrength(-100);

        aTimeSpan.add(anotherTimeSpan);

        assertThat(aTimeSpan.getAverageSignalStrength(), is(equalTo(-10)));
    }

    @Test
    public void whenCopy_thenCopyHasSameVsiMessageCounterAndAverageSignalStrength() {
        aTimeSpan.setVsiMessageCounter(9);
        aTimeSpan.setAverageSignalStrength(-100);

        TimeSpan copy = aTimeSpan.copy();

        assertThat(copy.getVsiMessageCounter(), is(equalTo(9)));
        assertThat(copy.getAverageSignalStrength(), is(equalTo(-100)));
    }

    @Test
    public void whenIncrementNumberOfVsiMessages_thenNumberOfVsiMessagesHasOneAdded() {
        aTimeSpan.setVsiMessageCounter(1);
        aTimeSpan.setAverageSignalStrength(-15);

        aTimeSpan.incrementNumberOfVsiMessages(-10);

        assertThat(aTimeSpan.getVsiMessageCounter(), is(equalTo(2)));
        assertThat(aTimeSpan.getAverageSignalStrength(), is(equalTo(-13)));
    }
}
