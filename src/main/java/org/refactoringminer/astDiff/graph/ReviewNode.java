package org.refactoringminer.astDiff.graph;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;

public interface ReviewNode {
  String getPromptId();
  boolean overlapLine(String path, String side, int line, @Nullable Integer startLine);
  JsonObject stringify();
}
