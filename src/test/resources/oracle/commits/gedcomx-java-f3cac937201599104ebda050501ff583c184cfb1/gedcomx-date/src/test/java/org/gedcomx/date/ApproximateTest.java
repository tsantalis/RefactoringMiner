package org.gedcomx.date;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * @author John Clark.
 */
public class ApproximateTest {

  @Test
  public void errorOnBlankString() {
    try {
      new GedcomxDateApproximate("");
      fail("GedcomxDateException expected because date must not be empty");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Approximate Date: Must start with A");
    }
  }

  @Test
  public void errorOnNonA() {
    try {
      new GedcomxDateApproximate("+1000");
      fail("GedcomxDateException expected because date must start with A");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Approximate Date: Must start with A");
    }
  }

  @Test
  public void errorOnJustA() {
    try {
      new GedcomxDateApproximate("A");
      fail("GedcomxDateException expected because date must have something after A");
    } catch(GedcomxDateException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Date: Must have at least [+-]YYYY");
    }
  }

  @Test
  public void success() {
    GedcomxDateApproximate date = new GedcomxDateApproximate("A+1000-01-01T23:15:10-00:30");
    assertThat(date.getYear()).isEqualTo(1000);
    assertThat(date.getMonth()).isEqualTo(1);
    assertThat(date.getDay()).isEqualTo(1);
    assertThat(date.getHours()).isEqualTo(23);
    assertThat(date.getMinutes()).isEqualTo(15);
    assertThat(date.getSeconds()).isEqualTo(10);
    assertThat(date.getTzHours()).isEqualTo(0);
    assertThat(date.getTzMinutes()).isEqualTo(30);
  }

  /**
   * Other Methods
   */

  @Test
  public void getSimpleDate() {
    GedcomxDateApproximate date = new GedcomxDateApproximate("A+1000");
    GedcomxDateSimple simple = date.getSimpleDate();
    assertThat(simple.getYear()).isEqualTo(1000);
    assertThat(simple.getMonth()).isEqualTo(null);
  }

  @Test
  public void getType() {
    GedcomxDateApproximate date = new GedcomxDateApproximate("A+1000");
    assertThat(date.getType()).isEqualTo(GedcomxDateType.APPROXIMATE);
  }

  @Test
  public void isApproximate() {
    GedcomxDateApproximate date = new GedcomxDateApproximate("A+1000");
    assertThat(date.isApproximate()).isEqualTo(true);
  }

  @Test
  public void toFormalString() {
    List<String> tests = Arrays.asList("A+1000-01-01T24:00:00Z", "A-1000-01-01T23:15:15-06:30", "A+0001-12", "A-0090");
    for(String test: tests) {
      GedcomxDateApproximate date = new GedcomxDateApproximate(test);
      assertThat(date.toFormalString()).isEqualTo(test);
    }
  }
}
