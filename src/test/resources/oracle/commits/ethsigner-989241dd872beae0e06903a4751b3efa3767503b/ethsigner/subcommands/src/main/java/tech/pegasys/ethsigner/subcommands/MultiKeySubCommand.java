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

import static tech.pegasys.ethsigner.DefaultCommandValues.PATH_FORMAT_HELP;
import static tech.pegasys.ethsigner.util.RequiredOptionsUtil.checkIfRequiredOptionsAreInitialized;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.ethsigner.annotations.RequiredOption;
import tech.pegasys.ethsigner.core.InitializationException;
import tech.pegasys.signers.secp256k1.api.SignerProvider;
import tech.pegasys.signers.secp256k1.common.SignerInitializationException;
import tech.pegasys.signers.secp256k1.multikey.MultiKeySignerProvider;

import java.nio.file.Path;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Multi platform authentication related sub-command. Metadata config TOML files containing signing
 * information from one of several providers.
 */
@Command(
    name = MultiKeySubCommand.COMMAND_NAME,
    description =
        "Access multiple keys (of any supported type). Each key's "
            + "parameters are defined in a separate TOML file contained within a given "
            + "directory.",
    mixinStandardHelpOptions = true)
public class MultiKeySubCommand extends SignerSubCommand {

  public static final String COMMAND_NAME = "multikey-signer";

  public MultiKeySubCommand() {}

  @SuppressWarnings("unused") // Picocli injects reference to command spec
  @Spec
  private CommandLine.Model.CommandSpec spec;

  @RequiredOption
  @Option(
      names = {"-d", "--directory"},
      description = "The path to a directory containing signing metadata TOML files",
      paramLabel = PATH_FORMAT_HELP,
      arity = "1")
  private Path directoryPath;

  @Override
  protected void validateArgs() throws InitializationException {
    checkIfRequiredOptionsAreInitialized(this);
    super.validateArgs();
  }

  @Override
  public SignerProvider createSignerFactory() throws SignerInitializationException {
    return MultiKeySignerProvider.create(directoryPath, new EthSignerFileSelector());
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @VisibleForTesting
  Path getDirectoryPath() {
    return directoryPath;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("directoryPath", directoryPath).toString();
  }
}
