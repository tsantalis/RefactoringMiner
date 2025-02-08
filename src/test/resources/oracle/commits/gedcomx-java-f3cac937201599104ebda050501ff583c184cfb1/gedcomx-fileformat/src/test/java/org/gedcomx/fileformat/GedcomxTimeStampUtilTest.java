package org.gedcomx.fileformat;

import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.testng.Assert.assertEquals;


public class GedcomxTimeStampUtilTest {
  private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

  @Test
  public void testFormatAsXmlUTC() throws Exception {
    SimpleDateFormat format1 = (SimpleDateFormat)DateFormat.getDateTimeInstance();
    SimpleDateFormat format2 = (SimpleDateFormat)DateFormat.getDateTimeInstance();
    SimpleDateFormat targetFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
    format1.applyPattern("d MMM yy");
    format2.applyPattern("d MMM yy HH:mm:ss.SSS");
    targetFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    targetFormat.setTimeZone(UTC_TIME_ZONE);

    Date myDate;
    String myDateFormatted;

    myDate = format1.parse("11 Nov 2011");
    myDateFormatted = GedcomxTimeStampUtil.formatAsXmlUTC(myDate);
    assertEquals(myDateFormatted, targetFormat.format(myDate));

    myDate = format2.parse("11 Nov 2011 11:11:11.111");
    myDateFormatted = GedcomxTimeStampUtil.formatAsXmlUTC(myDate);
    assertEquals(myDateFormatted, targetFormat.format(myDate));

    format1.setTimeZone(UTC_TIME_ZONE);
    format2.setTimeZone(UTC_TIME_ZONE);

    myDate = format1.parse("11 Nov 2011");
    myDateFormatted = GedcomxTimeStampUtil.formatAsXmlUTC(myDate);
    assertEquals(myDateFormatted, "2011-11-11T00:00:00.000Z");

    myDate = format2.parse("11 Nov 2011 11:11:11.111");
    myDateFormatted = GedcomxTimeStampUtil.formatAsXmlUTC(myDate);
    assertEquals(myDateFormatted, "2011-11-11T11:11:11.111Z");
  }
}
