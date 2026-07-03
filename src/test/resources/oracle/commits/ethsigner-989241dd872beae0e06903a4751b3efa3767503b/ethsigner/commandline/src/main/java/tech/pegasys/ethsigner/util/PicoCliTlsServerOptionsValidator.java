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

import tech.pegasys.ethsigner.config.PicoCliTlsServerOptions;

public class PicoCliTlsServerOptionsValidator {
  private final PicoCliTlsServerOptions option;

  public PicoCliTlsServerOptionsValidator(final PicoCliTlsServerOptions option) {
    this.option = option;
  }

  /**
   * Simulate ArgGroup validation similar to PicoCli
   *
   * @return Empty String for successful validation, otherwise contains error message
   */
  public String validateCliOptions() {
    if (!option.isTlsEnabled()) {
      return validateOtherTlsOptionsAreSpecified();
    }

    return validateMissingPasswordFile() + validateClientAuthOptions();
  }

  private String validateClientAuthOptions() {
    if (allowAnyClientEnabledAndAuthConstraintsAreDefined()
        || allowAnyClientDisabledAndClientAuthOptionNotDefined()) {
      return "Missing required argument(s): expecting either --tls-allow-any-client or one of --tls-known-clients-file=<FILE>, --tls-allow-ca-clients"
          + lineSeparator();
    }
    return "";
  }

  private String validateMissingPasswordFile() {
    if (option.getKeyStorePasswordFile() == null) {
      return "Missing required argument(s): '--tls-keystore-password-file=<FILE>'"
          + lineSeparator();
    }
    return "";
  }

  private String validateOtherTlsOptionsAreSpecified() {
    if ((option.getKeyStorePasswordFile() != null)
        || (option.getClientAuthConstraintsReference().getKnownClientsFile().isPresent())) {
      return "Missing required argument(s): '--tls-keystore-file=<FILE>'" + lineSeparator();
    }
    return "";
  }

  private boolean allowAnyClientEnabledAndAuthConstraintsAreDefined() {
    return option.tlsAllowAnyClient()
        && (option.getClientAuthConstraintsReference().getKnownClientsFile().isPresent()
            || option.getClientAuthConstraintsReference().isCaAuthorizedClientAllowed());
  }

  private boolean allowAnyClientDisabledAndClientAuthOptionNotDefined() {
    return !option.tlsAllowAnyClient()
        && option.getClientAuthConstraintsReference().getKnownClientsFile().isEmpty()
        && !option.getClientAuthConstraintsReference().isCaAuthorizedClientAllowed();
  }
}
