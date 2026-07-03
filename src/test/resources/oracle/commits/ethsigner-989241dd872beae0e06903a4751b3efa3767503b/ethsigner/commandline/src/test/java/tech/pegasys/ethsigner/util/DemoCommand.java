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

import tech.pegasys.ethsigner.annotations.RequiredOption;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "demo",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "demo command")
public class DemoCommand {
  @RequiredOption
  @Option(
      names = {"--x", "-x"},
      description = "x")
  Integer x = null;

  @RequiredOption
  @Option(
      names = {"-y", "--y"},
      description = "y")
  Integer y = null;

  @Option(
      names = {"-z", "--z"},
      description = "z")
  Integer z = null;
}
