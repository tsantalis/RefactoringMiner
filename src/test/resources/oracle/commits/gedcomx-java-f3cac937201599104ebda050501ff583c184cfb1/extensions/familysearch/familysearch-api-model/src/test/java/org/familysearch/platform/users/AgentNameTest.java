package org.familysearch.platform.users;

import org.gedcomx.common.URI;
import org.testng.annotations.Test;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;


public class AgentNameTest {
  @Test
  public void testAgentName() throws Exception {
    AgentName agentName = new AgentName();

    assertNull(agentName.getType());
    assertNull(agentName.getLang());
    assertNull(agentName.getValue());

    agentName.setType(URI.create("urn:type"));
    agentName.setLang("zh-Hant");
    agentName.setValue("junk");

    assertEquals("urn:type", agentName.getType().toString());
    assertEquals("zh-Hant", agentName.getLang());
    assertEquals("junk", agentName.getValue());

    agentName = new AgentName(URI.create("urn:type2"), "junk2", "es");

    assertEquals("urn:type2", agentName.getType().toString());
    assertEquals("es", agentName.getLang());
    assertEquals("junk2", agentName.getValue());

    agentName = new AgentName(URI.create("urn:type3"), "junk3");

    assertEquals("urn:type3", agentName.getType().toString());
    assertNull(agentName.getLang());
    assertEquals("junk3", agentName.getValue());
  }
}
