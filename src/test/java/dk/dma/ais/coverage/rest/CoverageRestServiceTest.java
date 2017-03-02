package dk.dma.ais.coverage.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.dma.ais.coverage.AisCoverageBuilder;
import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.export.data.Status;

/**
 * Tests in this class are synchronized because they are all using {@code Helper.firstMessage} and
 * {@code Helper.latestMessage}, which are static members.
 */
public class CoverageRestServiceTest {
    private AisCoverageBuilder aisCoverageBuilder;
    private CoverageRestService restService;

    @Before
    public void setUp() throws Exception {
        aisCoverageBuilder = new AisCoverageBuilder();
        aisCoverageBuilder.withMockAisCoverageConfiguration()
                .withMockAisBusConfiguration()
                .withMockDatabaseConfiguration()
                .withMockDatabaseInstanceForAnyType()
                .build();

        restService = new CoverageRestService();
    }

    @After
    public void tearDown() throws Exception {
        Helper.firstMessage = null;
        Helper.latestMessage = null;
    }

    @Test
    public synchronized void givenNoMessageReceivedYet_whenStatus_thenStatusRunningIsReturned() throws IOException {
        Date now = Helper.getFloorDate(new Date());

        Status status = (Status) restService.status();

        assertThatStatusIsRunning(status);
        assertThat(status.firstMessage, is(equalTo(now.getTime())));
        assertThat(status.lastMessage, is(equalTo(now.getTime())));
    }

    private void assertThatStatusIsRunning(Status status) {
        assertThat(status, is(not(nullValue())));
        assertThat(status.analysisStatus, is(equalTo("Running")));
    }

    @Test
    public synchronized void givenAFewMessagesReceived_whenStatus_thenStatusRunningIsReturned_andFirstMessageTimestampIsSet_andLatestMessageTimestampIsSet() throws IOException {
        Helper.firstMessage = Helper.getFloorDate(new Date());
        Helper.latestMessage = Helper.getCeilDate(new Date());

        Status status = (Status) restService.status();

        assertThatStatusIsRunning(status);
        assertThat(status.firstMessage, is(equalTo(Helper.firstMessage.getTime())));
        assertThat(status.lastMessage, is(equalTo(Helper.latestMessage.getTime())));
    }

    @Test
    public synchronized void givenNoTerrestrialMessageReceivedYet_whenStatus_thenStatusRunningIsReturned_andFirstMessageTimestampIsSet() throws IOException {
        Helper.firstMessage = Helper.getFloorDate(new Date());
        Date now = Helper.getFloorDate(new Date());

        Status status = (Status) restService.status();

        assertThatStatusIsRunning(status);
        assertThat(status.firstMessage, is(equalTo(Helper.firstMessage.getTime())));
        assertTrue(status.lastMessage >= now.getTime());
    }
}
