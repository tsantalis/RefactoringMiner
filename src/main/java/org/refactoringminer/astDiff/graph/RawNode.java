package org.refactoringminer.astDiff.graph;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class RawNode implements ReviewNode {

  private static final Pattern HUNK_HEADER =
      Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");

  private final String id;
  private String promptId;
  private final String path;
  private final String content;
  @Nullable
  private final LineRange srcRange;
  @Nullable
  private final LineRange dstRange;

  public RawNode(String path, String content, @Nullable LineRange srcRange,
      @Nullable LineRange dstRange) {
    this.path = path;
    this.content = content;
    this.srcRange = srcRange;
    this.dstRange = dstRange;
    this.id = formatId(path, srcRange, dstRange);
    this.promptId = Node.PROMPT_ID_PREFIX + Node.shortId(this.id, Node.PROMPT_ID_LENGTH);
  }

  public static String formatId(String path, @Nullable LineRange srcRange,
      @Nullable LineRange dstRange) {
    return String.format("%s-%s-%s", path, srcRange == null ? "" : srcRange,
        dstRange == null ? "" : dstRange);
  }

  /**
   * Splits a unified diff into single, sub-file level diffs: one {@link RawNode} per hunk. Prompt
   * ids are widened until they are unique across the returned nodes.
   */
  public static List<RawNode> parse(String diff) {
    List<RawNode> nodes = new ArrayList<>();
    if (diff == null || diff.isEmpty()) {
      return nodes;
    }

    String[] lines = diff.split("\n", -1);
    String path = null;
    int i = 0;

    while (i < lines.length) {
      String line = lines[i];

      if (line.startsWith("--- ")) {
        String srcPath = stripPathPrefix(line.substring(4));
        String dstPath = null;
        if (i + 1 < lines.length && lines[i + 1].startsWith("+++ ")) {
          dstPath = stripPathPrefix(lines[i + 1].substring(4));
          i++;
        }
        path = dstPath != null ? dstPath : srcPath;
        i++;
        continue;
      }

      Matcher header = HUNK_HEADER.matcher(line);
      if (path == null || !header.find()) {
        i++;
        continue;
      }

      int srcLine = Integer.parseInt(header.group(1));
      int srcCount = header.group(2) == null ? 1 : Integer.parseInt(header.group(2));
      int dstLine = Integer.parseInt(header.group(3));
      int dstCount = header.group(4) == null ? 1 : Integer.parseInt(header.group(4));

      List<String> body = new ArrayList<>();
      body.add(line);
      i++;

      // The counts in the header say exactly how many lines the hunk spans on each side, so they
      // delimit the hunk without having to guess where the body ends.
      int srcConsumed = 0;
      int dstConsumed = 0;
      int srcStart = -1, srcEnd = -1, dstStart = -1, dstEnd = -1;
      while (i < lines.length && (srcConsumed < srcCount || dstConsumed < dstCount)) {
        String bodyLine = lines[i];

        if (bodyLine.startsWith("\\")) { // "\ No newline at end of file"
          body.add(bodyLine);
          i++;
          continue;
        }

        if (bodyLine.startsWith("-")) {
          if (srcStart == -1) srcStart = srcLine + srcConsumed;
          srcEnd = srcLine + srcConsumed;
          srcConsumed++;
        } else if (bodyLine.startsWith("+")) {
          if (dstStart == -1) dstStart = dstLine + dstConsumed;
          dstEnd = dstLine + dstConsumed;
          dstConsumed++;
        } else if (bodyLine.startsWith(" ") || bodyLine.isEmpty()) {
          srcConsumed++;
          dstConsumed++;
        } else {
          break;
        }

        body.add(bodyLine);
        i++;
      }

      nodes.add(new RawNode(path, String.join("\n", body),
          srcStart == -1 ? null : new LineRange(srcStart, srcEnd),
          dstStart == -1 ? null : new LineRange(dstStart, dstEnd)));
    }

    assignPromptIds(nodes);
    return nodes;
  }

  private static void assignPromptIds(List<RawNode> nodes) {
    for (int length = Node.PROMPT_ID_LENGTH; length <= Node.MAX_PROMPT_ID_LENGTH; length++) {
      Set<String> promptIds = new HashSet<>();
      boolean collision = false;

      for (RawNode node : nodes) {
        node.assignPromptId(length);
        if (!promptIds.add(node.getPromptId())) {
          collision = true;
          break;
        }
      }

      if (!collision) {
        return;
      }
    }

    throw new IllegalStateException("Failed to assign unique prompt ids");
  }

  @Nullable
  private static String stripPathPrefix(String rawPath) {
    String trimmed = rawPath.trim();
    // git appends a tab-separated timestamp on some diffs
    int tab = trimmed.indexOf('\t');
    if (tab != -1) {
      trimmed = trimmed.substring(0, tab);
    }
    if (trimmed.equals("/dev/null")) {
      return null;
    }
    if (trimmed.startsWith("a/") || trimmed.startsWith("b/")) {
      return trimmed.substring(2);
    }
    return trimmed;
  }

  void assignPromptId(int length) {
    this.promptId = Node.PROMPT_ID_PREFIX + Node.shortId(this.id, length);
  }

  public String getId() {
    return id;
  }

  @Override
  public String getPromptId() {
    return promptId;
  }

  public String getContent() {
    return content;
  }

  @Override
  public boolean overlapLine(String path, String side, int line, @Nullable Integer startLine) {
    if (!this.path.equals(path)) {
      return false;
    }

    LineRange range = "LEFT".equals(side) ? srcRange : "RIGHT".equals(side) ? dstRange : null;
    if (range == null) {
      return false;
    }

    return startLine == null ? line >= range.start() && line <= range.end()
        : startLine <= range.end() && range.start() <= line;
  }

  /**
   * The block handed to the agent: the hunk verbatim, carrying the id it anchors its comments to
   * and the file it belongs to, which the hunk itself does not name. The line ranges are not
   * restated here; the `@@` header already gives them, and the anchor is the id, not the line.
   */
  public String prompt() {
    return "<diff id=\"" + promptId + "\" file=\"" + path + "\">\n" + content + "\n</diff>";
  }

  @Override
  public JsonObject stringify() {
    JsonObject nodeObj = new JsonObject();

    nodeObj.addProperty("id", this.id);
    nodeObj.addProperty("promptId", this.promptId);
    nodeObj.addProperty("path", this.path);
    nodeObj.addProperty("content", this.content);
    if (srcRange != null) {
      nodeObj.addProperty("srcStartLine", srcRange.start());
      nodeObj.addProperty("srcEndLine", srcRange.end());
    }
    if (dstRange != null) {
      nodeObj.addProperty("dstStartLine", dstRange.start());
      nodeObj.addProperty("dstEndLine", dstRange.end());
    }

    return nodeObj;
  }

  public record LineRange(int start, int end) {
    @Override
    public String toString() {
      return start + "-" + end;
    }
  }
}
