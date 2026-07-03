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
package tech.pegasys.ethsigner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public class CmdlineHelpers {
  private static final String TOML_STRING_PATTERN = "%s=\"%s\"%n";
  private static final String TOML_NUMBER_PATTERN = "%s=%d%n";
  private static final String TOML_BOOLEAN_PATTERN = "%s=%b%n";

  public static Map<String, Object> baseCommandOptions() {
    final Map<String, Object> optionsMap = new LinkedHashMap<>();
    optionsMap.put("downstream-http-host", "8.8.8.8");
    optionsMap.put("downstream-http-port", 5000);
    optionsMap.put("downstream-http-path", "/v3/projectid");
    optionsMap.put("downstream-http-request-timeout", 10_000);
    optionsMap.put("http-listen-port", 5001);
    optionsMap.put("http-listen-host", "localhost");
    optionsMap.put("chain-id", 6);
    optionsMap.put("logging", "INFO");
    optionsMap.put("tls-keystore-file", "./keystore.pfx");
    optionsMap.put("tls-keystore-password-file", "./keystore.passwd");
    optionsMap.put("tls-known-clients-file", "./known_clients");
    optionsMap.put("tls-allow-ca-clients", Boolean.TRUE);
    optionsMap.put("downstream-http-tls-enabled", Boolean.TRUE);
    optionsMap.put("downstream-http-tls-keystore-file", "./test.ks");
    optionsMap.put("downstream-http-tls-keystore-password-file", "./test.pass");
    optionsMap.put("downstream-http-tls-ca-auth-enabled", Boolean.FALSE);
    optionsMap.put("downstream-http-tls-known-servers-file", "./test.txt");

    return optionsMap;
  }

  public static Map<String, Object> removeOptions(final String... optionName) {
    return removeOptions(baseCommandOptions(), optionName);
  }

  public static Map<String, Object> removeOptions(
      final Map<String, Object> options, final String... optionName) {
    Arrays.asList(optionName).forEach(options::remove);
    return options;
  }

  public static Map<String, Object> modifyOptionValue(final String optionName, final Object value) {
    return modifyOptionValue(baseCommandOptions(), optionName, value);
  }

  public static Map<String, Object> modifyOptionValue(
      final Map<String, Object> options, final String optionName, final Object value) {
    if (options.containsKey(optionName)) {
      options.replace(optionName, value);
    }
    return options;
  }

  public static List<String> toOptionsList(final Map<String, Object> options) {
    final List<String> cmdLine = new ArrayList<>();
    options.forEach((option, value) -> cmdLine.add("--" + option + "=" + value));

    return cmdLine;
  }

  public static List<String> toConfigFileOptionsList(
      final Path tempDir, final Map<String, Object> options) {
    try {
      final Path tomlFile = Files.createTempFile(tempDir, "test", ".toml");
      Files.writeString(tomlFile, toToml(options));
      return Lists.newArrayList("--config-file=" + tomlFile.toString());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String toToml(final Map<String, Object> options) {
    final StringBuilder tomlBuilder = new StringBuilder();
    options.forEach(
        (option, value) -> {
          if (value instanceof Integer) {
            tomlBuilder.append(String.format(TOML_NUMBER_PATTERN, option, (Integer) value));
          } else if (value instanceof Boolean) {
            tomlBuilder.append(String.format(TOML_BOOLEAN_PATTERN, option, value));
          } else {
            tomlBuilder.append(String.format(TOML_STRING_PATTERN, option, value));
          }
        });
    return tomlBuilder.toString();
  }

  public static Map<String, String> toEnvironmentMap(
      final String mainCommand, final String subCommand, final Map<String, Object> options) {
    final Map<String, String> envMap = new HashMap<>();
    options.forEach(
        (option, value) -> {
          envMap.put(
              envVarFriendly(mainCommand)
                  + "_"
                  + envVarFriendly(subCommand)
                  + "_"
                  + envVarFriendly(option),
              String.valueOf(value));
        });
    return envMap;
  }

  public static Map<String, String> toEnvironmentMap(
      final String mainCommand, final Map<String, Object> options) {
    final Map<String, String> envMap = new HashMap<>();
    options.forEach(
        (option, value) -> {
          envMap.put(
              envVarFriendly(mainCommand) + "_" + envVarFriendly(option), String.valueOf(value));
        });
    return envMap;
  }

  private static String envVarFriendly(final String val) {
    return val.replace("-", "_").toUpperCase();
  }
}
