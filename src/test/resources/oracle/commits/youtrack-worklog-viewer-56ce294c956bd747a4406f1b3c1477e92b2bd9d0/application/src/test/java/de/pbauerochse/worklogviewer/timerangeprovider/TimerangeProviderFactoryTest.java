package de.pbauerochse.worklogviewer.timerangeprovider;

import de.pbauerochse.worklogviewer.domain.ReportTimerange;
import de.pbauerochse.worklogviewer.domain.TimerangeProvider;
import de.pbauerochse.worklogviewer.domain.timerangeprovider.TimerangeProviderFactory;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

/**
 * @author Patrick Bauerochse
 * @since 07.07.15
 */
public class TimerangeProviderFactoryTest {

    @Test
    public void performTest() {

        LocalDate now = LocalDate.now();

        for (ReportTimerange reportTimerange : ReportTimerange.values()) {
            TimerangeProvider timerangeProvider = TimerangeProviderFactory.getTimerangeProvider(reportTimerange, now, now);
            Assert.assertNotNull("TimerangeProvider for timerange " + reportTimerange.name() + " was null", timerangeProvider);

            if (reportTimerange == ReportTimerange.CUSTOM) {
                Assert.assertEquals(now, timerangeProvider.getTimeRange().getStart());
                Assert.assertEquals(now, timerangeProvider.getTimeRange().getEnd());
            }
        }
    }
}
