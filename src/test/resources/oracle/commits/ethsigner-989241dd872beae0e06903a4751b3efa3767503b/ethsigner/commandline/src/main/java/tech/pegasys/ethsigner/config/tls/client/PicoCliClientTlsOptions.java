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

import static tech.pegasys.ethsigner.DefaultCommandValues.BOOLEAN_FORMAT_HELP;
import static tech.pegasys.ethsigner.DefaultCommandValues.FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;

import java.nio.file.Path;
import java.util.Optional;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class PicoCliClientTlsOptions implements ClientTlsOptions {
  @SuppressWarnings("UnusedVariable")
  @Option(
      names = "--downstream-http-tls-enabled",
      description = "Flag to enable TLS connection to web3 provider. Defaults to disabled.",
      paramLabel = BOOLEAN_FORMAT_HELP,
      arity = "0..1")
  private boolean tlsEnabled = false;

  @Mixin private PicoCliKeyStoreOptions keyStoreOptions;

  @Option(
      names = "--downstream-http-tls-known-servers-file",
      description =
          "Path to a file containing the hostname, port and certificate fingerprints of web3 providers to trust. Must be specified if CA auth is disabled.",
      paramLabel = FILE_FORMAT_HELP,
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
    if (keyStoreOptions.getKeyStoreFile() == null && keyStoreOptions.getPasswordFile() == null) {
      return Optional.empty();
    }
    return Optional.of(keyStoreOptions);
  }

  @Override
  public boolean isCaAuthEnabled() {
    return caAuthEnabled;
  }

  @Override
  public Optional<Path> getKnownServersFile() {
    return Optional.ofNullable(knownServersFile);
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }
}
