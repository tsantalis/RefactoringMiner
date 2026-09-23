package org.refactoringminer.astDiff.graph;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class DiffNode implements ReviewNode {
  private final String id;
  private final String promptId;
  @Nullable
  private final String srcPath;
  @Nullable
  private final String dstPath;
  private final List<Node> srcChanges;
  private final List<Node> dstChanges;
  private String location;
  private List<DiffNode.DiffLine> diffLines;
  private List<String> movedTo = null;
  private List<String> movedFrom = null;

  private DiffNode(String id, String promptId, @Nullable String srcPath, @Nullable String dstPath,
                   List<Node> srcChanges, List<Node> dstChanges, String location, List<DiffNode.DiffLine> diffLines) {
    this.id = id;
    this.promptId = promptId;
    this.srcPath = srcPath;
    this.dstPath = dstPath;
    this.srcChanges = srcChanges;
    this.dstChanges = dstChanges;
    this.location = location;
    this.diffLines = diffLines;
  }

  public void setMovedTo(List<String> movedTo) {
    this.movedTo = movedTo;
  }

  public void setMovedFrom(List<String> movedFrom) {
    this.movedFrom = movedFrom;
  }

  public static DiffNode of(@Nullable String srcPath, @Nullable String dstPath, List<Node> srcChanges, List<Node> dstChanges,
                            String location, List<DiffNode.DiffLine> diffLines, int length) {
    List<Node> sortedSrcChanges = srcChanges.stream().sorted(Node.COMPARATOR).toList();
    List<Node> sortedDstChanges = dstChanges.stream().sorted(Node.COMPARATOR).toList();
    String id = formatId(srcPath, dstPath, sortedSrcChanges, sortedDstChanges);

    return new DiffNode(id, Node.PROMPT_ID_PREFIX + Node.shortId(id, length), srcPath, dstPath, sortedSrcChanges, sortedDstChanges, location, diffLines);
  }

  public static String formatId(@Nullable String srcPath, @Nullable String dstPath, List<Node> srcChanges, List<Node> dstChanges) {
    return String.format("%s-%s-%s-%s", srcPath == null ? "" : srcPath, dstPath == null ? "" : dstPath, join(srcChanges), join(dstChanges));
  }

  private static String join(List<Node> changes) {
    List<String> parts = new ArrayList<>();
    for (Node change : changes) {
      parts.add(change.getTree().getPos() + ":" + change.getTree().getEndPos());
    }

    return String.join(",", parts);
  }

  public String getId() {
    return id;
  }

  @Override
  public String getPromptId() {
    return promptId;
  }

  @Nullable
  public String getSrcPath() {
    return srcPath;
  }

  @Nullable
  public String getDstPath() {
    return dstPath;
  }

  public List<Node> getSrcChanges() {
    return srcChanges;
  }

  public List<Node> getDstChanges() {
    return dstChanges;
  }

  @Override
  public boolean overlapLine(String path, String side, int line, @Nullable Integer startLine) {
    List<Node> changes;
    if ("LEFT".equals(side)) {
      if (srcPath == null || !srcPath.equals(path)) {
        return false;
      }
      changes = srcChanges;
    } else if ("RIGHT".equals(side)) {
      if (dstPath == null || !dstPath.equals(path)) {
        return false;
      }
      changes = dstChanges;
    } else {
      return false;
    }

    for (Node change : changes) {
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(change.getTree(), change.getFileContent());
      boolean overlaps = startLine == null
              ? line >= lineRange.startLine() && line <= lineRange.endLine()
              : startLine <= lineRange.endLine() && lineRange.startLine() <= line;
      if (overlaps) {
        return true;
      }
    }

    return false;
  }

  public String render() {
    StringBuilder body = new StringBuilder();
    for (DiffNode.DiffLine diffLine : this.diffLines) {
      if (!body.isEmpty()) {
        body.append("\n");
      }

      body.append(diffLine.prefix()).append(diffLine.text());
    }

    StringBuilder sb = new StringBuilder("<diff id=\"").append(this.getPromptId()).append("\"");
    if (!this.location.isEmpty()) {
      sb.append(" location=\"").append(this.location).append("\"");
    }
    if (movedFrom != null && !movedFrom.isEmpty()) {
      sb.append(" moved_from=\"").append(String.join(", ", movedFrom)).append("\"");
    }
    if (movedTo != null && !movedTo.isEmpty()) {
      sb.append(" moved_to=\"").append(String.join(", ", movedTo)).append("\"");
    }
    sb.append(">\n").append(body).append("\n</diff>");

    return sb.toString();
  }

  @Override
  public JsonObject stringify() {
    JsonObject nodeObj = new JsonObject();

    nodeObj.addProperty("id", this.id);
    nodeObj.addProperty("promptId", this.promptId);
    if (srcPath != null) {
      nodeObj.addProperty("srcPath", this.srcPath);
    }
    if (dstPath != null) {
      nodeObj.addProperty("dstPath", this.dstPath);
    }
    nodeObj.add("srcChanges", stringify(srcChanges));
    nodeObj.add("dstChanges", stringify(dstChanges));

    return nodeObj;
  }

  private static JsonArray stringify(List<Node> changes) {
    JsonArray changesArr = new JsonArray();
    for (Node change : changes) {
      JsonObject changeObj = new JsonObject();
      changeObj.addProperty("pos", change.getTree().getPos());
      changeObj.addProperty("endPos", change.getTree().getEndPos());

      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(change.getTree(), change.getFileContent());
      changeObj.addProperty("startLine", lineRange.startLine());
      changeObj.addProperty("startLineOffset", lineRange.startLineOffset());
      changeObj.addProperty("endLine", lineRange.endLine());
      changeObj.addProperty("endLineOffset", lineRange.endLineOffset());
      changeObj.addProperty("length", change.getTree().getEndPos() - change.getTree().getPos() + 1);

      changesArr.add(changeObj);
    }

    return changesArr;
  }

  public record DiffLine(char prefix, String text) {
  }
}
