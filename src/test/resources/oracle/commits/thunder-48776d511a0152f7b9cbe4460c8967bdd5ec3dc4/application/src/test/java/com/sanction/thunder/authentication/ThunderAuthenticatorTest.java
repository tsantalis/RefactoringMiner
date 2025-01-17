package com.sanction.thunder.authentication;

import io.dropwizard.auth.basic.BasicCredentials;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class ThunderAuthenticatorTest {
  private static final Key key = new Key("application", "secret");
  private static final List<Key> keys = Collections.singletonList(key);

  // Test object //
  private static final ThunderAuthenticator authenticator = new ThunderAuthenticator(keys);

  @Test
  public void testAuthenticateWithValidCredentials() {
    BasicCredentials credentials = new BasicCredentials("application", "secret");

    Optional<Key> result = authenticator.authenticate(credentials);

    Assertions.assertAll("Assert valid credentials",
        () -> Assertions.assertTrue(result.isPresent()),
        () -> Assertions.assertEquals(key, result.get()));
  }

  @Test
  public void testAuthenticateWithInvalidCredentials() {
    BasicCredentials credentials = new BasicCredentials("invalidApplication", "secret");

    Optional<Key> result = authenticator.authenticate(credentials);

    Assertions.assertFalse(() -> result.isPresent());
  }
}
