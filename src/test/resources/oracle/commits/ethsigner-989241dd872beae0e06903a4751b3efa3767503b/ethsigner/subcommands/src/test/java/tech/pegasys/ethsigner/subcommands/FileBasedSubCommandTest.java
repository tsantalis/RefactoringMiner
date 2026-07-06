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
import static tech.pegasys.ethsigner.CmdlineHelpers.toConfigFileOptionsList;
import static tech.pegasys.ethsigner.CmdlineHelpers.toEnvironmentMap;
import static tech.pegasys.ethsigner.CmdlineHelpers.toOptionsList;
import static tech.pegasys.ethsigner.subcommands.FileBasedSubCommand.COMMAND_NAME;

import tech.pegasys.ethsigner.CmdlineHelpers;
import tech.pegasys.ethsigner.SignerSubCommand;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FileBasedSubCommandTest extends SubCommandTestBase {

  private static final String PASSWORD_FILE =
      Paths.get("/this/is/the/path/to/the/password/file").toString();
  private static final String KEY_FILE = Paths.get("/this/is/the/path/to/the/key/file").toString();

  @Override
  protected SignerSubCommand subCommand() {
    return new FileBasedSubCommand() {
      @Override
      public void run() {
        // we only want to perform validation in these unit test cases
        validateArgs();
      }
    };
  }

  private Map<String, Object> subCommandCliOptions() {
    return Map.of("password-file", PASSWORD_FILE, "key-file", KEY_FILE);
  }

  private Map<String, Object> subCommandConfigFileOptions() {
    return Map.of(
        COMMAND_NAME + ".password-file", PASSWORD_FILE, COMMAND_NAME + ".key-file", KEY_FILE);
  }

  @Test
  public void fullyPopulatedCommandLineParsesIntoVariables() {
    final List<String> options = toOptionsList(baseCommandOptions());
    options.add(COMMAND_NAME);
    options.addAll(toOptionsList(subCommandCliOptions()));
    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    assertThat(subCommand.toString()).contains(PASSWORD_FILE);
    assertThat(subCommand.toString()).contains(KEY_FILE);
  }

  @Test
  public void fullyPopulatedConfigFileParsesIntoVariables() {
    final Map<String, Object> options = baseCommandOptions();
    options.putAll(subCommandConfigFileOptions());
    final List<String> cmdOptions = CmdlineHelpers.toConfigFileOptionsList(tempDir, options);
    cmdOptions.add(COMMAND_NAME);

    final boolean result = parser.parseCommandLine(cmdOptions.toArray(String[]::new));

    assertThat(result).isTrue();
    assertThat(subCommand.toString()).contains(PASSWORD_FILE);
    assertThat(subCommand.toString()).contains(KEY_FILE);
  }

  @Test
  public void environmentVariablesParsesAndValidates() {
    environmentVariablesMap.putAll(toEnvironmentMap("ethsigner", baseCommandOptions()));
    environmentVariablesMap.putAll(
        toEnvironmentMap("ethsigner", COMMAND_NAME, subCommandCliOptions()));

    final boolean result = parser.parseCommandLine(COMMAND_NAME);

    assertThat(result).isTrue();
    assertThat(subCommand.toString()).contains(PASSWORD_FILE);
    assertThat(subCommand.toString()).contains(KEY_FILE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"password-file", "key-file"})
  public void missingRequiredParamShowsAppropriateError(final String optionToRemove) {
    final Map<String, Object> subCommandOptions = new LinkedHashMap<>(subCommandCliOptions());
    subCommandOptions.remove(optionToRemove);

    final List<String> options = toOptionsList(baseCommandOptions());
    options.add(COMMAND_NAME);
    options.addAll(toOptionsList(subCommandOptions));

    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));
    assertThat(result).isFalse();
    assertThat(commandError.toString())
        .contains(
            "Missing required option(s): --"
                + optionToRemove.substring(optionToRemove.lastIndexOf(".") + 1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"file-based-signer.password-file", "file-based-signer.key-file"})
  public void missingRequiredParamFromConfigFileShowsAppropriateError(final String optionToRemove) {
    final Map<String, Object> subCommandOptions =
        new LinkedHashMap<>(subCommandConfigFileOptions());
    subCommandOptions.remove(optionToRemove);

    final Map<String, Object> optionsMap = baseCommandOptions();
    optionsMap.putAll(subCommandOptions);

    final List<String> options = toConfigFileOptionsList(tempDir, optionsMap);
    options.add(COMMAND_NAME);

    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));
    assertThat(result).isFalse();
    assertThat(commandError.toString())
        .contains(
            "Missing required option(s): --"
                + optionToRemove.substring(optionToRemove.lastIndexOf(".") + 1));
  }
}
