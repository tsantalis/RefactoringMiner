package org.gedcomx.conclusion;

import org.gedcomx.common.URI;
import org.gedcomx.types.IdentifierType;
import org.junit.Test;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Heaton
 */
public class IdentifierTest {

  /**
   * tests identifier xml
   */
  @Test
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
  @Test
  public void testIdJson() throws Exception {
    Identifier id = new Identifier();
    id.setKnownType(IdentifierType.Deprecated);
    id.setValue(URI.create("value"));
    id = processThroughJson(id);
//    assertEquals(IdentifierType.Deprecated, id.getKnownType());
    assertEquals("value", id.getValue().toString());
  }

}
