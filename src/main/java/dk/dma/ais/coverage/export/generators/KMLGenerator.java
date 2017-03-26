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
package dk.dma.ais.coverage.export.generators;

import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.Source;
import dk.dma.ais.coverage.export.ExportDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

//TODO retrieve sources with larger cells.
//TODO retrieve cell data from both super and individual source

public class KMLGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(KMLGenerator.class);

    // public static void generateKML(CoverageCalculator calc, String path) {
    public static void generateKML(Collection<Source> grids, double latSize, double lonSize, int multiplicity,
                                   ExportDataType exportDataType, HttpServletResponse response) {

        LOG.info("Started KML generation");

        HttpServletResponse out = response;

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        String fileName = "aiscoverage-" + dateFormat.format(date) + "_latSize " + latSize + "_lonSize " + lonSize
                + "multiplicationfactor" + multiplicity + ".kml";
        out.setContentType("application/vnd.google-earth.kml+xml");
        out.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", out);
        writeLine("<kml>", out);
        writeLine("<Document>", out);
        writeLine("<name>AIS Coverage</name>", out);
        writeLine("<open>1</open>", out);
        writeLine("<Style id=\"redStyle\">", out);
        writeLine("    <IconStyle>", out);
        writeLine("        <scale>1.3</scale>", out);
        writeLine("        <Icon>", out);
        writeLine("            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>", out);
        writeLine("        </Icon>", out);
        writeLine("        <hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>", out);
        writeLine("    </IconStyle>", out);
        writeLine("    <LineStyle>", out);
        writeLine("        <color>ff0000ff</color>", out);
        writeLine("    </LineStyle>", out);
        writeLine("    <PolyStyle>", out);
        writeLine("        <color>ff0000ff</color>", out);
        writeLine("    </PolyStyle>", out);
        writeLine("</Style>", out);
        writeLine("<Style id=\"orangeStyle\">", out);
        writeLine("    <IconStyle>", out);
        writeLine("        <scale>1.3</scale>", out);
        writeLine("        <Icon>", out);
        writeLine("            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>", out);
        writeLine("        </Icon>", out);
        writeLine("        <hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>", out);
        writeLine("    </IconStyle>", out);
        writeLine("    <LineStyle>", out);
        writeLine("        <color>ff00aaff</color>", out);
        writeLine("    </LineStyle>", out);
        writeLine("    <PolyStyle>", out);
        writeLine("        <color>ff00aaff</color>", out);
        writeLine("    </PolyStyle>", out);
        writeLine("</Style>", out);
        writeLine("<Style id=\"greenStyle\">", out);
        writeLine("    <IconStyle>", out);
        writeLine("        <scale>1.3</scale>", out);
        writeLine("        <Icon>", out);
        writeLine("            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>", out);
        writeLine("        </Icon>", out);
        writeLine("        <hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>", out);
        writeLine("    </IconStyle>", out);
        writeLine("    <LineStyle>", out);
        writeLine("        <color>ff00ff00</color>", out);
        writeLine("    </LineStyle>", out);
        writeLine("    <PolyStyle>", out);
        writeLine("    <color>ff00ff55</color>", out);
        writeLine("</PolyStyle>", out);
        writeLine("</Style>", out);

        for (Source grid : grids) {
            generateGrid(grid.getIdentifier(), grid.getGrid().values(), out, latSize * multiplicity, lonSize * multiplicity, exportDataType);
        }

        writeLine("</Document>", out);
        writeLine("</kml>", out);

        // TODO check hvad det er der giver en aw snap internal error fejl efter kml generate er kørt
        try {
            out.getOutputStream().close();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        LOG.info("Finished KML generation");
    }

    private static void writeLine(String line, HttpServletResponse out) {
        try {
            out.getOutputStream().write((line + "\n").getBytes());
            out.getOutputStream().flush();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    private static void generateGrid(String bsMmsi, Collection<Cell> cells, HttpServletResponse out, double latSize, double lonSize, ExportDataType exportDataType) {
        writeLine("<Folder>", out);
        writeLine("    <name>" + bsMmsi + "</name>", out);
        writeLine("    <open>0</open>", out);
        for (Cell cell : cells) {
            double dataToExport;
            if (exportDataType == ExportDataType.RECEIVED_MESSAGES) {
                dataToExport = cell.getCoverage();
            } else {
                dataToExport = cell.getAverageSignalStrength();
            }

            if (dataToExport > exportDataType.greenThreshold()) { // green
                generatePlacemark("#greenStyle", cell, 300, out, latSize, lonSize);
            } else if (dataToExport > exportDataType.redThreshold()) { // orange
                generatePlacemark("#orangeStyle", cell, 200, out, latSize, lonSize);
            } else { // red
                generatePlacemark("#redStyle", cell, 100, out, latSize, lonSize);
            }
        }

        writeLine("</Folder>", out);
    }

    private static void generatePlacemark(String style, Cell cell, int z, HttpServletResponse out, double latSize, double lonSize) {

        writeLine("    <Placemark>", out);
        writeLine("        <name>" + cell.getId() + "</name>", out);
        writeLine("        <styleUrl>" + style + "</styleUrl>", out);
        writeLine("        <Polygon>", out);
        writeLine("            <altitudeMode>relativeToGround</altitudeMode>", out);
        writeLine("            <tessellate>1</tessellate>", out);
        writeLine("            <outerBoundaryIs>", out);
        writeLine("                <LinearRing>", out);
        writeLine("                    <coordinates>", out);

        writeLine(
                cell.getLongitude() + "," + cell.getLatitude() + "," + z + " " + (cell.getLongitude() + lonSize) + ","
                        + cell.getLatitude() + "," + z + " " + (cell.getLongitude() + lonSize) + ","
                        + (cell.getLatitude() + latSize) + "," + z + " " + cell.getLongitude() + ","
                        + (cell.getLatitude() + latSize) + "," + z, out);

        writeLine("                    </coordinates>", out);
        writeLine("                </LinearRing>", out);
        writeLine("            </outerBoundaryIs>", out);
        writeLine("        </Polygon>", out);
        writeLine("    </Placemark>", out);
    }

}
