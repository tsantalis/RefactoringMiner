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
package tech.pegasys.ethsigner.subcommands;

import static tech.pegasys.ethsigner.DefaultCommandValues.PATH_FORMAT_HELP;
import static tech.pegasys.ethsigner.util.RequiredOptionsUtil.checkIfRequiredOptionsAreInitialized;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.annotations.RequiredOption;
import tech.pegasys.ethsigner.core.InitializationException;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SignerProvider;
import tech.pegasys.signers.secp256k1.api.SingleSignerProvider;
import tech.pegasys.signers.secp256k1.azure.AzureConfig;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.signers.secp256k1.common.PasswordFileUtil;
import tech.pegasys.signers.secp256k1.common.SignerInitializationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = AzureSubCommand.COMMAND_NAME,
    description = "Sign transactions using the Azure signing service.",
    mixinStandardHelpOptions = true)
public class AzureSubCommand extends SignerSubCommand {

  @RequiredOption
  @Option(
      names = {"--keyvault-name", "--key-vault-name"},
      description = "Name of the vault to access - used as the sub-domain to vault.azure.net",
      paramLabel = "<KEY_VAULT_NAME>",
      arity = "1")
  private String keyVaultName;

  @RequiredOption
  @Option(
      names = {"--key-name"},
      description = "The name of the key which is to be used",
      paramLabel = "<KEY_NAME>")
  private String keyName;

  @Option(
      names = {"--key-version"},
      description = "The version of the requested key to use; defaults to latest if unset",
      paramLabel = "<KEY_VERSION>")
  private String keyVersion = "";

  @RequiredOption
  @Option(
      names = {"--client-id"},
      description = "The ID used to authenticate with Azure key vault",
      paramLabel = "<CLIENT_ID>")
  private String clientId;

  @RequiredOption
  @Option(
      names = {"--tenant-id"},
      description = "The unique identifier of the Azure Portal instance being used",
      paramLabel = "<TENANT_ID>")
  private String tenantId;

  @RequiredOption
  @Option(
      names = {"--client-secret-path"},
      description =
          "Path to a file containing the secret used to access the vault (along with client-id)",
      paramLabel = PATH_FORMAT_HELP)
  private Path clientSecretPath;

  private static final String READ_SECRET_FILE_ERROR = "Error when reading the secret from file.";
  public static final String COMMAND_NAME = "azure-signer";

  private Signer createSigner() throws SignerInitializationException {
    final String clientSecret;
    try {
      clientSecret = PasswordFileUtil.readPasswordFromFile(clientSecretPath);
    } catch (final FileNotFoundException fnfe) {
      throw new SignerInitializationException("File not found: " + clientSecretPath);
    } catch (final IOException e) {
      throw new SignerInitializationException(READ_SECRET_FILE_ERROR, e);
    }

    final AzureConfig config =
        new AzureConfig(keyVaultName, keyName, keyVersion, clientId, clientSecret, tenantId);

    final AzureKeyVaultSignerFactory factory = new AzureKeyVaultSignerFactory();

    return factory.createSigner(config);
  }

  @Override
  protected void validateArgs() throws InitializationException {
    checkIfRequiredOptionsAreInitialized(this);
    super.validateArgs();
  }

  @Override
  public SignerProvider createSignerFactory() throws SignerInitializationException {
    return new SingleSignerProvider(createSigner());
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
