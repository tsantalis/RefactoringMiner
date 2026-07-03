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
import tech.pegasys.ethsigner.config.InvalidCommandLineOptionsException;
import tech.pegasys.ethsigner.core.InitializationException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import picocli.CommandLine;

public class RequiredOptionsUtil {
  public static void checkIfRequiredOptionsAreInitialized(final Object... objects) {
    final Set<String> missingOptions = new LinkedHashSet<>();

    for (Object object : objects) {
      for (Field field : getAllFields(object.getClass())) {
        field.setAccessible(true);
        if (field.isAnnotationPresent(RequiredOption.class)
            && field.isAnnotationPresent(CommandLine.Option.class)) {
          final Object value;
          try {
            value = field.get(object);
          } catch (final IllegalAccessException e) {
            throw new InitializationException("Error parsing options: " + e.getMessage(), e);
          }
          if (value == null) {
            final CommandLine.Option optionAnnotation =
                field.getDeclaredAnnotation(CommandLine.Option.class);
            final String[] names = optionAnnotation.names();
            missingOptions.add(longestName(names));
          }
        }
      }
    }

    if (!missingOptions.isEmpty()) {
      throw new InvalidCommandLineOptionsException(
          "Missing required option(s): " + String.join(",", missingOptions));
    }
  }

  static List<Field> getAllFields(final Class<?> clazz) {
    final List<Field> allFields = new ArrayList<>();
    Class<?> currentClass = clazz;
    while (currentClass != null) {
      allFields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    }
    return allFields;
  }

  static String longestName(final String[] names) {
    final String[] namesCopy = Arrays.copyOf(names, names.length);
    Arrays.sort(namesCopy, Comparator.comparingInt(String::length).reversed());
    return namesCopy[0];
  }
}
