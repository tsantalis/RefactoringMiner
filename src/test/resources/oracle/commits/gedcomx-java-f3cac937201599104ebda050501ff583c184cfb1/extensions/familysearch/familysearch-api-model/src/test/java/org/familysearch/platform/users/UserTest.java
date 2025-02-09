package org.familysearch.platform.users;

import org.testng.annotations.Test;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;


public class UserTest {
  @Test
  public void testUser() throws Exception {
    User user = new User();

    assertNull(user.getId());
    assertNull(user.getContactName());
    assertNull(user.getHelperAccessPin());
    assertNull(user.getFullName());
    assertNull(user.getGivenName());
    assertNull(user.getFamilyName());
    assertNull(user.getEmail());
    assertNull(user.getAlternateEmail());
    assertNull(user.getCountry());
    assertNull(user.getGender());
    assertNull(user.getBirthDate());
    assertNull(user.getPhoneNumber());
    assertNull(user.getMobilePhoneNumber());
    assertNull(user.getMailingAddress());
    assertNull(user.getPreferredLanguage());
    assertNull(user.getDisplayName());
    assertNull(user.getPersonId());
    assertNull(user.getTreeUserId());

    user.setId("123");
    user.setContactName("username");
    user.setHelperAccessPin("helper-access-pin");
    user.setFullName("given middle surname");
    user.setGivenName("given");
    user.setFamilyName("surname");
    user.setEmail("no@spam.com");
    user.setAlternateEmail("not@spam.com");
    user.setCountry("us");
    user.setGender("Male");
    user.setBirthDate("5 May 1862");
    user.setPhoneNumber("385-555-1212");
    user.setMobilePhoneNumber("385-555-1213");
    user.setMailingAddress("1 Main St");
    user.setPreferredLanguage("en");
    user.setDisplayName("given surname");
    user.setPersonId("UUUU-001");
    user.setTreeUserId("PPPP-001");

    assertEquals("123", user.getId());
    assertEquals("username", user.getContactName());
    assertEquals("helper-access-pin", user.getHelperAccessPin());
    assertEquals("given middle surname", user.getFullName());
    assertEquals("given", user.getGivenName());
    assertEquals("surname", user.getFamilyName());
    assertEquals("no@spam.com", user.getEmail());
    assertEquals("not@spam.com", user.getAlternateEmail());
    assertEquals("us", user.getCountry());
    assertEquals("Male", user.getGender());
    assertEquals("5 May 1862", user.getBirthDate());
    assertEquals("385-555-1212", user.getPhoneNumber());
    assertEquals("385-555-1213", user.getMobilePhoneNumber());
    assertEquals("1 Main St", user.getMailingAddress());
    assertEquals("en", user.getPreferredLanguage());
    assertEquals("given surname", user.getDisplayName());
    assertEquals("UUUU-001", user.getPersonId());
    assertEquals("PPPP-001", user.getTreeUserId());
  }
}
