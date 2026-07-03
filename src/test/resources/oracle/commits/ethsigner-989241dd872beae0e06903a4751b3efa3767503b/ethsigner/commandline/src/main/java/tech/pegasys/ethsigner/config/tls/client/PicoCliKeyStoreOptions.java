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

import static tech.pegasys.ethsigner.DefaultCommandValues.FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;

import java.nio.file.Path;

import picocli.CommandLine.Option;

class PicoCliKeyStoreOptions implements KeyStoreOptions {

  @Option(
      names = "--downstream-http-tls-keystore-file",
      description =
          "Path to a PKCS#12 formatted keystore containing the key and certificate "
              + "to present to a TLS-enabled web3 provider that requires client authentication.",
      arity = "1",
      paramLabel = FILE_FORMAT_HELP)
  private Path keyStoreFile;

  @Option(
      names = "--downstream-http-tls-keystore-password-file",
      description = "Path to a file containing the password used to decrypt the keystore.",
      arity = "1",
      paramLabel = FILE_FORMAT_HELP)
  private Path passwordFile;

  @Override
  public Path getKeyStoreFile() {
    return keyStoreFile;
  }

  @Override
  public Path getPasswordFile() {
    return passwordFile;
  }
}
