package org.refactoringminer.astDiff.graph.cluster.representation;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.*;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import javax.annotation.Nullable;
import java.util.*;

class RawRepresentation implements Representation {
  public String specification() {
    StringBuilder spec = new StringBuilder();

    spec.append("### CHANGE REPRESENTATION\n")
            .append("- The chapter is a set of <diff> and <dependency> elements.\n")
            .append("- A <diff> reads as a unified diff, but it is produced from an accurate AST differentiation between two revisions, ")
            .append("which finds edits at expression and statement granularity and reports them at line granularity.\n")
            .append("- Each <diff> presents the construct the edits sit in, not only a fixed number of lines around them, so it begins and ends where that construct does.\n")
            .append("- Each <diff> carries id=\"#XXXXX\", which identifies it uniquely within the pull request, and location=\"<file>::<Type>#<member>\", ")
            .append("which reads \"<before> -> <after>\" when the location is changed.\n")
            .append("- For code leaving some blocks and arriving in some others, the corresponding diffs are mutually linked by carrying moved_to and moved_from, ")
            .append("each having a comma-separated list of the ids of the diffs its code moved to or came from.\n")
            .append("- Each <dependency> element provides unchanged code to resolve references to identifiers in edits, and carries location=\"<file>::<Type>#<member>\".\n")
            .append("- The elements are ordered by dependency in which a <diff> appears after all <diff> elements which it builds upon, and all the <dependency> elements which it relies on. ")
            .append("A <dependency> element sits as close as possible before the <diff> elements that rely on it. A <diff> element sits as close as possible before the <diff> elements that build upon it.\n\n");

    return spec.toString();
  }

  public NarrativeElement mergeGroup(MergeGroup mergeGroup, List<TraversalPattern> leaves, @Nullable List<Node> localSides,
                                     int length, Graph<Node, Edge> graph) {
    Set<Map<Node, Set<Node>>> groups = isolateLocationContexts(MergeGroup.aggregateByContextMapping(mergeGroup.nodes.stream().toList(), graph));
    List<Map<Node, Set<Node>>> contextGroups = MergeGroup.orderContextGroups(groups, mergeGroup, leaves);

    Map<Integer, List<Node>> indexDependencies = MergeGroup.dependenciesIndex(localSides, leaves, contextGroups);

    Map<DiffNode, Integer> diffNodeBlockIndex = new LinkedHashMap<>();
    List<String> blocks = new ArrayList<>();
    for (int i = 0; i <= contextGroups.size(); i++) {
      for (Node dependency : indexDependencies.getOrDefault(i, List.of())) {
        blocks.add(dependency(dependency, graph).getContent());
      }

      if (i == contextGroups.size()) {
        break;
      }

      Map<Node, Set<Node>> contextGroup = contextGroups.get(i);

      DiffNode diffNode = buildDiffBlock(contextGroup, length, graph);
      if (diffNode == null) {
        continue;
      }

      blocks.add(diffNode.render());
      diffNodeBlockIndex.put(diffNode, blocks.size() - 1);
    }

    linkMoves(diffNodeBlockIndex, blocks, graph);

    // TODO: show group by indenting in a <sub_chapter>?
    String content = String.join("\n", blocks);
    Set<ReviewNode> reviewNodes = new HashSet<>(diffNodeBlockIndex.keySet());
    return new NarrativeElement(content, reviewNodes, mergeGroup.nodes, graph);
  }

  private static void linkMoves(Map<DiffNode, Integer> diffNodeBlockIndex, List<String> blocks, Graph<Node, Edge> graph) {
    Map<DiffNode, List<String>> movedFrom = new LinkedHashMap<>();
    Map<DiffNode, List<String>> movedTo = new LinkedHashMap<>();

    for (DiffNode subject : diffNodeBlockIndex.keySet()) {
      for (DiffNode object : diffNodeBlockIndex.keySet()) {
        if (subject == object || !movesTo(subject, object, graph)) {
          continue;
        }

        movedTo.computeIfAbsent(subject, diffNode -> new ArrayList<>()).add(object.getPromptId());
        movedFrom.computeIfAbsent(object, diffNode -> new ArrayList<>()).add(subject.getPromptId());
      }
    }

    for (Map.Entry<DiffNode, Integer> blockIndex : diffNodeBlockIndex.entrySet()) {
      DiffNode diffNode = blockIndex.getKey();
      if (!movedFrom.containsKey(diffNode) && !movedTo.containsKey(diffNode)) {
        continue;
      }

      diffNode.setMovedTo(movedTo.get(diffNode));
      diffNode.setMovedFrom(movedFrom.get(diffNode));
      blocks.set(blockIndex.getValue(), diffNode.render());
    }
  }

