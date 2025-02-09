package org.gedcomx.atom;

import org.gedcomx.Gedcomx;
import org.gedcomx.common.URI;
import org.gedcomx.links.Link;
import org.gedcomx.rt.GedcomNamespaceManager;
import org.gedcomx.search.ResultConfidence;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;


/**
 * @author Ryan Heaton
 */
@Test
public class FeedTest {

  public void testFeedXml() throws Exception {
    Feed feed = createFeed();
    feed = processThroughXml(feed, Feed.class, JAXBContext.newInstance(Feed.class, CustomEntity.class, JunkEntity.class));
    assertFeed(feed);
  }

  public void testFeedJson() throws Exception {
    Feed feed = createFeed();
    GedcomNamespaceManager.registerKnownJsonType(CustomEntity.class);
    GedcomNamespaceManager.registerKnownJsonType(JunkEntity.class);
    feed = processThroughJson(feed);
    assertFeed(feed);
  }

  private Feed createFeed() {
    Entry entry = new Entry();
    entry.setAuthors(new ArrayList<Person>());
    entry.getAuthors().add(new Person());
    entry.getAuthors().get(0).setEmail("author2@author.com");
    entry.setCategories(new ArrayList<Category>());
    entry.getCategories().add(new Category());
    entry.getCategories().get(0).setLabel("label");
    entry.getCategories().get(0).setScheme(URI.create("urn:scheme"));
    entry.getCategories().get(0).setTerm("term");
    entry.setContent(new Content());
    entry.getContent().setType("application/x-gedcomx-v1+xml");
    entry.getContent().setGedcomx(new Gedcomx());
    entry.getContent().getGedcomx().setId("gxid");
    entry.setContributors(new ArrayList<Person>());
    entry.getContributors().add(new Person());
    entry.getContributors().get(0).setEmail("contributor2@contributor.com");
    entry.setId(URI.create("urn:id"));
    entry.setPublished(new Date(1234567L));
    entry.setRights("none");
    entry.setScore(0.6F);
    entry.setConfidence(ResultConfidence.four);
    entry.setTitle("entry title");
    entry.setUpdated(new Date(1234568L));

    assertNull(entry.getTransientProperty("junk1"));
    entry.setTransientProperty("junk1", "junkValue1");
    entry.setTransientProperty("junk2", "junkValue2");
    assertEquals("junkValue1", entry.getTransientProperty("junk1"));
    assertEquals("junkValue2", entry.getTransientProperty("junk2"));

    assertNull(entry.findExtensionOfType(String.class));
    assertNotNull(entry.findExtensionsOfType(String.class));
    assertEquals(0, entry.findExtensionsOfType(String.class).size());
    entry.setExtensionElements(new ArrayList<Object>());
    assertNull(entry.findExtensionOfType(String.class));
    assertNotNull(entry.findExtensionsOfType(String.class));
    assertEquals(0, entry.findExtensionsOfType(String.class).size());
    entry.setExtensionElements(null);

    entry.addExtensionElement(new CustomEntity());
    entry.findExtensionOfType(CustomEntity.class).setId("entityid");
    entry.findExtensionOfType(CustomEntity.class).addExtensionElement(new CustomEntity());
    entry.findExtensionOfType(CustomEntity.class).findExtensionOfType(CustomEntity.class).setId("subentityid");
    entry.addExtensionElement(new JunkEntity());
    entry.findExtensionOfType(JunkEntity.class).setId("entityId2");
    entry.setExtensionAttributes(new HashMap<QName, String>());

    assertNull(entry.getLinks());
    assertNotNull(entry.getLinks("self"));
    assertEquals(0, entry.getLinks("self").size());
    assertNull(entry.getLink("self"));
    entry.setLinks(new ArrayList<Link>());
    assertNull(entry.getLink("self"));
    entry.setLinks(null);

    Link link = new Link();
    link.setHref(URI.create("urn:href"));
    link.setTitle("link title");
    link.setHreflang("en");
    link.setRel("self");
    link.setType("text/plain");

    entry.addLink(link);
    entry.addLink("other", URI.create("urn:other"));
    entry.addTemplatedLink("template", "junkTemplate");

    Feed feed = new Feed();
    feed.setAuthors(new ArrayList<Person>());
    feed.getAuthors().add(new Person());
    feed.getAuthors().get(0).setEmail("author@author.com");
    feed.getAuthors().get(0).setName("Author");
    feed.getAuthors().get(0).setUri(URI.create("urn:author"));
    feed.getAuthors().get(0).setBase(URI.create("urn:base"));
    feed.getAuthors().get(0).setLang("en");
    feed.setContributors(new ArrayList<Person>());
    feed.getContributors().add(new Person());
    feed.getContributors().get(0).setEmail("contributor@contributor.com");
    feed.getContributors().get(0).setName("Contributor");
    feed.getContributors().get(0).setUri(URI.create("urn:contributor"));
    feed.setEntries(new ArrayList<Entry>());
    feed.getEntries().add(entry);
    feed.setGenerator(new Generator());
    feed.getGenerator().setBase(URI.create("urn:base"));
    feed.getGenerator().setLang("de");
    feed.getGenerator().setUri(URI.create("urn:generator"));
    feed.getGenerator().setValue("generator value");
    feed.getGenerator().setVersion("1.2");
    feed.setIcon(URI.create("urn:icon"));
    feed.setId(URI.create("urn:feedid"));
    feed.setIndex(123);
    feed.setLogo(URI.create("urn:logo"));
    feed.setRights("feed rights");
    feed.setSubtitle("subtitle");
    feed.setTitle("feed title");
    feed.setResults(7);
    feed.setUpdated(new Date(54321L));

    assertNull(feed.getLinks());
    assertNotNull(feed.getLinks("self"));
    assertEquals(0, feed.getLinks("self").size());
    assertNull(feed.getLink("self"));
    feed.setLinks(new ArrayList<Link>());
    assertNull(feed.getLink("self"));
    feed.setLinks(null);

    feed.addLink("self", URI.create("urn:feed"));
    feed.addLink("other", URI.create("urn:other"));
    feed.addTemplatedLink("template", "junkTemplate");

    return feed;
  }

