package com.nikodoko.javaimports.environment.maven;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Encapsulates a Maven dependency. */
class MavenDependency {
  private static final Pattern parameterPattern = Pattern.compile("\\$\\{(?<parameter>\\S+)\\}");
  final String groupId;
  final String artifactId;
  final String version;

  MavenDependency(String groupId, String artifactId, String version) {
    checkNotNull(groupId, "maven dependency does not accept a null groupId");
    checkNotNull(artifactId, "maven dependency does not accept a null artifactId");
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  Path getLocation() {
    return Paths.get(groupId.replace("\\.", "/"), artifactId, version);
  }

  boolean hasPlainVersion() {
    if (version == null) {
      return false;
    }

    Matcher m = parameterPattern.matcher(version);
    return !m.matches();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof MavenDependency)) {
      return false;
    }

    MavenDependency d = (MavenDependency) o;
    return Objects.equals(d.groupId, groupId)
        && Objects.equals(d.artifactId, artifactId)
        && Objects.equals(d.version, version);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("groupId", groupId)
        .add("artifactId", artifactId)
        .add("version", version)
        .toString();
  }
}
