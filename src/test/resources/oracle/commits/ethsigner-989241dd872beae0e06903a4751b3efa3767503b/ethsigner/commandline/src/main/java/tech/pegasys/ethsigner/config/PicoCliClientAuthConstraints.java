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

import java.io.File;
import java.util.Optional;

import picocli.CommandLine.Option;

public class PicoCliClientAuthConstraints implements ClientAuthConstraints {

  @Option(
      names = "--tls-known-clients-file",
      description = "Path to a file containing the fingerprints of authorized clients.",
      paramLabel = FILE_FORMAT_HELP,
      arity = "1")
  private File tlsKnownClientsFile = null;

  @Option(
      names = "--tls-allow-ca-clients",
      description = "If defined, allows clients authorized by the CA to connect to EthSigner.",
      arity = "0..1")
  private Boolean tlsAllowCaClients = false;

  @Override
  public Optional<File> getKnownClientsFile() {
    return Optional.ofNullable(tlsKnownClientsFile);
  }

  @Override
  public boolean isCaAuthorizedClientAllowed() {
    return tlsAllowCaClients;
  }
}
