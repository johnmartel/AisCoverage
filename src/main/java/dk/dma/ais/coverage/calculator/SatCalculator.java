/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.ais.coverage.calculator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.CustomMessage;
import dk.dma.ais.coverage.data.QueryParams;
import dk.dma.ais.coverage.data.Ship;
import dk.dma.ais.coverage.data.Ship.Hour;
import dk.dma.ais.coverage.data.Source;
import dk.dma.ais.coverage.data.Source.ReceiverType;
import dk.dma.ais.coverage.data.TimeSpan;
import dk.dma.ais.coverage.export.data.ExportShipTimeSpan;
import dk.dma.ais.packet.AisPacketTags.SourceType;

public class SatCalculator extends AbstractCalculator {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SatCalculator.class);
    private int timeMargin = 600000; // in ms

    /**
     * Retrieves a list of time spans based on a rectangle defined by two lat-lon points. Cells within the rectangle each contain a
     * number of time spans. Spans over the same time are merged.
     */
    public List<TimeSpan> getFixedTimeSpans(Date startTime, Date endTime, double latMin, double latMax, double lonMin,
            double lonMax, int interval) {

        // Initialize timespans
        Date floorDate = Helper.getFloorDate(startTime);
        Date ceilDate = Helper.getFloorDate(endTime);
        List<TimeSpan> result = new ArrayList<TimeSpan>();
        Map<Long, TimeSpan> timespanMap = new HashMap<Long, TimeSpan>();
        int timeDifference = (int) Math.ceil((ceilDate.getTime() - floorDate.getTime()) / 1000 / 60 / 60); // in hours
        for (int i = 0; i < timeDifference; i++) {
            TimeSpan t = new TimeSpan(new Date(floorDate.getTime() + (1000 * 60 * 60 * i)));
            t.setLastMessage(new Date(floorDate.getTime() + (1000 * 60 * 60 * (i + 1))));
            result.add(t);
            timespanMap.put(t.getFirstMessage().getTime(), t);
        }

        Collection<Cell> cells = dataHandler.getSource(AbstractCalculator.SUPERSOURCE_MMSI).getGrid().values();

        for (Cell fixedSpanCell : cells) {
            
            if (Helper.isInsideBox(fixedSpanCell, latMax, lonMin, lonMin, lonMax)) {

                Collection<TimeSpan> spans = fixedSpanCell.getFixedWidthSpans().values();
                for (TimeSpan timeSpan : spans) {
                    if (timeSpan.getLastMessage().getTime() <= endTime.getTime()
                            && timeSpan.getFirstMessage().getTime() >= startTime.getTime()) {
                        TimeSpan resultSpan = timespanMap.get(timeSpan.getFirstMessage().getTime());
                        resultSpan.add(timeSpan);
                    }
                }
            }
        }

        return result;
    }

    public List<ExportShipTimeSpan> getShipDynamicTimeSpans(Date startTime, Date endTime, int shipMmsi) {
        Ship ss = dataHandler.getShip(shipMmsi);
        if (ss == null) {
            return null;
        }

        List<ExportShipTimeSpan> result = new ArrayList<ExportShipTimeSpan>();
        short startHour = (short) ((startTime.getTime() - Helper.analysisStarted.getTime()) / 1000 / 60 / 60);
        short endHour = (short) ((endTime.getTime() - Helper.analysisStarted.getTime()) / 1000 / 60 / 60);
        int previousMinute = -1;
        int currentMinute = -1;
        for (short i = startHour; i < endHour; i++) {
            Hour h = ss.getHours().get(i);
            if (h != null) {
                for (int j = 0; j < 60; j++) {
                    boolean gotSignal = h.gotSignal(j);
                    if (gotSignal) {
                        currentMinute = i * 60 + j;
                        if (result.isEmpty()) {
                            // If result is empty, add the first timespan
                            ExportShipTimeSpan timespan = new ExportShipTimeSpan(Helper.analysisStarted.getTime() + currentMinute
                                    * 60 * 1000);
                            timespan.getPositions().add(timespan.new LatLon(h.getLat(j), h.getLon(j)));
                            result.add(timespan);
                        }
                        if (currentMinute - previousMinute < timeMargin / 1000 / 60) {
                            // If current minute is within the time margin, we expand the latest timespan
                            ExportShipTimeSpan timespan = result.get(result.size() - 1);
                            timespan.setLastMessage(Helper.analysisStarted.getTime() + currentMinute * 60 * 1000);
                            timespan.getPositions().add(timespan.new LatLon(h.getLat(j), h.getLon(j)));

                        } else {
                            // If current minute exceeds the time margin, we add a new timespan
                            ExportShipTimeSpan timespan = new ExportShipTimeSpan(Helper.analysisStarted.getTime() + currentMinute
                                    * 60 * 1000);
                            timespan.getPositions().add(timespan.new LatLon(h.getLat(j), h.getLon(j)));
                            result.add(timespan);
                        }
                        previousMinute = i * 60 + j;

                    }
                }
            }
        }
        return result;

    }

    /**
     * Retrieves a list of time spans based on a rectangle defined by two lat-lon points. Cells within the rectangle each contain a
     * number of time spans. Two time spans will be merged if they are close to each other (closeness is defined by timeMargin). In
     * that way the rectangle given by the user will be seen as one big cell.
     * 
     * @param latStart
     * @param lonStart
     * @param latEnd
     * @param lonEnd
     * @return
     */
    public List<TimeSpan> getDynamicTimeSpans(Date startTime, Date endTime, double latMin, double latMax, double lonMin,
            double lonMax) {

        // Retrieve cells within the specified rectangle
        Collection<Cell> cells = dataHandler.getCells(null);
        List<Cell> areaFiltered = new ArrayList<Cell>();
        for (Cell cell : cells) {

            if (Helper.isInsideBox(cell, latMax, lonMin, lonMin, lonMax)) {
                areaFiltered.add(cell);
            }

        }

        // Store every time span of the filtered cells
        List<TimeSpan> spans = new ArrayList<TimeSpan>();
        if (startTime != null && endTime != null) {
            for (Cell cell : areaFiltered) {
                List<TimeSpan> individualSpan = cell.getTimeSpans();
                if (cell.getTimeSpans() != null) {
                    for (TimeSpan timeSpan : individualSpan) {
                        if (timeSpan.getFirstMessage().getTime() > startTime.getTime()
                                && timeSpan.getLastMessage().getTime() < endTime.getTime()) {
                            spans.add(timeSpan);
                        }
                    }
                }
            }
        } else {
            for (Cell cell : areaFiltered) {
                List<TimeSpan> individualSpan = cell.getTimeSpans();
                if (cell.getTimeSpans() != null) {
                    for (TimeSpan timeSpan : individualSpan) {
                        spans.add(timeSpan);
                    }
                }
            }
        }

        // sort time spans based on date
        Collections.sort(spans, new SortByDate());

        // Merge time spans that are too close to each other (specified on timeMargin)
        List<TimeSpan> merged = new ArrayList<TimeSpan>();
        TimeSpan current = null;
        for (int i = 0; i < spans.size(); i++) {
            if (current == null) {
                current = spans.get(i).copy();
                merged.add(current);
            } else {
                TimeSpan next = spans.get(i).copy();
                if (next.getFirstMessage().getTime() < current.getLastMessage().getTime()
                        || Math.abs(next.getFirstMessage().getTime() - current.getLastMessage().getTime()) < timeMargin) {

                    // Merge current and next time span
                    TimeSpan m = mergeTimeSpans(current, next);
                    merged.remove(merged.size() - 1);
                    merged.add(m);
                    current = m;

                } else {
                    // Current and next don't need to be merged
                    current = next;
                    merged.add(current);
                }
            }

            LOG.debug(spans.get(i).getFirstMessage() + " " + spans.get(i).getLastMessage() + " "
                    + spans.get(i).getMessageCounterSat() + " " + spans.get(i).getDistinctShipsSat().size());
        }

        return merged;
    }

    public Collection<Cell> getCells(double latStart, double lonStart, double latEnd, double lonEnd) {
        Set<String> sourcesMap = new HashSet<String>();
        sourcesMap.add(AbstractCalculator.SUPERSOURCE_MMSI);
        QueryParams params = new QueryParams();
        params.latStart = latStart;
        params.latEnd = latEnd;
        params.lonStart = lonStart;
        params.lonEnd = lonEnd;
        params.sources = sourcesMap;
        params.multiplicationFactor = 1;

        return dataHandler.getCells(params);
    }

    private void calcFixedTimeSpan(CustomMessage m) {
        // get the right cell, or create it if it doesn't exist.
        Cell cell = dataHandler.getCell(AbstractCalculator.SUPERSOURCE_MMSI, m.getLatitude(), m.getLongitude());
        if (cell == null) {
            cell = dataHandler.createCell(AbstractCalculator.SUPERSOURCE_MMSI, m.getLatitude(), m.getLongitude());
        }

        // Fetch specific time span
        Date id = Helper.getFloorDate(m.getTimestamp());
        TimeSpan fixedSpan = cell.getFixedWidthSpans().get(id.getTime());
        if (fixedSpan == null) {
            fixedSpan = new TimeSpan(id);
            fixedSpan.setLastMessage(Helper.getCeilDate(m.getTimestamp()));
            cell.getFixedWidthSpans().put(id.getTime(), fixedSpan);
        }

        //Increment message counter and update distinct ship map
        if (m.getSourceType() == SourceType.SATELLITE) {
            fixedSpan.setMessageCounterSat(fixedSpan.getMessageCounterSat() + 1);
            fixedSpan.getDistinctShipsSat().put("" + m.getShipMMSI(), true);
        } else {
            fixedSpan.incrementMessageCounterTerrestrialUnfiltered();
            fixedSpan.getDistinctShipsTerrestrial().put("" + m.getShipMMSI(), true);
        }
        
        

    }

    /**
     * The message belongs to a cell. In this cell a new time span will be created if the time since the last message arrival is
     * more than the timeMargin. Else, the current time span will be updated.
     * 
     * The order of messages is not guaranteed. Some times we can not just use the latest time span of the cell because the message
     * might need to go into an older time span. Or a new time span might need to be created in between existing time spans. In this
     * case two spans might need to be merged, if the time difference is smaller than the timeMargin.
     */
    @Override
    public void calculate(CustomMessage m) {

        if (filterMessage(m)) {
            return;
        }

        if (Helper.analysisStarted == null) {
            Helper.analysisStarted = Helper.getFloorDate(m.getTimestamp());
        }

        calcFixedTimeSpan(m);

        if (m.getSourceType() != SourceType.SATELLITE) {
            return;
        }
        
        // register ship location
        dataHandler.getShip(m.getShipMMSI()).registerMessage(m.getTimestamp(), (float) m.getLatitude(), (float)m.getLongitude());


        // get the right cell, or create it if it doesn't exist.
        Cell c = dataHandler.getCell(AbstractCalculator.SUPERSOURCE_MMSI, m.getLatitude(), m.getLongitude());
        if (c == null) {
            c = dataHandler.createCell(AbstractCalculator.SUPERSOURCE_MMSI, m.getLatitude(), m.getLongitude());
            c.setTimeSpans(new ArrayList<TimeSpan>());
        }

        // If no time spans exist for corresponding cell, create one
        // System.out.println(c.getTimeSpans());
        if (c.getTimeSpans() == null) {
            c.setTimeSpans(new ArrayList<TimeSpan>());
        }
        if (c.getTimeSpans().isEmpty()) {
            c.getTimeSpans().add(new TimeSpan(m.getTimestamp()));
        }

        // We can not be sure that the message belongs to the latest time span (because order is not guaranteed).
        // Search through list backwards, until a time span is found where first message is older than the new one.
        TimeSpan timeSpan = null;
        int timeSpanPos = 0;
        for (int i = c.getTimeSpans().size() - 1; i >= 0; i--) {
            TimeSpan t = c.getTimeSpans().get(i);
            if (t.getFirstMessage().getTime() <= m.getTimestamp().getTime()) {
                timeSpan = t;
                timeSpanPos = i;
            }
        }

        // if no time span was found a new one has to be inserted at the beginning of the list
        if (timeSpan == null) {
            timeSpan = new TimeSpan(m.getTimestamp());
            c.getTimeSpans().add(0, timeSpan);
            timeSpanPos = 0; // not necessary.. should be 0 at this point. Just to be sure.
        }

        // if time span is out dated, create new one and add it right after timeSpan.
        if (Math.abs(m.getTimestamp().getTime() - timeSpan.getLastMessage().getTime()) > timeMargin) {
            timeSpan = new TimeSpan(m.getTimestamp());
            c.getTimeSpans().add(timeSpanPos + 1, timeSpan);
            timeSpanPos = timeSpanPos + 1;

        }

        // Set the last message, if the new one is newer than the existing last message
        if (timeSpan.getLastMessage().getTime() < m.getTimestamp().getTime()) {
            timeSpan.setLastMessage(m.getTimestamp());

            // Check if the time span needs to be merged with the next (if timeMargin is larger than time difference)
            if (c.getTimeSpans().size() > timeSpanPos + 1) {
                TimeSpan nextSpan = c.getTimeSpans().get(timeSpanPos + 1);
                if (Math.abs(nextSpan.getFirstMessage().getTime() - timeSpan.getLastMessage().getTime()) <= timeMargin) {
                    // remove old timespans from list
                    c.getTimeSpans().remove(timeSpanPos);
                    c.getTimeSpans().remove(timeSpanPos);

                    // add the merged time span to the list
                    TimeSpan merged = mergeTimeSpans(timeSpan, nextSpan);
                    c.getTimeSpans().add(timeSpanPos, merged);
                    timeSpan = merged;
                }
            }

        }

        // Put ship mmsi in the map
        timeSpan.getDistinctShipsSat().put("" + m.getShipMMSI(), true);

        // Increment message counter
        timeSpan.setMessageCounterSat(timeSpan.getMessageCounterSat() + 1);

    }

    private TimeSpan mergeTimeSpans(TimeSpan span1, TimeSpan span2) {

        TimeSpan merged = new TimeSpan(span1.getFirstMessage());
        // merge two timespans
        merged.setLastMessage(span2.getLastMessage());
        merged.setMessageCounterSat(span1.getMessageCounterSat() + span2.getMessageCounterSat());
        merged.addMessageCounterTerrestrialUnfiltered(span2.getMessageCounterTerrestrial());
        Set<String> span1DistinctShips = span1.getDistinctShipsSat().keySet();
        Set<String> span2DistinctShips = span2.getDistinctShipsSat().keySet();
        Set<String> span1DistinctShipsTer = span2.getDistinctShipsTerrestrial().keySet();
        Set<String> span2DistinctShipsTer = span2.getDistinctShipsTerrestrial().keySet();
        for (String string : span1DistinctShips) {
            merged.getDistinctShipsSat().put(string, true);
        }
        for (String string : span2DistinctShips) {
            merged.getDistinctShipsSat().put(string, true);
        }
        for (String string : span1DistinctShipsTer) {
            merged.getDistinctShipsTerrestrial().put(string, true);
        }
        for (String string : span2DistinctShipsTer) {
            merged.getDistinctShipsTerrestrial().put(string, true);
        }

        return merged;
    }

    /**
     * Rules for filtering
     */
    @Override
    public boolean filterMessage(CustomMessage customMessage) {
        return false;
    }



    public class SortByDate implements Comparator<TimeSpan> {

        public int compare(TimeSpan a1, TimeSpan a2) {
            Date s1 = a1.getFirstMessage();
            Date s2 = a2.getFirstMessage();
            if (!s1.before(s2)) {
                return 1;
            }

            return -1;
        }
    }

    public class CustomMessageDateComparator implements Comparator<CustomMessage> {

        public int compare(CustomMessage c1, CustomMessage c2) {

            Date d1 = c1.getTimestamp();
            Date d2 = c2.getTimestamp();
            if (!d1.before(d2)) {
                return 1;
            }

            return -1;
        }
    }

    public class FixedSpanCell {
        public double lat, lon;
        Map<Long, TimeSpan> timeSpans = new HashMap<Long, TimeSpan>();
    }
}
