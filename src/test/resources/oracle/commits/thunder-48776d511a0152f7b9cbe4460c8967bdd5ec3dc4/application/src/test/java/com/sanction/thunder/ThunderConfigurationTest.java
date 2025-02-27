package com.sanction.thunder;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.io.Resources;

import com.sanction.thunder.authentication.Key;
import com.sanction.thunder.validation.PropertyValidationRule;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;
import java.util.Collections;
import javax.validation.Validator;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class ThunderConfigurationTest {
  private final ObjectMapper mapper = Jackson.newObjectMapper();
  private final Validator validator = Validators.newValidator();
  private final YamlConfigurationFactory<ThunderConfiguration> factory
      = new YamlConfigurationFactory<>(ThunderConfiguration.class, validator, mapper, "dw");

  @Test
  public void testFromYaml() throws Exception {
    ThunderConfiguration configuration = factory.build(
        new File(Resources.getResource("fixtures/config.yaml").toURI()));

    Assertions.assertEquals("test.dynamodb.com", configuration.getDynamoConfiguration().getEndpoint());
    Assertions.assertEquals("test-region-1", configuration.getDynamoConfiguration().getRegion());
    Assertions.assertEquals("test-table", configuration.getDynamoConfiguration().getTableName());

    Assertions.assertEquals("test.email.com", configuration.getEmailConfiguration().getEndpoint());
    Assertions.assertEquals("test-region-2", configuration.getEmailConfiguration().getRegion());
    Assertions.assertEquals("test@sanctionco.com", configuration.getEmailConfiguration().getFromAddress());

    Assertions.assertEquals("test-success-page.html",
        configuration.getEmailConfiguration().getSuccessHtmlPath());
    Assertions.assertEquals("test-verification-email.html",
        configuration.getEmailConfiguration().getVerificationHtmlPath());
    Assertions.assertEquals("test-verification-email.txt",
        configuration.getEmailConfiguration().getVerificationTextPath());

    Assertions.assertEquals(1, configuration.getApprovedKeys().size());
    Assertions.assertEquals(
        Collections.singletonList(new Key("test-app", "test-secret")),
        configuration.getApprovedKeys());

    Assertions.assertEquals(1, configuration.getValidationRules().size());
    Assertions.assertEquals(
        new PropertyValidationRule("testProperty", "list"),
        configuration.getValidationRules().get(0));
  }
}
