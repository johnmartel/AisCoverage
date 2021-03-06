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
package dk.dma.ais.coverage.rest;

import dk.dma.ais.coverage.AisCoverage;
import dk.dma.ais.coverage.CoverageHandler;
import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.calculator.TerrestrialCalculator;
import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.ICoverageData;
import dk.dma.ais.coverage.data.OnlyMemoryData;
import dk.dma.ais.coverage.data.Source;
import dk.dma.ais.coverage.data.TimeSpan;
import dk.dma.ais.coverage.export.ExportDataType;
import dk.dma.ais.coverage.export.data.ExportShipTimeSpan;
import dk.dma.ais.coverage.export.data.JSonCoverageMap;
import dk.dma.ais.coverage.export.data.JsonConverter;
import dk.dma.ais.coverage.export.data.JsonSource;
import dk.dma.ais.coverage.export.data.Status;
import dk.dma.ais.coverage.export.generators.CSVGenerator;
import dk.dma.ais.coverage.export.generators.ChartGenerator;
import dk.dma.ais.coverage.export.generators.KMLGenerator;
import dk.dma.ais.coverage.export.generators.XMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//import dk.dma.ais.coverage.export.CSVGenerator;

/**
 * JAX-RS rest services
 */
@Path("/")
public class CoverageRestService {

    private final CoverageHandler handler;
    private static final Logger LOG = LoggerFactory.getLogger(CoverageRestService.class);

    public CoverageRestService() {
        this.handler = AisCoverage.get().getHandler();
    }

    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> test(@QueryParam("q") String q) {
        Objects.requireNonNull(handler);
        Map<String, String> map = new HashMap<String, String>();
        map.put("q", q);
        LOG.debug(q);
        return map;
    }

    @GET
    @Path("test2")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> test2(@Context UriInfo uriInfo) {
        Objects.requireNonNull(handler);
        Map<String, String> map = new HashMap<String, String>();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        for (String key : queryParams.keySet()) {
            map.put(key, queryParams.getFirst(key));
        }
        return map;
    }

    /*
     * returns the source list
     */
    @GET
    @Path("sources")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, JsonSource> sources(@Context UriInfo uriInfo) {
        Objects.requireNonNull(handler);
        LOG.info("getting sources");
        Collection<Source> sources = handler.getDataHandler().getSources();
        
        return JsonConverter.toJsonSources(sources);
    }

    /*
     * Returns the coverage. Takes a list of sources, multiplicationfactor and a geographical area as input
     */
    @GET
    @Path("coverage")
    @Produces(MediaType.APPLICATION_JSON)
    public JSonCoverageMap coverage(@Context HttpServletRequest request) {
        Date start = new Date();
        Objects.requireNonNull(handler);
        String sources = request.getParameter("sources");
        String area = request.getParameter("area");
        long starttime = Long.parseLong(request.getParameter("starttime"));
        long endtime = Long.parseLong(request.getParameter("endtime"));

        String[] areaArray = area.split(",");

        int multiplicationFactor = Integer.parseInt(request.getParameter("multiplicationFactor"));

        double latStart = Double.parseDouble(areaArray[0]);
        double lonStart = Double.parseDouble(areaArray[1]);
        double latEnd = Double.parseDouble(areaArray[2]);
        double lonEnd = Double.parseDouble(areaArray[3]);

        Set<String> sourcesMap = new HashSet<String>();
        if (sources != null) {
            String[] sourcesArray = sources.split(",");
            for (String string : sourcesArray) {
                sourcesMap.add(string);
            }
        }
        TerrestrialCalculator terCalc = (TerrestrialCalculator) handler.getCalculators().get(0);
        JSonCoverageMap result = terCalc.getTerrestrialCoverage(latStart, lonStart, latEnd, lonEnd, sourcesMap, multiplicationFactor,
                new Date(starttime), new Date(endtime));
        Date end = new Date();
        LOG.info("Coverage request completed in: " + ((double) (end.getTime() - start.getTime()) / 1000) + " seconds");
        return result;
    }

