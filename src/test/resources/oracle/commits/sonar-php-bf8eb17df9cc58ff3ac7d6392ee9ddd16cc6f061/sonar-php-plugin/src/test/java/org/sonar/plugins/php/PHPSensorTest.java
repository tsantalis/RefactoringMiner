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
package org.sonar.plugins.php;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.RecognitionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.php.PHPAnalyzer;
import org.sonar.plugins.php.api.Php;
import org.sonar.plugins.php.api.tree.CompilationUnitTree;
import org.sonar.plugins.php.api.visitors.PHPCheck;
import org.sonar.plugins.php.api.visitors.PHPCustomRulesDefinition;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.test.TestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;

public class PHPSensorTest {

  private final DefaultFileSystem fileSystem = new DefaultFileSystem();

  private PHPSensor sensor;

  private ProgressReport progressReport = mock(ProgressReport.class);

  @org.junit.Rule
  public final ExpectedException thrown = ExpectedException.none();

  private final PHPCustomRulesDefinition[] CUSTOM_RULES = {new PHPCustomRulesDefinition() {
    @Override
    public String repositoryName() {
      return "custom name";
    }

    @Override
    public String repositoryKey() {
      return "customKey";
    }

    @Override
    public ImmutableList<Class> checkClasses() {
      return ImmutableList.<Class>of(MyCustomRule.class);
    }
  }};

  @Rule(
    key = "key",
    name = "name",
    description = "desc",
    tags = {"bug"})
  public class MyCustomRule extends PHPVisitorCheck {
    @RuleProperty(
      key = "customParam",
      description = "Custom parameter",
      defaultValue = "value")
    public String customParam = "value";
  }


  @Before
  public void setUp() {
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    CheckFactory checkFactory = new CheckFactory(mock(ActiveRules.class));
    sensor = new PHPSensor(mock(ResourcePerspectives.class), fileSystem, fileLinesContextFactory, checkFactory, new NoSonarFilter(), CUSTOM_RULES);
  }

  @Test
  public void shouldExecuteOnProject() {
    DefaultFileSystem localFS = new DefaultFileSystem();
    PHPSensor localSensor = new PHPSensor(mock(ResourcePerspectives.class), localFS, null, new CheckFactory(mock(ActiveRules.class)), new NoSonarFilter());

    // empty file system
    assertThat(localSensor.shouldExecuteOnProject(null), is(false));

    localFS.add(new DefaultInputFile("file.php").setType(InputFile.Type.MAIN).setLanguage(Php.KEY));
    assertThat(localSensor.shouldExecuteOnProject(null), is(true));
  }

  @Test
  public void analyse() {
    SensorContext context = mock(SensorContext.class);
    analyseSingleFile(context, "PHPSquidSensor.php");

    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.LINES), Mockito.eq(55.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.NCLOC), Mockito.eq(32.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.COMPLEXITY_IN_CLASSES), Mockito.eq(7.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.COMPLEXITY_IN_FUNCTIONS), Mockito.eq(10.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.COMMENT_LINES), Mockito.eq(7.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.COMPLEXITY), Mockito.eq(12.0));

    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.CLASSES), Mockito.eq(1.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.STATEMENTS), Mockito.eq(16.0));
    verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.FUNCTIONS), Mockito.eq(3.0));
  }

  @Test
  public void parse_error() throws Exception {
    SensorContext context = mock(SensorContext.class);
    analyseSingleFile(context, "parseError.php");
    verifyZeroInteractions(context);
  }

  private void analyseSingleFile(SensorContext context, String fileName) {
    fileSystem.add(inputFile(fileName));

    Resource resource = mock(Resource.class);
    when(resource.getEffectiveKey()).thenReturn("someKey");
    when(context.getResource(any(InputFile.class))).thenReturn(resource);
    sensor.analyse(new Project(""), context);
  }

  private InputFile inputFile(String fileName) {
    return new DefaultInputFile(fileName)
      .setAbsolutePath(TestUtils.getResource(fileName).getAbsolutePath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(Php.KEY);
  }

  @Test
  public void progress_report_should_be_stopped() throws Exception {
    PHPAnalyzer phpAnalyzer = new PHPAnalyzer(StandardCharsets.UTF_8, ImmutableList.<PHPCheck>of());
    sensor.analyseFiles(phpAnalyzer, ImmutableList.<InputFile>of(), progressReport);
    verify(progressReport).stop();
  }

  @Test
  public void exception_should_report_file_name() throws Exception {
    PHPCheck check = new ExceptionRaisingCheck(new IllegalStateException());
    analyseFileWithException(check, inputFile("PHPSquidSensor.php"), "PHPSquidSensor.php");
  }

  @Test
  public void cancelled_analysis() throws Exception {
    PHPCheck check = new ExceptionRaisingCheck(new IllegalStateException(new InterruptedException()));
    analyseFileWithException(check, inputFile("PHPSquidSensor.php"), "Analysis cancelled");
  }

  @Test
  public void cancelled_analysis_causing_recognition_exception() throws Exception {
    PHPCheck check = new ExceptionRaisingCheck(new RecognitionException(42, "message", new InterruptedIOException()));
    analyseFileWithException(check, inputFile("PHPSquidSensor.php"), "Analysis cancelled");
  }

  private void analyseFileWithException(PHPCheck check, InputFile inputFile, String expectedMessageSubstring) {
    PHPAnalyzer phpAnalyzer = new PHPAnalyzer(StandardCharsets.UTF_8, ImmutableList.of(check));
    thrown.expect(AnalysisException.class);
    thrown.expectMessage(expectedMessageSubstring);
    try {
      sensor.analyseFiles(phpAnalyzer, ImmutableList.of(inputFile), progressReport);
    } finally {
      verify(progressReport).cancel();
    }
  }

  private final class ExceptionRaisingCheck extends PHPVisitorCheck {

    private final RuntimeException exception;

    public ExceptionRaisingCheck(RuntimeException exception) {
      this.exception = exception;
    }

    @Override
    public void visitCompilationUnit(CompilationUnitTree tree) {
      throw exception;
    }
  }

}
