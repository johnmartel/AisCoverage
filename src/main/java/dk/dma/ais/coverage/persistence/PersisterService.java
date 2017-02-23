package dk.dma.ais.coverage.persistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.coverage.data.ICoverageData;

/**
 * Performs asynchronous save operation to configured database.
 */
public class PersisterService {
    private static final Logger LOG = LoggerFactory.getLogger(PersisterService.class);

    private final DatabaseInstance databaseInstance;
    private final ICoverageData coverageData;
    private long persistenceIntervalInSeconds = 60;
    private ScheduledExecutorService executor;

    public PersisterService(DatabaseInstance databaseInstance, ICoverageData coverageData) {
        this.databaseInstance = databaseInstance;
        this.coverageData = coverageData;
    }

    public void start() {
        LOG.info("Starting PersisterService, persisting every [{}] seconds", persistenceIntervalInSeconds);

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new SaveOperation(), persistenceIntervalInSeconds, persistenceIntervalInSeconds, TimeUnit.SECONDS);

        LOG.info("PersisterService started");
    }

    public void stop() {
        LOG.info("Stopping PersisterService");

        executor.shutdown();
        try {
            if (!executor.awaitTermination(persistenceIntervalInSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(persistenceIntervalInSeconds, TimeUnit.SECONDS)) {
                    LOG.warn("PersisterService thread pool did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOG.info("PersisterService stopped");
    }

    public void intervalInSeconds(long persistenceIntervalInSeconds) {
        this.persistenceIntervalInSeconds = persistenceIntervalInSeconds;
    }

    long getIntervalInSeconds() {
        return persistenceIntervalInSeconds;
    }

    private class SaveOperation implements Runnable {

        @Override
        public void run() {
            PersistenceResult persistenceResult = databaseInstance.save(coverageData.getCells(null));

            if (PersistenceResult.Status.SUCCESS.equals(persistenceResult.getStatus())) {
                LOG.info("Saved [{}] cells to MongoDB database", persistenceResult.getWrittenCells());
            } else {
                LOG.info("Failed saving cells to MongoDB database");
            }
        }
    }
}
