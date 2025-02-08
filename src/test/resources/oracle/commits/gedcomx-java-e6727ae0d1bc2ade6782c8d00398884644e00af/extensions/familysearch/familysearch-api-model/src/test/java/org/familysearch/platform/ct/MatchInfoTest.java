package org.familysearch.platform.ct;

import org.gedcomx.common.URI;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatchInfoTest {

  @Test
  public void testMatchInfo() throws Exception {

    MatchInfo matchInfo = new MatchInfo();
    assertNull(matchInfo.getCollection());
    assertNull(matchInfo.getStatus());
    assertNull(matchInfo.getAddsPerson());
    assertNull(matchInfo.getAddsPerson110YearRule());
    assertNull(matchInfo.getAddsFact());
    assertNull(matchInfo.getAddsDateOrPlace());

    matchInfo.setKnownCollection(MatchCollection.tree);
    matchInfo.setKnownStatus(MatchStatus.Accepted);
    matchInfo.setAddsPerson(true);
    matchInfo.setAddsPerson110YearRule(true);
    matchInfo.setAddsFact(true);
    matchInfo.setAddsDateOrPlace(true);

    assertEquals(MatchCollection.tree.toQNameURI(), matchInfo.getCollection());
    assertEquals(MatchStatus.Accepted.toQNameURI(), matchInfo.getStatus());
    assertTrue(matchInfo.getAddsPerson());
    assertTrue(matchInfo.getAddsPerson110YearRule());
    assertTrue(matchInfo.getAddsFact());
    assertTrue(matchInfo.getAddsDateOrPlace());
  }

}
