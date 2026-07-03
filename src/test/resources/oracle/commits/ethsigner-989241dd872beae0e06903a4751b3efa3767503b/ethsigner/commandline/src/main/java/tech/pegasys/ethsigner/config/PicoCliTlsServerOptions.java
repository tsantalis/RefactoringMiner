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
package tech.pegasys.ethsigner.config;

import static tech.pegasys.ethsigner.DefaultCommandValues.FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.core.config.ClientAuthConstraints;
import tech.pegasys.ethsigner.core.config.TlsOptions;

import java.io.File;
import java.util.Optional;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class PicoCliTlsServerOptions implements TlsOptions {

  @Option(
      names = "--tls-keystore-file",
      description =
          "Path to a PKCS#12 formatted keystore; used to enable TLS on inbound connections.",
      arity = "1",
      paramLabel = FILE_FORMAT_HELP)
  private File keyStoreFile;

  @Option(
      names = "--tls-keystore-password-file",
      description = "Path to a file containing the password used to decrypt the keystore.",
      arity = "1",
      paramLabel = FILE_FORMAT_HELP)
  private File keyStorePasswordFile;

  @Option(
      names = "--tls-allow-any-client",
      description =
          "If defined, any client may connect, regardless of presented certificate. This cannot "
              + "be set if either a whitelist or CA clients have been enabled.",
      arity = "0..1")
  private Boolean tlsAllowAnyClient = false;

  @Mixin private PicoCliClientAuthConstraints clientAuthConstraints;

  public boolean isTlsEnabled() {
    return keyStoreFile != null;
  }

  @Override
  public File getKeyStoreFile() {
    return keyStoreFile;
  }

  @Override
  public File getKeyStorePasswordFile() {
    return keyStorePasswordFile;
  }

  @Override
  public Optional<ClientAuthConstraints> getClientAuthConstraints() {
    return tlsAllowAnyClient ? Optional.empty() : Optional.of(clientAuthConstraints);
  }

  public ClientAuthConstraints getClientAuthConstraintsReference() {
    return clientAuthConstraints;
  }

  public boolean tlsAllowAnyClient() {
    return tlsAllowAnyClient;
  }
}
