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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

class MultiKeySubCommandTest {

  private MultiKeySubCommand multiKeySubCommand;
  private CommandLine commandLine;

  @BeforeEach
  void beforeEach() {
    multiKeySubCommand = new MultiKeySubCommand();

    commandLine = new CommandLine(multiKeySubCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);
  }

  @Test
  void parseCommandSuccessfullySetDirectory() {
    final Path expectedPath = Path.of("/keys/directory/path");

    commandLine.parse("--directory", expectedPath.toString());

    final ParseResult parseResult = commandLine.getParseResult();
    assertThat(parseResult.errors()).isEmpty();

    final Path path = multiKeySubCommand.getDirectoryPath();
    assertThat(path).isEqualTo(expectedPath);
  }

  @Test
  void parseCommandSuccessfullyUsingShortcutSetDirectory() {
    final Path expectedPath = Path.of("/keys/directory/path");

    commandLine.parse("-d", "/keys/directory/path");

    final ParseResult parseResult = commandLine.getParseResult();
    assertThat(parseResult.errors()).isEmpty();

    final Path path = multiKeySubCommand.getDirectoryPath();
    assertThat(path).isEqualTo(expectedPath);
  }

  @Test
  void directoryParameterIsRequired() {
    assertThrows(
        CommandLine.MissingParameterException.class,
        () -> commandLine.parse("--foo", "/keys/directory/path"));
  }
}
