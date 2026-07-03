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
package tech.pegasys.ethsigner.subcommands;

import tech.pegasys.ethsigner.CommandlineParser;
import tech.pegasys.ethsigner.EthSignerBaseCommand;
import tech.pegasys.ethsigner.SignerSubCommand;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public abstract class SubCommandTestBase {
  @TempDir protected static Path tempDir;

  protected final StringWriter commandOutput = new StringWriter();
  protected final StringWriter commandError = new StringWriter();
  protected final PrintWriter outputWriter = new PrintWriter(commandOutput, true);
  protected final PrintWriter errorWriter = new PrintWriter(commandError, true);

  protected EthSignerBaseCommand config;
  protected CommandlineParser parser;
  protected SignerSubCommand subCommand;
  protected String defaultUsageText;
  protected String subCommandUsageText;
  protected Map<String, String> environmentVariablesMap = new HashMap<>();

  @BeforeEach
  void setup() {
    subCommand = subCommand();
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter, environmentVariablesMap);
    parser.registerSigners(subCommand);

    final CommandLine commandLine = new CommandLine(new EthSignerBaseCommand());
    commandLine.addSubcommand(subCommand.getCommandName(), subCommand);
    defaultUsageText = commandLine.getUsageMessage();
    subCommandUsageText =
        commandLine.getSubcommands().get(subCommand.getCommandName()).getUsageMessage();
  }

  protected abstract SignerSubCommand subCommand();
}