  private static boolean movesTo(DiffNode subject, DiffNode object, Graph<Node, Edge> graph) {
    for (Node change : subject.getSrcChanges()) {
      for (Node target : change.getMappingTargets(graph)) {
        if (object.getDstChanges().contains(target)) {
          return true;
        }
      }
    }

    return false;
  }

  private Set<Map<Node, Set<Node>>> isolateLocationContexts(Set<Map<Node, Set<Node>>> groups) {
    Set<Map<Node, Set<Node>>> isolated = new HashSet<>();
    for (Map<Node, Set<Node>> group : groups) {
      if (group.keySet().stream().noneMatch(context -> context.getNodeType().equals(NodeType.LOCATION_CONTEXT))) {
        isolated.add(group);
        continue;
      }

      for (Map.Entry<Node, Set<Node>> context : group.entrySet()) {
        for (Node change : context.getValue()) {
          Map<Node, Set<Node>> changeGroup = new HashMap<>();
          changeGroup.put(context.getKey(), Set.of(change));
          isolated.add(changeGroup);
        }
      }
    }

    return isolated;
  }

  private DiffNode buildDiffBlock(Map<Node, Set<Node>> contextGroup, int length, Graph<Node, Edge> graph) {
    DiffContexts contexts = resolveContexts(contextGroup, graph);
    Node srcContext = contexts.srcContext();
    Node dstContext = contexts.dstContext();
    Set<Node> srcChanges = contexts.srcChanges();
    Set<Node> dstChanges = contexts.dstChanges();

    DiffSide src = null, dst = null;
    if (srcContext != null) {
      src = new DiffSide(srcContext, srcChanges);
    }
    if (dstContext != null) {
      dst = new DiffSide(dstContext, dstChanges);
    }

    List<DiffNode.DiffLine> diffLines;
    String location;
    if (contexts.locationContext()) {
      // The context only says where these changes live, so nothing of it is printed but them
      diffLines = new ArrayList<>();
      if (src != null) {
        diffLines.addAll(src.changeLines());
      }
      if (dst != null) {
        diffLines.addAll(dst.changeLines());
      }

      Set<Node> located = srcChanges.isEmpty() ? dstChanges : srcChanges;
      location = located.isEmpty() ? "" : MergeGroup.sortNodes(located).get(0).getContextString();
    } else if (src != null && dst != null) {
      tagChanges(src, graph);
      tagChanges(dst, graph);

      List<int[]> anchors = buildAnchors(src, dst, mappingStores(srcContext, dstContext));
      propagateChanges(src, dst, anchors);
      diffLines = mergeSides(src, dst, meetingPoints(src, dst, anchors));
      location = diffLocation(srcContext, dstContext);
    } else {
      // One revision alone holds this context, so there is no counterpart to merge it against:
      // it is printed whole, and what its changes cover carries the marker
      DiffSide side = src != null ? src : dst;
      diffLines = side.taggedLines();
      location = side.context.getContextString();
    }

    if (diffLines.stream().noneMatch(diffLine -> diffLine.prefix() != ' ')) {
      return null;
    }

    return DiffNode.of(srcContext == null ? null : srcContext.getPath(),
            dstContext == null ? null : dstContext.getPath(),
            src == null ? List.of() : src.changes,
            dst == null ? List.of() : dst.changes,
            location, diffLines, length);
  }

  private DiffContexts resolveContexts(Map<Node, Set<Node>> contextGroup, Graph<Node, Edge> graph) {
    Node srcContext = null;
    Node dstContext = null;
    Set<Node> srcChanges = Set.of();
    Set<Node> dstChanges = Set.of();
    for (Map.Entry<Node, Set<Node>> context : contextGroup.entrySet()) {
      if (context.getKey().isSrc()) {
        srcContext = context.getKey();
        srcChanges = context.getValue();
      } else {
        dstContext = context.getKey();
        dstChanges = context.getValue();
      }
    }

    if (srcContext != null && dstContext == null) {
      List<Node> mappingTargets = srcContext.getMappingTargets(graph);
      if (!mappingTargets.isEmpty()) {
        dstContext = mappingTargets.get(0);
      }
    }
    if (srcContext == null && dstContext != null) {
      List<Node> mappingSources = dstContext.getMappingSources(graph);
      if (!mappingSources.isEmpty()) {
        srcContext = mappingSources.get(0);
      }
    }

    boolean locationContext = false;
    if ((srcContext != null && srcContext.getNodeType().equals(NodeType.LOCATION_CONTEXT))
            || (dstContext != null && dstContext.getNodeType().equals(NodeType.LOCATION_CONTEXT))) {
      locationContext = true;
    }

    return new DiffContexts(srcContext, dstContext, srcChanges, dstChanges, locationContext);
  }

  private static void tagChanges(DiffSide side, Graph<Node, Edge> graph) {
    for (Node node : graph.vertexSet()) {
      if (!node.isBase() || !node.getSrcDst().equals(side.context.getSrcDst()) || !node.getPath().equals(side.context.getPath())) {
        continue;
      }
      side.tag(node.getTree());
    }
  }

