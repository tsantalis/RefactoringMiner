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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.CmdlineHelpers.removeFieldsFrom;

import tech.pegasys.ethsigner.CommandlineParser;

import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CommandLineParserAssertions {
  public static void parseCommandLineWithMissingParamsShowsError(
      final CommandlineParser parser,
      final Writer outputWriter,
      final Writer errorWriter,
      final String defaultUsageText,
      final List<String> inputCmdLine,
      final List<String> paramsToRemove) {
    final List<String> cmdLine =
        removeFieldsFrom(inputCmdLine, paramsToRemove.stream().toArray(String[]::new));
    final boolean result = parser.parseCommandLine(cmdLine.toArray(String[]::new));
    assertThat(result).as("Parse Results After Removing Params").isFalse();

    final String output = errorWriter.toString();
    final String patternStart = "(.*)Missing required (argument|option)(.*)(";
    final String patternMiddle =
        paramsToRemove.stream().map(s -> "(.*)--" + s + "(.*)").collect(Collectors.joining("|"));
    final String patternEnd = ")(.*)\\s";

    boolean isMatched =
        Pattern.compile(patternStart + patternMiddle + patternEnd).matcher(output).find();
    assertThat(isMatched).isTrue();

    assertThat(outputWriter.toString()).containsOnlyOnce(defaultUsageText);
  }
}
