package org.familysearch.platform.messages;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UserMessageThreadsSummaryTest {
  private static final String USER_ID = "cis.user.UUUU-PPPP";
  private static final int MESSAGE_COUNT = 27;
  private static final int UNREAD_MESSAGE_COUNT = 2;
  private static final int THREAD_COUNT = 16;
  private static final int UNREAD_THREAD_COUNT = 3;


  @Test
  public void testUserId() {
    // Test using setter
    UserMessageThreadsSummary classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.setUserId(USER_ID);

    assertEquals(classUnderTest.getUserId(), USER_ID);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadsSummary().userId(USER_ID);

    assertEquals(classUnderTest.getUserId(), USER_ID);
  }

  @Test
  public void testMessageCount() {
    // Test using setter
    UserMessageThreadsSummary classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.setMessageCount(MESSAGE_COUNT);

    assertEquals(classUnderTest.getMessageCount(), MESSAGE_COUNT);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadsSummary().messageCount(MESSAGE_COUNT);

    assertEquals(classUnderTest.getMessageCount(), MESSAGE_COUNT);
  }

  @Test
  public void testUnreadMessageCount() {
    // Test using setter
    UserMessageThreadsSummary classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.setUnreadMessageCount(UNREAD_MESSAGE_COUNT);

    assertEquals(classUnderTest.getUnreadMessageCount(), UNREAD_MESSAGE_COUNT);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadsSummary().unreadMessageCount(UNREAD_MESSAGE_COUNT);

    assertEquals(classUnderTest.getUnreadMessageCount(), UNREAD_MESSAGE_COUNT);
  }

  @Test
  public void testThreadCount() {
    // Test using setter
    UserMessageThreadsSummary classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.setThreadCount(THREAD_COUNT);

    assertEquals(classUnderTest.getThreadCount(), THREAD_COUNT);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadsSummary().threadCount(THREAD_COUNT);

    assertEquals(classUnderTest.getThreadCount(), THREAD_COUNT);
  }

  @Test
  public void testUnreadThreadCount() {
    // Test using setter
    UserMessageThreadsSummary classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.setUnreadThreadCount(UNREAD_THREAD_COUNT);

    assertEquals(classUnderTest.getUnreadThreadCount(), UNREAD_THREAD_COUNT);

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadsSummary().unreadThreadCount(UNREAD_THREAD_COUNT);

    assertEquals(classUnderTest.getUnreadThreadCount(), UNREAD_THREAD_COUNT);
  }

  @Test
  public void testUserMessageThreadSummaries() {
    final UserMessageThreadSummary userMessageThreadSummary = new UserMessageThreadSummary()
        .userId(USER_ID)
        .messageCount(4)
        .unreadMessageCount(1);

    // Test using setter
    UserMessageThreadsSummary classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.setUserMessageThreadSummaries(Collections.singletonList(userMessageThreadSummary));

    List<UserMessageThreadSummary> userMessageThreadSummaries = classUnderTest.getUserMessageThreadSummaries();
    assertEquals(userMessageThreadSummaries.size(), 1);
    assertUserMessageThreadSummary(userMessageThreadSummaries.get(0));

    // Test using add method
    classUnderTest = new UserMessageThreadsSummary();
    classUnderTest.addUserMessageThreadSummary(userMessageThreadSummary);

    userMessageThreadSummaries = classUnderTest.getUserMessageThreadSummaries();
    assertEquals(userMessageThreadSummaries.size(), 1);
    assertUserMessageThreadSummary(userMessageThreadSummaries.get(0));

    // Test using builder pattern method
    classUnderTest = new UserMessageThreadsSummary()
        .userMessageThreadSummary(Collections.singletonList(userMessageThreadSummary));

    userMessageThreadSummaries = classUnderTest.getUserMessageThreadSummaries();
    assertEquals(userMessageThreadSummaries.size(), 1);
    assertUserMessageThreadSummary(userMessageThreadSummaries.get(0));
  }

  private void assertUserMessageThreadSummary(final UserMessageThreadSummary userMessageThreadSummary) {
    assertEquals(userMessageThreadSummary.getUserId(), USER_ID);
    assertEquals(userMessageThreadSummary.getMessageCount(), 4);
    assertEquals(userMessageThreadSummary.getUnreadMessageCount(), 1);
  }
}
