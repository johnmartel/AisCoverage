package dk.dma.ais.coverage.persistence;

import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;
import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.TimeSpan;
import dk.dma.ais.coverage.fixture.CellFixture;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MongoCoverageDataMarshallerTest {
    private static final double DELTA_WHEN_COMPARING_DOUBLE = 0.0001D;
    private MongoCoverageDataMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new MongoCoverageDataMarshaller();

        Helper.conf = new AisCoverageConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        Helper.conf = null;
    }

    @Test
    public void givenNoCoverageData_whenMarshall_thenDocumentWithEmptyCellsArrayAndTimestampIsReturned() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        Document marshalledCoverageData = marshaller.marshall(null, now);

        assertThatDocumentHasEmptyCellsAndDataTimestamp(marshalledCoverageData, now);

        marshalledCoverageData = marshaller.marshall(Collections.emptyMap(), now);

        assertThatDocumentHasEmptyCellsAndDataTimestamp(marshalledCoverageData, now);
    }

    private void assertThatDocumentHasEmptyCellsAndDataTimestamp(Document marshalledCoverageData, ZonedDateTime now) {
        ZonedDateTime dataTimestamp = ZonedDateTime.parse((String) marshalledCoverageData.get("dataTimestamp"), DateTimeFormatter.ISO_DATE_TIME);
        assertThat(dataTimestamp, is(equalTo(now)));

        Document decompressedCells = decompressCells(marshalledCoverageData);
        assertThat(((List<Map<String, Object>>) decompressedCells.get("cells")).isEmpty(), is(true));
    }

    private Document decompressCells(Document marshalledCoverageData) {
        String base64 = (String) marshalledCoverageData.get("compressedCells");
        byte[] gzippedData = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.US_ASCII));

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzippedData))) {
            String jsonData = IOUtils.toString(gzipInputStream, StandardCharsets.US_ASCII);
            return Document.parse(jsonData);
        } catch (IOException e) {
            fail("Failed decompressing cells: " + e.getMessage());
        }

        return null;
    }

    @Test
    public void givenManyCells_whenMarshall_thenDocumentWithTheseCellsAndTimestampIsReturned() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        List<Cell> cells = new ArrayList<>();
        Cell firstCell = CellFixture.createCellWithTimeSpans();
        Cell secondCell = CellFixture.createCellWithNoTimeSpan();
        cells.add(firstCell);
        cells.add(secondCell);
        Map<String, Collection<Cell>> cellsBySourceId = new LinkedHashMap<>();
        cellsBySourceId.put("default", cells);

        Document marshalledCoverageData = marshaller.marshall(cellsBySourceId, now);

        ZonedDateTime dataTimestamp = ZonedDateTime.parse((String) marshalledCoverageData.get("dataTimestamp"), DateTimeFormatter.ISO_DATE_TIME);
        assertThat(dataTimestamp, is(equalTo(now)));

        Document decompressedCells = decompressCells(marshalledCoverageData);
        List<Map<String, Object>> grid = (List<Map<String, Object>>) decompressedCells.get("cells");
        assertThat(grid.size(), is(equalTo(2)));

        Map<String, Object> firstMarshalledCell = grid.get(0);
        assertThat(firstMarshalledCell.get("cellId"), is(equalTo(firstCell.getId())));
        assertThat(firstMarshalledCell.get("latitude"), is(equalTo(firstCell.getLatitude())));
        assertThat(firstMarshalledCell.get("longitude"), is(equalTo(firstCell.getLongitude())));
        assertThat(firstMarshalledCell.get("numberOfReceivedSignals"), is(equalTo(firstCell.getNOofReceivedSignals())));
        assertThat(firstMarshalledCell.get("numberOfMissingSignals"), is(equalTo(firstCell.getNOofMissingSignals())));
        assertThat(firstMarshalledCell.get("numberOfVsiMessages"), is(equalTo(firstCell.getNumberOfVsiMessages())));
        assertThat(firstMarshalledCell.get("averageSignalStrength"), is(equalTo(firstCell.getAverageSignalStrength())));

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
            assertThat(marshalledTimeSpan.get("vsiMessageCounter"), is(equalTo(expectedTimeSpan.getVsiMessageCounter())));
            assertThat(marshalledTimeSpan.get("averageSignalStrength"), is(equalTo(expectedTimeSpan.getAverageSignalStrength())));
        }

        Map<String, Object> secondMarshalledCell = grid.get(1);
        assertThat(secondMarshalledCell.get("cellId"), is(equalTo(secondCell.getId())));
        assertThat(secondMarshalledCell.get("latitude"), is(equalTo(secondCell.getLatitude())));
        assertThat(secondMarshalledCell.get("longitude"), is(equalTo(secondCell.getLongitude())));
        assertThat(secondMarshalledCell.get("numberOfReceivedSignals"), is(equalTo(secondCell.getNOofReceivedSignals())));
        assertThat(secondMarshalledCell.get("numberOfMissingSignals"), is(equalTo(secondCell.getNOofMissingSignals())));
        assertThat(secondMarshalledCell.get("numberOfVsiMessages"), is(equalTo(secondCell.getNumberOfVsiMessages())));
        assertThat(secondMarshalledCell.get("averageSignalStrength"), is(equalTo(secondCell.getAverageSignalStrength())));

        Map<Long, Map<String, Number>> secondMarshalledCellTimespans = (Map<Long, Map<String, Number>>) secondMarshalledCell.get("timespans");
        assertThat(secondMarshalledCellTimespans.isEmpty(), is(true));
    }

    @Test
    public void givenNoCoverageData_whenUnmarshall_thenEmptyListIsReturned() {
        Map<String, Collection<Cell>> coverageData = marshaller.unmarshall(null);
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
        Map<String, Collection<Cell>> cellsBySourceId = new LinkedHashMap<>();
        cellsBySourceId.put("default", cells);
        Document marshalledCoverageData = marshaller.marshall(cellsBySourceId, now);

        Map<String, Collection<Cell>> unmarshalledCoverageData = marshaller.unmarshall(marshalledCoverageData);

        assertThat(unmarshalledCoverageData.get("default").size(), is(equalTo(2)));

        Iterator<Cell> cellsFromDefaultSource = unmarshalledCoverageData.get("default").iterator();
        Cell firstUnmarshalledCell = cellsFromDefaultSource.next();
        assertThat(firstUnmarshalledCell.getId(), is(equalTo(firstCell.getId())));
        assertThat(firstUnmarshalledCell.getLatitude(), is(closeTo(firstCell.getLatitude(), DELTA_WHEN_COMPARING_DOUBLE)));
        assertThat(firstUnmarshalledCell.getLongitude(), is(closeTo(firstCell.getLongitude(), DELTA_WHEN_COMPARING_DOUBLE)));
        assertThat(firstUnmarshalledCell.getNOofReceivedSignals(), is(equalTo(firstCell.getNOofReceivedSignals())));
        assertThat(firstUnmarshalledCell.getNOofMissingSignals(), is(equalTo(firstCell.getNOofMissingSignals())));
        assertThat(firstUnmarshalledCell.getNumberOfVsiMessages(), is(equalTo(firstCell.getNumberOfVsiMessages())));
        assertThat(firstUnmarshalledCell.getAverageSignalStrength(), is(equalTo(firstCell.getAverageSignalStrength())));

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
            assertThat(unmarshalledTimeSpan.getVsiMessageCounter(), is(equalTo(expectedTimeSpan.getVsiMessageCounter())));
            assertThat(unmarshalledTimeSpan.getAverageSignalStrength(), is(equalTo(expectedTimeSpan.getAverageSignalStrength())));
        }

        Cell secondUnmarshalledCell = cellsFromDefaultSource.next();
        assertThat(secondUnmarshalledCell.getId(), is(equalTo(secondCell.getId())));
        assertThat(secondUnmarshalledCell.getLatitude(), is(closeTo(secondCell.getLatitude(), DELTA_WHEN_COMPARING_DOUBLE)));
        assertThat(secondUnmarshalledCell.getLongitude(), is(closeTo(secondCell.getLongitude(), DELTA_WHEN_COMPARING_DOUBLE)));
        assertThat(secondUnmarshalledCell.getNOofReceivedSignals(), is(equalTo(secondCell.getNOofReceivedSignals())));
        assertThat(secondUnmarshalledCell.getNOofMissingSignals(), is(equalTo(secondCell.getNOofMissingSignals())));
        assertThat(secondUnmarshalledCell.getNumberOfVsiMessages(), is(equalTo(secondCell.getNumberOfVsiMessages())));
        assertThat(secondUnmarshalledCell.getAverageSignalStrength(), is(equalTo(secondCell.getAverageSignalStrength())));

        assertThat(secondUnmarshalledCell.getFixedWidthSpans().isEmpty(), is(true));
    }
}
