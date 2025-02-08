package org.familysearch.platform.places;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PlaceDescriptionInfoTest {

  @Test
  public void testZoomLevel() {
    final Integer zoomLevel = 7;

    PlaceDescriptionInfo result = new PlaceDescriptionInfo();
    result.setZoomLevel(zoomLevel);
    Assert.assertEquals(result.getZoomLevel(), zoomLevel);

    result = new PlaceDescriptionInfo().zoomLevel(zoomLevel);
    Assert.assertEquals(result.getZoomLevel(), zoomLevel);
  }

  @Test
  public void testRelatedPlaceDescriptionType() {
    final String type = "ASSOCIATED";

    PlaceDescriptionInfo result = new PlaceDescriptionInfo();
    result.setRelatedType(type);
    Assert.assertEquals(result.getRelatedType(), type);

    result = new PlaceDescriptionInfo().type(type);
    Assert.assertEquals(result.getRelatedType(), type);
  }

  @Test
  public void testRelatedPlaceDescriptionSubType() {
    final String subType = "Political";

    PlaceDescriptionInfo result = new PlaceDescriptionInfo();
    result.setRelatedSubType(subType);
    Assert.assertEquals(result.getRelatedSubType(), subType);

    result = new PlaceDescriptionInfo().subType(subType);
    Assert.assertEquals(result.getRelatedSubType(), subType);
  }

}
