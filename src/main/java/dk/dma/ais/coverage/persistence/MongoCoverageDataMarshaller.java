package dk.dma.ais.coverage.persistence;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.TimeSpan;

/**
 * A {@link CoverageDataMarshaller} that marshalls coverage data in a format usable to store in a MongoDB database.
 */
class MongoCoverageDataMarshaller implements CoverageDataMarshaller<Document> {

    @Override
    public Document marshall(Map<String, Collection<Cell>> coverageData, ZonedDateTime dataTimestamp) {
        Document coverageDataDocument = new Document();

        coverageDataDocument.put("dataTimestamp", dataTimestamp.withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME));

        List<Map<String, Object>> grid = new ArrayList<>();
        if (coverageData != null) {
            marshallCells(coverageData, grid);
        }
        coverageDataDocument.put("cells", grid);

        return coverageDataDocument;
    }

    private void marshallCells(Map<String, Collection<Cell>> coverageData, List<Map<String, Object>> grid) {
        for (String sourceId : coverageData.keySet()) {
            for (Cell cell : coverageData.get(sourceId)) {
                Map<String, Object> savedCell = marshallCell(sourceId, cell);
                grid.add(savedCell);
            }
        }
    }

    private Map<String, Object> marshallCell(String sourceId, Cell cell) {
        Map<String, Object> savedCell = new LinkedHashMap<>();
        savedCell.put("sourceId", sourceId);
        savedCell.put("cellId", cell.getId());
        savedCell.put("latitude", cell.getLatitude());
        savedCell.put("longitude", cell.getLongitude());
        savedCell.put("numberOfReceivedSignals", cell.getNOofReceivedSignals());
        savedCell.put("numberOfMissingSignals", cell.getNOofMissingSignals());

        Map<String, Map<String, Number>> fixedWidthTimeSpans = marshallCellTimeSpans(cell);
        savedCell.put("timespans", fixedWidthTimeSpans);

        return savedCell;
    }

    private Map<String, Map<String, Number>> marshallCellTimeSpans(Cell cell) {
        Map<String, Map<String, Number>> fixedWidthTimeSpans = new LinkedHashMap<>();
        for (Map.Entry<Long, TimeSpan> fixedWidthTimeSpan : cell.getFixedWidthSpans().entrySet()) {
            Map<String, Number> messages = new LinkedHashMap<>();
            messages.put("firstMessage", fixedWidthTimeSpan.getValue().getFirstMessage().getTime());
            messages.put("lastMessage", fixedWidthTimeSpan.getValue().getLastMessage().getTime());
            messages.put("messageCounterSat", fixedWidthTimeSpan.getValue().getMessageCounterSat());
            messages.put("messageCounterTerrestrial", fixedWidthTimeSpan.getValue().getMessageCounterTerrestrial());
            messages.put("messageCounterTerrestrialUnfiltered", fixedWidthTimeSpan.getValue().getMessageCounterTerrestrialUnfiltered());
            messages.put("missingSignals", fixedWidthTimeSpan.getValue().getMissingSignals());

            fixedWidthTimeSpans.put(fixedWidthTimeSpan.getKey().toString(), messages);
        }

        return fixedWidthTimeSpans;
    }

    @Override
    public Map<String, Collection<Cell>> unmarshall(Document coverageData) {
        Map<String, Collection<Cell>> unmarshalledCoverageData = new LinkedHashMap<>();

        if (coverageData != null) {
            unmarshalledCoverageData.putAll(unmarshallCells(coverageData));
        }

        return unmarshalledCoverageData;
    }

    private Map<String, Collection<Cell>> unmarshallCells(Document coverageData) {
        Map<String, Collection<Cell>> unmarshalledCoverageData = new LinkedHashMap<>();
        Object cells = coverageData.get("cells");

        if (cells != null && (cells instanceof List)) {
            List<Map<String, Object>> grid = (List<Map<String, Object>>) cells;
            for (Map<String, Object> cell : grid) {
                Cell unmarshalledCell = unmarshallCell(cell);
                String sourceId = (String) cell.get("sourceId");
                if (unmarshalledCoverageData.containsKey(sourceId)) {
                    unmarshalledCoverageData.get(sourceId).add(unmarshalledCell);
                } else {
                    unmarshalledCoverageData.put(sourceId, new ArrayList<>(Arrays.asList(unmarshalledCell)));
                }
            }
        }

        return unmarshalledCoverageData;
    }

    private Cell unmarshallCell(Map<String, Object> cell) {
        double latitude = Helper.roundLat((double) cell.get("latitude"), 1);
        double longitude = Helper.roundLon((double) cell.get("longitude"), 1);
        Cell unmarshalledCell = new Cell(latitude, longitude, (String) cell.get("cellId"));
        unmarshalledCell.addReceivedSignals(((Integer) cell.get("numberOfReceivedSignals")).intValue());
        unmarshalledCell.addNOofMissingSignals(((Integer) cell.get("numberOfMissingSignals")).intValue());

        Map<String, Map<String, Number>> fixedWidthTimeSpans = (Map<String, Map<String, Number>>) cell.get("timespans");
        Map<Long, TimeSpan> unmarshalledTimeSpans = unmarshallTimeSpans(fixedWidthTimeSpans);

        unmarshalledCell.setFixedWidthSpans(unmarshalledTimeSpans);
        return unmarshalledCell;
    }

    private Map<Long, TimeSpan> unmarshallTimeSpans(Map<String, Map<String, Number>> fixedWidthTimeSpans) {
        Map<Long, TimeSpan> unmarshalledTimeSpans = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Number>> timespan : fixedWidthTimeSpans.entrySet()) {
            TimeSpan unmarshalledTimeSpan = new TimeSpan(new Date(timespan.getValue().get("firstMessage").longValue()));
            unmarshalledTimeSpan.setLastMessage(new Date(timespan.getValue().get("lastMessage").longValue()));
            unmarshalledTimeSpan.setMessageCounterSat(timespan.getValue().get("messageCounterSat").intValue());
            unmarshalledTimeSpan.setMessageCounterTerrestrial(timespan.getValue().get("messageCounterTerrestrial").intValue());
            unmarshalledTimeSpan.setMessageCounterTerrestrialUnfiltered(timespan.getValue().get("messageCounterTerrestrialUnfiltered").intValue());
            unmarshalledTimeSpan.setMissingSignals(timespan.getValue().get("missingSignals").intValue());
            unmarshalledTimeSpans.put(Long.valueOf(timespan.getKey()), unmarshalledTimeSpan);
        }
        return unmarshalledTimeSpans;
    }
}
