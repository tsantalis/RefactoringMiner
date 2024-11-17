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
package org.sonar.plugins.php.phpunit;

import com.thoughtworks.xstream.XStreamException;
import java.io.File;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.php.MockUtils;
import org.sonar.plugins.php.PhpPlugin;
import org.sonar.plugins.php.api.Php;
import org.sonar.test.TestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PhpUnitSensorTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private final DefaultFileSystem fs = new DefaultFileSystem();

  @Mock
  private PhpUnitResultParser parser;

  @Mock
  private PhpUnitCoverageResultParser coverageParser;

  @Mock
  private PhpUnitItCoverageResultParser itCoverageParser;
  @Mock
  private PhpUnitOverallCoverageResultParser overallCoverageParser;

  @Mock
  private SensorContext context;

  private Project project;
  private Settings settings;
  private PhpUnitSensor sensor;
  private static final File TEST_REPORT_FILE = TestUtils.getResource(MockUtils.PHPUNIT_REPORT_NAME);
  private static final File COVERAGE_REPORT_FILE = TestUtils.getResource(MockUtils.PHPUNIT_COVERAGE_REPORT);

  @Before
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);

    settings = newSettings();
    project = mock(Project.class);
    sensor = createSensor(fs, settings);
    fs.setBaseDir(TestUtils.getResource(MockUtils.PHPUNIT_REPORT_DIR));
  }

  @Test
  public void testToString() {
    assertThat(sensor.toString()).isEqualTo("PHPUnit Sensor");
  }

  @Test
  public void shouldExecuteOnProject() {
    DefaultFileSystem localFS = new DefaultFileSystem();
    PhpUnitSensor localSensor = new PhpUnitSensor(localFS, null, null, null, null, null);

    // Empty file system
    assertThat(localSensor.shouldExecuteOnProject(project)).isFalse();

    localFS.add((new DefaultInputFile("file.php").setType(InputFile.Type.MAIN).setLanguage(Php.KEY)));
    assertThat(localSensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldParserReport() {
    sensor = createSensor(new DefaultFileSystem(), settings);

    sensor.analyse(project, context);

    verify(parser, times(1)).parse(TEST_REPORT_FILE);
    verify(coverageParser, times(1)).parse(COVERAGE_REPORT_FILE);
    verify(itCoverageParser, times(1)).parse(COVERAGE_REPORT_FILE);
    verify(overallCoverageParser, times(1)).parse(COVERAGE_REPORT_FILE);
  }

  @Test
  public void noReport() {
    sensor = createSensor(new DefaultFileSystem(), new Settings());
    sensor.analyse(project, context);

    verify(parser, never()).parse(any(File.class));
  }

  @Test
  public void badReport() {
    sensor = createSensor(new DefaultFileSystem(), settings("/fake/path.xml"));
    sensor.analyse(project, context);

    verify(parser, never()).parse(any(File.class));
  }

  @Test
  public void xstream_exception() throws Exception {
    Mockito.doThrow(new XStreamException("")).when(parser).parse((File) any());
    sensor = createSensor(fs, settings("phpunit.xml"));
    expected.expect(IllegalStateException.class);
    sensor.analyse(project, context);
  }

  @Test
  public void should_parse_relative_path_report() {
    sensor = createSensor(fs, settings("phpunit.xml"));
    sensor.analyse(project, context);

    verify(parser, times(1)).parse(TEST_REPORT_FILE);
  }

  private PhpUnitSensor createSensor(FileSystem fs, Settings settings) {
    return new PhpUnitSensor(fs, settings, parser, coverageParser, itCoverageParser, overallCoverageParser);
  }

  private Settings settings(String path) {
    Properties props = new Properties();
    props.put(PhpPlugin.PHPUNIT_TESTS_REPORT_PATH_KEY, path);
    Settings settings = new Settings();
    settings.addProperties(props);
    return settings;
  }

  private static Settings newSettings() {
    Settings settings = new Settings();
    Properties props = new Properties();

    props.put(PhpPlugin.PHPUNIT_TESTS_REPORT_PATH_KEY, PhpUnitSensorTest.TEST_REPORT_FILE.getAbsolutePath());
    props.put(PhpPlugin.PHPUNIT_COVERAGE_REPORT_PATH_KEY, PhpUnitSensorTest.COVERAGE_REPORT_FILE.getAbsolutePath());
    props.put(PhpPlugin.PHPUNIT_IT_COVERAGE_REPORT_PATH_KEY, PhpUnitSensorTest.COVERAGE_REPORT_FILE.getAbsolutePath());
    props.put(PhpPlugin.PHPUNIT_OVERALL_COVERAGE_REPORT_PATH_KEY, PhpUnitSensorTest.COVERAGE_REPORT_FILE.getAbsolutePath());

    settings.addProperties(props);

    return settings;
  }

}
