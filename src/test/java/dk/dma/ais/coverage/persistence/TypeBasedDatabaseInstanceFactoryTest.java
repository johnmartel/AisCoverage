package dk.dma.ais.coverage.persistence;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TypeBasedDatabaseInstanceFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private DatabaseInstanceFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new TypeBasedDatabaseInstanceFactory();
    }

    @Test
    public void givenMemoryOnlyDatabaseType_whenCreateDatabaseInstance_thenInstanceReturnedIsOfTypeMemoryOnly() {
        DatabaseInstance databaseInstance = factory.createDatabaseInstance("MemoryOnly");

        assertThat(databaseInstance, is(instanceOf(MemoryOnlyDatabaseInstance.class)));
    }

    @Test
    public void givenMemoryOnlyDatabaseType_whenCreateDatabaseInstance_thenTypeMatchingIsCaseInsensitive() {
        testThatTypeMatchingIsCaseInsensitive("MemoryOnly", MemoryOnlyDatabaseInstance.class);
    }

    private void testThatTypeMatchingIsCaseInsensitive(String databaseType, Class<? extends DatabaseInstance> expectedInstanceType) {
        DatabaseInstance camelCaseMemoryOnlyDatabaseInstance = factory.createDatabaseInstance(databaseType);
        DatabaseInstance lowerCaseMemoryOnlyDatabaseInstance = factory.createDatabaseInstance(databaseType.toLowerCase());
        DatabaseInstance upperCaseMemoryOnlyDatabaseInstance = factory.createDatabaseInstance(databaseType.toUpperCase());

        assertThat(camelCaseMemoryOnlyDatabaseInstance, is(instanceOf(expectedInstanceType)));
        assertThat(lowerCaseMemoryOnlyDatabaseInstance, is(instanceOf(expectedInstanceType)));
        assertThat(upperCaseMemoryOnlyDatabaseInstance, is(instanceOf(expectedInstanceType)));
    }

    @Test
    public void givenMongoDbDatabaseType_whenCreateDatabaseInstance_thenInstanceReturnedIsOfTypeMongoDb() {
        DatabaseInstance databaseInstance = factory.createDatabaseInstance("MongoDB");

        assertThat(databaseInstance, is(instanceOf(MongoDatabaseInstance.class)));
    }

    @Test
    public void givenMongoDbDatabaseType_whenCreateDatabaseInstance_thenTypeMatchingIsCaseInsensitive() {
        testThatTypeMatchingIsCaseInsensitive("MongoDB", MongoDatabaseInstance.class);
    }

    @Test
    public void givenUnknownDatabaseType_whenCreateDatabaseInstance_thenUnknownDatabaseTypeExceptionIsThrown() {
        thrown.expect(UnknownDatabaseTypeException.class);
        thrown.expectMessage(containsString("MySQL"));

        factory.createDatabaseInstance("MySQL");
    }
}
