/**
 * Copyright 2011 Intellectual Reserve, Inc. All Rights reserved.
 */
package org.familysearch.platform.discussions;

import org.gedcomx.common.ResourceReference;
import org.testng.annotations.Test;


import static junit.framework.Assert.assertEquals;

/**
 */
public class DiscussionsModelTest {
  @Test
  public void testModel() {
    Comment comment = new Comment();
    String s = "test string";
    comment.setId(s);
    assertEquals(s, comment.getId());
    comment.setText(s);
    assertEquals(s, comment.getText());
    ResourceReference contributor = new ResourceReference();
    comment.setContributor(contributor);
    assertEquals(contributor, comment.getContributor());
    java.util.Date now = new java.util.Date();
    comment.setCreated(now);
    assertEquals(now, comment.getCreated());

    Discussion discussion = new Discussion();
    discussion.setDetails(s);
    assertEquals(s, discussion.getDetails());
    discussion.setTitle(s);
    assertEquals(s, discussion.getTitle());
    discussion.setId(s);
    assertEquals(s, discussion.getId());
    discussion.setNumberOfComments(1);
    assertEquals(1, discussion.getNumberOfComments().intValue());
    discussion.setContributor(contributor);
    assertEquals(contributor, discussion.getContributor());
    discussion.setCreated(now);
    assertEquals(now, discussion.getCreated());
    discussion.setModified(now);
    assertEquals(now, discussion.getModified());
  }

}
