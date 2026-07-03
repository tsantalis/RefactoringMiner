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
package tech.pegasys.ethsigner.util;

import static java.lang.System.lineSeparator;
import static tech.pegasys.ethsigner.DefaultCommandValues.FILE_FORMAT_HELP;

import tech.pegasys.ethsigner.config.tls.client.PicoCliClientTlsOptions;

import java.util.ArrayList;
import java.util.List;

public class PicoCliClientTlsOptionValidator {
  final PicoCliClientTlsOptions picoCliClientTlsOptions;

  public PicoCliClientTlsOptionValidator(final PicoCliClientTlsOptions picoCliClientTlsOptions) {
    this.picoCliClientTlsOptions = picoCliClientTlsOptions;
  }

  /**
   * Simulate ArgGroup validation similar to PicoCli
   *
   * @return Empty String for successful validation, otherwise contains error message
   */
  public String validateCliOptions() {
    if (!picoCliClientTlsOptions.isTlsEnabled()) {
      return dependentOptionSpecifiedMessage();
    }

    return keyStoreConfigurationValidationMessage() + knownServerFileAndCaValidationMessage();
  }

  private String dependentOptionSpecifiedMessage() {
    final boolean keyStoreFilePresent =
        picoCliClientTlsOptions.getKeyStoreOptions().isPresent()
            && picoCliClientTlsOptions.getKeyStoreOptions().get().getKeyStoreFile() != null;
    final boolean keyStorePasswordFilePresent =
        picoCliClientTlsOptions.getKeyStoreOptions().isPresent()
            && picoCliClientTlsOptions.getKeyStoreOptions().get().getPasswordFile() != null;
    final boolean knownServerFilePresent =
        picoCliClientTlsOptions.getKnownServersFile().isPresent();

    if (knownServerFilePresent || keyStoreFilePresent || keyStorePasswordFilePresent) {
      return "Missing required argument(s): '--downstream-http-tls-enabled=true'" + lineSeparator();
    }
    return "";
  }

  private String knownServerFileAndCaValidationMessage() {
    if (picoCliClientTlsOptions.getKnownServersFile().isEmpty()
        && !picoCliClientTlsOptions.isCaAuthEnabled()) {
      return "Missing required argument(s): '--downstream-http-tls-known-servers-file' must be specified if '--downstream-http-tls-ca-auth-enabled=false'"
          + lineSeparator();
    }
    return "";
  }

  private String keyStoreConfigurationValidationMessage() {
    final boolean keyStoreFilePresent =
        picoCliClientTlsOptions.getKeyStoreOptions().isPresent()
            && picoCliClientTlsOptions.getKeyStoreOptions().get().getKeyStoreFile() != null;
    final boolean keyStorePasswordFilePresent =
        picoCliClientTlsOptions.getKeyStoreOptions().isPresent()
            && picoCliClientTlsOptions.getKeyStoreOptions().get().getPasswordFile() != null;

    final List<String> missingOptions = new ArrayList<>();

    // ArgGroup custom validation
    if (!keyStoreFilePresent && keyStorePasswordFilePresent) {
      missingOptions.add("'--downstream-http-tls-keystore-file=" + FILE_FORMAT_HELP + "'");
    } else if (keyStoreFilePresent && !keyStorePasswordFilePresent) {
      missingOptions.add("'--downstream-http-tls-keystore-password-file=" + FILE_FORMAT_HELP + "'");
    }

    if (!missingOptions.isEmpty()) {
      return "Missing required arguments(s): "
          + String.join(", ", missingOptions)
          + lineSeparator();
    }
    return "";
  }
}
