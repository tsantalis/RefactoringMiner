/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/**
 *
 */
package org.sonar.plugins.php;

import java.io.File;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;

public class MockUtils {

  public static final String PHPUNIT_REPORT_DIR = "/org/sonar/plugins/php/phpunit/sensor/";
  public static final String PHPUNIT_REPORT_NAME = PHPUNIT_REPORT_DIR + "phpunit.xml";
  public static final String PHPUNIT_COVERAGE_REPORT = PHPUNIT_REPORT_DIR + "phpunit.coverage.xml";

  private MockUtils() {
  }
  
  public static DefaultFileSystem getDefaultFileSystem() {
    return new DefaultFileSystem(getModuleBaseDir());
  }

  public static File getModuleBaseDir() {
    return new File("src/test/resources");
  }

}
