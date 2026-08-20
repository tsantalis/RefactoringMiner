package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

/** Finds all dependencies in a Maven project by parsing POM files. */
class MavenDependencyFinder {
  static final class Result {
    final List<MavenDependency> dependencies = new ArrayList<>();
    final List<MavenEnvironmentException> errors = new ArrayList<>();

    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("dependencies", dependencies)
          .add("errors", errors)
          .toString();
    }
  }

  private Result result = new Result();
  private boolean allFound = false;

  Result findAll(Path moduleRoot) {
    Path target = moduleRoot;
    while (!allFound && hasPom(target)) {
      tryToScanPom(target);
      target = target.getParent();
    }

    return result;
  }

  private boolean hasPom(Path directory) {
    Path pom = Paths.get(directory.toString(), "pom.xml");
    return Files.exists(pom);
  }

  private void tryToScanPom(Path directory) {
    try {
      scanPom(directory);
    } catch (IOException e) {
      result.errors.add(
          new MavenEnvironmentException(String.format("could not scan pom in %s", directory), e));
    }
  }

  private void scanPom(Path directory) throws IOException {
    File pom = Paths.get(directory.toString(), "pom.xml").toFile();
    Model model = new DefaultModelReader().read(pom, null);
    List<Dependency> deps = model.getDependencies();
    scanDependencies(deps);
  }

  private void scanDependencies(List<Dependency> deps) {
    boolean ok = true;
    for (Dependency dep : deps) {
      MavenDependency dependency =
          new MavenDependency(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
      result.dependencies.add(dependency);

      if (!dependency.hasPlainVersion()) {
        ok = false;
      }
    }

    allFound = ok;
  }
}
