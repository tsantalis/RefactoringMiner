package org.familysearch.platform.messages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserMessageThreadSummaryTest {

  @Test
  public void testUserId() {
    final String userId = "cis.user.UUUU-PPPP";

    // Test using setter
    UserMessageThreadSummary classUnderTest = new UserMessageThreadSummary();
    classUnderTest.setUserId(userId);

    assertEquals(classUnderTest.getUserId(), userId);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadSummary().userId(userId);

    assertEquals(classUnderTest.getUserId(), userId);
  }

  @Test
  public void testMessageCount() {
    final int messageCount = 13579;

    // Test using setter
    UserMessageThreadSummary classUnderTest = new UserMessageThreadSummary();
    classUnderTest.setMessageCount(messageCount);

    assertEquals(classUnderTest.getMessageCount(), messageCount);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadSummary().messageCount(messageCount);

    assertEquals(classUnderTest.getMessageCount(), messageCount);
  }

  @Test
  public void testUnreadMessageCount() {
    final int unreadMessageCount = 2;

    // Test using setter
    UserMessageThreadSummary classUnderTest = new UserMessageThreadSummary();
    classUnderTest.setUnreadMessageCount(unreadMessageCount);

    assertEquals(classUnderTest.getUnreadMessageCount(), unreadMessageCount);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadSummary().unreadMessageCount(unreadMessageCount);

    assertEquals(classUnderTest.getUnreadMessageCount(), unreadMessageCount);
  }

}
