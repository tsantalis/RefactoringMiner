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
package org.sonar.plugins.php.core;

import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.php.api.Php;
import org.sonar.squidbridge.measures.Metric;
import org.sonar.squidbridge.text.Source;
import org.sonar.test.TestUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NoSonarAndCommentedOutLocSensorTest {

  private DefaultFileSystem fs;
  private Project project;
  private NoSonarFilter noSonarFilter;
  private NoSonarAndCommentedOutLocSensor sensor;
  private SensorContext context;

  @Before
  public void setUp() throws Exception {
    fs = new DefaultFileSystem();
    project = mock(Project.class);

    context = mock(SensorContext.class);
    noSonarFilter = new NoSonarFilter();
    sensor = spy(new NoSonarAndCommentedOutLocSensor(fs, noSonarFilter));
  }

  @Test
  public void testAnalyse() {
    DefaultInputFile mainFile = new DefaultInputFile("Mail.php").setAbsolutePath(TestUtils.getResource("/Mail.php").getAbsolutePath()).setLanguage(Php.KEY).setType(InputFile.Type.MAIN);
    fs.add(mainFile);

    // TODO: remove when deprecated NoSonarFilter will be replaced.
    org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.create("Mail.php");
    when(context.getResource(any(Resource.class))).thenReturn(sonarFile);

    sensor.analyse(project, context);
    // Mail.php contains 9 commented oud code lines.
    verify(context).saveMeasure(sonarFile, CoreMetrics.COMMENTED_OUT_CODE_LINES, 9d);
  }

  @Test
  public void test_should_execute_on_project() {
    DefaultFileSystem localFS = new DefaultFileSystem();
    NoSonarAndCommentedOutLocSensor localSensor = new NoSonarAndCommentedOutLocSensor(localFS, noSonarFilter);

    // fs is empty
    assertThat(localSensor.shouldExecuteOnProject(null)).isFalse();

    localFS.add((new DefaultInputFile("file.php")).setType(InputFile.Type.MAIN).setLanguage(Php.KEY));
    assertThat(localSensor.shouldExecuteOnProject(null)).isTrue();
  }

  @Test
  public void testAnalyseEmptySourceFiles() {
    DefaultFileSystem localFS = new DefaultFileSystem();
    DefaultInputFile file = new DefaultInputFile("fake").setAbsolutePath("/fake/absolute/path").setType(InputFile.Type.MAIN).setLanguage(Php.KEY);
    localFS.add(file);

    NoSonarAndCommentedOutLocSensor localSensor = new NoSonarAndCommentedOutLocSensor(localFS, noSonarFilter);
    localSensor.analyse(project, context);
    verify(context, never()).saveMeasure(any(Resource.class), any(org.sonar.api.measures.Metric.class), any(Double.class));

  }

  @Test
  public void testAnalyseSourceCode() {
    File file = new File(this.getClass().getResource("/Mail.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file, UTF_8);
    assertEquals(1, source.getNoSonarTagLines().size());
    assertEquals(17, (int) source.getNoSonarTagLines().iterator().next());

    assertEquals(9, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test
  public void testAnalyseSourceCodeWithRegions() {
    File file = new File(this.getClass().getResource("/Math2.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file, UTF_8);
    assertEquals(1, source.getNoSonarTagLines().size());
    assertEquals(126, (int) source.getNoSonarTagLines().iterator().next());

    assertEquals(3, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test
  public void testAnalyseSourceCodeWithNoNoSonar() {
    File file = new File(this.getClass().getResource("/Math3.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file, UTF_8);
    assertEquals(0, source.getNoSonarTagLines().size());
    assertEquals(5, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  // TEST for SONARPLUGINS-662
  @Test
  public void testAnalyseSourceCodeWithMultiLineString() {
    File file = new File(this.getClass().getResource("/Math4.php").getPath());
    Source source = NoSonarAndCommentedOutLocSensor.analyseSourceCode(file, UTF_8);
    assertEquals(1, source.getNoSonarTagLines().size());
    assertEquals(91, (int) source.getNoSonarTagLines().iterator().next());

    assertEquals(5, source.getMeasure(Metric.COMMENTED_OUT_CODE_LINES));
  }

  @Test(expected = IllegalStateException.class)
  public void testAnalyseSourceCodeWithIOException() throws Exception {
    NoSonarAndCommentedOutLocSensor.analyseSourceCode(new File("xxx"), UTF_8);
  }
}
