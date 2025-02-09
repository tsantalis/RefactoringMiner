package org.gedcomx.conclusion;

import org.gedcomx.common.URI;
import org.gedcomx.types.IdentifierType;
import org.testng.annotations.Test;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Ryan Heaton
 */
@Test
public class IdentifierTest {

  /**
   * tests identifier xml
   */
  public void testIdXml() throws Exception {
    Identifier id = new Identifier();
    id.setKnownType(IdentifierType.Deprecated);
    id.setValue(URI.create("value"));
    id = processThroughXml(id);
    assertEquals(IdentifierType.Deprecated, id.getKnownType());
    assertEquals("value", id.getValue().toString());
  }

  /**
   * tests identifier json
   */
  public void testIdJson() throws Exception {
    Identifier id = new Identifier();
    id.setKnownType(IdentifierType.Deprecated);
    id.setValue(URI.create("value"));
    id = processThroughJson(id);
//    assertEquals(IdentifierType.Deprecated, id.getKnownType());
    assertEquals("value", id.getValue().toString());
  }

}
