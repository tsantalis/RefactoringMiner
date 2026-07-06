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
import static tech.pegasys.ethsigner.util.RequiredOptionsUtil.checkIfRequiredOptionsAreInitialized;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.annotations.RequiredOption;
import tech.pegasys.ethsigner.core.InitializationException;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SignerProvider;
import tech.pegasys.signers.secp256k1.api.SingleSignerProvider;
import tech.pegasys.signers.secp256k1.common.SignerInitializationException;
import tech.pegasys.signers.secp256k1.filebased.FileBasedSignerFactory;

import java.nio.file.Path;

import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** File-based authentication related sub-command */
@Command(
    name = FileBasedSubCommand.COMMAND_NAME,
    description = "Sign transactions with a key stored in an encrypted V3 Keystore file.",
    mixinStandardHelpOptions = true)
public class FileBasedSubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "file-based-signer";

  public FileBasedSubCommand() {}

  @SuppressWarnings("unused") // Picocli injects reference to command spec
  @Spec
  private CommandLine.Model.CommandSpec spec;

  @RequiredOption
  @Option(
      names = {"-p", "--password-file"},
      description = "The path to a file containing the password used to decrypt the keyfile.",
      paramLabel = FILE_FORMAT_HELP,
      arity = "1")
  private Path passwordFilePath;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @RequiredOption
  @Option(
      names = {"-k", "--key-file"},
      description = "The path to a file containing the key used to sign transactions.",
      paramLabel = FILE_FORMAT_HELP,
      arity = "1")
  private Path keyFilePath;

  private Signer createSigner() throws SignerInitializationException {
    return FileBasedSignerFactory.createSigner(keyFilePath, passwordFilePath);
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("passwordFilePath", passwordFilePath)
        .add("keyFilePath", keyFilePath)
        .toString();
  }
}
