package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class MavenDependencyTest {
  @Test
  void testVersionlessEquals() {
    var a = new MavenDependency("com.test", "dep", "12.0");
    var b = new MavenDependency("com.test", "dep", "14.0");
    assertThat(a.hideVersion()).isEqualTo(b.hideVersion());
  }
}
