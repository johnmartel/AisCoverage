package dk.dma.ais.coverage.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.TimeSpan;
import dk.dma.ais.coverage.fixture.CellFixture;

public class MongoCoverageDataMarshallerTest {
    private MongoCoverageDataMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new MongoCoverageDataMarshaller();
    }

    @Test
    public void givenNoCoverageData_whenMarshall_thenDocumentWithEmptyCellsArrayAndTimestampIsReturned() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        Document marshalledCoverageData = marshaller.marshall(null, now);

        assertThatDocumentHasEmptyCellsAndDataTimestamp(marshalledCoverageData, now);

        marshalledCoverageData = marshaller.marshall(Collections.emptyList(), now);

        assertThatDocumentHasEmptyCellsAndDataTimestamp(marshalledCoverageData, now);
    }

    private void assertThatDocumentHasEmptyCellsAndDataTimestamp(Document marshalledCoverageData, ZonedDateTime now) {
        ZonedDateTime dataTimestamp = ZonedDateTime.parse((String) marshalledCoverageData.get("dataTimestamp"), DateTimeFormatter.ISO_DATE_TIME);
        assertThat(dataTimestamp, is(equalTo(now)));
        assertThat(((List<Map<String, Object>>) marshalledCoverageData.get("cells")).isEmpty(), is(true));
    }

    @Test
    public void givenManyCells_whenMarshall_thenDocumentWithTheseCellsAndTimestampIsReturned() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        List<Cell> cells = new ArrayList<>();
        Cell firstCell = CellFixture.createCellWithTimeSpans();
        Cell secondCell = CellFixture.createCellWithNoTimeSpan();
        cells.add(firstCell);
        cells.add(secondCell);

        Document marshalledCoverageData = marshaller.marshall(cells, now);

        ZonedDateTime dataTimestamp = ZonedDateTime.parse((String) marshalledCoverageData.get("dataTimestamp"), DateTimeFormatter.ISO_DATE_TIME);
        assertThat(dataTimestamp, is(equalTo(now)));

        List<Map<String, Object>> grid = (List<Map<String, Object>>) marshalledCoverageData.get("cells");
        assertThat(grid.size(), is(equalTo(2)));

        Map<String, Object> firstMarshalledCell = grid.get(0);
        assertThat(firstMarshalledCell.get("cellId"), is(equalTo(firstCell.getId())));
        assertThat(firstMarshalledCell.get("latitude"), is(equalTo(firstCell.getLatitude())));
        assertThat(firstMarshalledCell.get("longitude"), is(equalTo(firstCell.getLongitude())));

        Map<String, Map<String, Number>> firstMarshalledCellTimespans = (Map<String, Map<String, Number>>) firstMarshalledCell.get("timespans");
        assertThat(firstMarshalledCellTimespans.size(), is(equalTo(2)));

        for (Long timespanKey : firstCell.getFixedWidthSpans().keySet()) {
            TimeSpan expectedTimeSpan = firstCell.getFixedWidthSpans().get(timespanKey);
            Map<String, Number> marshalledTimeSpan = firstMarshalledCellTimespans.get(timespanKey.toString());

            assertThat(marshalledTimeSpan.get("firstMessage"), is(equalTo(expectedTimeSpan.getFirstMessage().getTime())));
            assertThat(marshalledTimeSpan.get("lastMessage"), is(equalTo(expectedTimeSpan.getLastMessage().getTime())));
            assertThat(marshalledTimeSpan.get("messageCounterSat"), is(equalTo(expectedTimeSpan.getMessageCounterSat())));
            assertThat(marshalledTimeSpan.get("messageCounterTerrestrial"), is(equalTo(expectedTimeSpan.getMessageCounterTerrestrial())));
            assertThat(marshalledTimeSpan.get("messageCounterTerrestrialUnfiltered"), is(equalTo(expectedTimeSpan.getMessageCounterTerrestrialUnfiltered())));
            assertThat(marshalledTimeSpan.get("missingSignals"), is(equalTo(expectedTimeSpan.getMissingSignals())));
        }

        Map<String, Object> secondMarshalledCell = grid.get(1);
        assertThat(secondMarshalledCell.get("cellId"), is(equalTo(secondCell.getId())));
        assertThat(secondMarshalledCell.get("latitude"), is(equalTo(secondCell.getLatitude())));
        assertThat(secondMarshalledCell.get("longitude"), is(equalTo(secondCell.getLongitude())));

        Map<Long, Map<String, Number>> secondMarshalledCellTimespans = (Map<Long, Map<String, Number>>) secondMarshalledCell.get("timespans");
        assertThat(secondMarshalledCellTimespans.isEmpty(), is(true));
    }

    @Test
    public void givenNoCoverageData_whenUnmarshall_thenEmptyListIsReturned() {
        List<Cell> coverageData = marshaller.unmarshall(null);
        assertThat(coverageData.isEmpty(), is(true));

        coverageData = marshaller.unmarshall(new Document());
        assertThat(coverageData.isEmpty(), is(true));

        Document documentWithEmptyCells = new Document();
        documentWithEmptyCells.put("cells", Collections.emptyMap());
        coverageData = marshaller.unmarshall(documentWithEmptyCells);
        assertThat(coverageData.isEmpty(), is(true));
    }

    @Test
    public void givenManyCells_whenUnmarshall_thenTheseCellsAreReturned() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        List<Cell> cells = new ArrayList<>();
        Cell firstCell = CellFixture.createCellWithTimeSpans();
        Cell secondCell = CellFixture.createCellWithNoTimeSpan();
        cells.add(firstCell);
        cells.add(secondCell);
        Document marshalledCoverageData = marshaller.marshall(cells, now);

        List<Cell> unmarshalledCoverageData = marshaller.unmarshall(marshalledCoverageData);

        assertThat(unmarshalledCoverageData.size(), is(equalTo(2)));

        Cell firstUnmarshalledCell = unmarshalledCoverageData.get(0);
        assertThat(firstUnmarshalledCell.getId(), is(equalTo(firstCell.getId())));
        assertThat(firstUnmarshalledCell.getLatitude(), is(equalTo(firstCell.getLatitude())));
        assertThat(firstUnmarshalledCell.getLongitude(), is(equalTo(firstCell.getLongitude())));

        Map<Long, TimeSpan> firstUnmarshalledCellFixedWidthSpans = firstUnmarshalledCell.getFixedWidthSpans();
        assertThat(firstUnmarshalledCellFixedWidthSpans.size(), is(equalTo(2)));

        for (Long timespanKey : firstUnmarshalledCellFixedWidthSpans.keySet()) {
            TimeSpan expectedTimeSpan = firstCell.getFixedWidthSpans().get(timespanKey);
            TimeSpan unmarshalledTimeSpan = firstUnmarshalledCellFixedWidthSpans.get(timespanKey);

            assertThat(unmarshalledTimeSpan.getFirstMessage(), is(equalTo(expectedTimeSpan.getFirstMessage())));
            assertThat(unmarshalledTimeSpan.getLastMessage(), is(equalTo(expectedTimeSpan.getLastMessage())));
            assertThat(unmarshalledTimeSpan.getMessageCounterSat(), is(equalTo(expectedTimeSpan.getMessageCounterSat())));
            assertThat(unmarshalledTimeSpan.getMessageCounterTerrestrial(), is(equalTo(expectedTimeSpan.getMessageCounterTerrestrial())));
            assertThat(unmarshalledTimeSpan.getMessageCounterTerrestrialUnfiltered(), is(equalTo(expectedTimeSpan.getMessageCounterTerrestrialUnfiltered())));
            assertThat(unmarshalledTimeSpan.getMissingSignals(), is(equalTo(expectedTimeSpan.getMissingSignals())));
        }

        Cell secondUnmarshalledCell = unmarshalledCoverageData.get(1);
        assertThat(secondUnmarshalledCell.getId(), is(equalTo(secondCell.getId())));
        assertThat(secondUnmarshalledCell.getLatitude(), is(equalTo(secondCell.getLatitude())));
        assertThat(secondUnmarshalledCell.getLongitude(), is(equalTo(secondCell.getLongitude())));

        assertThat(secondUnmarshalledCell.getFixedWidthSpans().isEmpty(), is(true));
    }
}
