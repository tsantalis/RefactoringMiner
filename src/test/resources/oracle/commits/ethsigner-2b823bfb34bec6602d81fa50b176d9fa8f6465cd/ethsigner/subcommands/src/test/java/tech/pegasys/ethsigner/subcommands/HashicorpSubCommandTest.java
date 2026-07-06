/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.subcommands;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.CmdlineHelpers.modifyField;

import tech.pegasys.ethsigner.CmdlineHelpers;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class HashicorpSubCommandTest {

  public static final String TLS_KNOWN_SERVER_FILE = "./knownServerFiles.txt";
  private static final String THIS_IS_THE_PATH_TO_THE_FILE =
      Paths.get("/this/is/the/path/to/the/file").toString();
  private static final String HTTP_HOST_COM = "http://host.com";
  private static final String PORT = "23000";
  private static final String PATH_TO_SIGNING_KEY = Paths.get("/path/to/signing/key").toString();
  private static final String FIFTEEN = "15";
  private final ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();

  private HashicorpSubCommand hashicorpSubCommand;

  @BeforeEach
  void init() {
    hashicorpSubCommand = new HashicorpSubCommand();
  }

  private boolean parseCommand(final List<String> cmdLine) {
    final CommandLine commandLine = new CommandLine(hashicorpSubCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);

    try {
      commandLine.parseArgs(cmdLine.toArray(String[]::new));
    } catch (final CommandLine.ParameterException e) {
      return false;
    }
    return true;
  }

  private List<String> validCommandLine() {
    return Lists.newArrayList(
        "--auth-file=" + THIS_IS_THE_PATH_TO_THE_FILE,
        "--host=" + HTTP_HOST_COM,
        "--port=" + PORT,
        "--signing-key-path=" + PATH_TO_SIGNING_KEY,
        "--timeout=" + FIFTEEN,
        "--tls-known-server-file=" + TLS_KNOWN_SERVER_FILE);
  }

  private List<String> validWithTlsDisabledCommandLine() {
    return Lists.newArrayList(
        "--auth-file=" + THIS_IS_THE_PATH_TO_THE_FILE,
        "--host=" + HTTP_HOST_COM,
        "--port=" + PORT,
        "--signing-key-path=" + PATH_TO_SIGNING_KEY,
        "--timeout=" + FIFTEEN,
        "--tls-enabled=false");
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parseCommand(validCommandLine());

    assertThat(result).isTrue();
    final String string = hashicorpSubCommand.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains(HTTP_HOST_COM);
    assertThat(string).contains(PORT);
    assertThat(string).contains(PATH_TO_SIGNING_KEY);
    assertThat(string).contains(FIFTEEN);

    assertThat(hashicorpSubCommand.isTlsEnabled()).isTrue();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().isPresent()).isTrue();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().get().toString())
        .isEqualTo(TLS_KNOWN_SERVER_FILE);
  }

  @Test
  public void commandLineWithTlsDisabledParsesIntoVariables() {
    final boolean result = parseCommand(validWithTlsDisabledCommandLine());

    assertThat(result).isTrue();
    final String string = hashicorpSubCommand.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains(HTTP_HOST_COM);
    assertThat(string).contains(PORT);
    assertThat(string).contains(PATH_TO_SIGNING_KEY);
    assertThat(string).contains(FIFTEEN);

    assertThat(hashicorpSubCommand.isTlsEnabled()).isFalse();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().isEmpty()).isTrue();
  }

  @Test
  public void nonIntegerInputForPortShowsError() {
    final List<String> cmdLine = modifyField(validCommandLine(), "port", "noInteger");
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }

  @Test
  public void nonIntegerInputForTimeoutShowsError() {
    final List<String> cmdLine = modifyField(validCommandLine(), "timeout", "noInteger");
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    missingParameterShowsError("auth-file");
  }

  @Test
  public void missingOptionalParametersAreSetToDefault() {
    // Must recreate commandLineConfig before executions, to prevent stale data remaining in the
    // object.
    HashicorpSubCommand hcConfig = new HashicorpSubCommand();
    missingOptionalParameterIsValidAndMeetsDefault("host", hcConfig::toString, "localhost");

    hcConfig = new HashicorpSubCommand();
    missingOptionalParameterIsValidAndMeetsDefault("host", hcConfig::toString, "8200");

    hcConfig = new HashicorpSubCommand();
    missingOptionalParameterIsValidAndMeetsDefault(
        "host", hcConfig::toString, "/secret/data/ethsignerSigningKey");
  }

  @Test
  void cmdlineIsValidIftlsKnownServerFileIsMissing() {
    final List<String> cmdLine =
        CmdlineHelpers.removeFieldsFrom(validCommandLine(), "tls-known-server-file");
    final boolean result = parseCommand(cmdLine);

    assertThat(result).isTrue();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().isEmpty()).isTrue();
  }

  private void missingParameterShowsError(final String paramToRemove) {
    final List<String> cmdLine = CmdlineHelpers.removeFieldsFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isFalse();
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove,
      final Supplier<String> actualValueGetter,
      final String expectedValue) {
    final List<String> cmdLine = CmdlineHelpers.removeFieldsFrom(validCommandLine(), paramToRemove);
    final boolean result = parseCommand(cmdLine);
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).contains(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }
}
