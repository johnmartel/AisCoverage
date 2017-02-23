package dk.dma.ais.coverage.persistence;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.data.Cell;

/**
 * {@link DatabaseInstance} implementation that keeps data in a MongoDB database.
 */
class MongoDatabaseInstance implements DatabaseInstance {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDatabaseInstance.class);
    private static final String COVERAGE_DATA = "coverageData";

    private String mongoServerHost;
    private int mongoServerPort;
    private String databaseName;
    private MongoClientOptions mongoClientOptions = MongoClientOptions.builder().build();
    private MongoClient client;

    @Override
    public void open(DatabaseConfiguration configuration) {
        mongoServerHost = configuration.getAddr();
        mongoServerPort = configuration.getPort();
        databaseName = configuration.getDbName();

        LOG.info("Establishing connection with MongoDB database");

        try {
            client = connectToMongoServer();
            LOG.info("Connection established with MongoDB database");
        } catch (MongoException e) {
            logAndTransformException(e);
        }
    }

    private MongoClient connectToMongoServer() {
        ServerAddress serverAddress = new ServerAddress(mongoServerHost, mongoServerPort);
        return new MongoClient(serverAddress, mongoClientOptions);
    }

    private void logAndTransformException(Exception cause) throws DatabaseConnectionException {
        String errorMessage = String.format("Could not connect to MongoDB database [%s] at [%s:%d]", databaseName, mongoServerHost, mongoServerPort);
        LOG.error(errorMessage, cause);
        throw new DatabaseConnectionException(errorMessage, cause);
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            LOG.info("Closing connection with MongoDB database");
            client.close();
            LOG.info("Connection with MongoDB database closed");
        }
    }

    @Override
    public void createDatabase() {
        requireOpenConnection();

        try {
            createCoverageDataCollection();
        } catch (MongoException e) {
            logAndTransformException(e);
        }
    }

    private void requireOpenConnection() {
        if (client == null) {
            throw new IllegalStateException("DatabaseInstance must be opened before executing operations: #open() should be invoked first.");
        }
    }

    private void createCoverageDataCollection() {
        MongoDatabase database = client.getDatabase(databaseName);
        if (!collectionExists(database)) {
            database.createCollection(COVERAGE_DATA);
        }
    }

    private boolean collectionExists(MongoDatabase database) {
        for (String collectionName : database.listCollectionNames()) {
            if (COVERAGE_DATA.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public PersistenceResult save(List<Cell> coverageData) {
        requireOpenConnection();

        Document coverageDataDocument = new Document();
        long numberOfSavedCells = 0;
        coverageDataDocument.put("dataTimestamp", ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME));
        List<Map<String, Object>> grid = new ArrayList<>();
        for (Cell cell : coverageData) {
            Map<String, Object> savedCell = new LinkedHashMap<>();
            savedCell.put("cellId", cell.getId());
            savedCell.put("latitude", cell.getLatitude());
            savedCell.put("longitude", cell.getLongitude());
            savedCell.put("numberOfReceivedSignals", cell.getNOofReceivedSignals());
            savedCell.put("numberOfMissedSignals", cell.getNOofMissingSignals());
            grid.add(savedCell);

            numberOfSavedCells++;
        }
        coverageDataDocument.put("cells", grid);

        try {
            client.getDatabase(databaseName).getCollection(COVERAGE_DATA).insertOne(coverageDataDocument);
            return PersistenceResult.success(numberOfSavedCells);
        } catch (MongoException e) {
            logAndTransformException(e);
        }

        return PersistenceResult.failure();
    }

    void setMongoClientOptions(MongoClientOptions mongoClientOptions) {
        this.mongoClientOptions = mongoClientOptions;
    }
}
