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
import static tech.pegasys.ethsigner.CmdlineHelpers.toOptionsList;
import static tech.pegasys.ethsigner.subcommands.MultiKeySubCommand.COMMAND_NAME;
import static tech.pegasys.ethsigner.util.CommandLineParserAssertions.assertMissingOptionsAreReported;

import tech.pegasys.ethsigner.CmdlineHelpers;
import tech.pegasys.ethsigner.SignerSubCommand;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MultiKeySubCommandTest extends SubCommandTestBase {

  @Override
  protected SignerSubCommand subCommand() {
    return new MultiKeySubCommand() {
      @Override
      public void run() {
        // we only want to perform validation in these unit test cases
        validateArgs();
      }
    };
  }

  @ParameterizedTest
  @ValueSource(strings = {"--directory", "-d"})
  void parseCommandSuccessfullySetDirectory(final String subCommandOption) {
    final Path expectedPath = Path.of("/keys/directory/path");
    final List<String> subCommandOptions = List.of(subCommandOption, expectedPath.toString());

    final List<String> options = getOptions(subCommandOptions);

    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    assertThat(((MultiKeySubCommand) subCommand).getDirectoryPath()).isEqualTo(expectedPath);
  }

  @Test
  void configFileSuccessfullyParsedAndValidates() {
    final Path expectedPath = Path.of("/keys/directory/path");

    final Map<String, Object> optionsMap = baseCommandOptions();
    optionsMap.put("multikey-signer.directory", expectedPath.toString());

    final List<String> options = CmdlineHelpers.toConfigFileOptionsList(tempDir, optionsMap);
    options.add(COMMAND_NAME);

    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertThat(result).isTrue();
    assertThat(((MultiKeySubCommand) subCommand).getDirectoryPath()).isEqualTo(expectedPath);
  }

  private List<String> getOptions(final List<String> subCommandOptions) {
    final List<String> options = toOptionsList(baseCommandOptions());
    options.add(COMMAND_NAME);
    options.addAll(subCommandOptions);
    return options;
  }

  @Test
  void directoryParameterIsRequired() {
    final List<String> options = getOptions(Collections.emptyList());

    final boolean result = parser.parseCommandLine(options.toArray(String[]::new));

    assertMissingOptionsAreReported(
        commandOutput, commandError, subCommandUsageText, List.of("directory"), result);
  }
}
