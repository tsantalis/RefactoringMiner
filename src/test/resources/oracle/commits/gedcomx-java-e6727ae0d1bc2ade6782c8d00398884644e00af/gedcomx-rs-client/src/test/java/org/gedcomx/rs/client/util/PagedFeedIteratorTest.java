/**
 * Copyright Intellectual Reserve, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gedcomx.rs.client.util;

import org.gedcomx.atom.Feed;
import org.gedcomx.common.URI;
import org.junit.Test;

import static org.junit.Assert.*;

public class PagedFeedIteratorTest {
  private static final String baseUri = "http://www.example.com/feed";

  @Test
  public void testFromUriConstructor() {
    URI expected = URI.create(baseUri);
    PagedFeedIterator pagedFeedIterator = PagedFeedIterator.fromUri(expected);
    URI actual = pagedFeedIterator.nextHRef();
    assertEquals(actual, expected);
    assertNull(pagedFeedIterator.firstHRef());
    assertNull(pagedFeedIterator.lastHRef());
    assertNull(pagedFeedIterator.previousHRef());
    assertTrue(pagedFeedIterator.hasNext());
    assertFalse(pagedFeedIterator.hasFirst());
    assertFalse(pagedFeedIterator.hasLast());
    assertFalse(pagedFeedIterator.hasPrevious());
  }

  @Test
  public void testFromFeedConstructor() {
    URI expected = URI.create(baseUri + "/next");
    Feed feed = new Feed();
    feed.addLink("next", expected);
    PagedFeedIterator pagedFeedIterator = PagedFeedIterator.fromFeed(feed);
    URI actual = pagedFeedIterator.nextHRef();
    assertEquals(actual, expected);
    assertNull(pagedFeedIterator.firstHRef());
    assertNull(pagedFeedIterator.lastHRef());
    assertNull(pagedFeedIterator.previousHRef());
    assertTrue(pagedFeedIterator.hasNext());
    assertFalse(pagedFeedIterator.hasFirst());
    assertFalse(pagedFeedIterator.hasLast());
    assertFalse(pagedFeedIterator.hasPrevious());
  }

  @Test
  public void testFromFeedConstructor2() {
    URI[] expected = new URI[]{
        URI.create(baseUri + "/first"),
        URI.create(baseUri + "/last"),
        URI.create(baseUri + "/previous"),
        URI.create(baseUri + "/next")
    };
    Feed feed = new Feed();
    for (URI uri : expected) {
      String rel = uri.toString().substring(baseUri.length() + 1);
      //System.out.println(rel);
      feed.addLink(rel, uri);
    }
    PagedFeedIterator pagedFeedIterator = PagedFeedIterator.fromFeed(feed);
    URI[] actual = new URI[]{
        pagedFeedIterator.firstHRef(),
        pagedFeedIterator.lastHRef(),
        pagedFeedIterator.previousHRef(),
        pagedFeedIterator.nextHRef(),
    };
    assertEquals(actual, expected);
    assertTrue(pagedFeedIterator.hasNext());
    assertTrue(pagedFeedIterator.hasFirst());
    assertTrue(pagedFeedIterator.hasLast());
    assertTrue(pagedFeedIterator.hasPrevious());
  }
}