    @GET
    @Path("export")
    @Produces(MediaType.APPLICATION_JSON)
    public Object export(@QueryParam("exportType") String exportType, @QueryParam("exportMultiFactor") String exportMultiFactor,
            @Context HttpServletResponse response, @QueryParam("startTime") String startTime, @QueryParam("endTime") String endTime,
            @QueryParam("exportDataType") String exportDataType) {

        int multiplicity = Integer.parseInt(exportMultiFactor);
        Date starttime = new Date(Long.parseLong(startTime));
        Date endtime = new Date(Long.parseLong(endTime));
        ExportDataType dataType = ExportDataType.forType(exportDataType);

        //
        // // BaseStationHandler gh = new BaseStationHandler();
        ICoverageData dh = new OnlyMemoryData();
        // double latsize = Helper.latSize;
        // double lonsize = Helper.lonSize;
        // System.out.println(Helper.latSize);
        // Helper helper = new Helper();
        // helper.latSize = latsize*multiplicity;
        // helper.lonSize = lonsize*multiplicity;
        //
        //
        Collection<Source> sources = handler.getDataHandler().getSources();
        //
        // Collection<BaseStation> superSource = covH.getSupersourceCalculator().getDataHandler().getSources();
        //
        Source superbs = handler.getDataHandler().getSource("supersource");
        //
        for (Source bs : sources) {
            Source summedbs = dh.createSource(bs.getIdentifier());
            // summedbs.setLatSize(Helper.latSize*multiplicity);
            // summedbs.setLonSize(Helper.lonSize*multiplicity);
            // dh.setLatSize(bs.getLatSize()*multiplicity);
            // dh.setLonSize(bs.getLonSize()*multiplicity);
            // BaseStation tempSource = new BaseStation(basestation.getIdentifier(), gridHandler.getLatSize()*multiplicationFactor,
            // gridHandler.getLonSize()*multiplicationFactor);

            Collection<Cell> cells = bs.getGrid().values();

            for (Cell cell : cells) {
                Cell dhCell = summedbs.getTempCell(cell.getLatitude(), cell.getLongitude(), multiplicity);
                if (dhCell == null) {
                    dhCell = summedbs.createTempCell(cell.getLatitude(), cell.getLongitude(), multiplicity);
                }

                Cell activesbscell = superbs.getGrid().get(cell.getId());
                if (activesbscell != null) {
                    int receivedsignals = cell.getNOofReceivedSignals(starttime, endtime);
                    dhCell.addReceivedSignals(receivedsignals);

                    int sbstotalmessages = activesbscell.getNOofReceivedSignals(starttime, endtime)
                            + activesbscell.getNOofMissingSignals(starttime, endtime);
                    dhCell.addNOofMissingSignals(sbstotalmessages - receivedsignals);

                    dhCell.addVsiMessages(activesbscell.getNumberOfVsiMessages(starttime, endtime),
                            activesbscell.getAverageSignalStrength(starttime, endtime));
                }
            }
        }
        if (exportType.equals("KML")) {
            KMLGenerator.generateKML(dh.getSources(), AisCoverage.get().getConf().getLatSize(), AisCoverage.get().getConf().getLonSize(), multiplicity, dataType, response);
        } else if (exportType.equals("CSV")) {
            CSVGenerator.generateCSV(dh.getSources(), AisCoverage.get().getConf().getLatSize(), AisCoverage.get().getConf().getLonSize(), multiplicity, response);
        } else if (exportType.equals("XML")) {
            XMLGenerator.generateXML(dh.getSources(), AisCoverage.get().getConf().getLatSize(), AisCoverage.get().getConf().getLonSize(), multiplicity, response);
        } else {
            System.out.println("wrong exporttype");
        }

        // helper.latSize = latsize;
        // helper.lonSize = lonsize;
        return null;
    }

