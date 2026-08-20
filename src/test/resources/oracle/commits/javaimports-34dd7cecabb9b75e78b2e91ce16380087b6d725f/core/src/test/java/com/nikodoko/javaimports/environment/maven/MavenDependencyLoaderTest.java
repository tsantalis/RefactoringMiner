package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.nikodoko.javaimports.parser.Import;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MavenDependencyLoaderTest {
  static final URL repositoryURL = MavenDependencyLoaderTest.class.getResource("/testrepository");
  // TODO split between loader and resolver
  MavenDependencyLoader loader;
  MavenDependencyResolver resolver;

  @BeforeEach
  void setup() throws Exception {
    Path repository = Paths.get(repositoryURL.toURI());
    Path reference = repository;
    loader = new MavenDependencyLoader();
    resolver = MavenDependencyResolver.withRepository(repository);
  }

  @Test
  void testDependencyWithPlainVersionIsResolved() throws Exception {
    List<Import> expected = ImmutableList.of(new Import("App", "com.mycompany.app", false));

    List<Import> got =
        loader.load(
            resolver.resolve(new MavenDependency("com.mycompany.app", "a-dependency", "1.0")));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testJava9DependencyIsResolved() throws Exception {
    List<Import> expected = ImmutableList.of(new Import("App", "com.mycompany.app", false));

    List<Import> got =
        loader.load(
            resolver.resolve(
                new MavenDependency("com.mycompany.app", "a-java9-dependency", "1.0")));

    // TODO: this should be an exact comparison, but we don't really have java9 support for now and
    // also extract imports we shouldnt (the ones not exposed by module-info.class)
    assertThat(got).containsAtLeastElementsIn(expected);
  }

  @Test
  void testSubclassIsResolved() throws Exception {
    List<Import> expected =
        ImmutableList.of(
            new Import("Subclass", "com.mycompany.app.App", false),
            new Import("Subsubclass", "com.mycompany.app.App.Subclass", false),
            new Import("App", "com.mycompany.app", false));

    List<Import> got =
        loader.load(
            resolver.resolve(new MavenDependency("com.mycompany.app", "a-dependency", "2.0")));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testDependencyWithoutPlainVersionIsResolvedToLatest() throws Exception {
    List<Import> expected =
        ImmutableList.of(
            new Import("Subclass", "com.mycompany.app.App", false),
            new Import("Subsubclass", "com.mycompany.app.App.Subclass", false),
            new Import("App", "com.mycompany.app", false));

    List<Import> got =
        loader.load(
            resolver.resolve(new MavenDependency("com.mycompany.app", "a-dependency", null)));

    assertThat(got).containsExactlyElementsIn(expected);
  }
}
