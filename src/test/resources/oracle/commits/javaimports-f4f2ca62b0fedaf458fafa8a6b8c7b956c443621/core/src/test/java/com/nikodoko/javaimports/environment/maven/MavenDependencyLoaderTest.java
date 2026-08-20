package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.nikodoko.javaimports.parser.Import;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MavenDependencyLoaderTest {
  static final URL repositoryURL = MavenDependencyLoaderTest.class.getResource("/testrepository");
  MavenDependencyLoader loader;
  Path repository;

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
    loader = new MavenDependencyLoader();
  }

  static Stream<Arguments> jarPathProvider() {
    return Stream.of(
        Arguments.of(
            "Classes are found",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar",
            List.of(new Import("App", "com.mycompany.app", false))),
        Arguments.of(
            "Subclasses are found",
            "com/mycompany/app/a-dependency/2.0/a-dependency-2.0.jar",
            List.of(
                new Import("Subclass", "com.mycompany.app.App", false),
                new Import("Subsubclass", "com.mycompany.app.App.Subclass", false),
                new Import("App", "com.mycompany.app", false))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("jarPathProvider")
  void testDependencyLoading(String name, String jarPath, List<Import> expected) throws Exception {
    var got = loader.load(repository.resolve(jarPath));
    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testJava9DependencyIsResolved() throws Exception {
    List<Import> expected = ImmutableList.of(new Import("App", "com.mycompany.app", false));

    List<Import> got =
        loader.load(
            repository.resolve(
                "com/mycompany/app/a-java9-dependency/1.0/a-java9-dependency-1.0.jar"));

    // TODO: this should be an exact comparison, but we don't really have java9 support for now and
    // also extract imports we shouldnt (the ones not exposed by module-info.class)
    assertThat(got).containsAtLeastElementsIn(expected);
  }
}
