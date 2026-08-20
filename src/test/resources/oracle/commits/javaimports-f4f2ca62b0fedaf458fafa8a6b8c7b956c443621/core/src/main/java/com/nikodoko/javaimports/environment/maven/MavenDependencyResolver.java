package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Resolves Maven dependencies to their location on disk. */
class MavenDependencyResolver {
  static class PrimaryArtifact {
    final Path pom;
    final Path jar;

    PrimaryArtifact(Path pom, Path jar) {
      this.pom = pom;
      this.jar = jar;
    }

    public String toString() {
      return MoreObjects.toStringHelper(this).add("pom", pom).add("jar", jar).toString();
    }
  }

  private final Path repository;

  private MavenDependencyResolver(Path repository) {
    this.repository = repository;
  }

  static MavenDependencyResolver withRepository(Path repository) {
    return new MavenDependencyResolver(repository);
  }

  PrimaryArtifact resolve(MavenDependency dependency) throws IOException {
    var artifactPath = artifactPath(dependency);
    return new PrimaryArtifact(
        artifactPath.resolveSibling(artifactPath.getFileName() + ".pom"),
        artifactPath.resolveSibling(artifactPath.getFileName() + ".jar"));
  }

  private Path artifactPath(MavenDependency dependency) throws IOException {
    Path dependencyRepository = directoryFor(dependency);
    String version = dependency.version;
    if (!dependency.hasPlainVersion()) {
      version = getLatestAvailableVersion(dependencyRepository).name;
    }

    return Paths.get(
        dependencyRepository.toString(), version, artifactName(dependency.artifactId, version));
  }

  private Path directoryFor(MavenDependency dependency) {
    return Paths.get(
        repository.toString(), dependency.groupId.replace(".", "/"), dependency.artifactId);
  }

  private String artifactName(String artifactId, String version) {
    return String.format("%s-%s", artifactId, version);
  }

  private MavenDependencyVersion getLatestAvailableVersion(Path dependencyRepository)
      throws IOException {
    List<MavenDependencyVersion> versions =
        Files.find(dependencyRepository, 1, (path, attributes) -> Files.isDirectory(path))
            .map(p -> MavenDependencyVersion.of(p.getFileName().toString()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    return Collections.max(versions);
  }
}
