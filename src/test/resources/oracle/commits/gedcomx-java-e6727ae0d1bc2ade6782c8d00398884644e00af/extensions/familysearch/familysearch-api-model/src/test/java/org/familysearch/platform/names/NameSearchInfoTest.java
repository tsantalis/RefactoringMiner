package org.familysearch.platform.names;

import org.junit.Test;

import org.gedcomx.types.NamePartType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class NameSearchInfoTest {

  private static final String TEST_TEXT = "just some text that would be a name";
  private static final String TEST_NAME_ID = "12349874561";
  private static final int TEST_WEIGHT = 83;

  @Test
  public void testNameSearchInfo() throws Exception {

    NameSearchInfo nameSearchInfo = new NameSearchInfo();
    assertNull(nameSearchInfo.getText());
    assertNull(nameSearchInfo.getNameId());
    assertNull(nameSearchInfo.getNamePartType());
    assertNull(nameSearchInfo.getKnownNamePartType());
    assertNull(nameSearchInfo.getWeight());

    nameSearchInfo.setText(TEST_TEXT);
    nameSearchInfo.setNameId(TEST_NAME_ID);
    nameSearchInfo.setNamePartType(NamePartType.Given.toQNameURI());
    nameSearchInfo.setWeight(TEST_WEIGHT);

    assertEquals(nameSearchInfo.getText(), TEST_TEXT);
    assertEquals(nameSearchInfo.getNameId(), TEST_NAME_ID);
    assertEquals(nameSearchInfo.getNamePartType(), NamePartType.Given.toQNameURI());
    assertEquals(nameSearchInfo.getKnownNamePartType(), NamePartType.Given);
    assertEquals(nameSearchInfo.getWeight(), Integer.valueOf(TEST_WEIGHT));

    nameSearchInfo.setKnownNamePartType(NamePartType.Surname);
    assertEquals(nameSearchInfo.getNamePartType(), NamePartType.Surname.toQNameURI());
    assertEquals(nameSearchInfo.getKnownNamePartType(), NamePartType.Surname);

  }
}
