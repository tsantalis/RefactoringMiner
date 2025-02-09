package org.familysearch.platform.messages;

import java.util.Date;

import org.gedcomx.agent.Agent;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MessageTest extends MessageTestBase {

  @Test
  public void testAuthor() {
    // Test using setter
    Message classUnderTest = new Message();
    classUnderTest.setAuthor(MESSAGE_AUTHOR);

    Agent author = classUnderTest.getAuthor();
    assertEquals(author.getNames().size(), 1);
    testAgent(author, MESSAGE_AUTHOR);

    // Test using builder pattern method
    classUnderTest = new Message().author(MESSAGE_AUTHOR);

    author = classUnderTest.getAuthor();
    assertEquals(author.getNames().size(), 1);
    testAgent(author, MESSAGE_AUTHOR);
  }

  @Test
  public void testBody() {
    final String body = "This is the body of the message.";

    // Test using setter
    Message classUnderTest = new Message();
    classUnderTest.setBody(body);

    assertEquals(classUnderTest.getBody(), body);

    // Test using builder pattern method
    classUnderTest = new Message().body(body);

    assertEquals(classUnderTest.getBody(), body);
  }

  @Test
  public void testCreated() {
    final Date createdDateTime = new Date();

    // Test using setter
    Message classUnderTest = new Message();
    classUnderTest.setCreated(createdDateTime);
    assertEquals(createdDateTime, classUnderTest.getCreated());

    // Test using builder pattern method
    classUnderTest = new Message().created(createdDateTime);
    assertEquals(createdDateTime, classUnderTest.getCreated());
  }

}
