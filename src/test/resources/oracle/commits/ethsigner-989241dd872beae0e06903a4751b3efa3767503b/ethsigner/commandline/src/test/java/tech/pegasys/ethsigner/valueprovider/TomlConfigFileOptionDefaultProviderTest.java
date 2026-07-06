/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.valueprovider;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class TomlConfigFileOptionDefaultProviderTest {

  private DemoCommand demoCommand;
  private DemoCommand.SubCommand subCommand;
  private CommandLine commandLine;

  @BeforeEach
  void setUp() {
    demoCommand = new DemoCommand();
    subCommand = new DemoCommand.SubCommand();

    commandLine = new CommandLine(demoCommand);
    commandLine.addSubcommand(subCommand);
  }

  @Test
  void validTomlFileIsUsedAsDefaultValueProvider(@TempDir Path tempDir) throws IOException {
    final Path configFile = Files.writeString(tempDir.resolve("test.toml"), validToml());

    final TomlConfigFileDefaultProvider defaultProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);
    commandLine.setDefaultValueProvider(defaultProvider);
    commandLine.parseArgs("country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(10);
    assertThat(demoCommand.y).isEqualTo(20);
    assertThat(demoCommand.name).isEqualTo("test name");
    assertThat(subCommand.countryCodes).containsExactlyInAnyOrder("AU", "US");
  }

  @Test
  void validTomlFileAndCliOptionsMixed(@TempDir Path tempDir) throws IOException {
    final Path configFile = Files.writeString(tempDir.resolve("test.toml"), validToml());

    final TomlConfigFileDefaultProvider defaultProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);

    commandLine.setDefaultValueProvider(defaultProvider);
    commandLine.parseArgs("--name", "test name2", "country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(10);
    assertThat(demoCommand.y).isEqualTo(20);
    assertThat(demoCommand.name).isEqualTo("test name2");
    assertThat(subCommand.countryCodes).containsExactlyInAnyOrder("AU", "US");
  }

  @Test
  void invalidOptionInTomlFileThrowsException(@TempDir Path tempDir) throws IOException {
    final Path configFile =
        Files.writeString(tempDir.resolve("test.toml"), validToml() + "invalidOption=10");

    final TomlConfigFileDefaultProvider defaultProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);

    commandLine.setDefaultValueProvider(defaultProvider);

    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs())
        .withMessage("Unknown option in TOML configuration file: invalidOption");
  }

  @Test
  void invalidOptionsInTomlFileThrowsException(@TempDir Path tempDir) throws IOException {
    final Path configFile =
        Files.writeString(
            tempDir.resolve("test.toml"),
            validToml() + "invalidOption=10\ncountry.name=\"\"" + lineSeparator());

    final TomlConfigFileDefaultProvider defaultProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);

    commandLine.setDefaultValueProvider(defaultProvider);

    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs())
        .withMessage("Unknown options in TOML configuration file: country.name, invalidOption");
  }

  @Test
  void emptyTomlFileThrowsException(@TempDir Path tempDir) throws IOException {
    final Path configFile = Files.writeString(tempDir.resolve("test.toml"), "");

    final TomlConfigFileDefaultProvider defaultProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);

    commandLine.setDefaultValueProvider(defaultProvider);

    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs())
        .withMessage("Empty TOML configuration file: " + configFile.toString());
  }

  private String validToml() {
    return "x=10"
        + lineSeparator()
        + "y=20"
        + lineSeparator()
        + "name=\"test name\""
        + lineSeparator()
        + "country.codes=[\"AU\", \"US\"]"
        + lineSeparator();
  }
}
