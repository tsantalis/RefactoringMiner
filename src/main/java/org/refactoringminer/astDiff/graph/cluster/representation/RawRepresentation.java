package org.refactoringminer.astDiff.graph.cluster.representation;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.*;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public class RawRepresentation {
  public static String specification() {
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
            .append("- A <diff> whose code is relied by other diffs in the chapter carries serves, a comma-separated list of the ids of those relying diffs.\n")
            .append("- Each <dependency> element provides code outside the chapter's own <diff> elements to resolve references to identifiers in edits, ")
            .append("and carries location=\"<file>::<Type>#<member>\" and serves, a comma-separated list of the ids of the diffs in the chapter which rely on it.\n")
            .append("- A <dependency> whose code is changed by the pull request outside the chapter shows that change exactly as a <diff> does, but carries no id.\n")
            .append("- The elements are ordered by dependency in which a <diff> appears after all <diff> elements which it builds upon, and all the <dependency> elements which it relies on. ")
            .append("A <dependency> element sits as close as possible before the <diff> elements that rely on it. A <diff> element sits as close as possible before the <diff> elements that build upon it.\n\n");

    return spec.toString();
  }

  // Every change of the given elements mapped to the diff presenting it, so a unit can show a change of another unit it relies on
  public static Map<Node, DiffNode> changeDiffNodes(List<NarrativeElement> elements, int length) {
    Map<Node, DiffNode> changeDiffNodes = new HashMap<>();
    for (NarrativeElement element : elements) {
      for (Block block : elementBlocks(element, List.of(), length)) {
        if (block instanceof DiffBlock diffBlock) {
          putChanges(changeDiffNodes, diffBlock.diffNode());
        }
      }
    }

    return changeDiffNodes;
  }

  private static void putChanges(Map<Node, DiffNode> changeDiffNodes, DiffNode diffNode) {
    for (Node change : diffNode.getSrcChanges()) {
      changeDiffNodes.putIfAbsent(change, diffNode);
    }
    for (Node change : diffNode.getDstChanges()) {
      changeDiffNodes.putIfAbsent(change, diffNode);
    }
  }

  public static RepresentedUnit represent(List<NarrativeElement> elements, Map<Node, DiffNode> allChangeDiffNodes, int length) {
    UnitDependencies dependencies = new UnitDependencies(elements);

    List<List<Block>> elementsBlocks = new ArrayList<>();
    for (int i = 0; i < elements.size(); i++) {
      elementsBlocks.add(elementBlocks(elements.get(i), dependencies.localSidesOf(i), length));
    }

    // Dependencies name the diffs they serve, so every diff of the unit is known before printing
    List<DiffNode> diffNodes = new ArrayList<>();
    Map<Node, DiffNode> changeDiffNodes = new HashMap<>();
    Map<DiffNode, List<Node>> shownContexts = new HashMap<>();
    for (List<Block> elementBlocks : elementsBlocks) {
      for (Block block : elementBlocks) {
        if (!(block instanceof DiffBlock diffBlock)) {
          continue;
        }

        DiffNode diffNode = diffBlock.diffNode();
        diffNodes.add(diffNode);
        putChanges(changeDiffNodes, diffNode);
        shownContexts.put(diffNode, diffBlock.shownContexts());
      }
    }

    // set serve ids for diff nodes
    for (DiffNode diffNode : diffNodes) {
      List<Node> changes = new ArrayList<>();
      changes.addAll(diffNode.getSrcChanges());
      changes.addAll(diffNode.getDstChanges());

      Set<DiffNode> served = servedDiffNodes(changes, dependencies, changeDiffNodes);
      served.remove(diffNode);
      diffNode.setServes(getOrderedIds(served, diffNodes));
    }

    List<String> rendered = new ArrayList<>();
    Set<DiffNode> renderedChangeDependencies = new HashSet<>();
    for (int i = 0; i < elements.size(); i++) {
      for (Node globalSide : dependencies.globalSidesBefore(i)) {
        if (isApparent(globalSide, dependencies, changeDiffNodes, shownContexts)) {
          continue;
        }

        renderDependency(rendered, globalSide, allChangeDiffNodes, renderedChangeDependencies, dependencies, changeDiffNodes, diffNodes);
      }

      for (Block block : elementsBlocks.get(i)) {
        if (block instanceof DependencyBlock dependencyBlock) {
          if (isApparent(dependencyBlock.dependency(), dependencies, changeDiffNodes, shownContexts)) {
            continue;
          }

          renderDependency(rendered, dependencyBlock.dependency(), allChangeDiffNodes, renderedChangeDependencies, dependencies,
                  changeDiffNodes, diffNodes);
        } else if (block instanceof DiffBlock diffBlock) {
          rendered.add(diffBlock.diffNode().render());
        }
      }
    }

    // TODO: show group by indenting in a <sub_chapter>?
    return new RepresentedUnit(String.join("\n", rendered), new LinkedHashSet<>(diffNodes));
  }

  private static boolean isApparent(Node dependency, UnitDependencies dependencies, Map<Node, DiffNode> changeDiffNodes,
                                    Map<DiffNode, List<Node>> shownContexts) {
    Set<Node> relyingMains = dependencies.relyingMains(dependency);
    if (relyingMains.isEmpty()) {
      return false;
    }

    for (Node main : relyingMains) {
      DiffNode diffNode = changeDiffNodes.get(main);
      if (diffNode == null || shownContexts.get(diffNode).stream().noneMatch(context -> withinLines(dependency, context))) {
        return false;
      }
    }

    return true;
  }

  private static boolean withinLines(Node node, Node context) {
    if (!node.getSrcDst().equals(context.getSrcDst()) || !node.getPath().equals(context.getPath())) {
      return false;
    }

    TreeUtilFunctions.LineRange nodeRange = TreeUtilFunctions.getLineRange(node.getTree(), node.getFileContent());
    TreeUtilFunctions.LineRange contextRange = TreeUtilFunctions.getLineRange(context.getTree(), context.getFileContent());
    return contextRange.startLine() <= nodeRange.startLine() && nodeRange.endLine() <= contextRange.endLine();
  }

  // A dependency changed by another unit is shown as the diff presenting that change there, once for all
  // of its changes the unit relies on
  private static void renderDependency(List<String> rendered, Node dependency, Map<Node, DiffNode> allChangeDiffNodes,
                                       Set<DiffNode> renderedChangeDependencies, UnitDependencies dependencies,
                                       Map<Node, DiffNode> changeDiffNodes, List<DiffNode> diffNodes) {
    DiffNode changeDependency = allChangeDiffNodes.get(dependency);
    if (changeDependency == null) {
      rendered.add(dependency(dependency.getContextString(), servedIds(dependency, dependencies, changeDiffNodes, diffNodes),
              dependency.dedentContent()));
      return;
    }

    if (!renderedChangeDependencies.add(changeDependency)) {
      return;
    }

    List<Node> changes = new ArrayList<>();
    changes.addAll(changeDependency.getSrcChanges());
    changes.addAll(changeDependency.getDstChanges());
    List<String> servedIds = getOrderedIds(servedDiffNodes(changes, dependencies, changeDiffNodes), diffNodes);
    rendered.add(dependency(changeDependency.getLocation(), servedIds, changeDependency.renderBody()));
  }

  private static List<Block> elementBlocks(NarrativeElement element, List<Node> localSides, int length) {
    Graph<Node, Edge> graph = element.getGraph();
    Set<Node> mains = element.getMainsToSides().keySet();
    Set<Map<Node, Set<Node>>> groups = isolateLocationContexts(MergeGroup.aggregateByContextMapping(mains.stream().toList(), graph));
    List<Map<Node, Set<Node>>> contextGroups = MergeGroup.orderContextGroups(groups, mains, element.getLeaves());

    Map<Integer, List<Node>> indexDependencies = MergeGroup.dependenciesIndex(localSides, element.getMainsToSides(), contextGroups);

    List<Block> blocks = new ArrayList<>();
    List<DiffNode> diffNodes = new ArrayList<>();
    for (int i = 0; i <= contextGroups.size(); i++) {
      for (Node dependency : indexDependencies.getOrDefault(i, List.of())) {
        blocks.add(new DependencyBlock(dependency));
      }

      if (i == contextGroups.size()) {
        break;
      }

      DiffBlock diffBlock = buildDiffBlock(contextGroups.get(i), length, graph);
      if (diffBlock == null) {
        continue;
      }

      blocks.add(diffBlock);
      diffNodes.add(diffBlock.diffNode());
    }

    linkMoves(diffNodes, graph);

    return blocks;
  }

  private static List<String> servedIds(Node dependency, UnitDependencies dependencies, Map<Node, DiffNode> changeDiffNodes,
                                        List<DiffNode> diffNodes) {
    return getOrderedIds(servedDiffNodes(List.of(dependency), dependencies, changeDiffNodes), diffNodes);
  }

  private static Set<DiffNode> servedDiffNodes(Collection<Node> dependencyNodes, UnitDependencies dependencies,
                                               Map<Node, DiffNode> changeDiffNodes) {
    Set<DiffNode> served = new HashSet<>();
    for (Node dependencyNode : dependencyNodes) {
      for (Node main : dependencies.relyingMains(dependencyNode)) {
        DiffNode diffNode = changeDiffNodes.get(main);
        if (diffNode != null) {
          served.add(diffNode);
        }
      }
    }

    return served;
  }

  private static List<String> getOrderedIds(Set<DiffNode> served, List<DiffNode> diffNodes) {
    return diffNodes.stream().filter(served::contains).map(DiffNode::getPromptId).toList();
  }

  private static void linkMoves(List<DiffNode> diffNodes, Graph<Node, Edge> graph) {
    Map<DiffNode, List<String>> movedFrom = new LinkedHashMap<>();
    Map<DiffNode, List<String>> movedTo = new LinkedHashMap<>();

    for (DiffNode subject : diffNodes) {
      for (DiffNode object : diffNodes) {
        if (subject == object || !movesTo(subject, object, graph)) {
          continue;
        }

        movedTo.computeIfAbsent(subject, diffNode -> new ArrayList<>()).add(object.getPromptId());
        movedFrom.computeIfAbsent(object, diffNode -> new ArrayList<>()).add(subject.getPromptId());
      }
    }

    for (DiffNode diffNode : diffNodes) {
      if (!movedFrom.containsKey(diffNode) && !movedTo.containsKey(diffNode)) {
        continue;
      }

      diffNode.setMovedTo(movedTo.get(diffNode));
      diffNode.setMovedFrom(movedFrom.get(diffNode));
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

  private static Set<Map<Node, Set<Node>>> isolateLocationContexts(Set<Map<Node, Set<Node>>> groups) {
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

  private static DiffBlock buildDiffBlock(Map<Node, Set<Node>> contextGroup, int length, Graph<Node, Edge> graph) {
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

    DiffNode diffNode = DiffNode.of(srcContext == null ? null : srcContext.getPath(),
            dstContext == null ? null : dstContext.getPath(),
            src == null ? List.of() : src.changes,
            dst == null ? List.of() : dst.changes,
            location, diffLines, length);

    // A location context prints none of itself but the changes, so no other code is shown within it
    List<Node> shownContexts = new ArrayList<>();
    if (!contexts.locationContext()) {
      if (srcContext != null) {
        shownContexts.add(srcContext);
      }
      if (dstContext != null) {
        shownContexts.add(dstContext);
      }
    }

    return new DiffBlock(diffNode, shownContexts);
  }

  private static DiffContexts resolveContexts(Map<Node, Set<Node>> contextGroup, Graph<Node, Edge> graph) {
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

  private static String dependency(String location, List<String> servedIds, String body) {
    StringBuilder sb = new StringBuilder("<dependency");

    if (!location.isEmpty()) {
      sb.append(" location=\"").append(location).append("\"");
    }
    if (!servedIds.isEmpty()) {
      sb.append(" serves=\"").append(String.join(", ", servedIds)).append("\"");
    }
    sb.append(">\n").append(body).append("\n</dependency>");

    return sb.toString();
  }

  public record RepresentedUnit(String content, Set<ReviewNode> anchoredNodes) {
  }

  private interface Block {
  }

  private record DependencyBlock(Node dependency) implements Block {
  }

  private record DiffBlock(DiffNode diffNode, List<Node> shownContexts) implements Block {
  }
}
