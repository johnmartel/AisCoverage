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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.bus.consumer.DistributerConsumer;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;
import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.persistence.DatabaseInstance;
import dk.dma.ais.coverage.persistence.DatabaseInstanceFactory;
import dk.dma.ais.coverage.persistence.PersisterService;
import dk.dma.ais.coverage.persistence.TypeBasedDatabaseInstanceFactory;
import dk.dma.ais.coverage.web.WebServer;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.reader.AisReader;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;


/**
 * AIS coverage analyzer
 */
@ThreadSafe
public final class AisCoverage {

    private static final Logger LOG = LoggerFactory.getLogger(AisCoverage.class);

    @GuardedBy("AisCoverage")
    private static AisCoverage instance;

    private final AisCoverageConfiguration conf;
    private final CoverageHandler handler;
    private final AisBus aisBus;
    private final WebServer webServer;
    private AisReader aisReader;
    private PersisterService persisterService;
    private final DatabaseInstance databaseInstance;

    private AisCoverage(AisCoverageConfiguration conf, DatabaseInstanceFactory databaseInstanceFactory) {
        this.conf = conf;
        handler = new CoverageHandler(conf);

        databaseInstance = databaseInstanceFactory.createDatabaseInstance(conf.getDatabaseConfiguration().getType());
        databaseInstance.open(conf.getDatabaseConfiguration());
        databaseInstance.createDatabase();
        loadCoverageDataFromDatabase();

        aisBus = conf.getAisbusConfiguration().getInstance();
        createPersisterService();

        // Create web server
        if (conf.getServerConfiguration() != null) {
            webServer = new WebServer(conf.getServerConfiguration());
        } else {
            webServer = null;
        }

        final DistributerConsumer unfilteredConsumer = new DistributerConsumer(true);
        unfilteredConsumer.init();
        // Delegate unfiltered packets to handler
        unfilteredConsumer.getConsumers().add(new Consumer<AisPacket>() {
            @Override
            public void accept(AisPacket packet) {
                handler.receiveUnfiltered(packet);
            }
        });                
        aisBus.registerConsumer(unfilteredConsumer);
    }

    private void loadCoverageDataFromDatabase() {
        Map<String, Collection<Cell>> loadedCoverageData = databaseInstance.loadLatestSavedCoverageData();

        for (String sourceId : loadedCoverageData.keySet()) {
            for (Cell cell : loadedCoverageData.get(sourceId)) {
                handler.getDataHandler().updateCell(sourceId, cell);

                List<Long> timeSpanIds = IterableUtils.toList(cell.getFixedWidthSpans().keySet());
                Collections.sort(timeSpanIds);
                if (Helper.firstMessage != null && (Helper.firstMessage.getTime() > timeSpanIds.get(0))) {
                    Helper.firstMessage = new Date(timeSpanIds.get(0));
                }
            }
        }
    }

    private void createPersisterService() {
        persisterService = new PersisterService(databaseInstance, handler.getDataHandler());
        persisterService.intervalInSeconds(conf.getDatabaseConfiguration().getPersistenceIntervalInSeconds());
    }

    public void start() {
        // Start aisBus
        if (aisReader == null) {
            aisBus.start();
            aisBus.startConsumers();
            aisBus.startProviders();
            LOG.info("aisbus started");
        }
        // Start web server
        if (webServer != null) {
            try {
                webServer.start();
                LOG.info("webserver started");
            } catch (Exception e) {
                LOG.error("Failed to start web server: " + e.getMessage());
                e.printStackTrace();
            }
        }

        persisterService.start();
    }

    public void stop() {
        if (webServer != null) {
            try {
                webServer.stop();
                LOG.info("webserver stopped");
            } catch (Exception e) {
                LOG.error("Failed to stop web server: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        aisBus.cancel();
        LOG.info("aisbus stopped");

        persisterService.stop();
        try {
            databaseInstance.close();
        } catch (Exception e) {
            LOG.warn("Could not close DatabaseInstance cleanly", e);
        }
    }

    public AisCoverageConfiguration getConf() {
        return conf;
    }

    public CoverageHandler getHandler() {
        return handler;
    }

    public static synchronized AisCoverage create(AisCoverageConfiguration conf) {
        instance = new AisCoverage(conf, new TypeBasedDatabaseInstanceFactory());
        return instance;
    }

    public static synchronized AisCoverage create(AisCoverageConfiguration conf, DatabaseInstanceFactory databaseInstanceFactory) {
        instance = new AisCoverage(conf, databaseInstanceFactory);
        return instance;
    }

    public static synchronized AisCoverage get() {
        return instance;
    }

    void setPersisterService(PersisterService persisterService) {
        this.persisterService = persisterService;
    }
}
