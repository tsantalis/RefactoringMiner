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
import static tech.pegasys.ethsigner.CmdlineHelpers.baseCommandOptions;
import static tech.pegasys.ethsigner.CmdlineHelpers.modifyOptionValue;
import static tech.pegasys.ethsigner.CmdlineHelpers.removeOptions;
import static tech.pegasys.ethsigner.CmdlineHelpers.toOptionsList;
import static tech.pegasys.ethsigner.subcommands.HashicorpSubCommand.COMMAND_NAME;

import tech.pegasys.ethsigner.SignerSubCommand;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class HashicorpSubCommandTest extends SubCommandTestBase {

  public static final String TLS_KNOWN_SERVER_FILE = "./knownServerFiles.txt";
  private static final String THIS_IS_THE_PATH_TO_THE_FILE =
      Paths.get("/this/is/the/path/to/the/file").toString();
  private static final String HTTP_HOST_COM = "http://host.com";
  private static final Integer PORT = 23000;
  private static final String PATH_TO_SIGNING_KEY = Paths.get("/path/to/signing/key").toString();
  private static final Integer TIMEOUT = 15;

  @Override
  protected SignerSubCommand subCommand() {
    return new HashicorpSubCommand() {
      @Override
      public void run() {
        // we only want to perform validation in these unit test cases
        validateArgs();
      }
    };
  }

  private Map<String, Object> validSubCommandOptions() {
    final Map<String, Object> subCommandOptions = new LinkedHashMap<>();
    subCommandOptions.put("auth-file", THIS_IS_THE_PATH_TO_THE_FILE);
    subCommandOptions.put("host", HTTP_HOST_COM);
    subCommandOptions.put("port", PORT);
    subCommandOptions.put("signing-key-path", PATH_TO_SIGNING_KEY);
    subCommandOptions.put("timeout", TIMEOUT);
    subCommandOptions.put("tls-known-server-file", TLS_KNOWN_SERVER_FILE);
    return subCommandOptions;
  }

  private Map<String, Object> validSubCommandOptionsWithTlsDisabled() {
    final Map<String, Object> subCommandOptions = new LinkedHashMap<>();
    subCommandOptions.put("auth-file", THIS_IS_THE_PATH_TO_THE_FILE);
    subCommandOptions.put("host", HTTP_HOST_COM);
    subCommandOptions.put("port", PORT);
    subCommandOptions.put("signing-key-path", PATH_TO_SIGNING_KEY);
    subCommandOptions.put("timeout", TIMEOUT);
    subCommandOptions.put("tls-enabled", Boolean.FALSE);
    return subCommandOptions;
  }

  private List<String> getOptions(final List<String> subCommandOptions) {
    final List<String> options = toOptionsList(baseCommandOptions());
    options.add(COMMAND_NAME);
    options.addAll(subCommandOptions);
    return options;
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final List<String> options = getOptions(toOptionsList(validSubCommandOptions()));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    final String string = subCommand.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains(HTTP_HOST_COM);
    assertThat(string).contains(String.valueOf(PORT));
    assertThat(string).contains(PATH_TO_SIGNING_KEY);
    assertThat(string).contains(String.valueOf(TIMEOUT));

    final HashicorpSubCommand hashicorpSubCommand = (HashicorpSubCommand) subCommand;
    assertThat(hashicorpSubCommand.isTlsEnabled()).isTrue();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().isPresent()).isTrue();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().get().toString())
        .isEqualTo(TLS_KNOWN_SERVER_FILE);
  }

  @Test
  public void commandLineWithTlsDisabledParsesIntoVariables() {
    final List<String> options = getOptions(toOptionsList(validSubCommandOptionsWithTlsDisabled()));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    final String string = subCommand.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains(HTTP_HOST_COM);
    assertThat(string).contains(String.valueOf(PORT));
    assertThat(string).contains(PATH_TO_SIGNING_KEY);
    assertThat(string).contains(String.valueOf(TIMEOUT));

    final HashicorpSubCommand hashicorpSubCommand = (HashicorpSubCommand) subCommand;
    assertThat(hashicorpSubCommand.isTlsEnabled()).isFalse();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile().isEmpty()).isTrue();
  }

  @Test
  public void nonIntegerInputForPortShowsError() {
    final Map<String, Object> subCommandOptions =
        modifyOptionValue(validSubCommandOptions(), "port", "NotInteger");
    final List<String> options = getOptions(toOptionsList(subCommandOptions));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));
    assertThat(result).isFalse();
  }

  @Test
  public void nonIntegerInputForTimeoutShowsError() {
    final Map<String, Object> subCommandOptions =
        modifyOptionValue(validSubCommandOptions(), "timeout", "NotInteger");
    final List<String> options = getOptions(toOptionsList(subCommandOptions));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));
    assertThat(result).isFalse();
  }

  @Test
  public void missingRequiredParamShowsAppropriateError() {
    final Map<String, Object> subCommandOptions =
        removeOptions(validSubCommandOptions(), "auth-file");
    final List<String> options = getOptions(toOptionsList(subCommandOptions));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));
    assertThat(result).isFalse();
  }

  @Test
  public void missingOptionalParametersAreSetToDefault() {
    final Map<String, Object> subCommandOptions =
        removeOptions(validSubCommandOptions(), "host", "port", "signing-key-path");
    final List<String> options = getOptions(toOptionsList(subCommandOptions));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    final String string = subCommand.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains("localhost");
    assertThat(string).contains(String.valueOf(8200));
    assertThat(string).contains("/v1/secret/data/ethsignerSigningKey");
    assertThat(string).contains(String.valueOf(TIMEOUT));
  }

  @Test
  void cmdlineIsValidIftlsKnownServerFileIsMissing() {
    final Map<String, Object> subCommandOptions =
        removeOptions(validSubCommandOptions(), "tls-known-server-file");
    final List<String> options = getOptions(toOptionsList(subCommandOptions));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    final String string = subCommand.toString();
    assertThat(string).contains(THIS_IS_THE_PATH_TO_THE_FILE);
    assertThat(string).contains(HTTP_HOST_COM);
    assertThat(string).contains(String.valueOf(PORT));
    assertThat(string).contains(PATH_TO_SIGNING_KEY);
    assertThat(string).contains(String.valueOf(TIMEOUT));

    final HashicorpSubCommand hashicorpSubCommand = (HashicorpSubCommand) subCommand;
    assertThat(hashicorpSubCommand.isTlsEnabled()).isTrue();
    assertThat(hashicorpSubCommand.getTlsKnownServerFile()).isEmpty();
  }
}
