package org.familysearch.platform.places;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlaceDescriptionInfoTest {

  @Test
  public void testZoomLevel() {
    final Integer zoomLevel = 7;

    PlaceDescriptionInfo result = new PlaceDescriptionInfo();
    result.setZoomLevel(zoomLevel);
    assertEquals(result.getZoomLevel(), zoomLevel);

    result = new PlaceDescriptionInfo().zoomLevel(zoomLevel);
    assertEquals(result.getZoomLevel(), zoomLevel);
  }

  @Test
  public void testRelatedPlaceDescriptionType() {
    final String type = "ASSOCIATED";

    PlaceDescriptionInfo result = new PlaceDescriptionInfo();
    result.setRelatedType(type);
    assertEquals(result.getRelatedType(), type);

    result = new PlaceDescriptionInfo().type(type);
    assertEquals(result.getRelatedType(), type);
  }

  @Test
  public void testRelatedPlaceDescriptionSubType() {
    final String subType = "Political";

    PlaceDescriptionInfo result = new PlaceDescriptionInfo();
    result.setRelatedSubType(subType);
    assertEquals(result.getRelatedSubType(), subType);

    result = new PlaceDescriptionInfo().subType(subType);
    assertEquals(result.getRelatedSubType(), subType);
  }

}
