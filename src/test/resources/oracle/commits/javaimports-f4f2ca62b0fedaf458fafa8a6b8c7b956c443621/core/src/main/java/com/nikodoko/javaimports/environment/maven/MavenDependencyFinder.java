package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Finds all dependencies in a Maven project by parsing POM files. */
// TODO: iterate over parent poms if the dependencies do not have a set version
// TODO: handle dependency management
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

  private static final Path POM = Paths.get("pom.xml");
  private Result result = new Result();

  Result findAll(Path moduleRoot) {
    var loaded = new MavenPomLoader().load(moduleRoot.resolve(POM));
    result.dependencies.addAll(loaded.dependencies);
    result.errors.addAll(loaded.errors);

    return result;
  }
}
