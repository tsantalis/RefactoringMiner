package org.gedcomx.conclusion;

import org.gedcomx.common.Attribution;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.source.SourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.types.EventRoleType;
import org.gedcomx.types.EventType;
import org.junit.Test;

import java.util.ArrayList;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Ryan Heaton
 */
public class EventTest {

  @Test
  public void testCtors() throws Exception {
    Event event;

    event = new Event();
    assertNull(event.getAttribution());
    assertNull(event.getConfidence());
    assertNull(event.getDate());
    assertNull(event.getExtensionElements());
    assertNull(event.getId());
    assertNull(event.getKnownType());
    assertNull(event.getLang());
    assertNull(event.getLink("junkRel"));
    assertNull(event.getLinks());
    assertEquals(0, event.getLinks("junkRel").size());
    assertNull(event.getNotes());
    assertNull(event.getPlace());
    assertNull(event.getRoles());
    assertNull(event.getSources());
    assertNull(event.getType());
    assertNull(event.getTransientProperty("junkProp"));

    event = new Event(EventType.Birth);
    assertEquals(EventType.Birth, event.getKnownType());
    assertEquals("http://gedcomx.org/Birth", event.getType().toURI().toString());
    assertNull(event.getAttribution());
    assertNull(event.getConfidence());
    assertNull(event.getDate());
    assertNull(event.getExtensionElements());
    assertNull(event.getId());
    assertNull(event.getLang());
    assertNull(event.getLink("junkRel"));
    assertNull(event.getLinks());
    assertEquals(0, event.getLinks("junkRel").size());
    assertNull(event.getNotes());
    assertNull(event.getPlace());
    assertNull(event.getRoles());
    assertNull(event.getSources());
    assertNull(event.getTransientProperty("junkProp"));

    Date date = new Date();
    date.setOriginal("junkDate");
    PlaceReference place = new PlaceReference();
    place.setOriginal("junkPlace");
    event = new Event(EventType.Birth, date, place);
    assertEquals(EventType.Birth, event.getKnownType());
    assertEquals("http://gedcomx.org/Birth", event.getType().toURI().toString());
    assertEquals("junkDate", event.getDate().getOriginal());
    assertEquals("junkPlace", event.getPlace().getOriginal());
    assertNull(event.getAttribution());
    assertNull(event.getConfidence());
    assertNull(event.getExtensionElements());
    assertNull(event.getId());
    assertNull(event.getLang());
    assertNull(event.getLink("junkRel"));
    assertNull(event.getLinks());
    assertEquals(0, event.getLinks("junkRel").size());
    assertNull(event.getNotes());
    assertNull(event.getRoles());
    assertNull(event.getSources());
    assertNull(event.getTransientProperty("junkProp"));
  }

  @Test
  public void testSetKnownTypeWithNull() throws Exception {
    Event event = new Event();
    event.setKnownType(null);
    assertNull(event.getKnownType());
  }

  /**
   * tests processing a event through xml...
   */
  @Test
  public void testEventXml() throws Exception {
    Event event = createTestEvent();
    event = processThroughXml(event);
    assertTestEvent(event);
  }

  /**
   * tests processing a event through json...
   */
  @Test
  public void testPersonJson() throws Exception {
    Event event = createTestEvent();
    event = processThroughJson(event);
    assertTestEvent(event);
  }

  private Event createTestEvent() {
    Event event = new Event();
    event.setKnownType(EventType.Marriage);
    event.setAttribution(new Attribution());
    event.getAttribution().setChangeMessage("explanation");
    event.setDate(new Date());
    event.getDate().setOriginal("date");
    event.setPlace(new PlaceReference());
    event.getPlace().setOriginal("place");
    event.setRoles(new ArrayList<EventRole>());
    EventRole role = new EventRole();
    role.setKnownType(EventRoleType.Official);
    role.setPerson(new ResourceReference());
    role.getPerson().setResource(URI.create("urn:person"));
    event.getRoles().add(role);
    SourceReference sourceReference = new SourceReference();
    sourceReference.setDescriptionRef(URI.create("urn:source-ref"));
    event.addSource(sourceReference);
    return event;
  }

  private void assertTestEvent(Event event) {
    assertEquals(EventType.Marriage, event.getKnownType());
    assertEquals("explanation", event.getAttribution().getChangeMessage());
    assertEquals("date", event.getDate().getOriginal());
    assertEquals("place", event.getPlace().getOriginal());
    assertEquals(1, event.getRoles().size());
    assertEquals(EventRoleType.Official, event.getRoles().get(0).getKnownType());
    assertEquals("urn:person", event.getRoles().get(0).getPerson().getResource().toString());
    assertEquals("urn:source-ref", event.getSources().get(0).getDescriptionRef().toURI().toString());
  }

}
