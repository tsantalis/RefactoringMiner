package org.gedcomx.conclusion;

import org.gedcomx.types.FactType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FactTest {
  @Test
  public void testCtors() throws Exception {
    Fact fact;

    fact = new Fact();
    assertNull(fact.getAttribution());
    assertNull(fact.getConfidence());
    assertNull(fact.getDate());
    assertNull(fact.getExtensionElements());
    assertNull(fact.getId());
    assertNull(fact.getKnownType());
    assertNull(fact.getLang());
    assertNull(fact.getLink("junkRel"));
    assertNull(fact.getLinks());
    assertEquals(0, fact.getLinks("junkRel").size());
    assertNull(fact.getNotes());
    assertNull(fact.getPlace());
    assertNull(fact.getSources());
    assertNull(fact.getType());
    assertNull(fact.getTransientProperty("junkProp"));
    assertNull(fact.getValue());

    fact = new Fact(FactType.Birth, "junkValue");
    assertEquals(FactType.Birth, fact.getKnownType());
    assertEquals("http://gedcomx.org/Birth", fact.getType().toURI().toString());
    assertEquals("junkValue", fact.getValue());
    assertNull(fact.getAttribution());
    assertNull(fact.getConfidence());
    assertNull(fact.getDate());
    assertNull(fact.getExtensionElements());
    assertNull(fact.getId());
    assertNull(fact.getLang());
    assertNull(fact.getLink("junkRel"));
    assertNull(fact.getLinks());
    assertEquals(0, fact.getLinks("junkRel").size());
    assertNull(fact.getNotes());
    assertNull(fact.getPlace());
    assertNull(fact.getSources());
    assertNull(fact.getTransientProperty("junkProp"));

    Date date = new Date();
    date.setOriginal("junkDate");
    PlaceReference place = new PlaceReference();
    place.setOriginal("junkPlace");
    fact = new Fact(FactType.Birth, date, place, "junkValue");
    assertEquals(FactType.Birth, fact.getKnownType());
    assertEquals("http://gedcomx.org/Birth", fact.getType().toURI().toString());
    assertEquals("junkDate", fact.getDate().getOriginal());
    assertEquals("junkPlace", fact.getPlace().getOriginal());
    assertEquals("junkValue", fact.getValue());
    assertNull(fact.getAttribution());
    assertNull(fact.getConfidence());
    assertNull(fact.getExtensionElements());
    assertNull(fact.getId());
    assertNull(fact.getLang());
    assertNull(fact.getLink("junkRel"));
    assertNull(fact.getLinks());
    assertEquals(0, fact.getLinks("junkRel").size());
    assertNull(fact.getNotes());
    assertNull(fact.getSources());
    assertNull(fact.getTransientProperty("junkProp"));
  }

  @Test
  public void testSetKnownTypeWithNull() throws Exception {
    Fact fact = new Fact();
    fact.setKnownType(null);
    assertNull(fact.getKnownType());
  }
}
