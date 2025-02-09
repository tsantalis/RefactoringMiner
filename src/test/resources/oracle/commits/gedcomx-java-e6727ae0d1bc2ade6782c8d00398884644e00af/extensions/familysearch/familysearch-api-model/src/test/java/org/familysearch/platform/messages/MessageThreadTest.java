package org.familysearch.platform.messages;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import org.gedcomx.agent.Agent;
import org.gedcomx.common.TextValue;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.Identifier;
import org.gedcomx.types.IdentifierType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the MessageThread class. Note that this also tests the AbstractMessageThread class.
 */
public class MessageThreadTest extends MessageTestBase {

  @Test
  public void testAuthor() {
    final TextValue threadAuthorName = new TextValue().lang(Locale.US.toString()).value("A. Thread Writer");
    final Identifier threadAuthorIdentifier = new Identifier(new URI("cis.user.THRD-ATHR"), IdentifierType.Primary);
    final Agent threadAuthor = new Agent()
        .name(threadAuthorName)
        .identifier(threadAuthorIdentifier);

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setAuthor(threadAuthor);

    Agent author = classUnderTest.getAuthor();
    assertEquals(author.getNames().size(), 1);
    testAgent(author, threadAuthor);

    // Test using builder pattern method
    classUnderTest = new MessageThread().author(threadAuthor);

    author = classUnderTest.getAuthor();
    assertEquals(author.getNames().size(), 1);
    testAgent(author, threadAuthor);
  }

  @Test
  public void testParticipants() {

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setParticipants(Collections.singletonList(MESSAGE_AUTHOR));

    assertEquals(classUnderTest.getParticipants().size(), 1);
    testAgent(classUnderTest.getParticipants().get(0), MESSAGE_AUTHOR);

    // Test using builder pattern method
    classUnderTest = new MessageThread().participants(Collections.singletonList(MESSAGE_AUTHOR));

    assertEquals(classUnderTest.getParticipants().size(), 1);
    testAgent(classUnderTest.getParticipants().get(0), MESSAGE_AUTHOR);
  }

  @Test
  public void testSubject() {
    final String subject = "This is the subject of the Message Thread";

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setSubject(subject);

    assertEquals(classUnderTest.getSubject(), subject);

    // Test using builder pattern method
    classUnderTest = new MessageThread().subject(subject);

    assertEquals(classUnderTest.getSubject(), subject);
  }

  @Test
  public void testAbout() {
    final String person = "A. Human Being";

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setAbout(person);

    assertEquals(classUnderTest.getAbout(), person);

    // Test using builder pattern method
    classUnderTest = new MessageThread().about(person);

    assertEquals(classUnderTest.getAbout(), person);
  }

  @Test
  public void testAboutUri() {
    final URI personUri = new URI("HUMN-BENG");

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setAboutUri(personUri);

    assertEquals(classUnderTest.getAboutUri(), personUri);

    // Test using builder pattern method
    classUnderTest = new MessageThread().aboutUri(personUri);

    assertEquals(classUnderTest.getAboutUri(), personUri);
  }

  @Test
  public void testLastModified() {
    final Date lastModifiedDateTime = new Date();

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setLastModified(lastModifiedDateTime);
    assertEquals(lastModifiedDateTime, classUnderTest.getLastModified());

    // Test using builder pattern method
    classUnderTest = new MessageThread().lastModified(lastModifiedDateTime);
    assertEquals(lastModifiedDateTime, classUnderTest.getLastModified());
  }

  @Test
  public void testMessages() {
    final String expectedMessageBody = "This is a test message in a User-to-User message thread";
    final Message userToUserMessage = new Message().author(MESSAGE_AUTHOR).body(expectedMessageBody);

    // Test using setter
    MessageThread classUnderTest = new MessageThread();
    classUnderTest.setMessages(Collections.singletonList(userToUserMessage));

    testMessages(classUnderTest, MESSAGE_AUTHOR, expectedMessageBody);

    // Test using builder pattern method
    classUnderTest = new MessageThread().messages(Collections.singletonList(userToUserMessage));

    testMessages(classUnderTest, MESSAGE_AUTHOR, expectedMessageBody);

    // Test using addMessage method
    classUnderTest = new MessageThread();
    classUnderTest.addMessage(userToUserMessage);

    testMessages(classUnderTest, MESSAGE_AUTHOR, expectedMessageBody);
  }

  private void testMessages(final MessageThread classUnderTest, final Agent expectedAuthor,
                            final String expectedMessageBody) {
    assertEquals(classUnderTest.getMessages().size(), 1);
    Message resultingMessage = classUnderTest.getMessages().get(0);
    assertEquals(resultingMessage.getBody(), expectedMessageBody);

    testAgent(resultingMessage.getAuthor(), expectedAuthor);
  }

}