    @GET
    @Path("satCoverage")
    @Produces(MediaType.APPLICATION_JSON)
    public Object satCoverage(@Context HttpServletRequest request, @QueryParam("area") String area) {
        String[] points = area.split(",");
        if (points.length != 4) {
            LOG.warn("SatCoverage requires 2 latlon coordinates.");
            return null;
        }
        LOG.info("Finding sat coverage for area: " + area);
        double latPoint1 = Double.parseDouble(points[1]);
        double latPoint2 = Double.parseDouble(points[3]);
        double lonPoint1 = Double.parseDouble(points[0]);
        double lonPoint2 = Double.parseDouble(points[2]);

        // Determine which points are which
        double lonLeft;
        double lonRight;
        if (lonPoint1 < lonPoint2) {
            lonLeft = lonPoint1;
            lonRight = lonPoint2;
        } else {
            lonLeft = lonPoint2;
            lonRight = lonPoint1;
        }
        double latTop;
        double latBottom;
        if (latPoint1 < latPoint2) {
            latTop = latPoint2;
            latBottom = latPoint1;
        } else {
            latTop = latPoint1;
            latBottom = latPoint2;
        }

        // List<TimeSpan> spans =handler.getSatCalc().getTimeSpans(latTop, lonLeft, latBottom, lonRight);
        // if(!spans.isEmpty()){
        // ChartGenerator cg = new ChartGenerator();
        // cg.generateChart(spans.get(0).getFirstMessage(), spans.get(spans.size()-1).getLastMessage(), spans);
        // }
        return JsonConverter.toJsonTimeSpan(handler.getSatCalc().getDynamicTimeSpans(null, null, latTop, lonLeft, latBottom,
                lonRight));
    }

    @GET
    @Path("shipTrackExport")
    @Produces(MediaType.APPLICATION_JSON)
    public Object shipTrackExport(@QueryParam("startTime") String startTime, @QueryParam("endTime") String endTime,
            @QueryParam("shipmmsi") int shipmmsi, @Context HttpServletResponse response) throws IOException {

        Date startDate = new Date(Long.parseLong(startTime));
        Date endDate = new Date(Long.parseLong(endTime));

        return handler.getSatCalc().getShipDynamicTimeSpans(startDate, endDate, shipmmsi);

    }

    @GET
    @Path("shipTrackExportPNG")
    @Produces(MediaType.APPLICATION_JSON)
    public Object shipTrackExportPNG(@QueryParam("startTime") String startTime, @QueryParam("endTime") String endTime,
            @QueryParam("shipmmsi") int shipmmsi, @Context HttpServletResponse response) throws IOException {

        Date startDate = new Date(Long.parseLong(startTime));
        Date endDate = new Date(Long.parseLong(endTime));

        response.setContentType("image/png");
        ServletOutputStream out = response.getOutputStream();

        ChartGenerator cg = new ChartGenerator();
        if (handler.getDataHandler().getShip(shipmmsi) == null) {
            cg.printMessage("No such ship: " + shipmmsi);
        } else {
            List<ExportShipTimeSpan> spans = handler.getSatCalc().getShipDynamicTimeSpans(startDate, endDate, shipmmsi);
            if (spans.isEmpty()) {
                cg.printMessage("No data available");
            } else {
                cg.generateChartMethod3(startDate, endDate, shipmmsi, spans, true);
            }
        }
        cg.exportAsPNG(out);
        out.flush();

        return null;
    }

