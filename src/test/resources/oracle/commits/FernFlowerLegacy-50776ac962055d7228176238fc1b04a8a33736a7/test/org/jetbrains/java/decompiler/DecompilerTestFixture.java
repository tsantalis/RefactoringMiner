/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.decompiler;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class DecompilerTestFixture {
  private File testDataDir;
  private File tempDir;
  private File targetDir;
  private ConsoleDecompiler decompiler;
  public boolean cleanup = true;

  public void setUp() throws IOException {
    setUp(Collections.<String, Object>emptyMap());
  }

  public void setUp(final Map<String, Object> options) throws IOException {
    testDataDir = new File("testData");
    if (!isTestDataDir(testDataDir)) testDataDir = new File("community/plugins/java-decompiler/engine/testData");
    if (!isTestDataDir(testDataDir)) testDataDir = new File("plugins/java-decompiler/engine/testData");
    if (!isTestDataDir(testDataDir)) testDataDir = new File("../community/plugins/java-decompiler/engine/testData");
    if (!isTestDataDir(testDataDir)) testDataDir = new File("../plugins/java-decompiler/engine/testData");
    assertTrue("current dir: " + new File("").getAbsolutePath(), isTestDataDir(testDataDir));

    //noinspection SSBasedInspection
    tempDir = getRandomDir();
    if (tempDir.exists()) tempDir.delete();
    targetDir = new File(tempDir, "decompiled");
    targetDir.mkdirs();
    decompiler = new ConsoleDecompiler(this.targetDir, new HashMap<String, Object>() {{
      put(IFernflowerPreferences.LOG_LEVEL, "warn");
      put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
      put(IFernflowerPreferences.REMOVE_SYNTHETIC, "1");
      put(IFernflowerPreferences.REMOVE_BRIDGE, "1");
      put(IFernflowerPreferences.LITERALS_AS_IS, "1");
      put(IFernflowerPreferences.MAX_PROCESSING_METHOD, "60000");
      putAll(options);
    }});
  }

  protected File getRandomDir() throws IOException {
	  return File.createTempFile("decompiler_test_", "_dir");
  }

public void tearDown() {
    if (tempDir != null && cleanup) {
      delete(tempDir);
    }
  }

  public File getTestDataDir() {
    return testDataDir;
  }

  public File getTempDir() {
    return tempDir;
  }

  public File getTargetDir() {
    return targetDir;
  }

  public ConsoleDecompiler getDecompiler() {
    return decompiler;
  }

  private static boolean isTestDataDir(File dir) {
    return dir.isDirectory() && new File(dir, "classes").isDirectory() && new File(dir, "results").isDirectory();
  }

  private static void delete(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File f : files) delete(f);
      }
    }
  }
}
