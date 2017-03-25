package dk.dma.ais.coverage.fixture;

import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.calculator.geotools.SphereProjection;
import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.TimeSpan;
import org.apache.commons.lang3.RandomUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class CellFixture {

    public static Cell createCellWithTimeSpans() {
        Cell cell = createCell();

        Map<Long, TimeSpan> fixedWidthTimeSpans = createTimeSpans();
        cell.setFixedWidthSpans(fixedWidthTimeSpans);

        return cell;
    }

    private static Cell createCell() {
        double latitude = randomLatitude();
        double longitude = randomLongitude(latitude);
        String cellId = Helper.getCellId(latitude, longitude, 1);

        Cell cell = new Cell(latitude, longitude, cellId);
        return cell;
    }

    public static double randomLatitude() {
        return Helper.roundLat(SphereProjection.metersToLatDegree(RandomUtils.nextDouble()), 1);
    }

    public static double randomLongitude(double latitude) {
        return Helper.roundLon(SphereProjection.metersToLonDegree(latitude, RandomUtils.nextDouble()), 1);
    }

    private static Map<Long, TimeSpan> createTimeSpans() {
        Map<Long, TimeSpan> fixedWidthTimeSpans = new LinkedHashMap<>();

        for (int i = 0; i <= 1; i++) {
            long timespanKey = ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli() + (i * 10000);
            TimeSpan timespan = new TimeSpan(new Date(timespanKey));
            timespan.setLastMessage(new Date(timespanKey + ((i + 1) * 10000)));
            timespan.setMessageCounterSat(RandomUtils.nextInt());
            timespan.setMessageCounterTerrestrial(RandomUtils.nextInt());
            timespan.setMessageCounterTerrestrialUnfiltered(RandomUtils.nextInt());
            timespan.setMissingSignals(RandomUtils.nextInt());
            timespan.setVsiMessageCounter(RandomUtils.nextInt(1, 10));
            timespan.setAverageSignalStrength(Math.negateExact(RandomUtils.nextInt(10, 60)));

            fixedWidthTimeSpans.put(timespanKey, timespan);
        }

        return fixedWidthTimeSpans;
    }

    public static Cell createCellWithNoTimeSpan() {
        Cell cell = createCell();

        Map<Long, TimeSpan> fixedWidthTimeSpans = new LinkedHashMap<>();
        cell.setFixedWidthSpans(fixedWidthTimeSpans);

        return cell;
    }
}