    @GET
    @Path("satExportPNG")
    @Produces(MediaType.APPLICATION_JSON)
    public Object satExportPNG(@QueryParam("startTime") String startTime, @QueryParam("endTime") String endTime,
            @QueryParam("lat1") String lat1, @QueryParam("lon1") String lon1, @QueryParam("lat2") String lat2,
            @QueryParam("lon2") String lon2, @QueryParam("satChartMethod") String satChartMethod,
            @Context HttpServletResponse response) throws IOException {

        // LOG.info("Finding sat coverage for area: "+area);
        double latPoint1 = Double.parseDouble(lat1);
        double latPoint2 = Double.parseDouble(lat2);
        double lonPoint1 = Double.parseDouble(lon1);
        double lonPoint2 = Double.parseDouble(lon2);

        Date startDate = new Date(Long.parseLong(startTime));
        Date endDate = new Date(Long.parseLong(endTime));

        // Determine which points are which
        double lonMin;
        double lonMax;
        if (lonPoint1 < lonPoint2) {
            lonMin = lonPoint1;
            lonMax = lonPoint2;
        } else {
            lonMin = lonPoint2;
            lonMax = lonPoint1;
        }
        double latMax;
        double latMin;
        if (latPoint1 < latPoint2) {
            latMax = latPoint2;
            latMin = latPoint1;
        } else {
            latMax = latPoint1;
            latMin = latPoint2;
        }

        response.setContentType("image/png");
        // response.setHeader("Content-Disposition", "attachment; filename=" + "satexport.txt");
        ServletOutputStream out = response.getOutputStream();

        ChartGenerator cg = new ChartGenerator();
        if (satChartMethod.equals("satonly")) {
            List<TimeSpan> spans = handler.getSatCalc().getDynamicTimeSpans(startDate, endDate, latMin, latMax, lonMin, lonMax);
            cg.generateChartMethod1(startDate, endDate, spans, latMin, latMax, lonMin, lonMax);
        } else {
            List<TimeSpan> spans = handler.getSatCalc().getFixedTimeSpans(startDate, endDate, latMin, latMax, lonMin, lonMax, 1);
            cg.generateChartMethod2(startDate, endDate, spans, latMin, latMax, lonMin, lonMax, true);
        }

        cg.exportAsPNG(out);

        out.flush();

        return null;
    }

    @GET
    @Path("satExport")
    @Produces(MediaType.APPLICATION_JSON)
    public Object satExport(@QueryParam("test") String test, @Context HttpServletResponse response) throws IOException {
        double latTop = 62.47;
        double latBottom = 57.5;
        double lonRight = -35;
        double lonLeft = -55;

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=" + "satexport.txt");
        ServletOutputStream out = response.getOutputStream();

        List<TimeSpan> timeSpans = handler.getSatCalc().getDynamicTimeSpans(null, null, latTop, lonLeft, latBottom, lonRight);
        TimeSpan first = null;
        TimeSpan previous = null;
        for (TimeSpan timeSpan : timeSpans) {
            if (first == null) {
                first = timeSpan;
            }

            long timeSinceLastTimeSpan = 0;
            if (previous != null) {
                timeSinceLastTimeSpan = Math.abs(timeSpan.getFirstMessage().getTime() - previous.getLastMessage().getTime()) / 1000 / 60;
            }

            // last is determined by the order of reception, but it is not guaranteed that the tag is actually the last
            // from time, to time, data time, time since last time span, accumulated time, signals, distinct ships
            String outstring = formatter.format(timeSpan.getFirstMessage()) + "," + // from time
                    formatter.format(timeSpan.getLastMessage()) + "," + // to time
                    Math.abs(timeSpan.getLastMessage().getTime() - timeSpan.getFirstMessage().getTime()) / 1000 / 60 + "," + // Timespan
                                                                                                                             // length
                    timeSinceLastTimeSpan + "," + // Time since last timestamp
                    Math.abs(timeSpan.getLastMessage().getTime() - first.getLastMessage().getTime()) / 1000 / 60 + "," + // accumulated
                                                                                                                         // time
                    timeSpan.getMessageCounterSat() + "," + // signals
                    timeSpan.getDistinctShipsSat().size() + // distinct ships
                    "\n";
            out.write(outstring.getBytes());
            previous = timeSpan;

        }
        out.flush();

        return null;
    }

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public Object status() throws IOException {
        LOG.info("getting status");

        Status status = new Status();
        Date now = Helper.getFloorDate(new Date());

        setFirstMessageTimestamp(status, now);
        setLastMessageTimestamp(status, now);

        status.analysisStatus = "Running";

        return status;
    }

    private void setFirstMessageTimestamp(Status status, Date now) {
        if (messageReceived()) {
            status.firstMessage = Helper.firstMessage.getTime();
        } else {
            status.firstMessage = now.getTime();
        }
    }

    private boolean messageReceived() {
        return Helper.firstMessage != null;
    }

    private void setLastMessageTimestamp(Status status, Date now) {
        if (terrestrialMessageReceived()) {
            status.lastMessage = Helper.latestMessage.getTime();
        } else {
            status.lastMessage = now.getTime();
        }
    }

    private boolean terrestrialMessageReceived() {
        return Helper.latestMessage != null;
    }
}
