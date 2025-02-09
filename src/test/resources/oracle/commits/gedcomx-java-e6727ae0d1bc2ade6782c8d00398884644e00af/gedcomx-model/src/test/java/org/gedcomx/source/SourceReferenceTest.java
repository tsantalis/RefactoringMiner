package org.gedcomx.source;

import org.gedcomx.common.Attribution;
import org.gedcomx.common.CustomEntity;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.rt.SerializationUtil;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Ryan Heaton
 */
public class SourceReferenceTest {

  /**
   * tests source reference xml
   */
  @Test
  public void testSourceReferenceXml() throws Exception {
    SourceReference reference = new SourceReference();
    reference.setAttribution(new Attribution());
    reference.getAttribution().setContributor(new ResourceReference(URI.create("urn:contributorid")));
    reference.setDescriptionRef(URI.create("urn:srcDescInstance"));
    reference.addExtensionElement(new CustomEntity("alt1"));
    reference.addExtensionElement(new CustomEntity("alt2"));
    CustomEntity custom = new CustomEntity();
    custom.setSource(reference);
    custom = SerializationUtil.processThroughXml(custom);
    assertEquals("urn:contributorid", custom.getSource().getAttribution().toString());
    assertEquals("urn:srcDescInstance", custom.getSource().getDescriptionRef().toString());
    assertEquals("alt1", ((CustomEntity) custom.getSource().getExtensionElements().get(0)).getId());
    assertEquals("alt2", ((CustomEntity) custom.getSource().getExtensionElements().get(1)).getId());
    assertNull(custom.getSource().findExtensionOfType(String.class));
    assertEquals("alt1", custom.getSource().findExtensionOfType(CustomEntity.class).getId());
    assertEquals(0, custom.getSource().findExtensionsOfType(String.class).size());
    assertEquals(2, custom.getSource().findExtensionsOfType(CustomEntity.class).size());
    assertEquals("alt2", custom.getSource().findExtensionsOfType(CustomEntity.class).get(1).getId());

    reference.setDescriptionRef((URI) null);
    reference.setAttribution(null);
    reference.setExtensionElements(null);
    assertNull(reference.getDescriptionRef());
    assertNull(reference.getAttribution());
    assertNull(reference.findExtensionOfType(CustomEntity.class));
    assertEquals(0, reference.findExtensionsOfType(CustomEntity.class).size());
  }

  /**
   * tests source reference json
   */
  public void testSourceReferenceJson() throws Exception {
    SourceReference reference = new SourceReference();
    reference.setDescriptionRef(URI.create("urn:srcDescInstance"));
    reference.setExtensionElements(new ArrayList<Object>());
    reference.getExtensionElements().add(new CustomEntity("alt"));
    CustomEntity custom = new CustomEntity();
    custom.setSource(reference);
    custom = SerializationUtil.processThroughJson(custom);
    assertEquals("urn:srcDescInstance", custom.getSource().getDescriptionRef().toString());
    assertEquals("alt", ((CustomEntity) custom.getSource().getExtensionElements().get(0)).getId());
  }

}