  private static List<MappingStore> mappingStores(Node srcContext, Node dstContext) {
    Set<ASTDiff> diffs = new LinkedHashSet<>();
    diffs.addAll(srcContext.getDiffs());
    diffs.addAll(dstContext.getDiffs());
    return diffs.stream().map(diff -> diff.getAllMappings().getMonoMappingStore()).toList();
  }

  private static List<int[]> buildAnchors(DiffSide src, DiffSide dst, List<MappingStore> mappingStores) {
    if (mappingStores.isEmpty()) {
      return List.of();
    }

    List<Tree> trees = new ArrayList<>();
    trees.add(src.context.getTree());
    trees.addAll(src.context.getTree().getDescendants());

    Map<Integer, Map<Integer, Integer>> votes = new HashMap<>();
    for (Tree tree : trees) {
      Tree alternative = null;
      for (MappingStore mappingStore : mappingStores) {
        alternative = mappingStore.getDstForSrc(tree);
        if (alternative != null) {
          break;
        }
      }
      if (alternative == null) {
        continue;
      }


      voteAnchor(votes, src, tree, dst, alternative);
    }

    List<int[]> anchors = new ArrayList<>();
    for (int srcLine : votes.keySet().stream().sorted().toList()) {
      int dstLine = -1;
      int dstLineVotes = -1;
      for (Map.Entry<Integer, Integer> candidate : votes.get(srcLine).entrySet()) {
        if (candidate.getValue() > dstLineVotes || (candidate.getValue() == dstLineVotes && candidate.getKey() < dstLine)) {
          dstLine = candidate.getKey();
          dstLineVotes = candidate.getValue();
        }
      }

      anchors.add(new int[]{srcLine, dstLine});
    }

    return anchors;
  }

  private static List<int[]> meetingPoints(DiffSide src, DiffSide dst, List<int[]> anchors) {
    List<int[]> meetingPoints = new ArrayList<>();

    int lastDstLine = -1;
    for (int[] anchor : anchors) {
      if (!pairs(src, anchor[0], dst, anchor[1])) {
        continue;
      }

      // A pair crossing the ones before it is a reordering; only an increasing chain keeps
      // the two walks in step
      if (anchor[1] <= lastDstLine) {
        continue;
      }

      meetingPoints.add(anchor);
      lastDstLine = anchor[1];
    }

    return meetingPoints;
  }

  private static void voteAnchor(Map<Integer, Map<Integer, Integer>> votes, DiffSide src, Tree srcTree, DiffSide dst, Tree dstTree) {
    TreeUtilFunctions.LineRange srcRange = TreeUtilFunctions.getLineRange(srcTree, src.context.getFileContent());
    TreeUtilFunctions.LineRange dstRange = TreeUtilFunctions.getLineRange(dstTree, dst.context.getFileContent());
    voteLine(votes, src, srcRange.startLine(), dst, dstRange.startLine());
    voteLine(votes, src, srcRange.endLine(), dst, dstRange.endLine());
  }

  private static void voteLine(Map<Integer, Map<Integer, Integer>> votes, DiffSide src, int srcLine, DiffSide dst, int dstLine) {
    if (!src.inWindow(srcLine) || !dst.inWindow(dstLine)) {
      return;
    }
    votes.computeIfAbsent(srcLine, line -> new HashMap<>()).merge(dstLine, 1, Integer::sum);
  }

  private static void propagateChanges(DiffSide src, DiffSide dst, List<int[]> anchors) {
    for (int[] anchor : anchors) {
      if (src.changed.contains(anchor[0])) {
        dst.changed.add(anchor[1]);
      }
      if (dst.changed.contains(anchor[1])) {
        src.changed.add(anchor[0]);
      }
    }
  }

  private static List<DiffNode.DiffLine> mergeSides(DiffSide src, DiffSide dst, List<int[]> anchors) {
    List<DiffNode.DiffLine> merged = new ArrayList<>();

    int srcLine = src.top;
    int dstLine = dst.top;
    for (int[] anchor : anchors) {
      if (anchor[0] < srcLine || anchor[1] < dstLine) {
        continue;
      }

      mergeSegment(merged, src, anchor[0] - 1, dst, anchor[1] - 1, srcLine, dstLine);
      merged.add(new DiffNode.DiffLine(' ', dst.line(anchor[1])));

      srcLine = anchor[0] + 1;
      dstLine = anchor[1] + 1;
    }
    mergeSegment(merged, src, src.bottom, dst, dst.bottom, srcLine, dstLine);

    return merged;
  }

