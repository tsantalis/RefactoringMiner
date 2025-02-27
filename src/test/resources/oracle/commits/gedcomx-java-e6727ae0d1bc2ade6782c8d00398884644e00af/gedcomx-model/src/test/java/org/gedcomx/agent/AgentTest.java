package org.gedcomx.agent;

import org.gedcomx.common.CustomEntity;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.TextValue;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.Identifier;
import org.gedcomx.rt.json.GedcomJacksonModule;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import java.util.ArrayList;
import java.util.Arrays;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class AgentTest {
  @Test
  public void testExtensionsThruXML() throws Exception {
    Agent agent = new Agent();
    agent.setId("1234");
    agent.addExtensionElement(new CustomEntity("4321"));
    agent.addExtensionElement(new CustomEntity("5432"));
    agent.getExtensionElements().add(new CustomEntity("6543"));
    agent = processThroughXml(agent, Agent.class, JAXBContext.newInstance(Agent.class, CustomEntity.class));
    assertEquals(agent.getExtensionElements().size(), 3);
    assertNull(agent.findExtensionOfType(String.class));
    assertEquals(agent.findExtensionOfType(CustomEntity.class).getId(), "4321");
    assertEquals(agent.findExtensionsOfType(String.class).size(), 0);
    assertEquals(agent.findExtensionsOfType(CustomEntity.class).size(), 3);
    assertEquals(agent.findExtensionsOfType(CustomEntity.class).get(1).getId(), "5432");
    assertEquals(((CustomEntity) agent.getExtensionElements().get(2)).getId(), "6543");

    agent.setExtensionElements(null);
    assertNull(agent.getExtensionElements());
    assertNull(agent.findExtensionOfType(CustomEntity.class));
    assertEquals(agent.findExtensionsOfType(CustomEntity.class).size(), 0);
  }

  @Test
  public void testExtensionsThruJSON() throws Exception {
    Agent agent = new Agent();
    agent.setId("1234");
    agent.addExtensionElement(new CustomEntity("4321"));
    agent.addExtensionElement(new CustomEntity("5432"));
    agent.getExtensionElements().add(new CustomEntity("6543"));
    agent = processThroughJson(agent, Agent.class, GedcomJacksonModule.createObjectMapper(Agent.class, CustomEntity.class));
    assertEquals(agent.getExtensionElements().size(), 3);
    assertEquals(agent.findExtensionOfType(CustomEntity.class).getId(), "4321");
    assertEquals(agent.findExtensionsOfType(String.class).size(), 0);
    assertEquals(agent.findExtensionsOfType(CustomEntity.class).size(), 3);
    assertEquals(agent.findExtensionsOfType(CustomEntity.class).get(1).getId(), "5432");
    assertEquals(((CustomEntity) agent.getExtensionElements().get(2)).getId(), "6543");

    agent.setExtensionElements(null);
    assertNull(agent.getExtensionElements());
    assertNull(agent.findExtensionOfType(CustomEntity.class));
    assertEquals(agent.findExtensionsOfType(CustomEntity.class).size(), 0);
  }

  @Test
  public void testPersonXml() throws Exception {
    Agent person = createPerson();
    person = processThroughXml(person);
    assertPerson(person);
  }

  /**
   * tests id json
   */
  @Test
  public void testPersonJson() throws Exception {
    Agent person = createPerson();
    person = processThroughJson(person);
    assertPerson(person);
  }

  private Agent createPerson() {
    Agent person = new Agent();
    person.setAccounts(new ArrayList<OnlineAccount>());
    OnlineAccount account = new OnlineAccount();
    account.setAccountName("account name");
    account.setServiceHomepage(new ResourceReference());
    account.getServiceHomepage().setResource(URI.create("http://familysearch.org"));
    person.getAccounts().add(account);
    person.setAddresses(new ArrayList<Address>());
    Address address = new Address();
    address.setValue("street1\nstreet2\nstreet3\ncity UT, 88888\ncountry");
    address.setCity("city");
    address.setCountry("country");
    address.setPostalCode("88888");
    address.setStateOrProvince("UT");
    address.setStreet("street1");
    address.setStreet2("street2");
    address.setStreet3("street3");
    person.getAddresses().add(address);
    person.setEmails(new ArrayList<ResourceReference>());
    ResourceReference email = new ResourceReference();
    email.setResource(URI.create("mailto:heatonra@familysearch.org"));
    person.getEmails().add(email);
    person.setHomepage(new ResourceReference(URI.create("http://familysearch.org/heatonra")));
    person.setNames(Arrays.asList(new TextValue("Ryan Heaton")));
    person.setOpenid(new ResourceReference(URI.create("openid")));
    person.setPhones(new ArrayList<ResourceReference>());
    Identifier identifier = new Identifier();
    identifier.setValue(URI.create("identifier"));
    person.setIdentifiers(Arrays.asList(identifier));
    ResourceReference phone = new ResourceReference();
    phone.setResource(URI.create("tel:+18012401000"));
    person.getPhones().add(phone);
    return person;
  }

  private void assertPerson(Agent person) {
    assertEquals(1, person.getAccounts().size());
    assertEquals("account name", person.getAccounts().get(0).getAccountName());
    assertEquals("http://familysearch.org", person.getAccounts().get(0).getServiceHomepage().getResource().toString());
    assertEquals(1, person.getAddresses().size());
    assertEquals("street1\nstreet2\nstreet3\ncity UT, 88888\ncountry", person.getAddresses().get(0).getValue());
    assertEquals("city", person.getAddresses().get(0).getCity());
    assertEquals("country", person.getAddresses().get(0).getCountry());
    assertEquals("88888", person.getAddresses().get(0).getPostalCode());
    assertEquals("UT", person.getAddresses().get(0).getStateOrProvince());
    assertEquals("street1", person.getAddresses().get(0).getStreet());
    assertEquals("street2", person.getAddresses().get(0).getStreet2());
    assertEquals("street3", person.getAddresses().get(0).getStreet3());
    assertEquals(1, person.getEmails().size());
    assertEquals("mailto:heatonra@familysearch.org", person.getEmails().get(0).getResource().toString());
    assertEquals("http://familysearch.org/heatonra", person.getHomepage().getResource().toString());
    assertEquals("Ryan Heaton", person.getName().getValue());
    assertEquals("openid", person.getOpenid().getResource().toString());
    assertEquals(1, person.getPhones().size());
    assertEquals("tel:+18012401000", person.getPhones().get(0).getResource().toString());
    assertEquals("identifier", person.getIdentifiers().get(0).getValue().toString());
  }
}
