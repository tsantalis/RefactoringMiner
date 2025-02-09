package org.gedcomx.conclusion;

import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Class for testing the Family class.
 * User: Randy Wilson
 * Date: 15 May 2015
 */
public class FamilyTest {

  @Test
  public void testFamily() {
    FamilyView family = new FamilyView();

    // Test parents and children
    family.setParent1(new ResourceReference(new URI("#father"), "father"));
    family.setParent2(new ResourceReference(new URI("#mother"), "mother"));
    family.addChild(new ResourceReference(new URI("#child1"), "child1"));
    family.addChild(new ResourceReference(new URI("#child2"), "child2"));
    assertEquals("father", family.getParent1().getResourceId());
    assertEquals("#father", family.getParent1().getResource().toString());
    assertEquals("#mother", family.getParent2().getResource().toString());
    assertEquals(2, family.getChildren().size());
    assertEquals("#child1", family.getChildren().get(0).getResource().toString());
    assertEquals("#child2", family.getChildren().get(1).getResource().toString());
  }
}
