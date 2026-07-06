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

import static tech.pegasys.ethsigner.DefaultCommandValues.HOST_FORMAT_HELP;

import tech.pegasys.ethsigner.SignerSubCommand;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.signers.secp256k1.api.SignerProvider;
import tech.pegasys.signers.secp256k1.api.SingleSignerProvider;
import tech.pegasys.signers.secp256k1.common.SignerInitializationException;
import tech.pegasys.signers.secp256k1.filebased.CredentialSigner;

import com.google.common.base.MoreObjects;
import org.web3j.crypto.Credentials;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Raw sub-command - DO NOT USE IN PRODUCTION, Private Key is in plain text */
@Command(
    name = RawSubCommand.COMMAND_NAME,
    description =
        "Sign transactions with an unecrypted key specified on cmdline. NOT SUITABLE FOR PRODUCTION.",
    mixinStandardHelpOptions = true,
    hidden = true)
public class RawSubCommand extends SignerSubCommand {

  static final String COMMAND_NAME = "raw-signer";

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--key"},
      description = "The private key used to generate signatures.",
      paramLabel = HOST_FORMAT_HELP,
      arity = "1")
  private String privateKey;

  private Signer createSigner() throws SignerInitializationException {
    final Credentials credentials = Credentials.create(privateKey);
    return new CredentialSigner(credentials);
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
    return MoreObjects.toStringHelper(this).add("privateKey", privateKey).toString();
  }
}
