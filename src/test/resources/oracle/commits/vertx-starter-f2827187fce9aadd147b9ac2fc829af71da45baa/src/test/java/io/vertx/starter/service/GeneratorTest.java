/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.starter.service;

import com.julienviet.childprocess.Process;
import com.julienviet.childprocess.ProcessOptions;
import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.starter.GeneratorVerticle;
import io.vertx.starter.Util;
import io.vertx.starter.VertxProjectCodec;
import io.vertx.starter.config.Topics;
import io.vertx.starter.model.BuildTool;
import io.vertx.starter.model.JdkVersion;
import io.vertx.starter.model.Language;
import io.vertx.starter.model.VertxProject;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.starter.model.ArchiveFormat.TGZ;
import static io.vertx.starter.model.BuildTool.GRADLE;
import static io.vertx.starter.model.BuildTool.MAVEN;
import static io.vertx.starter.model.Language.JAVA;
import static io.vertx.starter.model.Language.KOTLIN;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Thomas Segismont
 */
@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = TimeUnit.MINUTES)
@Tag("generator")
class GeneratorTest {

  @TempDir
  static Path m2dir;
  @TempDir
  static Path mavenRepository;
  static Path settingsFile;

  @TempDir
  Path workdir;
  List<Runnable> cleanupTasks = new ArrayList<>();

  @BeforeAll
  static void beforeAll() throws Exception {
    settingsFile = m2dir.resolve("settings.xml");
    Files.copy(GeneratorTest.class.getClassLoader().getResourceAsStream("settings-test.xml"), settingsFile);
  }

