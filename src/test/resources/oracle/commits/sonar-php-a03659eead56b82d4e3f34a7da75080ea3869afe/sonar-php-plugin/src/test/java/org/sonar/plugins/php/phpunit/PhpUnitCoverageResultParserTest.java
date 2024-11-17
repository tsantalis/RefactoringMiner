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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.php.MockUtils;
import org.sonar.plugins.php.api.Php;
import org.sonar.test.TestUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES;

public class PhpUnitCoverageResultParserTest {

  private static final String BASE_DIR = "/org/sonar/plugins/php/phpunit/sensor/src/";
  private static final String MONKEY_FILE_NAME = "Monkey.php";
  private static final String BANANA_FILE_NAME = "Banana.php";
  private static final File MONKEY_FILE = TestUtils.getResource(BASE_DIR + MONKEY_FILE_NAME);
  private static final File BANANA_FILE = TestUtils.getResource(BASE_DIR + BANANA_FILE_NAME);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private PhpUnitCoverageResultParser parser;

  private SensorContext context;

  private DefaultFileSystem fileSystem;

  @Before
  public void setUp() throws Exception {
    context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(org.sonar.api.resources.File.create(MONKEY_FILE_NAME));

    fileSystem = new DefaultFileSystem(TestUtils.getResource(BASE_DIR));
    DefaultInputFile monkeyFile = new DefaultInputFile("moduleKey", MONKEY_FILE_NAME)
        .setType(InputFile.Type.MAIN)
        .setLanguage(Php.KEY)
        .setLines(50);
    fileSystem.add(monkeyFile);

    parser = new PhpUnitCoverageResultParser(context, fileSystem);
  }

  @Test
  public void shouldThrowAnExceptionWhenReportNotFound() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Can't read phpUnit report:");

    parser.parse(new File("notfound.txt"));
  }

  /**
   * Should parse even when there's a package node.
   */
  @Test
  public void shouldParseEvenWithPackageNode() throws Exception {
    parser.parse(getReportsWithAbsolutePath("phpunit.coverage-with-package.xml"));

    verify(context).saveMeasure(any(Resource.class), eq(LINES_TO_COVER), eq(4.0));
    verify(context).saveMeasure(any(Resource.class), eq(UNCOVERED_LINES), eq(2.0));
  }

  /**
   * Should generate coverage metrics.
   */
  @Test
  public void shouldGenerateCoverageMeasures() throws Exception {
    parser.parse(getReportsWithAbsolutePath("phpunit.coverage.xml"));

    ArgumentCaptor<Measure> measures = ArgumentCaptor.forClass(Measure.class);
    verify(context, atLeastOnce()).saveMeasure(any(Resource.class), measures.capture());

    Measure coverageLineHitsDataMeasure = getMeasure(measures, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);
    assertThat(coverageLineHitsDataMeasure).isNotNull();
    assertThat(coverageLineHitsDataMeasure.getData()).isEqualTo("34=1;35=1;38=1;40=0;45=1;46=1");

    verify(context, atLeastOnce()).saveMeasure(any(Resource.class), eq(CoreMetrics.UNCOVERED_LINES), eq(2.));
  }

  private static Measure getMeasure(ArgumentCaptor<Measure> measures, String metric) {
    for (Measure measure : measures.getAllValues()) {
      if (measure.getMetricKey().equals(metric)) {
        return measure;
      }
    }
    return null;
  }

  /**
   * SONARPLUGINS-1591
   */
  @Test
  public void shouldNotFailIfNoStatementCount() throws Exception {
    parser.parse(getReportsWithAbsolutePath("phpunit.coverage-with-no-statements-covered.xml"));
    
    verify(context).saveMeasure(any(Resource.class), eq(CoreMetrics.LINES_TO_COVER), eq(0.0d));
    verify(context).saveMeasure(any(Resource.class), eq(CoreMetrics.UNCOVERED_LINES), eq(0.0d));
  }

  /**
   * SONARPLUGINS-1675
   */
  @Test
  public void shouldNotFailIfNoLineForFileNode() throws Exception {
    parser.parse(getReportsWithAbsolutePath("phpunit.coverage-with-filenode-without-line.xml"));
    
    verify(context).saveMeasure(any(Resource.class), eq(CoreMetrics.LINES_TO_COVER), eq(0.0d));
    verify(context).saveMeasure(any(Resource.class), eq(CoreMetrics.UNCOVERED_LINES), eq(0.0d));
  }

  @Test
  public void should_set_metrics_to_ncloc_for_missing_files() throws Exception {
    when(context.getMeasure(any(Resource.class), eq(CoreMetrics.NCLOC)))
      .thenReturn(new Measure<Integer>(CoreMetrics.NCLOC, 42.));

    parser.parse(getReportsWithAbsolutePath("phpunit.coverage-empty.xml"));

    verify(context).saveMeasure(any(Resource.class), eq(CoreMetrics.LINES_TO_COVER), eq(42.0d));
    verify(context).saveMeasure(any(Resource.class), eq(CoreMetrics.UNCOVERED_LINES), eq(42.0d));
  }

  @Test
  public void should_skip_missing_files_with_no_ncloc() throws Exception {
    parser.parse(getReportsWithAbsolutePath("phpunit.coverage-empty.xml"));

    verify(context, Mockito.never()).saveMeasure(any(Resource.class), eq(CoreMetrics.LINES_TO_COVER), any(Double.class));
  }

  /**
   * Replace file name with absolute path in coverage report.
   *
   * This hack allow to have this unit test, as only absolute path
   * in report is supported.
   * */
  private File getReportsWithAbsolutePath(String reportName) throws Exception {
    File fileWIthAbsolutePaths = folder.newFile("report_with_absolute_paths.xml");

    Files.write(
      Files.toString(TestUtils.getResource(MockUtils.PHPUNIT_REPORT_DIR + reportName), Charsets.UTF_8)
        .replace("/" + MONKEY_FILE_NAME, MONKEY_FILE.getAbsolutePath())
        .replace("/" + BANANA_FILE_NAME, BANANA_FILE.getAbsolutePath()),
      fileWIthAbsolutePaths, Charsets.UTF_8);

    return fileWIthAbsolutePaths;
  }

}
