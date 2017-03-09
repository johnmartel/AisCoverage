package dk.dma.ais.coverage.persistence;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.data.Cell;

/**
 * {@link DatabaseInstance} implementation that keeps data in a MongoDB database.
 */
class MongoDatabaseInstance implements DatabaseInstance {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDatabaseInstance.class);
    private static final String COVERAGE_DATA = "coverageData";

    private CoverageDataMarshaller<Document> marshaller;
    private String mongoServerHost;
    private int mongoServerPort;
    private String databaseName;
    private MongoClientOptions mongoClientOptions = MongoClientOptions.builder().build();
    private MongoClient client;

    public MongoDatabaseInstance(CoverageDataMarshaller<Document> marshaller) {
        this.marshaller = marshaller;
    }

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
    public PersistenceResult save(Map<String, Collection<Cell>> coverageData) {
        requireOpenConnection();

        Document coverageDataDocument = marshaller.marshall(coverageData, ZonedDateTime.now(ZoneId.of("UTC")));
        long numberOfSavedCells = ((List<Map<String, Object>>) coverageDataDocument.get("cells")).size();

        try {
            client.getDatabase(databaseName).getCollection(COVERAGE_DATA).insertOne(coverageDataDocument);
            LOG.info("Saved [{}] cells with timestamp [{}]", numberOfSavedCells, coverageDataDocument.get("dataTimestamp"));
            return PersistenceResult.success(numberOfSavedCells);
        } catch (MongoException e) {
            logAndTransformException(e);
        }

        return PersistenceResult.failure();
    }

    @Override
    public Map<String, Collection<Cell>> loadLatestSavedCoverageData() {
        requireOpenConnection();

        Map<String, Collection<Cell>> latestCoverageData = new LinkedHashMap<>();

        try {
            Document orderByDataTimestamp = new Document();
            orderByDataTimestamp.put("dataTimestamp", -1);

            FindIterable<Document> foundDocuments = client.getDatabase(databaseName).getCollection(COVERAGE_DATA).find().sort(orderByDataTimestamp).limit(1);
            if (foundDocuments.iterator().hasNext()) {
                Document document = foundDocuments.iterator().next();
                latestCoverageData.putAll(marshaller.unmarshall(document));

                long loadedCells = 0L;
                for (Collection<Cell> cells : latestCoverageData.values()) {
                    loadedCells = loadedCells + cells.size();
                }

                LOG.info("Loaded [{}] cells from previously saved state at [{}]", loadedCells, document.get("dataTimestamp"));
            }
        } catch (MongoException e) {
            logAndTransformException(e);
        }

        return Collections.unmodifiableMap(latestCoverageData);
    }

    void setMongoClientOptions(MongoClientOptions mongoClientOptions) {
        this.mongoClientOptions = mongoClientOptions;
    }

    void setMarshaller(MongoCoverageDataMarshaller marshaller) {
        this.marshaller = marshaller;
    }
}
