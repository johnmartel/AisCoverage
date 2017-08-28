package dk.dma.ais.coverage;

import dk.dma.commons.util.DateTimeUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.PeriodType;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PurgerTest {

    @Test
    public void givenLargeWindowSize_whenGetTrimPoint_windowSizeCorrectlyConvertedToHours() {
        Purger purger = new Purger(1000, null, 1);
        Helper.firstMessage = new Date();
        final int maxWindowSize = 1000;
        Helper.latestMessage = DateUtils.addHours(new Date(), 2 * maxWindowSize);

        Date trimPoint = purger.getTrimPoint();

        assertThat(DateTimeUtil.toInterval(Helper.firstMessage, trimPoint).toPeriod(PeriodType.hours()).getHours(), is(equalTo(maxWindowSize)));
    }
}