  private static void mergeSegment(List<DiffNode.DiffLine> merged, DiffSide src, int srcTo, DiffSide dst, int dstTo,
                                   int srcFrom, int dstFrom) {
    while (srcFrom <= srcTo && dstFrom <= dstTo && pairs(src, srcFrom, dst, dstFrom)) {
      merged.add(new DiffNode.DiffLine(' ', dst.line(dstFrom)));
      srcFrom++;
      dstFrom++;
    }

    List<DiffNode.DiffLine> tail = new ArrayList<>();
    while (srcTo >= srcFrom && dstTo >= dstFrom && pairs(src, srcTo, dst, dstTo)) {
      tail.add(0, new DiffNode.DiffLine(' ', dst.line(dstTo)));
      srcTo--;
      dstTo--;
    }

    for (int line = srcFrom; line <= srcTo; line++) {
      merged.add(new DiffNode.DiffLine('-', src.line(line)));
    }
    for (int line = dstFrom; line <= dstTo; line++) {
      merged.add(new DiffNode.DiffLine('+', dst.line(line)));
    }
    merged.addAll(tail);
  }

  private static boolean pairs(DiffSide src, int srcLine, DiffSide dst, int dstLine) {
    return !src.changed.contains(srcLine) && !dst.changed.contains(dstLine)
            && src.line(srcLine).trim().equals(dst.line(dstLine).trim());
  }

  private static String diffLocation(Node srcContext, Node dstContext) {
    String srcLocation = srcContext.getContextString();
    String dstLocation = dstContext.getContextString();

    if (srcLocation.equals(dstLocation) || dstLocation.isEmpty()) {
      return srcLocation;
    }
    if (srcLocation.isEmpty()) {
      return dstLocation;
    }

    return srcLocation + " -> " + dstLocation;
  }

  private static final class DiffSide {
    private final Node context;
    private final int top;
    private final int bottom;
    private final String[] lines;
    private final List<Node> changes = new ArrayList<>();
    private final Set<Integer> changed = new HashSet<>();
    private final char prefix;

    private DiffSide(Node context, Set<Node> changeNodes) {
      this.context = context;
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(context.getTree(), context.getFileContent());
      this.top = lineRange.startLine();
      this.bottom = lineRange.endLine();
      this.prefix = context.isSrc() ? '-' : '+';
      this.lines = context.getFileContent().split("\n", -1);
      changes.addAll(MergeGroup.sortNodes(changeNodes));

      for (Node change : changes) {
        tag(change.getTree());
      }
    }

    private void tag(Tree tree) {
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, context.getFileContent());
      for (int line = Math.max(lineRange.startLine(), top); line <= Math.min(lineRange.endLine(), bottom); line++) {
        changed.add(line);
      }
    }

    private List<int[]> changeRanges() {
      List<int[]> ranges = new ArrayList<>();
      for (Node change : changes) {
        TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(change.getTree(), context.getFileContent());
        int startLine = lineRange.startLine();
        int endLine = lineRange.endLine();

        if (!ranges.isEmpty() && startLine <= ranges.get(ranges.size() - 1)[1] + 1) {
          ranges.get(ranges.size() - 1)[1] = Math.max(ranges.get(ranges.size() - 1)[1], endLine);
          continue;
        }

        ranges.add(new int[]{startLine, endLine});
      }

      return ranges;
    }

    // The context exists in one revision only, so every line of it is printed and the lines its
    // changes cover are the ones marked
    public List<DiffNode.DiffLine> taggedLines() {
      List<DiffNode.DiffLine> diffLines = new ArrayList<>();
      for (int line = top; line <= bottom; line++) {
        diffLines.add(new DiffNode.DiffLine(changed.contains(line) ? prefix : ' ', line(line)));
      }

      return diffLines;
    }

    public List<DiffNode.DiffLine> changeLines() {
      List<DiffNode.DiffLine> diffLines = new ArrayList<>();
      for (int[] range : this.changeRanges()) {
        for (int line = range[0]; line <= range[1]; line++) {
          diffLines.add(new DiffNode.DiffLine(this.prefix, this.line(line)));
        }
      }

      return diffLines;
    }

    private String line(int line) {
      return lines[line - 1];
    }

    private boolean inWindow(int line) {
      return line >= top && line <= bottom;
    }
  }

  private record DiffContexts(Node srcContext, Node dstContext, Set<Node> srcChanges, Set<Node> dstChanges,
                              boolean locationContext) {
  }

  public NarrativeElement dependency(Node dependency, Graph<Node, Edge> graph) {
    StringBuilder sb = new StringBuilder("<dependency");

    String contextString = dependency.getContextString();
    if (!contextString.isEmpty()) {
      sb.append(" location=\"").append(contextString).append("\"");
    }
    sb.append(">\n").append(dependency.dedentContent()).append("\n</dependency>");

    return new NarrativeElement(sb.toString(), new HashSet<>(), Set.of(dependency), graph);
  }
}
