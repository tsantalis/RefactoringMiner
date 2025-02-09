package org.familysearch.platform.ct;

import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.testng.annotations.Test;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;


public class ChangeInfoTest {
  @Test
  public void testGetReason() throws Exception {
    ChangeInfo changeInfo = new ChangeInfo();
    assertNull(changeInfo.getReason());
    assertNull(changeInfo.getKnownOperation());
    assertNull(changeInfo.getObjectType());
    assertNull(changeInfo.getParent());
    assertNull(changeInfo.getResulting());
    assertNull(changeInfo.getOriginal());
    assertNull(changeInfo.getRemoved());

    changeInfo.setReason("junkReason");
    changeInfo.setKnownOperation(ChangeOperation.Delete);
    changeInfo.setObjectType(URI.create("urn:hi"));
    changeInfo.setParent(new ResourceReference(URI.create("urn:junkParent")));
    changeInfo.setResulting(new ResourceReference(URI.create("#CHNG-001.PRSN-001.resulting")));
    changeInfo.setOriginal(new ResourceReference(URI.create("#CHNG-001.PRSN-001.original")));
    changeInfo.setRemoved(new ResourceReference(URI.create("#CHNG-001.PRSN-001.removed")));
    assertEquals("junkReason", changeInfo.getReason());
    assertEquals(ChangeOperation.Delete, changeInfo.getKnownOperation());
    assertEquals("urn:hi", changeInfo.getObjectType().toString());
    assertEquals("urn:junkParent", changeInfo.getParent().getResource().toString());
    assertEquals("#CHNG-001.PRSN-001.resulting", changeInfo.getResulting().getResource().toString());
    assertEquals("#CHNG-001.PRSN-001.original", changeInfo.getOriginal().getResource().toString());
    assertEquals("#CHNG-001.PRSN-001.removed", changeInfo.getRemoved().getResource().toString());
  }
}