  private void assertFeed(Feed feed) {
    assertEquals(1, feed.getAuthors().size());
    Person author = feed.getAuthors().get(0);
    assertEquals("author@author.com", author.getEmail());
    assertEquals("Author", author.getName());
    assertEquals(URI.create("urn:author"), author.getUri());
    assertEquals(URI.create("urn:base"), author.getBase());
    assertEquals("en", author.getLang());

    assertEquals(1, feed.getContributors().size());
    Person contributor = feed.getContributors().get(0);
    assertEquals("contributor@contributor.com", contributor.getEmail());
    assertEquals("Contributor", contributor.getName());
    assertEquals(URI.create("urn:contributor"), contributor.getUri());

    assertEquals(1, feed.getEntries().size());
    Entry entry = feed.getEntries().get(0);
    assertEquals(1, entry.getAuthors().size());
    Person author2 = entry.getAuthors().get(0);
    assertEquals("author2@author.com", author2.getEmail());
    assertEquals(1, entry.getCategories().size());
    Category category = entry.getCategories().get(0);
    assertEquals("label", category.getLabel());
    assertEquals(URI.create("urn:scheme"), category.getScheme());
    assertEquals("term", category.getTerm());

    assertEquals("application/x-gedcomx-v1+xml", entry.getContent().getType());
    assertNotNull(entry.getContent().getGedcomx());
    assertEquals(entry.getContent().getGedcomx().getId(), "gxid");

    assertEquals(1, entry.getContributors().size());
    assertEquals("contributor2@contributor.com", entry.getContributors().get(0).getEmail());
    assertEquals(URI.create("urn:id"), entry.getId());

    assertNotNull(entry.getLinks());
    assertEquals(3, entry.getLinks().size());
    assertNotNull(entry.getLinks("self"));
    assertEquals(1, entry.getLinks("self").size());
    Link link = entry.getLink("self");
    assertEquals(URI.create("urn:href"), link.getHref());
    assertEquals("link title", link.getTitle());
    assertEquals("en", link.getHreflang());
    assertEquals("self", link.getRel());
    assertEquals("text/plain", link.getType());
    assertNotNull(entry.getLinks("other"));
    assertEquals(1, entry.getLinks("other").size());
    assertEquals("other", entry.getLinks("other").get(0).getRel());
    assertEquals(URI.create("urn:other"), entry.getLink("other").getHref());
    assertNotNull(entry.getLinks("template"));
    assertEquals(1, entry.getLinks("template").size());
    assertEquals("template", entry.getLinks("template").get(0).getRel());
    assertEquals("junkTemplate", entry.getLink("template").getTemplate());
    assertNull(entry.getLink("template").getHref());

    assertEquals(new Date(1234567L), entry.getPublished());
    assertEquals("none", entry.getRights());
    assertEquals(0.6F, entry.getScore());
    assertEquals(ResultConfidence.four, entry.getConfidence());
    assertEquals("entry title", entry.getTitle());
    assertEquals(new Date(1234568L), entry.getUpdated());

    assertNull(entry.getTransientProperty("junk1"));
    assertNull(entry.getTransientProperty("junk2"));

    assertNotNull(entry.getExtensionElements());
    assertEquals(2, entry.getExtensionElements().size());
    assertNotNull(entry.findExtensionsOfType(CustomEntity.class));
    assertEquals(1, entry.findExtensionsOfType(CustomEntity.class).size());
    CustomEntity customEntity = entry.findExtensionOfType(CustomEntity.class);
    assertEquals("entityid", customEntity.getId());
    assertEquals(1, customEntity.getExtensionElements().size());
    assertNotNull(customEntity.findExtensionOfType(CustomEntity.class));
    assertEquals("subentityid", customEntity.findExtensionOfType(CustomEntity.class).getId());
    assertNotNull(entry.findExtensionsOfType(JunkEntity.class));
    assertEquals(1, entry.findExtensionsOfType(JunkEntity.class).size());
    assertEquals("entityId2", entry.findExtensionOfType(JunkEntity.class).getId());

    assertEquals(URI.create("urn:base"), feed.getGenerator().getBase());
    assertEquals("de", feed.getGenerator().getLang());
    assertEquals(URI.create("urn:generator"), feed.getGenerator().getUri());
    assertEquals("generator value", feed.getGenerator().getValue());
    assertEquals("1.2", feed.getGenerator().getVersion());

    assertEquals(URI.create("urn:icon"), feed.getIcon());
    assertEquals(URI.create("urn:feedid"), feed.getId());
    assertEquals(123, feed.getIndex().intValue());
    assertNotNull(feed.getLinks());
    assertEquals(3, feed.getLinks().size());
    assertNotNull(feed.getLinks("self"));
    assertEquals(1, feed.getLinks("self").size());
    assertEquals("self", feed.getLinks("self").get(0).getRel());
    assertEquals(URI.create("urn:feed"), feed.getLink("self").getHref());
    assertNotNull(feed.getLinks("other"));
    assertEquals(1, feed.getLinks("other").size());
    assertEquals("other", feed.getLinks("other").get(0).getRel());
    assertEquals(URI.create("urn:other"), feed.getLink("other").getHref());
    assertNotNull(feed.getLinks("template"));
    assertEquals(1, feed.getLinks("template").size());
    assertEquals("template", feed.getLinks("template").get(0).getRel());
    assertEquals("junkTemplate", feed.getLink("template").getTemplate());
    assertNull(feed.getLink("template").getHref());
    assertEquals(URI.create("urn:logo"), feed.getLogo());

    assertEquals("feed rights", feed.getRights());
    assertEquals("subtitle", feed.getSubtitle());
    assertEquals("feed title", feed.getTitle());
    assertEquals(7, feed.getResults().intValue());
    assertEquals(new Date(54321L), feed.getUpdated());
  }

}
