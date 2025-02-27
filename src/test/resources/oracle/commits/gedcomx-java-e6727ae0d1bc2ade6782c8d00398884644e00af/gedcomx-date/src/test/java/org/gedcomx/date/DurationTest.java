package org.gedcomx.date;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * @author John Clark.
 */
public class DurationTest {

  @Test
  public void errorOnMissingP() {
    try {
      new GedcomxDateDuration("1000Y");
      fail("GedcomxDateException expected because duration must start with P");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Must start with P");
    }
  }

  @Test
  public void errorOnMissingValuesAfterP() {
    try {
      new GedcomxDateDuration("P");
      fail("GedcomxDateException expected because P must be followed by something");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: You must have a duration value");
    }
  }

  @Test
  public void errorOnSpaceAfterP() {
    try {
      new GedcomxDateDuration("P100 years");
      fail("GedcomxDateException expected because no spaces allowed in duration");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Non normalized durations are not implemented yet");
    }
  }

  @Test
  public void errorOnUnknownLetters() {
    try {
      new GedcomxDateDuration("P1000U");
      fail("GedcomxDateException expected because U is unknown");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Unknown letter U");
    }
  }

  @Test
  public void errorOnMissingLetters() {
    try {
      new GedcomxDateDuration("P1000");
      fail("GedcomxDateException expected because we're missing letters");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: No letter after 1000");
    }
  }

  @Test
  public void errorOnInvalidYear() {
    try {
      new GedcomxDateDuration("PY");
      fail("GedcomxDateException expected because missing year before Y");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Invalid years");
    }
  }

  @Test
  public void errorOnDuplicateYear() {
    try {
      new GedcomxDateDuration("P1000Y10Y");
      fail("GedcomxDateException expected because duplicate years");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate years");
    }
  }

  @Test
  public void errorOnOutOfOrderYear() {
    try {
      new GedcomxDateDuration("P10M10Y");
      fail("GedcomxDateException expected because year is out of order");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Years out of order");
    }
  }

  @Test
  public void errorOnInvalidMonth() {
    try {
      new GedcomxDateDuration("P1000YM");
      fail("GedcomxDateException expected because missing months before M");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Invalid months");
    }
  }

  @Test
  public void errorOnDuplicateMonths() {
    try {
      new GedcomxDateDuration("P10M5M");
      fail("GedcomxDateException expected because duplicate months");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate months");
    }
  }

  @Test
  public void errorOnOutOfOrderMonth() {
    try {
      new GedcomxDateDuration("P1Y1D1M");
      fail("GedcomxDateException expected because months is out of order");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Months out of order");
    }
  }

  @Test
  public void errorOnInvalidDay() {
    try {
      new GedcomxDateDuration("P1MD");
      fail("GedcomxDateException expected because no value for day");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Invalid days");
    }
  }

  @Test
  public void errorOnDuplicateDays() {
    try {
      new GedcomxDateDuration("P1D2D");
      fail("GedcomxDateException expected because duplicate days");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate days");
    }
  }

  @Test
  public void errorOnOutOfOrderDay() {
    try {
      new GedcomxDateDuration("P1MT01D");
      fail("GedcomxDateException expected because day is out of order");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Days out of order");
    }
  }

  @Test
  public void errorOnDuplicateTs() {
    try {
      new GedcomxDateDuration("P1Y2M3DT1HT");
      fail("GedcomxDateException expected because duplicate Ts");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate T");
    }
  }

  @Test
  public void errorOnMissingTBeforeHours() {
    try {
      new GedcomxDateDuration("P1Y2M03D4H");
      fail("GedcomxDateException expected because missing T before hours");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Missing T before hours");
    }
  }

  @Test
  public void errorOnInvalidHour() {
    try {
      new GedcomxDateDuration("P1Y2M03DTH");
      fail("GedcomxDateException expected because no value for hour");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Invalid hours");
    }
  }

  @Test
  public void errorOnDuplicateHours() {
    try {
      new GedcomxDateDuration("P1000Y01MT01H01H");
      fail("GedcomxDateException expected because hours are duplicated");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate hours");
    }
  }

  @Test
  public void errorOnOutOfOrderHours() {
    try {
      new GedcomxDateDuration("P1000Y01MT01M01H");
      fail("GedcomxDateException expected because hours is out of order");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Hours out of order");
    }
  }

  @Test
  public void errorOnInvalidMinute() {
    try {
      new GedcomxDateDuration("P1000Y01M01DT01HM");
      fail("GedcomxDateException expected because");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Invalid minutes");
    }
  }

  @Test
  public void errorOnDuplicateMinutes() {
    try {
      new GedcomxDateDuration("P1000Y01MT01H01M01M");
      fail("GedcomxDateException expected because minutes are duplicated");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate minutes");
    }
  }

  @Test
  public void errorOnOutOfOrderMinutes() {
    try {
      new GedcomxDateDuration("P1000Y01MT01H01S01M");
      fail("GedcomxDateException expected because minutes are out of order");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Minutes out of order");
    }
  }

  @Test
  public void errorOnMissingTBeforeSeconds() {
    try {
      new GedcomxDateDuration("P1000Y01M01D01S");
      fail("GedcomxDateException expected because missing T before seconds");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Missing T before seconds");
    }
  }

  @Test
  public void errorOnInvalidSeconds() {
    try {
      new GedcomxDateDuration("P1000Y01M01DT01H01MS");
      fail("GedcomxDateException expected because seconds has no value");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Invalid seconds");
    }
  }

  @Test
  public void errorOnDuplicateSeconds() {
    try {
      new GedcomxDateDuration("P1000Y01MT01H01M01S01S");
      fail("GedcomxDateException expected because seconds is duplicated");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Duplicate seconds");
    }
  }

  @Test
  public void succeed() {
    GedcomxDateDuration duration = new GedcomxDateDuration("P1000Y1M02DT3H04M57S");
    assertThat(duration.getYears()).isEqualTo(1000);
    assertThat(duration.getMonths()).isEqualTo(1);
    assertThat(duration.getDays()).isEqualTo(2);
    assertThat(duration.getHours()).isEqualTo(3);
    assertThat(duration.getMinutes()).isEqualTo(4);
    assertThat(duration.getSeconds()).isEqualTo(57);
  }

  @Test
  public void succeedWithoutTime() {
    GedcomxDateDuration duration = new GedcomxDateDuration("P1000Y1M02D");
    assertThat(duration.getYears()).isEqualTo(1000);
    assertThat(duration.getMonths()).isEqualTo(1);
    assertThat(duration.getDays()).isEqualTo(2);
    assertThat(duration.getHours()).isEqualTo(null);
    assertThat(duration.getMinutes()).isEqualTo(null);
    assertThat(duration.getSeconds()).isEqualTo(null);
  }

  @Test
  public void getType() {
    GedcomxDateDuration duration = new GedcomxDateDuration("P1Y");
    assertThat(duration.getType()).isEqualTo(GedcomxDateType.DURATION);
  }

  @Test
  public void isApproximate() {
    GedcomxDateDuration duration = new GedcomxDateDuration("P1Y");
    assertThat(duration.isApproximate()).isEqualTo(false);
  }

  @Test
  public void toFormalString() {
    List<String> tests = Arrays.asList("P1Y2M3DT4H5M6S", "P100YT3H", "PT1S");
    for(String test: tests) {
      GedcomxDateDuration duration = new GedcomxDateDuration(test);
      assertThat(duration.toFormalString()).isEqualTo(test);
    }
  }

}
