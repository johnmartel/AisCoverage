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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public class CSVGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(KMLGenerator.class);

    // public static void generateKML(CoverageCalculator calc, String path) {
    public static void generateCSV(Collection<Source> grids, double latSize, double lonSize, int multiplicity,
            HttpServletResponse response) {

        LOG.info("Started CSV generation");

        HttpServletResponse out = response;

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        String fileName = "aiscoverage-" + dateFormat.format(date) + "_latSize " + latSize + "_lonSize " + lonSize
                + "multiplicationfactor" + multiplicity + ".csv";
        // out.setContentType("application/vnd.google-earth.kml+xml");
        out.setContentType("text/csv");
        out.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        writeLine("latstart, longstart, latend, longend, received, missing, coverage percentage", out);

        for (Source grid : grids) {
            generateGrid(grid.getIdentifier(), grid.getGrid().values(), out, latSize * multiplicity, lonSize * multiplicity);
        }

        // TODO check hvad det er der giver en aw snap internal error fejl efter kml generate er kørt
        try {
            out.getOutputStream().close();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        LOG.info("Finished CSV generation");
    }

    private static void writeLine(String line, HttpServletResponse out) {
        try {
            out.getOutputStream().write((line + "\n").getBytes());
            out.getOutputStream().flush();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    private static void generateGrid(String bsMmsi, Collection<Cell> cells, HttpServletResponse out, double latSize, double lonSize) {
        for (Cell cell : cells) {
            StringBuilder lineBuilder = new StringBuilder();
            lineBuilder.append(cell.getLatitude()).append(",")
                    .append(cell.getLongitude()).append(",")
                    .append(cell.getLatitude() + latSize).append(",")
                    .append(cell.getLongitude() + lonSize).append(",")
                    .append(cell.getNOofReceivedSignals()).append(",")
                    .append(cell.getNOofMissingSignals()).append(",")
                    .append(cell.getCoverage() * 100).append(",")
                    .append(cell.getNumberOfVsiMessages()).append(",")
                    .append(cell.getAverageSignalStrength());

            writeLine(lineBuilder.toString(), out);
        }
    }

}
