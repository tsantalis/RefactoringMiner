package org.gedcomx.links;

import org.gedcomx.common.URI;
import org.junit.Test;

import java.util.ArrayList;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Heaton
 */
public class HypermediaConrollableDataTest {

  /**
   * tests link xml
   */
  @Test
  public void testDataXml() throws Exception {
    CustomData data = createData();
    data = processThroughXml(data);
    assertLink(data);
  }

  /**
   * tests link json
   */
  @Test
  public void testDataJson() throws Exception {
    CustomData data = createData();
    data = processThroughJson(data);
    assertLink(data);
  }

  private void assertLink(CustomData data) {
    assertEquals(3, data.getLinks().size());
    assertEquals("href1", data.getLink("item").getHref().toString());
    assertEquals("template1", data.getLinks("item").get(1).getTemplate());
    assertEquals("href3", data.getLink("rel2").getHref().toString());
  }

  private CustomData createData() {
    CustomData data = new CustomData();
    data.setLinks(new ArrayList<Link>());
    data.addLink("item", URI.create("href1"));
    data.addTemplatedLink("item", "template1");
    data.getLinks().add(new Link("rel2", URI.create("href3")));
    return data;
  }

}
