package dk.dma.ais.coverage.data;

import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;
import dk.dma.ais.coverage.fixture.CellFixture;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CellTest {

    @Before
    public void setUp() throws Exception {
        Helper.conf = new AisCoverageConfiguration();
    }

    @Test
    public void whenIncrementNumberOfVsiMessages_thenNumberOfVsiMessagesHasOneAdded() {
        Cell aCell = CellFixture.createCellWithNoTimeSpan();

        aCell.incrementNumberOfVsiMessages(-10);
        aCell.incrementNumberOfVsiMessages(-20);

        assertThat(aCell.getNumberOfVsiMessages(), is(equalTo(2)));
        assertThat(aCell.getAverageSignalStrength(), is(equalTo(-15)));
    }

    @Test
    public void whenAddVsiMessages_thenNumberOfVsiMessagesAreSummedAndAverageSignalStrengthIsRecomputed() {
        Cell aCell = CellFixture.createCellWithNoTimeSpan();

        aCell.incrementNumberOfVsiMessages(-10);
        aCell.incrementNumberOfVsiMessages(-20);

        aCell.addVsiMessages(3, -12);

        assertThat(aCell.getNumberOfVsiMessages(), is(equalTo(5)));
        assertThat(aCell.getAverageSignalStrength(), is(equalTo(-14)));
    }

    @Test
    public void whenGettNumberOfVsiMessagesWithTimestamps_thenNumberOfVsiMessagesFromMatchingTimespansAreReturned() {
        Cell aCell = CellFixture.createCellWithTimeSpans();
        TimeSpan firstTimespan = aCell.getFixedWidthSpans().values().iterator().next();

        int numberOfVsiMessages = aCell.getNumberOfVsiMessages(firstTimespan.getFirstMessage(), firstTimespan.getLastMessage());

        assertThat(numberOfVsiMessages, is(equalTo(firstTimespan.getVsiMessageCounter())));
    }

    @Test
    public void whengetAverageSignalStrength_thenAverageSignalStrengthFromMatchingTimespansAreReturned() {
        Cell aCell = CellFixture.createCellWithTimeSpans();
        Iterator<TimeSpan> timeSpanIterator = aCell.getFixedWidthSpans().values().iterator();
        TimeSpan firstTimespan = timeSpanIterator.next();
        TimeSpan secondTimespan = timeSpanIterator.next();

        int averageSignalStrength = aCell.getAverageSignalStrength(firstTimespan.getFirstMessage(), secondTimespan.getLastMessage());

        int firstTimespanAverageSignalStrength = firstTimespan.getAverageSignalStrength() * firstTimespan.getVsiMessageCounter();
        int secondTimespanAverageSignalStrength = secondTimespan.getAverageSignalStrength() * secondTimespan.getVsiMessageCounter();
        int totalVsiMessagesForBothTimespans = firstTimespan.getVsiMessageCounter() + secondTimespan.getVsiMessageCounter();
        int expectedAverageSignalStrength = Math.floorDiv(firstTimespanAverageSignalStrength + secondTimespanAverageSignalStrength, totalVsiMessagesForBothTimespans);
        assertThat(averageSignalStrength, is(equalTo(expectedAverageSignalStrength)));
    }
}
