package dk.dma.ais.coverage.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.calculator.AbstractCalculator;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;
import dk.dma.ais.coverage.fixture.CellFixture;

public class OnlyMemoryDataTest {

    private ICoverageData coverageData;
    private Cell aCell;
    private Date now;

    @Before
    public void setUp() throws Exception {
        Helper.conf = new AisCoverageConfiguration();

        coverageData = new OnlyMemoryData();
        aCell = CellFixture.createCellWithNoTimeSpan();
        coverageData.updateCell(AbstractCalculator.SUPERSOURCE_MMSI, aCell);
        now = new Date(ZonedDateTime.now(ZoneId.of("UTC")).toInstant().getEpochSecond());
    }

    @Test
    public void whenIncrementMissingSignals_thenCellGlobalMissingSignalsAreIncremented() {
        coverageData.incrementMissingSignals(AbstractCalculator.SUPERSOURCE_MMSI, aCell.getLatitude(), aCell.getLongitude(), now);

        assertThat(aCell.getNOofMissingSignals(), is(equalTo(1)));
    }

    @Test
    public void whenIncrementReceivedSignals_thenCellGlobalReceivedSignalsAreIncremented() {
        coverageData.incrementReceivedSignals(AbstractCalculator.SUPERSOURCE_MMSI, aCell.getLatitude(), aCell.getLongitude(), now);

        assertThat(aCell.getNOofReceivedSignals(), is(equalTo(1)));
    }
}
