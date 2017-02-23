package dk.dma.ais.coverage.persistence;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoIterable;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import dk.dma.ais.coverage.configuration.DatabaseConfiguration;
import dk.dma.ais.coverage.data.Cell;

public class MongoDatabaseInstanceTest {
    private static final String NORDIC_COVERAGE_DATABASE = "nordicCoverage";
    private static final String COVERAGE_DATA_COLLECTION = "coverageData";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MongoServer mongoServer;
    private DatabaseConfiguration configuration;
    private MongoDatabaseInstance databaseInstance;
    private InetSocketAddress serverAddress;
    private MongoClient mongoClient;

    @Before
    public void setUp() throws Exception {
        databaseInstance = new MongoDatabaseInstance();
    }

    @After
    public void tearDown() throws Exception {
        databaseInstance.close();
        closeMongoClient();
        shutdownMongoServer();
    }

    private void closeMongoClient() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    private void shutdownMongoServer() {
        if (mongoServer != null) {
            mongoServer.shutdown();
        }
    }

    @Test
    public void givenNoDatabase_whenCreateDatabase_thenDatabaseIsCreated() {
        startMongoServer();
        useStartedMongoServer();

        databaseInstance.createDatabase();

        initializeMongoClient();
        MongoIterable<String> existingDatabases = mongoClient.listDatabaseNames();
        assertThat(existingDatabases, hasItem(NORDIC_COVERAGE_DATABASE));
        assertThat(mongoClient.getDatabase(NORDIC_COVERAGE_DATABASE), is(notNullValue()));
    }


    private void startMongoServer() {
        mongoServer = new MongoServer(new MemoryBackend());
        serverAddress = mongoServer.bind();
    }

    private void useStartedMongoServer() {
        configuration = new DatabaseConfiguration();
        configuration.setType("MongoDB");
        configuration.setDbName(NORDIC_COVERAGE_DATABASE);
        configuration.setAddr(serverAddress.getHostName());
        configuration.setPort(serverAddress.getPort());

        databaseInstance.open(configuration);
    }

    private void initializeMongoClient() {
        MongoClientOptions options = MongoClientOptions.builder().serverSelectionTimeout(10).build();
        mongoClient = new MongoClient(new ServerAddress(serverAddress), options);
    }

    @Test
    public void givenAnExistingDatabase_whenCreateDatabase_thenNoExceptionIsThrown() {
        startMongoServer();
        useStartedMongoServer();
        preCreateCollection();

        databaseInstance.createDatabase();
    }

    private void preCreateCollection() {
        initializeMongoClient();
        mongoClient.getDatabase(NORDIC_COVERAGE_DATABASE).createCollection(COVERAGE_DATA_COLLECTION);
    }

    @Test
    public void givenATimeOut_whenCreateDatabase_thenDatabaseConnectionExceptionIsThrown() {
        testThatInvalidConfigurationThrowsDatabaseConnectionException("10.1.1.1", 16256);
    }

    private void testThatInvalidConfigurationThrowsDatabaseConnectionException(String host, int port) {
        thrown.expect(DatabaseConnectionException.class);
        thrown.expectCause(is(notNullValue(Throwable.class)));
        thrown.expectMessage(containsString(NORDIC_COVERAGE_DATABASE));
        thrown.expectMessage(containsString(String.format("%s:%d", host, port)));

        DatabaseConfiguration invalidConfiguration = new DatabaseConfiguration();
        invalidConfiguration.setType("MongoDB");
        invalidConfiguration.setDbName(NORDIC_COVERAGE_DATABASE);
        invalidConfiguration.setAddr(host);
        invalidConfiguration.setPort(16256);

        databaseInstance.setMongoClientOptions(MongoClientOptions.builder().serverSelectionTimeout(10).build());

        databaseInstance.open(invalidConfiguration);
        databaseInstance.createDatabase();
    }

    @Test
    public void givenAnUnknownHost_whenCreateDatabase_thenDatabaseConnectionExceptionIsThrown() {
        testThatInvalidConfigurationThrowsDatabaseConnectionException("unknownHost", 16256);
    }

    @Test
    public void givenACell_whenSave_thenCellIsPersistedInCoverageDataCollection() {
        startMongoServer();
        useStartedMongoServer();
        databaseInstance.createDatabase();

        Cell aCell = new Cell(49.9324, -64.6364, null);
        aCell.addReceivedSignals(3);
        aCell.addNOofMissingSignals(1);

        PersistenceResult result = databaseInstance.save(Arrays.asList(aCell));

        initializeMongoClient();
        assertThat(result.getStatus(), is(equalTo(PersistenceResult.Status.SUCCESS)));
        assertThat(result.getWrittenCells(), is(1L));
        assertThat(mongoClient.getDatabase(NORDIC_COVERAGE_DATABASE).getCollection(COVERAGE_DATA_COLLECTION).count(), is(1L));
    }

    @Test
    public void givenDatabaseInstanceIsNotOpened_whenCreateDatabase_thenIllegalStateExceptionIsThrown() {
        thrown.expect(IllegalStateException.class);

        databaseInstance.createDatabase();
    }

    @Test
    public void givenDatabaseInstanceIsNotOpened_whenSave_thenIllegalStateExceptionIsThrown() {
        thrown.expect(IllegalStateException.class);

        databaseInstance.save(Collections.emptyList());
    }
}
