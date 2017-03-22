package dk.dma.ais.coverage.persistence;

import dk.dma.ais.coverage.data.Cell;
import dk.dma.ais.coverage.data.ICoverageData;
import dk.dma.ais.coverage.data.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performs asynchronous save operation to configured database.
 */
public class PersisterService {
    private static final Logger LOG = LoggerFactory.getLogger(PersisterService.class);

    private final DatabaseInstance databaseInstance;
    private final ICoverageData coverageData;
    private long persistenceIntervalInMinutes = 60;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public PersisterService(DatabaseInstance databaseInstance, ICoverageData coverageData) {
        this.databaseInstance = databaseInstance;
        this.coverageData = coverageData;
    }

    public void start() {
        LOG.info("Starting PersisterService, persisting every [{}] minutes", persistenceIntervalInMinutes);

        executor.scheduleAtFixedRate(new SaveOperation(), persistenceIntervalInMinutes, persistenceIntervalInMinutes, TimeUnit.MINUTES);

        LOG.info("PersisterService started");
    }

    public void stop() {
        LOG.info("Stopping PersisterService");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOG.warn("PersisterService thread pool did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOG.info("PersisterService stopped");
    }

    public void intervalInMinutes(long persistenceIntervalInMinutes) {
        this.persistenceIntervalInMinutes = persistenceIntervalInMinutes;
    }

    long getIntervalInMinutes() {
        return persistenceIntervalInMinutes;
    }

    void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    private class SaveOperation implements Runnable {

        @Override
        public void run() {
            LOG.info("Starting save operation...");
            Instant start = Instant.now();
            Map<String, Collection<Cell>> cellsBySource = new LinkedHashMap<>();
            for (Source source : coverageData.getSources()) {
                cellsBySource.put(source.getIdentifier(), source.getGrid().values());
            }

            PersistenceResult persistenceResult = null;
            try {
                persistenceResult = databaseInstance.save(cellsBySource);
            } catch (RuntimeException e) {
                LOG.error("Error while saving coverage data", e);
            }
            Instant end = Instant.now();

            if ((persistenceResult != null) && PersistenceResult.Status.SUCCESS.equals(persistenceResult.getStatus())) {
                LOG.info("Saved [{}] cells to MongoDB database", persistenceResult.getWrittenCells());
            } else {
                LOG.info("Failed saving cells to MongoDB database");
            }

            LOG.info("Save operation took [{}] ms", Duration.between(start, end).toMillis());
        }
    }
}