  @BeforeEach
  void beforeEach(Vertx vertx, VertxTestContext testContext) throws Exception {
    vertx.eventBus().registerDefaultCodec(VertxProject.class, new VertxProjectCodec());
    vertx.deployVerticle(new GeneratorVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @AfterEach
  void afterEach() {
    cleanupTasks.forEach(Runnable::run);
  }

  static VertxProject defaultProject() {
    return new VertxProject()
      .setId("demo")
      .setGroupId("com.example")
      .setArtifactId("demo")
      .setLanguage(JAVA)
      .setBuildTool(MAVEN)
      .setVertxVersion("4.0.3")
      .setArchiveFormat(TGZ)
      .setJdkVersion(JdkVersion.JDK_1_8);
  }

  @ParameterizedTest
  @MethodSource("testProjectsJdk8")
  @Tag("generator-8")
  void testProjectJdk8(VertxProject project, Vertx vertx, VertxTestContext testContext) {
    testProject(project, vertx, testContext);
  }

  static Stream<VertxProject> testProjectsJdk8() throws IOException {
    List<String> versions = Util.loadStarterData().getJsonArray("versions").stream()
      .map(JsonObject.class::cast)
      .map(obj -> obj.getString("number"))
      .filter(version -> !version.endsWith("-SNAPSHOT"))
      .collect(toList());

    List<Set<String>> testDeps = Stream.of("vertx-unit", "vertx-junit5")
      .map(dep -> Stream.of(dep).collect(Collectors.toSet()))
      .collect(toList());

    Stream.Builder<VertxProject> builder = Stream.builder();
    for (BuildTool buildTool : BuildTool.values()) {
      for (Language language : Language.values()) {
        for (String version : versions) {
          for (Set<String> vertxDependencies : testDeps) {
            VertxProject vertxProject = defaultProject()
              .setBuildTool(buildTool)
              .setLanguage(language)
              .setVertxVersion(version)
              .setVertxDependencies(vertxDependencies)
              .setPackageName("com.mycompany.project.special");
            builder.add(vertxProject);
          }
        }
      }
    }
    return builder.build();
  }

  @ParameterizedTest
  @MethodSource("testProjectsJdk11")
  @Tag("generator-11")
  void testProjectJdk11(VertxProject project, Vertx vertx, VertxTestContext testContext) {
    assumeThat(javaSpecVersion()).isGreaterThanOrEqualTo(11);
    testProject(project, vertx, testContext);
  }

  static Stream<VertxProject> testProjectsJdk11() throws IOException {
    return testProjectsJdk8().map(vertxProject -> vertxProject.setJdkVersion(JdkVersion.JDK_11));
  }

  @ParameterizedTest
  @MethodSource("testProjectsJdk15")
  @Tag("generator-15")
  void testProjectJdk15(VertxProject project, Vertx vertx, VertxTestContext testContext) {
    assumeThat(javaSpecVersion()).isGreaterThanOrEqualTo(15);
    testProject(project, vertx, testContext);
  }

  static Stream<VertxProject> testProjectsJdk15() throws IOException {
    return testProjectsJdk8().map(vertxProject -> vertxProject.setJdkVersion(JdkVersion.JDK_15));
  }

  private int javaSpecVersion() {
    String property = System.getProperty("java.specification.version");
    Assume.assumeNotNull(property);
    try {
      return Integer.parseInt(property);
    } catch (NumberFormatException e) {
      throw new AssumptionViolatedException("Not a number", e);
    }
  }

  private void testProject(VertxProject project, Vertx vertx, VertxTestContext testContext) {
    vertx.eventBus().<Buffer>request(Topics.PROJECT_REQUESTED, project, testContext.succeeding(msg -> {
      unpack(vertx, testContext, workdir, msg.body(), testContext.succeeding(unpacked -> {
        testContext.verify(() -> {

          verifyBaseFiles();

          BuildTool buildTool = project.getBuildTool();
          Language language = project.getLanguage();
          if (buildTool == MAVEN) {
            verifyMavenFiles();
          } else if (buildTool == GRADLE) {
            verifyGradleFiles(language);
          } else {
            testContext.failNow(new NoStackTraceThrowable(unsupported(buildTool)));
            return;
          }

          try {
            verifySourceFiles(language);
          } catch (IOException e) {
            throw new AssertionError(e);
          }

          if (Utils.isWindows()) {
            testContext.completeNow();
          } else {

            buildProject(vertx, buildTool, testContext.succeeding(projectBuilt -> {
              testContext.verify(() -> {

                if (buildTool == MAVEN) {
                  try {
                    verifyMavenOutputFiles();
                  } catch (IOException e) {
                    throw new AssertionError(e);
                  }
                } else if (buildTool == GRADLE) {
                  try {
                    verifyGradleOutputFiles();
                  } catch (IOException e) {
                    throw new AssertionError(e);
                  }
                } else {
                  testContext.failNow(new NoStackTraceThrowable(unsupported(buildTool)));
                }

                runDevMode(vertx, buildTool, testContext.succeeding(devModeRan -> testContext.completeNow()));
              });
            }));
          }
        });
      }));
    }));
  }

  private void verifyBaseFiles() {
    assertThat(workdir.resolve(".editorconfig")).isRegularFile();
    assertThat(workdir.resolve(".gitignore")).isRegularFile();
    assertThat(workdir.resolve("README.adoc")).isRegularFile();
  }

  private void verifyMavenFiles() {
    assertThat(workdir.resolve("pom.xml")).isRegularFile();
    assertThat(workdir.resolve("mvnw")).isRegularFile().isExecutable();
    assertThat(workdir.resolve("mvnw.cmd")).isRegularFile();
    assertThat(workdir.resolve(".mvn/wrapper/maven-wrapper.properties")).isRegularFile();
    assertThat(workdir.resolve(".mvn/wrapper/maven-wrapper.jar")).isRegularFile().isExecutable();
    assertThat(workdir.resolve(".mvn/wrapper/MavenWrapperDownloader.java")).isRegularFile();
  }

  private void verifyMavenOutputFiles() throws IOException {
    Optional<Path> testResult = Files.walk(workdir).filter(p -> p.toString().endsWith("TestMainVerticle.txt")).findFirst();
    assertThat(testResult).isPresent().hasValueSatisfying(p -> assertThat(p).isRegularFile());
    assertThat(workdir.resolve("target/demo-1.0.0-SNAPSHOT-fat.jar")).isRegularFile();
  }

  private void verifyGradleFiles(Language language) {
    assertThat(workdir.resolve("build.gradle.kts")).isRegularFile();
    assertThat(workdir.resolve("settings.gradle.kts")).isRegularFile();
    if (language == KOTLIN) {
      assertThat(workdir.resolve("gradle.properties")).isRegularFile();
    }
    assertThat(workdir.resolve("gradlew")).isRegularFile().isExecutable();
    assertThat(workdir.resolve("gradlew.bat")).isRegularFile();
    assertThat(workdir.resolve("gradle/wrapper/gradle-wrapper.properties")).isRegularFile();
    assertThat(workdir.resolve("gradle/wrapper/gradle-wrapper.jar")).isRegularFile();
  }

  private void verifyGradleOutputFiles() throws IOException {
    Optional<Path> testResult = Files.walk(workdir).filter(p -> p.toString().endsWith("TestMainVerticle.xml")).findFirst();
    assertThat(testResult).isPresent().hasValueSatisfying(p -> assertThat(p).isRegularFile());
    assertThat(workdir.resolve("build/libs/demo-1.0.0-SNAPSHOT-fat.jar")).isRegularFile();
  }

  private void verifySourceFiles(Language language) throws IOException {
    Optional<Path> verticleFile = Files.walk(workdir.resolve("src/main/" + language.getName()))
      .filter(p -> p.endsWith("MainVerticle" + language.getExtension()))
      .findFirst();
    assertThat(verticleFile).isPresent().hasValueSatisfying(p -> assertThat(p).isRegularFile());
    Optional<Path> testFile = Files.walk(workdir.resolve("src/test/" + language.getName()))
      .filter(p -> p.endsWith("TestMainVerticle" + language.getExtension()))
      .findFirst();
    assertThat(testFile).isPresent().hasValueSatisfying(p -> assertThat(p).isRegularFile());
  }

  private void buildProject(Vertx vertx, BuildTool buildTool, Handler<AsyncResult<Void>> handler) {
    ProcessOptions processOptions = new ProcessOptions().setCwd(workdir.toString());
    String command;
    List<String> args;
    if (buildTool == MAVEN) {
      command = "./mvnw";
      args = Stream.<String>builder()
        .add("-Dmaven.repo.local=" + mavenRepository.toAbsolutePath().toString())
        .add("-s")
        .add(settingsFile.toAbsolutePath().toString())
        .add("-B")
        .add("verify")
        .build()
        .collect(toList());
    } else if (buildTool == GRADLE) {
      command = "./gradlew";
      args = Stream.<String>builder()
        .add("--no-build-cache")
        .add("--no-daemon")
        .add("assemble")
        .add("check")
        .build()
        .collect(toList());
    } else {
      handler.handle(Future.failedFuture(unsupported(buildTool)));
      return;
    }
    Process process = Process.create(vertx, command, args, processOptions);
    cleanupTasks.add(() -> process.kill(true));
    Buffer buffer = Buffer.buffer();
    process.stdout().handler(buffer::appendBuffer);
    process.stderr().handler(buffer::appendBuffer);
    process.exitHandler(code -> {
      if (code == 0) {
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture(String.format("Failed to build project%n%s", buffer.toString())));
      }
    });
    process.start();
  }

  private void runDevMode(Vertx vertx, BuildTool buildTool, Handler<AsyncResult<Void>> handler) {
    ProcessOptions processOptions = new ProcessOptions().setCwd(workdir.toString());
    String command;
    List<String> args;
    if (buildTool == MAVEN) {
      command = "./mvnw";
      args = Arrays.asList("clean", "compile", "exec:java");
    } else if (buildTool == GRADLE) {
      command = "./gradlew";
      args = Arrays.asList("clean", "run");
    } else {
      handler.handle(Future.failedFuture(unsupported(buildTool)));
      return;
    }
    Process process = Process.create(vertx, command, args, processOptions);
    cleanupTasks.add(() -> process.kill(true));
    Promise<Void> promise = Promise.promise();
    promise.future().onComplete(handler);
    RecordParser parser = RecordParser.newDelimited("\n")
      .exceptionHandler(promise::tryFail)
      .handler(buffer -> {
        String line = buffer.toString().trim();
        if (line.contains("HTTP server started on port 8888")) {
          promise.tryComplete();
        }
      });
    process.stdout().exceptionHandler(promise::tryFail).handler(parser);
    process.start();
  }

  private void unpack(Vertx vertx, VertxTestContext testContext, Path workdir, Buffer buffer, Handler<AsyncResult<Void>> handler) {
    vertx.executeBlocking(fut -> {
      try (ByteBufInputStream bbis = new ByteBufInputStream(buffer.getByteBuf());
           TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(bbis))) {

        TarArchiveEntry entry = null;
        while ((entry = archiveInputStream.getNextTarEntry()) != null) {

          if (!archiveInputStream.canReadEntryData(entry)) {
            fut.fail("Can't read entry " + entry.getName());
            return;
          }
          File f = workdir.resolve(entry.getName()).toFile();
          if (entry.isDirectory()) {
            if (!f.isDirectory() && !f.mkdirs()) {
              fut.fail(new IOException("Failed to create directory " + f));
              return;
            }
          } else {
            File parent = f.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
              fut.fail(new IOException("Failed to create directory " + parent));
              return;
            }
            try (OutputStream outputStream = Files.newOutputStream(f.toPath())) {
              IOUtils.copy(archiveInputStream, outputStream);
              if (entry.getMode() == 0100744) {
                f.setExecutable(true);
              }
            }
          }
        }

        fut.complete();

      } catch (IOException e) {
        fut.fail(e);
      }
    }, false, handler);
  }

  private String unsupported(BuildTool buildTool) {
    return "Unsupported build tool: " + buildTool.name();
  }
}
