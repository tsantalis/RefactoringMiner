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

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

public class CmdlineHelpers {

  public static List<String> validBaseCommandOptions() {
    return Lists.newArrayList(
        "--downstream-http-host=8.8.8.8",
        "--downstream-http-port=5000",
        "--downstream-http-path=/v3/projectid",
        "--downstream-http-request-timeout=10000",
        "--http-listen-port=5001",
        "--http-listen-host=localhost",
        "--chain-id=6",
        "--logging=INFO",
        "--tls-keystore-file=./keystore.pfx",
        "--tls-keystore-password-file=./keystore.passwd",
        "--tls-known-clients-file=./known_clients",
        "--tls-allow-ca-clients",
        "--downstream-http-tls-enabled",
        "--downstream-http-tls-keystore-file=./test.ks",
        "--downstream-http-tls-keystore-password-file=./test.pass",
        "--downstream-http-tls-ca-auth-enabled=false",
        "--downstream-http-tls-known-servers-file=./test.txt");
  }

  public static List<String> removeFieldsFrom(
      final List<String> cmdlineArgs, final String... fieldNames) {
    final List<String> fieldsToRemove = Lists.newArrayList(fieldNames);
    return cmdlineArgs.stream()
        .filter(arg -> fieldsToRemove.stream().noneMatch(arg::contains))
        .collect(Collectors.toList());
  }

  public static List<String> modifyField(
      List<String> cmdlineArgs, final String fieldName, final String value) {
    final String replacement = "--" + fieldName + "=" + value;
    return cmdlineArgs.stream()
        .map(arg -> arg.contains(fieldName) ? replacement : arg)
        .collect(Collectors.toList());
  }
}
