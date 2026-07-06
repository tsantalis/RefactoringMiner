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
import static tech.pegasys.ethsigner.CmdlineHelpers.removeOptions;
import static tech.pegasys.ethsigner.CmdlineHelpers.toConfigFileOptionsList;
import static tech.pegasys.ethsigner.CmdlineHelpers.toOptionsList;
import static tech.pegasys.ethsigner.util.CommandLineParserAssertions.parseCommandLineWithMissingParamsShowsError;

import tech.pegasys.ethsigner.core.config.ClientAuthConstraints;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

class CommandlineParserTest {

  @TempDir static Path tempDir;

  private final StringWriter commandOutput = new StringWriter();
  private final StringWriter commandError = new StringWriter();
  private final PrintWriter outputWriter = new PrintWriter(commandOutput, true);
  private final PrintWriter errorWriter = new PrintWriter(commandError, true);

  private EthSignerBaseCommand config;
  private CommandlineParser parser;
  private NullSignerSubCommand subCommand;
  private String defaultUsageText;
  private String nullCommandHelp;

  @BeforeEach
  void setup() {
    subCommand = new NullSignerSubCommand();
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter, emptyMap());
    parser.registerSigners(subCommand);

    final CommandLine commandLine = new CommandLine(new EthSignerBaseCommand());
    commandLine.addSubcommand(subCommand.getCommandName(), subCommand);
    defaultUsageText = commandLine.getUsageMessage();
    nullCommandHelp =
        commandLine.getSubcommands().get(subCommand.getCommandName()).getUsageMessage();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void fullyPopulatedCommandLineParsesIntoVariables(final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    final List<String> argsList =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    argsList.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(argsList.toArray(String[]::new));

    assertThat(result).isTrue();

    final ClientAuthConstraints tlsClientConstaints =
        config.getTlsOptions().get().getClientAuthConstraints().get();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getDownstreamHttpHost()).isEqualTo("8.8.8.8");
    assertThat(config.getDownstreamHttpPort()).isEqualTo(5000);
    assertThat(config.getDownstreamHttpPath()).isEqualTo("/v3/projectid");
    assertThat(config.getDownstreamHttpRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
    assertThat(config.getCorsAllowedOrigins()).isEmpty();
    assertThat(config.getTlsOptions()).isNotEmpty();
    assertThat(config.getTlsOptions().get().getKeyStoreFile())
        .isEqualTo(new File("./keystore.pfx"));
    assertThat(config.getTlsOptions().get().getKeyStorePasswordFile())
        .isEqualTo(new File("./keystore.passwd"));
    assertThat(tlsClientConstaints.getKnownClientsFile())
        .isEqualTo(Optional.of(new File("./known_clients")));
    assertThat(tlsClientConstaints.isCaAuthorizedClientAllowed()).isTrue();

    final Optional<ClientTlsOptions> downstreamTlsOptionsOptional = config.getClientTlsOptions();
    assertThat(downstreamTlsOptionsOptional.isPresent()).isTrue();
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelp() {
    final boolean result = parser.parseCommandLine("--help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(defaultUsageText);
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelpWithoutDashes() {
    final boolean result = parser.parseCommandLine("help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @Test
  void reverseHelpRequestShowsSubCommandHelp() {
    final boolean result = parser.parseCommandLine("help", subCommand.getCommandName());
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(nullCommandHelp);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingSubCommandShowsErrorAndUsageText(final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    final List<String> argsList =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    final boolean result = parser.parseCommandLine(argsList.toArray(String[]::new));

    assertThat(result).isFalse();
    assertThat(commandError.toString()).contains("Missing required subcommand");
    assertThat(commandOutput.toString()).contains(defaultUsageText);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void nonIntegerInputForDownstreamPortShowsError(final boolean useConfigFile) {
    final Map<String, Object> options = modifyOptionValue("downstream-http-port", "abc");
    final List<String> args =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    final boolean result = parser.parseCommandLine(args.toArray(String[]::new));
    assertThat(result).isFalse();
    assertThat(commandError.toString()).contains("--downstream-http-port", "'abc' is not an int");
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingRequiredParamShowsAppropriateError(final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("downstream-http-port"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @Test
  void missingLoggingDefaultsToInfoLevel() {
    // Must recreate config before executions, to prevent stale data remaining in the object.
    missingOptionalParameterIsValidAndMeetsDefault("logging", config::getLogLevel, Level.INFO);
  }

  @Test
  void missingDownstreamHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-host", config::getDownstreamHttpHost, "127.0.0.1");
  }

  @Test
  void missingDownstreamPortDefaultsTo8545() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-port", config::getHttpListenPort, 8545);
  }

  @Test
  void missingDownstreamPathDefaultsToRootPath() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-path", config::getDownstreamHttpPath, "/");
  }

  @Test
  void missingDownstreamTimeoutDefaultsToFiveSeconds() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-request-timeout",
        config::getDownstreamHttpRequestTimeout,
        Duration.ofSeconds(5));
  }

  @Test
  void missingListenHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host", config::getHttpListenHost, "127.0.0.1");
  }

  @Test
  void illegalSubCommandDisplaysErrorMessage() {
    // NOTE: all required params must be specified
    parser.parseCommandLine("--downstream-http-port=8500", "--chain-id=1", "illegalSubCommand");
    assertThat(commandOutput.toString())
        .containsOnlyOnce("Did you mean: " + subCommand.getCommandName());
    assertThat(commandOutput.toString()).doesNotContain(defaultUsageText);
  }

  @Test
  void misspeltCommandLineOptionDisplaysErrorMessage() {
    final boolean result =
        parser.parseCommandLine(
            "--downstream-http-port=8500",
            "--chain-id=1",
            "--nonExistentOption=9",
            subCommand.getCommandName());
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    final List<String> cmdLine = toOptionsList(removeOptions(paramToRemove));
    cmdLine.add(subCommand.getCommandName());

    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void creatingSignerDisplaysFailureToCreateSignerText(final boolean useConfigFile) {
    subCommand = new NullSignerSubCommand(true);
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter, emptyMap());
    parser.registerSigners(subCommand);

    final Map<String, Object> options = baseCommandOptions();
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).isFalse();
    assertThat(commandError.toString())
        .contains(
            CommandlineParser.SIGNER_CREATION_ERROR
                + System.lineSeparator()
                + "Cause: "
                + NullSignerSubCommand.ERROR_MSG);
    assertThat(commandOutput.toString()).contains(nullCommandHelp);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void settingTlsKnownClientAndDisablingClientAuthenticationShowsError(
      final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    options.put("tls-allow-any-client", Boolean.TRUE);
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    // cmdLine.add("--tls-allow-any-client");
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).isFalse();
    assertThat(commandError.toString())
        .contains(
            "Missing required argument(s): expecting either --tls-allow-any-client or one of --tls-known-clients-file=<FILE>, --tls-allow-ca-clients");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void tlsClientAuthenticationCanBeDisabledByRemovingKnownClientsAndSettingOption(
      final boolean useConfigFile) {
    final Map<String, Object> options =
        removeOptions("tls-known-clients-file", "tls-allow-ca-clients");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);

    cmdLine.add("--tls-allow-any-client");
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).isTrue();
    assertThat(config.getTlsOptions().get().getClientAuthConstraints()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void notExplicitlySettingTlsClientAuthFailsParsing(final boolean useConfigFile) {
    final Map<String, Object> options =
        removeOptions("tls-known-clients-file", "tls-allow-ca-clients");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).isFalse();
  }

  @Test
  void missingTlsClientWhitelistIsValidIfCaIsSpecified() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "tls-known-clients-file",
        () -> config.getTlsOptions().get().getClientAuthConstraints().get().getKnownClientsFile(),
        Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingTlsKeyStorePasswordShowsErrorWhenKeystorePasswordIsSet(final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("tls-keystore-file"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingTlsPasswordFileShowsErrorWhenKeyStoreIsSet(final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("tls-keystore-password-file"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void specifyingOnlyTheTlsClientWhiteListShowsError(final boolean useConfigFile) {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        List.of("tls-keystore-file", "tls-keystore-password-file"),
        useConfigFile ? Optional.of(tempDir) : Optional.empty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void ethSignerStartsValidlyIfNoTlsOptionsAreSet(final boolean useConfigFile) {
    final Map<String, Object> options =
        removeOptions(
            "tls-keystore-file",
            "tls-keystore-password-file",
            "tls-known-clients-file",
            "tls-allow-ca-clients");
    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));

    assertThat(result).isTrue();
    assertThat(config.getTlsOptions()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"=", " ", "&", "?", "#"})
  void illegalDownStreamPathThrowsException(final String illegalChar) {
    final Map<String, Object> options =
        modifyOptionValue("downstream-http-path", "path1/" + illegalChar + "path2 ");

    final List<String> cmdLine = toOptionsList(options);

    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void configContainsCorsValueSetOnCmdline(final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    options.put("http-cors-origins", "sample.com");

    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).isTrue();
    assertThat(config.getCorsAllowedOrigins()).containsOnly("sample.com");
  }

  @Test
  void corsCanBeACommaSeparatedList() {
    final List<String> cmdLine = toOptionsList(baseCommandOptions());
    cmdLine.add("--http-cors-origins=sample.com,mydomain.com");
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).isTrue();
    assertThat(config.getCorsAllowedOrigins()).contains("sample.com", "mydomain.com");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void corsValueOfNoneLiteralProducesEmptyListInConfig(final boolean useConfigFile) {
    final Map<String, Object> options = baseCommandOptions();
    options.put("http-cors-origins", "none");

    final List<String> cmdLine =
        useConfigFile ? toConfigFileOptionsList(tempDir, options) : toOptionsList(options);
    cmdLine.add(subCommand.getCommandName());
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).isTrue();
    assertThat(config.getCorsAllowedOrigins()).isEmpty();
  }
}
