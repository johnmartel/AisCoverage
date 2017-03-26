package dk.dma.ais.coverage.export;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class ExportDataTypeTest {

    @Test
    public void givenReceivedMessages_whenValueOf_thenReceivedMessagesIsReturned() {
        ExportDataType exportDataType = ExportDataType.forType("received_messages");

        assertThat(exportDataType, is(ExportDataType.RECEIVED_MESSAGES));
    }

    @Test
    public void givenSignalStrength_whenValueOf_thensignalStrengthIsReturned() {
        ExportDataType exportDataType = ExportDataType.forType("signal_strength");

        assertThat(exportDataType, is(ExportDataType.SIGNAL_STRENGTH));
    }

    @Test
    public void givenReceivedMessages_whenGreenThreshold_thenValueIs80Percent() {
        assertThat(ExportDataType.RECEIVED_MESSAGES.greenThreshold(), is(closeTo(0.8, 0.001)));
    }

    @Test
    public void givenReceivedMessages_whenRedThreshold_thenValueIs50Percent() {
        assertThat(ExportDataType.RECEIVED_MESSAGES.redThreshold(), is(closeTo(0.5, 0.001)));
    }
}
