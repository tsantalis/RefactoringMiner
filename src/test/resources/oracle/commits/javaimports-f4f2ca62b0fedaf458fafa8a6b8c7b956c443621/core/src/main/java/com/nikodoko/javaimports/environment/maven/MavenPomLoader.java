package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.DefaultModelReader;

public class MavenPomLoader {
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

  Result load(Path pom) {
    tryToScan(pom);
    return result;
  }

  private void tryToScan(Path pom) {
    try {
      scan(pom);
    } catch (IOException e) {
      result.errors.add(
          new MavenEnvironmentException(String.format("could not scan pom %s", pom), e));
    }
  }

  private void scan(Path pom) throws IOException {
    var model = new DefaultModelReader().read(pom.toFile(), null);
    var dependencies = model.getDependencies();
    dependencies.stream().forEach(this::addDependency);
  }

  private void addDependency(Dependency dependency) {
    result.dependencies.add(
        new MavenDependency(
            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
  }
}
