/*
 * Copyright 2018 ConsenSys AG.
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

import static tech.pegasys.ethsigner.DefaultCommandValues.FILE_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.HOST_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.LONG_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.PORT_FORMAT_HELP;
import static tech.pegasys.ethsigner.util.RequiredOptionsUtil.checkIfRequiredOptionsAreInitialized;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.annotations.RequiredOption;
import tech.pegasys.ethsigner.core.InitializationException;
import tech.pegasys.signers.hashicorp.TrustStoreType;
import tech.pegasys.signers.hashicorp.config.ConnectionParameters;
import tech.pegasys.signers.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.signers.hashicorp.config.KeyDefinition;
import tech.pegasys.signers.hashicorp.config.TlsOptions;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SignerProvider;
import tech.pegasys.signers.secp256k1.api.SingleSignerProvider;
import tech.pegasys.signers.secp256k1.common.SignerInitializationException;
import tech.pegasys.signers.secp256k1.hashicorp.HashicorpSignerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.MoreObjects;
import io.vertx.core.Vertx;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Hashicorp vault related sub-command */
@Command(
    name = HashicorpSubCommand.COMMAND_NAME,
    description = "Sign transactions with a key stored in a Hashicorp Vault.",
    mixinStandardHelpOptions = true)
public class HashicorpSubCommand extends SignerSubCommand {
  static final String COMMAND_NAME = "hashicorp-signer";
  private static final String DEFAULT_HASHICORP_VAULT_HOST = "localhost";
  private static final String DEFAULT_KEY_PATH = "/v1/secret/data/ethsignerSigningKey";
  private static final String DEFAULT_PORT_STRING = "8200";
  private static final Integer DEFAULT_PORT = Integer.valueOf(DEFAULT_PORT_STRING);
  private static final Long DEFAULT_TIMEOUT = Duration.ofSeconds(10).toMillis();
  public static final boolean DEFAULT_TLS_ENABLED = true;

  private static final String AUTH_FILE_ERROR_MSG_FMT =
      "Unable to read file containing the authentication information for Hashicorp Vault: %s";

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--host"},
      description = "Host of the Hashicorp vault server (default: ${DEFAULT-VALUE})",
      paramLabel = HOST_FORMAT_HELP,
      arity = "1")
  private String serverHost = DEFAULT_HASHICORP_VAULT_HOST;

  @Option(
      names = {"--port"},
      description = "Port of the Hashicorp vault server (default: ${DEFAULT-VALUE})",
      paramLabel = PORT_FORMAT_HELP,
      arity = "1")
  private final Integer serverPort = DEFAULT_PORT;

  @Option(
      names = {"--timeout"},
      description =
          "Timeout in milliseconds for requests to the Hashicorp vault server (default: ${DEFAULT-VALUE})",
      paramLabel = LONG_FORMAT_HELP,
      arity = "1")
  private final Long timeout = DEFAULT_TIMEOUT;

  @RequiredOption
  @Option(
      names = {"--auth-file"},
      description = "Path to a File containing authentication data for Hashicorp vault",
      paramLabel = FILE_FORMAT_HELP,
      arity = "1")
  private final Path authFilePath = null;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--signing-key-path"},
      description =
          "Path to a secret in the Hashicorp vault containing the private key used for signing transactions. The "
              + "key needs to be a base 64 encoded private key for ECDSA for curve secp256k1 (default: ${DEFAULT-VALUE})",
      paramLabel = "<SIGNING_KEY_PATH>",
      arity = "1")
  private String signingKeyPath = DEFAULT_KEY_PATH;

  @Option(
      names = {"--tls-enabled"},
      description = "Connect to Hashicorp Vault server using TLS (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Boolean tlsEnabled = DEFAULT_TLS_ENABLED;

  @Option(
      names = "--tls-known-server-file",
      description =
          "Path to the file containing Hashicorp Vault's host, port and self-signed certificate fingerprint",
      paramLabel = FILE_FORMAT_HELP,
      arity = "1")
  private Path tlsKnownServerFile = null;

  private Signer createSigner() throws SignerInitializationException {

    final HashicorpKeyConfig keyConfig =
        new HashicorpKeyConfig(
            new ConnectionParameters(
                serverHost,
                Optional.of(serverPort),
                Optional.ofNullable(generateTlsOptions()),
                Optional.of(timeout)),
            new KeyDefinition(signingKeyPath, Optional.empty(), readTokenFromFile(authFilePath)));

    final HashicorpSignerFactory factory = new HashicorpSignerFactory(Vertx.vertx());
    try {
      return factory.create(keyConfig);
    } finally {
      factory.shutdown();
    }
  }

  private TlsOptions generateTlsOptions() {

    if (!tlsEnabled) {
      return null;
    }

    if (tlsKnownServerFile != null) {
      return new TlsOptions(Optional.of(TrustStoreType.WHITELIST), tlsKnownServerFile, null);
    } else {
      return new TlsOptions(Optional.empty(), null, null);
    }
  }

  @Override
  protected void validateArgs() throws InitializationException {
    checkIfRequiredOptionsAreInitialized(this);
    super.validateArgs();
  }

  @Override
  public SignerProvider createSignerFactory() throws SignerInitializationException {
    return new SingleSignerProvider((createSigner()));
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public Optional<Path> getTlsKnownServerFile() {
    return Optional.ofNullable(tlsKnownServerFile);
  }

  private String readTokenFromFile(final Path path) {
    try (final Stream<String> stream = Files.lines(path)) {
      return stream
          .findFirst()
          .orElseThrow(
              () ->
                  new SignerInitializationException(
                      String.format(AUTH_FILE_ERROR_MSG_FMT, path.toString())));
    } catch (final IOException e) {
      throw new SignerInitializationException(
          String.format(AUTH_FILE_ERROR_MSG_FMT, path.toString()));
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serverHost", serverHost)
        .add("serverPort", serverPort)
        .add("authFilePath", authFilePath)
        .add("timeout", timeout)
        .add("signingKeyPath", signingKeyPath)
        .add("tlsEnabled", tlsEnabled)
        .add("tlsKnownServerFile", tlsKnownServerFile)
        .toString();
  }
}
