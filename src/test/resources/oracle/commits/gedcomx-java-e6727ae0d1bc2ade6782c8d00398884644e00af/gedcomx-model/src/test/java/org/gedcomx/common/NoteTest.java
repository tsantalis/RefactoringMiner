package org.gedcomx.common;

import org.junit.Test;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.*;


/**
 * @author Ryan Heaton
 */
public class NoteTest {

  /**
   * tests note xml
   */
  @Test
  public void testNoteXml() throws Exception {
    Note note = new Note();
    note.setText("hello, there");
    note.setId("id");
    note.setSubject("subject");
    note.setAttribution(new Attribution());
    note.getAttribution().setChangeMessage("note statement");
    Note otherNote = new Note();
    otherNote.setText("note of a note");
    Note otherNote2 = new Note();
    otherNote2.setText("note2 of a note");
    note.setLang("en");

    note = processThroughXml(note);

    assertEquals("hello, there", note.getText());
    assertEquals("subject", note.getSubject());
    assertEquals("note statement", note.getAttribution().getChangeMessage());
    assertEquals("en", note.getLang());
    assertEquals("id", note.getId());

    assertTrue(note.toString().contains("hello, there"));
    note.setText("long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long note");
    assertTrue(note.toString().contains("..."));
    note.setText(null);
    assertNotNull(note.toString());
  }

  /**
   * tests note json
   */
  @Test
  public void testNoteJson() throws Exception {
    Note note = new Note();
    note.setSubject("subject");
    note.setText("hello, there");
    note.setAttribution(new Attribution());
    note.getAttribution().setChangeMessage("note statement");
    note.setLang("en");

    note = processThroughJson(note);

    assertEquals("subject", note.getSubject());
    assertEquals("hello, there", note.getText());
    assertEquals("note statement", note.getAttribution().getChangeMessage());
    assertEquals("en", note.getLang());
  }

}
