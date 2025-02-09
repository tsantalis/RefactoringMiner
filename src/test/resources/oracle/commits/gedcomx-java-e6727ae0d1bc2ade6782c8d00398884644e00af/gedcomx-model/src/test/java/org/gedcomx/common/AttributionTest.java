package org.gedcomx.common;

import org.gedcomx.rt.GedcomNamespaceManager;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.util.Date;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author Ryan Heaton
 */
public class AttributionTest {

  static {
    GedcomNamespaceManager.registerKnownJsonType(CustomEntity.class);
  }

  /**
   * tests attribution xml
   */
  @Test
  public void testAttributionXml() throws Exception {
    Date ts = new Date();

    Attribution attribution = new Attribution();
    attribution.setContributor(new ResourceReference());
    attribution.getContributor().setResource(URI.create("urn:someid"));
    attribution.setModified(ts);
    attribution.setChangeMessage("hello, there.");
    attribution.addExtensionElement(new CustomEntity("alt1"));
    attribution.addExtensionElement(new CustomEntity("alt2"));

    attribution = processThroughXml(attribution, Attribution.class, JAXBContext.newInstance(Attribution.class, CustomEntity.class));
    assertEquals("urn:someid", attribution.getContributor().getResource().toString());
    assertEquals(ts, attribution.getModified());
    assertEquals("hello, there.", attribution.getChangeMessage());
    assertEquals(((CustomEntity) attribution.getExtensionElements().get(0)).getId(), "alt1");
    assertEquals(((CustomEntity) attribution.getExtensionElements().get(1)).getId(), "alt2");
    assertNull(attribution.findExtensionOfType(String.class));
    assertEquals(attribution.findExtensionOfType(CustomEntity.class).getId(), "alt1");
    assertEquals(0, attribution.findExtensionsOfType(String.class).size());
    assertEquals(2, attribution.findExtensionsOfType(CustomEntity.class).size());
    assertEquals(attribution.findExtensionsOfType(CustomEntity.class).get(1).getId(), "alt2");

    attribution.setExtensionElements(null);
    assertNull(attribution.findExtensionOfType(CustomEntity.class));
    assertEquals(attribution.findExtensionsOfType(CustomEntity.class).size(), 0);
  }

  /**
   * tests attribution json
   */
  @Test
  public void testAttributionJson() throws Exception {
    Date ts = new Date();

    Attribution attribution = new Attribution();
    attribution.setContributor(new ResourceReference());
    attribution.getContributor().setResource(URI.create("urn:someid"));
    attribution.setModified(ts);
    attribution.setChangeMessage("hello, there.");
    attribution.addExtensionElement(new CustomEntity("alt1"));
    attribution.addExtensionElement(new CustomEntity("alt2"));

    attribution = processThroughJson(attribution);
    assertEquals("urn:someid", attribution.getContributor().getResource().toString());
    assertEquals(ts, attribution.getModified());
    assertEquals("hello, there.", attribution.getChangeMessage());
    assertEquals(((CustomEntity) attribution.getExtensionElements().get(0)).getId(), "alt1");
    assertEquals(((CustomEntity) attribution.getExtensionElements().get(1)).getId(), "alt2");
    assertNull(attribution.findExtensionOfType(String.class));
    assertEquals(attribution.findExtensionOfType(CustomEntity.class).getId(), "alt1");
    assertEquals(0, attribution.findExtensionsOfType(String.class).size());
    assertEquals(2, attribution.findExtensionsOfType(CustomEntity.class).size());
    assertEquals(attribution.findExtensionsOfType(CustomEntity.class).get(1).getId(), "alt2");

    assertEquals("urn:someid", attribution.toString());
    attribution.setContributor(null);
    assertEquals("", attribution.toString());
  }

}
