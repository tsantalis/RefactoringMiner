package org.gedcomx.atom.rt;

import org.gedcomx.Gedcomx;
import org.gedcomx.atom.Category;
import org.gedcomx.atom.Content;
import org.gedcomx.atom.Entry;
import org.gedcomx.atom.Feed;
import org.gedcomx.atom.Person;
import org.gedcomx.links.Link;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BaseAtomModelVisitorTest {
  @Test ( expected = NullPointerException.class )
  public void testNullVisitor() throws Exception {
    Feed feed = new Feed();
    feed.accept(null);
  }

  @Test
  public void testVisitFeed() throws Exception {
    AtomModelVisitorBase visitor = new AtomModelVisitorBase();
    assertNotNull(visitor.getContextStack());
    assertEquals(visitor.getContextStack().size(), 0);

    Feed feed = new Feed();

    // visit empty feed
    feed.accept(visitor);

    // re-visit feed; empty lists
    feed.setAuthors(new ArrayList<Person>());
    feed.setContributors(new ArrayList<Person>());
    feed.setEntries(new ArrayList<Entry>());
    feed.setExtensionElements(new ArrayList<Object>());
    feed.setLinks(new ArrayList<Link>());
    feed.accept(visitor);

    Entry entry;

    // re-visit feed; single element lists
    feed.getAuthors().add(new Person());
    feed.getContributors().add(new Person());
    entry = new Entry();
    feed.getEntries().add(entry);
    feed.getExtensionElements().add(new Object());
    feed.getLinks().add(new Link());
    feed.accept(visitor);

    Content content;
    ArrayList<Person> authors;
    ArrayList<Person> contributors;

    // re-visit feed; initialize entry
    content = new Content();
    authors = new ArrayList<Person>();
    contributors = new ArrayList<Person>();
    entry.setContent(content);
    entry.setAuthors(authors);
    entry.setCategories(new ArrayList<Category>());
    entry.setContributors(contributors);
    entry.setExtensionElements(new ArrayList<Object>());
    entry.setExtensionAttributes(new HashMap<QName, String>());
    entry.setLinks(new ArrayList<Link>());
    feed.accept(visitor);

    // re-visit feed; populate content; add element to authors and contributors
    content.setGedcomx(new Gedcomx());
    authors.add(new Person());
    contributors.add(new Person());
    feed.accept(visitor);
  }
}
