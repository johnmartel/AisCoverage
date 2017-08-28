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
package dk.dma.ais.coverage;

import dk.dma.ais.coverage.data.ICoverageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class Purger extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(Purger.class);

    private final int maxWindowSize;
    private final ICoverageData dataHandler;
    private final int pollTimeInSeconds;

    public Purger(int maxWindowSize, ICoverageData dataHandler, int pollTimeInSeconds) {
        this.maxWindowSize = maxWindowSize;
        this.dataHandler = dataHandler;
        this.pollTimeInSeconds = pollTimeInSeconds;
    }

    @Override
    public void run() {
        while (true) {
            if (Helper.latestMessage != null && Helper.firstMessage != null) {
                int currentWindowSize = getCurrentWindowSize();

                if (currentWindowSize > maxWindowSize) {
                    Date trimPoint = getTrimPoint();
                    LOG.info("Window size: {}. Max window size: {}. Lets purge data until {}", currentWindowSize, maxWindowSize, trimPoint);

                    dataHandler.trimWindow(trimPoint);
                }
            }

            try {
                Thread.sleep(pollTimeInSeconds * 1000);
            } catch (InterruptedException e) {
                LOG.error("Failed sleeping", e);
            }
        }
    }

    private int getCurrentWindowSize() {
        long latestMessageCeilingDate = Helper.getCeilDate(Helper.latestMessage).getTime();
        long firstMessageFloorDate = Helper.getFloorDate(Helper.firstMessage).getTime();
        return (int) ((latestMessageCeilingDate - firstMessageFloorDate) / 1000 / 60 / 60);
    }

    Date getTrimPoint() {
        return new Date(Helper.getCeilDate(Helper.latestMessage).getTime() - (1000L * 60 * 60 * maxWindowSize));
    }

}
