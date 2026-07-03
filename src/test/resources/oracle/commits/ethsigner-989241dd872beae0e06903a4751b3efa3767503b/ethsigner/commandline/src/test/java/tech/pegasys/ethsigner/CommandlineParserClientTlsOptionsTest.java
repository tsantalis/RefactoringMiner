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
package tech.pegasys.ethsigner;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.CmdlineHelpers.baseCommandOptions;
import static tech.pegasys.ethsigner.CmdlineHelpers.modifyOptionValue;
import static tech.pegasys.ethsigner.CmdlineHelpers.toConfigFileOptionsList;
import static tech.pegasys.ethsigner.CmdlineHelpers.toOptionsList;
import static tech.pegasys.ethsigner.util.CommandLineParserAssertions.parseCommandLineWithMissingParamsShowsError;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

class CommandlineParserClientTlsOptionsTest {

  @TempDir static Path tempDir;

  private final StringWriter commandOutput = new StringWriter();
  private final StringWriter commandError = new StringWriter();
  private final PrintWriter outputWriter = new PrintWriter(commandOutput, true);
  private final PrintWriter errorWriter = new PrintWriter(commandError, true);

  private EthSignerBaseCommand config;
  private CommandlineParser parser;
  private NullSignerSubCommand subCommand;
  private String defaultUsageText;

  @BeforeEach
  void setup() {
    subCommand = new NullSignerSubCommand();
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter, emptyMap());
    parser.registerSigners(subCommand);

    final CommandLine commandLine = new CommandLine(new EthSignerBaseCommand());
    commandLine.addSubcommand(subCommand.getCommandName(), subCommand);
    defaultUsageText = commandLine.getUsageMessage();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineIsValidIfOnlyDownstreamTlsIsEnabled(final boolean useConfigFile) {
    final Map<String, Object> updatedOptions =
        CmdlineHelpers.removeOptions(
            "downstream-http-tls-keystore-file",
            "downstream-http-tls-keystore-password-file",
            "downstream-http-tls-ca-auth-enabled",
            "downstream-http-tls-known-servers-file");
    final List<String> cmdLine =
        useConfigFile
            ? CmdlineHelpers.toConfigFileOptionsList(tempDir, updatedOptions)
            : CmdlineHelpers.toOptionsList(updatedOptions);

    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();

    assertThat(optionalDownstreamTlsOptions.isPresent()).as("TLS Enabled").isTrue();
    assertThat(optionalDownstreamTlsOptions.get().getKnownServersFile().isEmpty()).isTrue();
    assertThat(optionalDownstreamTlsOptions.get().getKeyStoreOptions().isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineIsValidWithoutDownstreamTlsOptions(final boolean useConfigFile) {
    final Map<String, Object> options =
        CmdlineHelpers.removeOptions(
            "downstream-http-tls-enabled",
            "downstream-http-tls-keystore-file",
            "downstream-http-tls-keystore-password-file",
            "downstream-http-tls-ca-auth-enabled",
            "downstream-http-tls-known-servers-file");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isEmpty()).as("Downstream TLS Options").isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineIsValidWithAllTlsOptions(final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();

    final ClientTlsOptions clientTlsOptions = optionalDownstreamTlsOptions.get();
    assertThat(clientTlsOptions.getKnownServersFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsOptions.isCaAuthEnabled()).isFalse();

    final KeyStoreOptions keyStoreOptions = clientTlsOptions.getKeyStoreOptions().get();
    assertThat(keyStoreOptions.getKeyStoreFile()).isEqualTo(Path.of("./test.ks"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineFailsIfDownstreamTlsOptionsAreUsedWithoutTlsEnabled(final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("downstream-http-tls-enabled"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingClientCertificateFileDisplaysErrorIfPasswordIsStillIncluded(
      final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("downstream-http-tls-keystore-file"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingClientCertificatePasswordFileDisplaysErrorIfCertificateIsStillIncluded(
      final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("downstream-http-tls-keystore-password-file"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineIsValidWhenTlsClientCertificateOptionsAreMissing(final boolean useConfigFile) {
    final Map<String, Object> options =
        CmdlineHelpers.removeOptions(
            "downstream-http-tls-keystore-file", "downstream-http-tls-keystore-password-file");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);

    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).isTrue();
    final ClientTlsOptions clientTlsOptions = config.getClientTlsOptions().get();
    assertThat(clientTlsOptions.getKnownServersFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsOptions.isCaAuthEnabled()).isFalse();
    assertThat(clientTlsOptions.getKeyStoreOptions().isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineIsValidIfOnlyDownstreamKnownServerIsSpecified(final boolean useConfigFile) {
    final Map<String, Object> options =
        CmdlineHelpers.removeOptions(
            "downstream-http-tls-keystore-file",
            "downstream-http-tls-keystore-password-file",
            "downstream-http-tls-ca-auth-enabled");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());

    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).isTrue();

    final ClientTlsOptions clientTlsOptions = config.getClientTlsOptions().get();
    assertThat(clientTlsOptions.getKnownServersFile().get()).isEqualTo(Path.of("./test.txt"));
    assertThat(clientTlsOptions.isCaAuthEnabled()).isTrue();
    assertThat(clientTlsOptions.getKeyStoreOptions().isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void downstreamKnownServerIsRequiredIfCaSignedDisableWithoutKnownServersFile(
      final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("downstream-http-tls-known-servers-file"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cmdLineIsValidWithCaAuthEnabledExplicitly(final boolean useConfigFile) {
    final Map<String, Object> options =
        modifyOptionValue("downstream-http-tls-ca-auth-enabled", Boolean.TRUE);
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);

    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void downstreamKnownServerIsNotRequiredIfCaSignedEnabledExplicitly(final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    options.replace("downstream-http-tls-ca-auth-enabled", Boolean.TRUE);
    options.remove("downstream-http-tls-known-servers-file");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);

    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).as("CLI Parse result").isTrue();
    final Optional<ClientTlsOptions> optionalDownstreamTlsOptions = config.getClientTlsOptions();
    assertThat(optionalDownstreamTlsOptions.isPresent()).as("Downstream TLS Options").isTrue();
  }
}
