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
package tech.pegasys.ethsigner.config.tls.client;

import static tech.pegasys.ethsigner.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;

import java.nio.file.Path;
import java.util.Optional;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class PicoCliClientTlsOptions implements ClientTlsOptions {
  @SuppressWarnings("UnusedVariable")
  @Option(
      names = "--downstream-http-tls-enabled",
      description = "Flag to enable TLS connection to web3 provider. Defaults to disabled.",
      arity = "0",
      required = true)
  private boolean tlsEnabled = false;

  @ArgGroup(exclusive = false)
  private PicoCliKeyStoreOptions keyStoreOptions;

  @Option(
      names = "--downstream-http-tls-known-servers-file",
      description =
          "Path to a file containing the hostname, port and certificate fingerprints of web3 providers to trust. Must be specified if CA auth is disabled.",
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      arity = "1")
  private Path knownServersFile;

  @Option(
      names = "--downstream-http-tls-ca-auth-enabled",
      description =
          "If set, will use the system's CA to validate received server certificates. Defaults to enabled.",
      arity = "1")
  private boolean caAuthEnabled = true;

  @Override
  public Optional<KeyStoreOptions> getKeyStoreOptions() {
    return Optional.ofNullable(keyStoreOptions);
  }

  @Override
  public boolean isCaAuthEnabled() {
    return caAuthEnabled;
  }

  @Override
  public Optional<Path> getKnownServersFile() {
    return Optional.ofNullable(knownServersFile);
  }
}
