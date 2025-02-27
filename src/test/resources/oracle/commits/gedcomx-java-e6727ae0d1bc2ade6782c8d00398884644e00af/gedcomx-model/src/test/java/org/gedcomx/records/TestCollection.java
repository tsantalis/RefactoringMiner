package org.gedcomx.records;

import org.gedcomx.common.URI;
import org.gedcomx.conclusion.Identifier;
import org.gedcomx.types.IdentifierType;
import org.gedcomx.types.ResourceType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

/**
 * Class for testing the Collection class.
 * User: Randy Wilson
 * Date: 23 June 2015
 */
public class TestCollection {

  @Test
  public void testCollection() {
    Collection collection = new Collection();
    collection.setId("c1");
    collection.setIdentifiers(Collections.singletonList(new Identifier(new URI("https://api.familysearch.org/platform/records/collections/12345"), IdentifierType.Primary)));
    collection.setTitle("Pretend collection");
    collection.setLang("en");

    CollectionContent recordContents = new CollectionContent();
    recordContents.setCompleteness(1.0f);
    recordContents.setCount(55555);
    recordContents.setKnownResourceType(ResourceType.Record);
    CollectionContent imageContents = new CollectionContent();
    imageContents.setCompleteness(0.5f);
    imageContents.setCount(4444);
    imageContents.setKnownResourceType(ResourceType.DigitalArtifact);
    collection.setContent(Arrays.asList(recordContents, imageContents));

    assertEquals("c1", collection.getId());
    assertTrue(collection.getIdentifiers().get(0).getValue().toString().endsWith("12345"));
    assertEquals(2, collection.getContent().size());
    assertEquals(ResourceType.Record, collection.getContent().get(0).getKnownResourceType());
    assertEquals(4444, collection.getContent().get(1).getCount().intValue());
    assertEquals("Pretend collection", collection.getTitle());
    assertEquals("en", collection.getLang());
  }
}
