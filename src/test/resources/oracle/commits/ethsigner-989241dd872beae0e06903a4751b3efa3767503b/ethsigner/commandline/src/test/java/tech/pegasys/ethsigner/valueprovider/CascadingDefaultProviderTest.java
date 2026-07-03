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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class CascadingDefaultProviderTest {
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
  void environmentVariableTakePrecedence(@TempDir Path tempDir) throws IOException {
    final Path configFile = Files.writeString(tempDir.resolve("test.toml"), validToml());

    final TomlConfigFileDefaultProvider tomlProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);
    final EnvironmentVariableDefaultProvider envProvider =
        new EnvironmentVariableDefaultProvider(validEnvMap());

    final CascadingDefaultProvider cascadingDefaultProvider =
        new CascadingDefaultProvider(envProvider, tomlProvider);
    commandLine.setDefaultValueProvider(cascadingDefaultProvider);

    commandLine.parseArgs("country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(10);
    assertThat(demoCommand.y).isEqualTo(20);
    assertThat(demoCommand.name).isEqualTo("env test name");
    assertThat(subCommand.countryCodes).containsExactlyInAnyOrder("AU", "US");
  }

  @Test
  void missingEnvGetsPickedFromConfig(@TempDir Path tempDir) throws IOException {
    final Path configFile = Files.writeString(tempDir.resolve("test.toml"), validPartialToml());

    final TomlConfigFileDefaultProvider tomlProvider =
        new TomlConfigFileDefaultProvider(commandLine, configFile);

    final EnvironmentVariableDefaultProvider envProvider =
        new EnvironmentVariableDefaultProvider(validPartialEnvMap());

    final CascadingDefaultProvider cascadingDefaultProvider =
        new CascadingDefaultProvider(envProvider, tomlProvider);
    commandLine.setDefaultValueProvider(cascadingDefaultProvider);

    commandLine.parseArgs("country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(10);
    assertThat(demoCommand.y).isEqualTo(20);
    assertThat(demoCommand.name).isEqualTo("toml test name");
    assertThat(subCommand.countryCodes).containsExactlyInAnyOrder("CA", "UK");
  }

  private Map<String, String> validEnvMap() {
    return Map.of(
        "DEMO_X",
        "10",
        "DEMO_Y",
        "20",
        "DEMO_NAME",
        "env test name",
        "DEMO_COUNTRY_CODES",
        "AU,US");
  }

  private Map<String, String> validPartialEnvMap() {
    return Map.of("DEMO_X", "10", "DEMO_Y", "20");
  }

  private String validToml() {
    return "x=30"
        + lineSeparator()
        + "y=40"
        + lineSeparator()
        + "name=\"toml test name\""
        + lineSeparator()
        + "country.codes=[\"CA\", \"UK\"]"
        + lineSeparator();
  }

  private String validPartialToml() {
    return "name=\"toml test name\""
        + lineSeparator()
        + "country.codes=[\"CA\", \"UK\"]"
        + lineSeparator();
  }
}
