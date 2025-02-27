package org.familysearch.platform.messages;

import java.util.Locale;

import org.gedcomx.agent.Agent;
import org.gedcomx.common.TextValue;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.Identifier;
import org.gedcomx.types.IdentifierType;

import static org.testng.Assert.assertEquals;

/**
 * Methods and attributes shared between multiple User-to-User Messaging tests.
 */
abstract class MessageTestBase {
  private static final TextValue MESSAGE_AUTHOR_NAME = new TextValue().lang(Locale.US.toString()).value("A. Message Writer");
  private static final Identifier MESSAGE_AUTHOR_IDENTIFIER = new Identifier(new URI("cis.user.MSGE-ATHR"), IdentifierType.Primary);

  static final Agent MESSAGE_AUTHOR = new Agent()
      .name(MESSAGE_AUTHOR_NAME)
      .identifier(MESSAGE_AUTHOR_IDENTIFIER);

  void testAgent(final Agent agentToTest, final Agent expectedAgent) {
    assertEquals(agentToTest.getNames().size(), 1);
    final TextValue agentNameToTest = agentToTest.getName();
    final TextValue expectedAgentName = expectedAgent.getName();
    assertEquals(agentNameToTest.getLang(), expectedAgentName.getLang());
    assertEquals(agentNameToTest.getValue(), expectedAgentName.getValue());

    assertEquals(agentToTest.getIdentifiers().size(), 1);
    final Identifier agentIdentifierToTest = agentToTest.getIdentifiers().get(0);
    final Identifier expectedAgentIdentifier = expectedAgent.getIdentifiers().get(0);
    assertEquals(agentIdentifierToTest.getValue(), expectedAgentIdentifier.getValue());
    assertEquals(agentIdentifierToTest.getKnownType(), expectedAgentIdentifier.getKnownType());
  }

}
