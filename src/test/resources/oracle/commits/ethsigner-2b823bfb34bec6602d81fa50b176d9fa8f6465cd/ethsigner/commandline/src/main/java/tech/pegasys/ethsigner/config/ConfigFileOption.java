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
package tech.pegasys.ethsigner.config;

import java.io.File;

import picocli.CommandLine.Option;

public class ConfigFileOption {
  @Option(
      names = "--config-file",
      description = "Config file in toml format (default: none)",
      defaultValue = "${env:ETHSIGNER_CONFIG_FILE}",
      hidden = true // TODO: Unhide in acceptance test PR
      )
  private File configFile = null;

  public File getConfigFile() {
    return configFile;
  }
}
