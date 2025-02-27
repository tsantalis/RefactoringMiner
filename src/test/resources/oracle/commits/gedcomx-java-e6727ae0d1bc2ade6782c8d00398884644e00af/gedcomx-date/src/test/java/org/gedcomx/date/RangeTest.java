package org.gedcomx.date;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * @author John Clark.
 */
public class RangeTest {

  @Test
  public void errorOnBlankString() {
    try {
      new GedcomxDateRange("");
      fail("GedcomxDateException expected because date must not be empty");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Range");
    }
  }

  @Test
  public void errorOnJustA() {
    try {
      new GedcomxDateRange("A");
      fail("GedcomxDateException expected because range must have a /");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Range: / is required");
    }
  }

  @Test
   public void errorOnOnePart() {
    try {
      new GedcomxDateRange("A+1000");
      fail("GedcomxDateException expected because range must have a /");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Range: / is required");
    }
  }

  @Test
  public void errorOnJustSlash() {
    try {
      new GedcomxDateRange("/");
      fail("GedcomxDateException expected because range must have something other than a /");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Range: One or two parts are required");
    }
  }

  @Test
  public void errorOnManySlashes() {
    try {
      new GedcomxDateRange("+1000/+2000/+3000");
      fail("GedcomxDateException expected because range must have only 1 slash");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Range: One or two parts are required");
    }
  }

  @Test
  public void errorOnInvalidPart1() {
    try {
      new GedcomxDateRange("+1000_10/");
      fail("GedcomxDateException expected because range has invalid part 1");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Date: Invalid Year-Month Separator in Range Start Date");
    }
  }

  @Test
  public void errorOnDurationOnly() {
    try {
      new GedcomxDateRange("/P100Y");
      fail("GedcomxDateException expected because range only has a duration");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Range: A range may not end with a duration if missing a start date");
    }
  }

  @Test
  public void errorOnInvalidDuration() {
    try {
      new GedcomxDateRange("+1000/P100Q");
      fail("GedcomxDateException expected because range only has a duration");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Duration: Unknown letter Q in Range End Duration");
    }
  }

  @Test
  public void errorOnInvalidPart2() {
    try {
      new GedcomxDateRange("/+1000_10");
      fail("GedcomxDateException expected because range has invalid part 2");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Date: Invalid Year-Month Separator in Range End Date");
    }
  }

  @Test
  public void successOnDuration() {
    GedcomxDateRange range = new GedcomxDateRange("+1000/P1Y2M3DT4H5M6S");
    GedcomxDateSimple start = range.getStart();
    GedcomxDateSimple end = range.getEnd();
    GedcomxDateDuration duration = range.getDuration();

    assertThat(start.getYear()).isEqualTo(1000);
    assertThat(start.getMonth()).isEqualTo(null);

    assertThat(end.getYear()).isEqualTo(1001);
    assertThat(end.getMonth()).isEqualTo(3);
    assertThat(end.getDay()).isEqualTo(4);
    assertThat(end.getHours()).isEqualTo(4);
    assertThat(end.getMinutes()).isEqualTo(5);
    assertThat(end.getSeconds()).isEqualTo(6);

    assertThat(duration.getYears()).isEqualTo(1);
    assertThat(duration.getMonths()).isEqualTo(2);
    assertThat(duration.getDays()).isEqualTo(3);
    assertThat(duration.getHours()).isEqualTo(4);
    assertThat(duration.getMinutes()).isEqualTo(5);
    assertThat(duration.getSeconds()).isEqualTo(6);
  }

  @Test
  public void successOnEndDate() {
    GedcomxDateRange range = new GedcomxDateRange("+1000/+2000-10-01");
    GedcomxDateSimple start = range.getStart();
    GedcomxDateSimple end = range.getEnd();
    GedcomxDateDuration duration = range.getDuration();

    assertThat(start.getYear()).isEqualTo(1000);
    assertThat(start.getMonth()).isEqualTo(null);

    assertThat(end.getYear()).isEqualTo(2000);
    assertThat(end.getMonth()).isEqualTo(10);
    assertThat(end.getDay()).isEqualTo(1);
    assertThat(end.getHours()).isEqualTo(null);
    assertThat(end.getMinutes()).isEqualTo(null);
    assertThat(end.getSeconds()).isEqualTo(null);

    assertThat(duration.getYears()).isEqualTo(1000);
    assertThat(duration.getMonths()).isEqualTo(9);
    assertThat(duration.getDays()).isEqualTo(null);
    assertThat(duration.getHours()).isEqualTo(null);
    assertThat(duration.getMinutes()).isEqualTo(null);
    assertThat(duration.getSeconds()).isEqualTo(null);
  }

  /**
   * Other Methods
   */

  @Test
  public void getType() {
    GedcomxDateRange range = new GedcomxDateRange("+1000/+2000-10-01");
    assertThat(range.getType()).isEqualTo(GedcomxDateType.RANGE);
  }

  @Test
  public void isApproximate() {
    GedcomxDateRange range;

    range = new GedcomxDateRange("A+1000/+2000-10-01");
    assertThat(range.isApproximate()).isEqualTo(true);

    range = new GedcomxDateRange("+1000/+2000-10-01");
    assertThat(range.isApproximate()).isEqualTo(false);
  }

  @Test
  public void toFormalString() {
    List<String> tests = Arrays.asList("+1000/P1000Y9M", "A+1000/P1000Y9M", "/+1000");
    for(String test: tests) {
      GedcomxDateRange range = new GedcomxDateRange(test);
      assertThat(range.toFormalString()).isEqualTo(test);
    }
  }

}
