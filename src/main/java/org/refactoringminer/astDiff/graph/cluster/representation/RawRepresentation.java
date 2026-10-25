package org.refactoringminer.astDiff.graph.cluster.representation;

import gr.uom.java.xmi.LocationInfo;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.*;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import javax.annotation.Nullable;
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
            .append("- A <diff> carrying moved_from whose code all came from one place shows it against the lines it left there: ")
            .append("code that arrived as it was reads as unchanged lines, and what changed on the way as removed and added lines.\n")
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
    List<DiffBlock> diffBlocks = new ArrayList<>();
    Map<Node, DiffNode> changeDiffNodes = new HashMap<>();
    Map<DiffNode, List<ShownLines>> shownContexts = new HashMap<>();
    for (List<Block> elementBlocks : elementsBlocks) {
      for (Block block : elementBlocks) {
        if (!(block instanceof DiffBlock diffBlock)) {
          continue;
        }

        DiffNode diffNode = diffBlock.diffNode();
        diffNodes.add(diffNode);
        diffBlocks.add(diffBlock);
        putChanges(changeDiffNodes, diffNode);
        shownContexts.put(diffNode, diffBlock.shownLines());
      }
    }

    // The blocks in printing order, global sides before the first element they exist in
    List<Block> ordered = new ArrayList<>();
    for (int i = 0; i < elements.size(); i++) {
      for (Node globalSide : dependencies.globalSidesBefore(i)) {
        ordered.add(new DependencyBlock(globalSide));
      }
      ordered.addAll(elementsBlocks.get(i));
    }

    // A dependency of several nodes, such as one both revisions hold as it is and changes of each rely on, is printed
    // once, where the first of its nodes is
    Map<Node, List<Node>> sameCodeNodes = sameCodeNodes(ordered, allChangeDiffNodes);

    // A dependency whose code a diff printed before already shows is not printed again; that diff serves the
    // diffs relying on it instead
    Map<DiffNode, Set<DiffNode>> showingServes = new HashMap<>();
    List<DiffBlock> printedDiffBlocks = new ArrayList<>();
    Set<Node> handledDependencies = new HashSet<>();
    Map<String, Node> printedCodes = new HashMap<>();
    List<Block> printed = new ArrayList<>();
    for (Block block : ordered) {
      if (block instanceof DiffBlock diffBlock) {
        printedDiffBlocks.add(diffBlock);
        printed.add(diffBlock);
        continue;
      }

      Node dependency = ((DependencyBlock) block).dependency();
      List<Node> dependencyNodes = sameCodeNodes.getOrDefault(dependency, List.of(dependency));
      if (!handledDependencies.add(dependencyNodes.get(0))) {
        continue;
      }

      if (dependencyNodes.stream().allMatch(node -> isApparent(node, dependencies, changeDiffNodes, shownContexts))) {
        continue;
      }

      // So is one a diff printed later shows, when that diff still reads before every diff relying on the dependency
      int firstServed = servedDiffNodes(dependencyNodes, dependencies, changeDiffNodes).stream().mapToInt(diffNodes::indexOf)
              .min().orElse(-1);
      Optional<DiffBlock> showing = diffBlocks.stream()
              .filter(diffBlock -> printedDiffBlocks.contains(diffBlock) || diffNodes.indexOf(diffBlock.diffNode()) < firstServed)
              .filter(diffBlock -> diffBlock.shownLines().stream()
                      .anyMatch(context -> dependencyNodes.stream().anyMatch(node -> withinLines(node, context))))
              .findFirst();
      if (showing.isPresent()) {
        showingServes.computeIfAbsent(showing.get().diffNode(), diffNode -> new HashSet<>())
                .addAll(servedDiffNodes(dependencyNodes, dependencies, changeDiffNodes));
        continue;
      }

      // Unchanged code printed the same at the same location reads as one dependency, such as the variables of one name
      // and type that two loops of a method declare
      if (!allChangeDiffNodes.containsKey(dependencyNodes.get(0))) {
        Node printedSame = printedCodes.putIfAbsent(printedCode(dependencyNodes.get(0)), dependencyNodes.get(0));
        if (printedSame != null) {
          List<Node> printedSameNodes = new ArrayList<>(sameCodeNodes.getOrDefault(printedSame, List.of(printedSame)));
          printedSameNodes.addAll(dependencyNodes);
          sameCodeNodes.put(printedSame, printedSameNodes);
          continue;
        }
      }

      printed.add(new DependencyBlock(dependencyNodes.get(0)));
    }

    // set serve ids for diff nodes
    for (DiffNode diffNode : diffNodes) {
      List<Node> changes = new ArrayList<>();
      changes.addAll(diffNode.getSrcChanges());
      changes.addAll(diffNode.getDstChanges());

      Set<DiffNode> served = servedDiffNodes(changes, dependencies, changeDiffNodes);
      served.addAll(showingServes.getOrDefault(diffNode, Set.of()));
      served.remove(diffNode);
      diffNode.setServes(getOrderedIds(served, diffNodes));
    }

    List<String> rendered = new ArrayList<>();
    Set<DiffNode> renderedChangeDependencies = new HashSet<>();
    for (Block block : printed) {
      if (block instanceof DependencyBlock dependencyBlock) {
        Node dependency = dependencyBlock.dependency();
        renderDependency(rendered, dependency, sameCodeNodes.getOrDefault(dependency, List.of(dependency)), allChangeDiffNodes,
                renderedChangeDependencies, dependencies, changeDiffNodes, diffNodes);
      } else if (block instanceof DiffBlock diffBlock) {
        rendered.add(diffBlock.diffNode().render());
      }
    }

    // TODO: show group by indenting in a <sub_chapter>?
    return new RepresentedUnit(String.join("\n", rendered), new LinkedHashSet<>(diffNodes));
  }

  private static String printedCode(Node dependency) {
    return dependency.getContextString() + "\n" + dependency.getDeclaration().dedentContent();
  }

  private static boolean isApparent(Node dependency, UnitDependencies dependencies, Map<Node, DiffNode> changeDiffNodes,
                                    Map<DiffNode, List<ShownLines>> shownContexts) {
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

  private static boolean withinLines(Node node, ShownLines shown) {
    if (!node.getSrcDst().equals(shown.srcDst()) || !node.getPath().equals(shown.path())) {
      return false;
    }

    TreeUtilFunctions.LineRange nodeRange = TreeUtilFunctions.getLineRange(node.getTree(), node.getFileContent());
    return shown.top() <= declarationStartLine(node) && nodeRange.endLine() <= shown.bottom();
  }

  // The doc comment and the annotations leading a declaration are not what a diff shows it by: a diff of a method body
  // shows the method from its signature on
  private static int declarationStartLine(Node node) {
    return TreeUtilFunctions.getLineRange(node.getRM().signatureStart(), node.getFileContent()).startLine();
  }

  // A body is printed from the start of the code it is the body of, as its header (a signature, a condition, a resource
  // list) cut at the line of its brace reads as broken code, unless it is printed as it is (see RMInfo#bodyFrom). An
  // expression is printed with the rest of the simple statement it is in, as a chain cut at its lines reads as broken
  // code too
  private static DiffSide contextSide(Node context, Set<Node> changes) {
    String content = context.getFileContent();
    TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(context.getTree(), content);
    RMInfo rm = context.getRM();
    if (!rm.body()) {
      Tree statement = rm.simpleStatement();
      if (statement == null) {
        return new DiffSide(context, changes);
      }

      TreeUtilFunctions.LineRange statementRange = TreeUtilFunctions.getLineRange(statement, content);
      return new DiffSide(context, changes, Math.min(statementRange.startLine(), range.startLine()),
              Math.max(statementRange.endLine(), range.endLine()));
    }
    if (rm.bodyFrom() == null) {
      return new DiffSide(context, changes);
    }

    int headerStart = TreeUtilFunctions.getLineRange(rm.bodyFrom(), content).startLine();
    return new DiffSide(context, changes, Math.min(headerStart, range.startLine()), range.endLine());
  }

  // A dependency changed by another unit is shown as the diff presenting that change there, once for all
  // of its changes the unit relies on
  private static void renderDependency(List<String> rendered, Node dependency, List<Node> dependencyNodes,
                                       Map<Node, DiffNode> allChangeDiffNodes, Set<DiffNode> renderedChangeDependencies,
                                       UnitDependencies dependencies, Map<Node, DiffNode> changeDiffNodes, List<DiffNode> diffNodes) {
    DiffNode changeDependency = allChangeDiffNodes.get(dependency);
    if (changeDependency == null) {
      rendered.add(dependency(dependency.getContextString(),
              getOrderedIds(servedDiffNodes(dependencyNodes, dependencies, changeDiffNodes), diffNodes),
              dependency.getDeclaration().dedentContent()));
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
    Set<Map<Node, Set<Node>>> groups = isolateLocationContexts(MergeGroup.aggregateByContextMapping(mains.stream().toList(), graph), graph);
    List<Map<Node, Set<Node>>> contextGroups = absorbShownChanges(MergeGroup.orderContextGroups(groups, mains, element.getLeaves()),
            length, graph);

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
    }
    blocks = mergeOverlappingDiffs(blocks, length);
    for (Block block : blocks) {
      if (block instanceof DiffBlock diffBlock) {
        diffNodes.add(diffBlock.diffNode());
      }
    }

    linkMoves(diffNodes, graph);

    return blocks;
  }

  // Two diffs printed one after the other at one location, the end of the first sharing its lines with the start of the
  // second, such as the edits of two branches of an if/else-if chain meeting on the line of the else, read as one diff
  // printing those lines once; what the second relies on is printed before both
  private static List<Block> mergeOverlappingDiffs(List<Block> blocks, int length) {
    List<Block> merged = new ArrayList<>(blocks);
    for (int i = 0; i < merged.size(); i++) {
      if (!(merged.get(i) instanceof DiffBlock first)) {
        continue;
      }

      int next = i + 1;
      while (next < merged.size() && merged.get(next) instanceof DependencyBlock) {
        next++;
      }
      if (next == merged.size() || !(merged.get(next) instanceof DiffBlock second)) {
        continue;
      }

      int shared = sharedLines(first, second);
      if (shared == 0) {
        continue;
      }

      DiffNode firstNode = first.diffNode();
      DiffNode secondNode = second.diffNode();
      List<DiffNode.DiffLine> lines = new ArrayList<>(firstNode.getDiffLines());
      lines.addAll(secondNode.getDiffLines().subList(shared, secondNode.getDiffLines().size()));
      List<Node> srcChanges = new ArrayList<>(firstNode.getSrcChanges());
      srcChanges.addAll(secondNode.getSrcChanges());
      List<Node> dstChanges = new ArrayList<>(firstNode.getDstChanges());
      dstChanges.addAll(secondNode.getDstChanges());
      List<ShownLines> shownLines = new ArrayList<>();
      for (ShownLines shown : first.shownLines()) {
        ShownLines other = second.shownLines().stream().filter(side -> side.srcDst().equals(shown.srcDst())).findFirst().orElseThrow();
        shownLines.add(new ShownLines(shown.srcDst(), shown.path(), shown.top(), other.bottom()));
      }
      DiffBlock mergedBlock = new DiffBlock(DiffNode.of(firstNode.getSrcPath(), firstNode.getDstPath(), srcChanges, dstChanges,
              firstNode.getLocation(), lines, length), shownLines);

      List<Block> dependencies = new ArrayList<>(merged.subList(i + 1, next));
      merged.subList(i, next + 1).clear();
      merged.addAll(i, dependencies);
      merged.add(i + dependencies.size(), mergedBlock);
      i = i + dependencies.size() - 1;
    }

    return merged;
  }

  // The number of lines ending the first diff that start the second one, printed unchanged by both: the two diffs show
  // the same file at the same location in both revisions, and their windows overlap by those lines on each side
  private static int sharedLines(DiffBlock first, DiffBlock second) {
    if (!first.diffNode().getLocation().equals(second.diffNode().getLocation()) || first.shownLines().size() != 2
            || second.shownLines().size() != 2) {
      return 0;
    }

    int shared = -1;
    for (ShownLines shown : first.shownLines()) {
      ShownLines other = second.shownLines().stream()
              .filter(side -> side.srcDst().equals(shown.srcDst()) && side.path().equals(shown.path())).findFirst().orElse(null);
      if (other == null || other.top() <= shown.top() || shown.bottom() < other.top() || shown.bottom() >= other.bottom()) {
        return 0;
      }
      int sideShared = shown.bottom() - other.top() + 1;
      if (shared != -1 && shared != sideShared) {
        return 0;
      }
      shared = sideShared;
    }

    List<DiffNode.DiffLine> firstLines = first.diffNode().getDiffLines();
    List<DiffNode.DiffLine> secondLines = second.diffNode().getDiffLines();
    if (shared > firstLines.size() || shared > secondLines.size()) {
      return 0;
    }
    for (int k = 0; k < shared; k++) {
      DiffNode.DiffLine ending = firstLines.get(firstLines.size() - shared + k);
      DiffNode.DiffLine starting = secondLines.get(k);
      if (ending.prefix() != ' ' || !ending.equals(starting)) {
        return 0;
      }
    }
    return shared;
  }

  // A diff prints as changed every change of its element within its lines, so the changes of a group within the lines
  // another diff shows, such as a parameter renamed in the signature a diff of the body starts with, are presented by
  // that diff rather than printed again by one of their own
  private static List<Map<Node, Set<Node>>> absorbShownChanges(List<Map<Node, Set<Node>>> contextGroups, int length,
                                                              Graph<Node, Edge> graph) {
    List<Map<Node, Set<Node>>> groups = new ArrayList<>(contextGroups);
    boolean absorbed = true;
    while (absorbed) {
      absorbed = false;
      List<DiffBlock> blocks = groups.stream().map(group -> buildDiffBlock(group, length, graph)).toList();
      for (int i = 0; i < groups.size() && !absorbed; i++) {
        DiffBlock showing = blocks.get(i);
        if (showing == null || showing.shownLines().isEmpty()) {
          continue;
        }

        for (int j = 0; j < groups.size(); j++) {
          Set<Node> changes = MergeGroup.changesOf(groups.get(j));
          if (i == j || changes.isEmpty() || !changes.stream().allMatch(change -> showing.shownLines().stream()
                  .anyMatch(shown -> changeWithinLines(change, shown)))) {
            continue;
          }

          groups.set(i, absorb(groups.get(i), changes, graph));
          groups.remove(j);
          absorbed = true;
          break;
        }
      }
    }

    return groups;
  }

  private static boolean changeWithinLines(Node change, ShownLines shown) {
    if (!change.getSrcDst().equals(shown.srcDst()) || !change.getPath().equals(shown.path())) {
      return false;
    }

    TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(change.getTree(), change.getFileContent());
    return shown.top() <= range.startLine() && range.endLine() <= shown.bottom();
  }

  private static Map<Node, Set<Node>> absorb(Map<Node, Set<Node>> contextGroup, Set<Node> changes, Graph<Node, Edge> graph) {
    Map<Node, Set<Node>> absorbing = new HashMap<>();
    contextGroup.forEach((context, contextChanges) -> absorbing.put(context, new HashSet<>(contextChanges)));

    DiffContexts contexts = resolveContexts(contextGroup, graph);
    for (Node change : changes) {
      absorbing.computeIfAbsent(change.isSrc() ? contexts.srcContext() : contexts.dstContext(), context -> new HashSet<>()).add(change);
    }

    return absorbing;
  }

  // A file both revisions hold as it is has no diff to map its code with, so the same code is at the same place in both
  private static boolean isRevisionTwin(Node srcDependency, Node alternative, Node dstDependency) {
    if (!dstDependency.getContent().equals(srcDependency.getContent())) {
      return false;
    }

    if (alternative != null) {
      return dstDependency.getTree() == alternative.getTree();
    }

    return srcDependency.getPath().equals(dstDependency.getPath())
            && srcDependency.getFileContent().equals(dstDependency.getFileContent())
            && srcDependency.getTree().getPos() == dstDependency.getTree().getPos()
            && srcDependency.getTree().getEndPos() == dstDependency.getTree().getEndPos();
  }

  // The nodes printed as one dependency, the first in printing order first: the nodes of one code (as a context and
  // as an extension of it), and the nodes of both revisions of code unchanged between them
  private static Map<Node, List<Node>> sameCodeNodes(List<Block> ordered, Map<Node, DiffNode> allChangeDiffNodes) {
    List<Node> unchangedDependencies = ordered.stream()
            .filter(block -> block instanceof DependencyBlock)
            .map(block -> ((DependencyBlock) block).dependency())
            .filter(dependency -> !allChangeDiffNodes.containsKey(dependency))
            .distinct().toList();

    Map<Node, Node> alternatives = new HashMap<>();
    for (Node dependency : unchangedDependencies) {
      if (dependency.isSrc()) {
        alternatives.put(dependency, dependency.getAlternative());
      }
    }

    Map<Node, List<Node>> sameCodeNodes = new HashMap<>();
    for (Node dependency : unchangedDependencies) {
      if (sameCodeNodes.containsKey(dependency)) {
        continue;
      }

      List<Node> nodes = new ArrayList<>(List.of(dependency));
      for (int i = 0; i < nodes.size(); i++) {
        Node node = nodes.get(i);
        for (Node other : unchangedDependencies) {
          if (!nodes.contains(other) && isSameCode(node, other, alternatives)) {
            nodes.add(other);
          }
        }
      }
      if (nodes.size() == 1) {
        continue;
      }

      List<Node> printingOrder = nodes.stream().sorted(Comparator.comparingInt(unchangedDependencies::indexOf)).toList();
      for (Node node : printingOrder) {
        sameCodeNodes.put(node, printingOrder);
      }
    }

    return sameCodeNodes;
  }

  private static boolean isSameCode(Node subject, Node object, Map<Node, Node> alternatives) {
    if (subject.getSrcDst().equals(object.getSrcDst())) {
      return subject.getTree() == object.getTree();
    }

    Node srcDependency = subject.isSrc() ? subject : object;
    Node dstDependency = subject.isSrc() ? object : subject;
    return isRevisionTwin(srcDependency, alternatives.get(srcDependency), dstDependency);
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

  // The changes of a location context are printed with nothing around them, so each is printed apart from the others
  // unless no code lies between them, and a change updated in place is printed with its counterpart. Changes sharing a
  // line are printed together, as each would print that line
  private static Set<Map<Node, Set<Node>>> isolateLocationContexts(Set<Map<Node, Set<Node>>> groups, Graph<Node, Edge> graph) {
    Set<Map<Node, Set<Node>>> isolated = new HashSet<>();
    List<Map<Node, Set<Node>>> located = new ArrayList<>();
    Map<Node, Node> changesContexts = new LinkedHashMap<>();
    for (Map<Node, Set<Node>> group : groups) {
      if (group.keySet().stream().noneMatch(context -> context.getNodeType().equals(NodeType.LOCATION_CONTEXT))) {
        Map<Node, Set<Node>> header = headerDeclarations(group, graph);
        if (header == null) {
          isolated.add(group);
          continue;
        }
        group = header;
      }

      for (Map.Entry<Node, Set<Node>> context : group.entrySet()) {
        for (Node change : context.getValue()) {
          changesContexts.put(change, context.getKey());
        }
      }
    }

    Set<Node> grouped = new HashSet<>();
    // The changes of the header of a declaration print together, with the lines declaring it
    Map<Node, Map<Node, Set<Node>>> headers = new LinkedHashMap<>();
    for (Map.Entry<Node, Node> changeContext : changesContexts.entrySet()) {
      Node context = changeContext.getValue();
      if (MergeGroup.headerChild(changeContext.getKey().getTree(), context) == null) {
        continue;
      }

      List<Node> sources = context.isSrc() ? List.of() : context.getMappingSources(graph);
      Node type = sources.isEmpty() ? context : sources.get(0);
      headers.computeIfAbsent(type, header -> new HashMap<>()).computeIfAbsent(context, side -> new HashSet<>())
              .add(changeContext.getKey());
      grouped.add(changeContext.getKey());
    }
    located.addAll(headers.values());

    List<Node> alone = new ArrayList<>();
    for (Map.Entry<Node, Node> changeContext : changesContexts.entrySet()) {
      Node change = changeContext.getKey();
      if (!grouped.add(change)) {
        continue;
      }

      Node counterpart = updatedCounterpart(change, changesContexts.keySet(), graph);
      if (counterpart == null || !grouped.add(counterpart)) {
        alone.add(change);
        continue;
      }

      Map<Node, Set<Node>> changeGroup = new HashMap<>();
      changeGroup.put(changeContext.getValue(), new HashSet<>(Set.of(change)));
      changeGroup.computeIfAbsent(changesContexts.get(counterpart), context -> new HashSet<>()).add(counterpart);
      located.add(changeGroup);
    }

    List<Node> sorted = MergeGroup.sortNodes(new HashSet<>(alone));
    List<Node> run = new ArrayList<>();
    for (Node change : sorted) {
      if (!run.isEmpty() && !isAdjacent(run.get(run.size() - 1), change, changesContexts, graph)) {
        located.add(new HashMap<>(Map.of(changesContexts.get(run.get(0)), new HashSet<>(run))));
        run = new ArrayList<>();
      }
      run.add(change);
    }
    if (!run.isEmpty()) {
      located.add(new HashMap<>(Map.of(changesContexts.get(run.get(0)), new HashSet<>(run))));
    }

    isolated.addAll(mergeSharingLines(located));
    return isolated;
  }

  // The changes of a part of the header of a type or a method, as its doc comment or an annotation, are changes of its
  // header, so they are located at the declaration
  @Nullable
  private static Map<Node, Set<Node>> headerDeclarations(Map<Node, Set<Node>> group, Graph<Node, Edge> graph) {
    Map<Node, Set<Node>> declarations = new HashMap<>();
    for (Map.Entry<Node, Set<Node>> context : group.entrySet()) {
      Node part = context.getKey();
      Node declaration = Context.get(graph, part).stream()
              .filter(contextNode -> contextNode.getNodeType().equals(NodeType.LOCATION_CONTEXT)).findFirst().orElse(null);
      if (declaration == null || MergeGroup.headerChild(part.getTree(), declaration) == null) {
        return null;
      }
      declarations.put(declaration, context.getValue());
    }
    return declarations;
  }

  private static List<Map<Node, Set<Node>>> mergeSharingLines(List<Map<Node, Set<Node>>> groups) {
    List<Map<Node, Set<Node>>> merged = new ArrayList<>();
    for (Map<Node, Set<Node>> group : groups) {
      Map<Node, Set<Node>> merging = group;
      boolean widened = true;
      while (widened) {
        Map<Node, Set<Node>> current = merging;
        Optional<Map<Node, Set<Node>>> sharing = merged.stream().filter(other -> shareLine(current, other)).findFirst();
        widened = sharing.isPresent();
        if (widened) {
          merged.remove(sharing.get());
          merging = new HashMap<>(sharing.get());
          for (Map.Entry<Node, Set<Node>> context : current.entrySet()) {
            Node sameSide = merging.keySet().stream().filter(other -> other.getSrcDst().equals(context.getKey().getSrcDst()))
                    .findFirst().orElse(context.getKey());
            Set<Node> changes = new HashSet<>(merging.getOrDefault(sameSide, Set.of()));
            changes.addAll(context.getValue());
            merging.put(sameSide, changes);
          }
        }
      }
      merged.add(merging);
    }

    return merged;
  }

  private static boolean shareLine(Map<Node, Set<Node>> group, Map<Node, Set<Node>> other) {
    for (Node change : MergeGroup.changesOf(group)) {
      TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(change.getTree(), change.getFileContent());
      for (Node otherChange : MergeGroup.changesOf(other)) {
        if (!change.getSrcDst().equals(otherChange.getSrcDst()) || !change.getPath().equals(otherChange.getPath())) {
          continue;
        }

        TreeUtilFunctions.LineRange otherRange = TreeUtilFunctions.getLineRange(otherChange.getTree(), otherChange.getFileContent());
        if (range.startLine() <= otherRange.endLine() && otherRange.startLine() <= range.endLine()) {
          return true;
        }
      }
    }

    return false;
  }

  // In a file one revision alone holds, the code that is not represented between the changes (the package and the
  // imports) is new or gone with them, so it does not part them
  private static boolean isAdjacent(Node previous, Node next, Map<Node, Node> changesContexts, Graph<Node, Edge> graph) {
    Node context = changesContexts.get(previous);
    if (!previous.getSrcDst().equals(next.getSrcDst()) || !previous.getPath().equals(next.getPath())
            || !context.equals(changesContexts.get(next))) {
      return false;
    }

    String content = previous.getFileContent();
    TreeUtilFunctions.LineRange previousRange = TreeUtilFunctions.getLineRange(previous.getTree(), content);
    TreeUtilFunctions.LineRange nextRange = TreeUtilFunctions.getLineRange(next.getTree(), content);
    if (nextRange.startLine() <= previousRange.endLine()) {
      return true;
    }

    String[] lines = content.split("\n", -1);
    Set<Integer> unrepresented = isWholeFile(context, graph) ? unrepresentedLines(context) : Set.of();
    for (int line = previousRange.endLine() + 1; line < nextRange.startLine(); line++) {
      if (!lines[line - 1].isBlank() && !unrepresented.contains(line)) {
        return false;
      }
    }
    return true;
  }

  // The root of a file only one revision holds
  private static boolean isWholeFile(Node context, Graph<Node, Edge> graph) {
    Constants constants = new Constants(context.getPath());
    return constants.isRoot(context.getTree().getType().name)
            && (context.isSrc() ? context.getMappingTargets(graph) : context.getMappingSources(graph)).isEmpty();
  }

  // The package declaration and the imports of the file. RM gap: RM gives no export statement (TypeScript) and no empty
  // statement of a file, so they do not count as unrepresented
  private static Set<Integer> unrepresentedLines(Node root) {
    Set<Integer> lines = new HashSet<>();
    for (LocationInfo part : root.getRM().fileParts()) {
      for (int line = part.getStartLine(); line <= part.getEndLine(); line++) {
        lines.add(line);
      }
    }
    return lines;
  }

  // The only counterpart of a change, when the change stays where it is and its code is updated there
  @Nullable
  private static Node updatedCounterpart(Node change, Set<Node> changes, Graph<Node, Edge> graph) {
    List<Node> counterparts = change.isSrc() ? change.getMappingTargets(graph) : change.getMappingSources(graph);
    if (counterparts.size() != 1) {
      return null;
    }

    Node counterpart = counterparts.get(0);
    List<Node> changeCounterparts = counterpart.isSrc() ? counterpart.getMappingTargets(graph) : counterpart.getMappingSources(graph);
    if (!changes.contains(counterpart) || changeCounterparts.size() != 1
            || !change.getContextString().equals(counterpart.getContextString())
            || change.getContent().equals(counterpart.getContent())) {
      return null;
    }

    return counterpart;
  }

  private static DiffBlock buildDiffBlock(Map<Node, Set<Node>> contextGroup, int length, Graph<Node, Edge> graph) {
    DiffContexts contexts = resolveContexts(contextGroup, graph);
    Node srcContext = contexts.srcContext();
    Node dstContext = contexts.dstContext();
    Set<Node> srcChanges = contexts.srcChanges();
    Set<Node> dstChanges = contexts.dstChanges();

    DiffSide src = null, dst = null;
    if (srcContext != null) {
      src = contextSide(srcContext, srcChanges);
    }
    if (dstContext != null) {
      dst = contextSide(dstContext, dstChanges);
    }

    List<DiffNode.DiffLine> diffLines;
    String location;
    boolean printsContext = true;
    int[] partnerWindow = contexts.locationContext() ? partnerWindow(contexts) : null;
    // Code that left is linked to where it went, which shows what it changed
    DiffSide origin = contexts.locationContext() && partnerWindow == null && srcChanges.isEmpty() && !dstChanges.isEmpty()
            ? movedOrigin(dstChanges, graph) : null;
    if (contexts.locationContext() && srcContext != null && dstContext != null && isHeader(srcContext, srcChanges)
            && isHeader(dstContext, dstChanges)) {
      // The changes are of the header of a type or a method, which reads with the lines declaring it, as a signature
      // does: from the first part of the header they change on to the end of the declaration
      List<MappingStore> mappingStores = mappingStores(srcContext, dstContext);
      src = new DiffSide(srcContext, srcChanges, headerTop(srcContext, srcChanges, dstContext, dstChanges, mappingStores),
              headerBottom(srcContext));
      dst = new DiffSide(dstContext, dstChanges, headerTop(dstContext, dstChanges, srcContext, srcChanges, mappingStores),
              headerBottom(dstContext));
      diffLines = mergedLines(src, dst, mappingStores, graph);
      location = MergeGroup.sortNodes(srcChanges.isEmpty() ? dstChanges : srcChanges).get(0).getContextString();
    } else if (contexts.locationContext() && srcChanges.size() == 1 && dstChanges.size() == 1) {
      // A change updated in place reads against its counterpart, which nothing of the context is printed around
      Node srcChange = srcChanges.iterator().next();
      Node dstChange = dstChanges.iterator().next();
      src = new DiffSide(srcChange, srcChanges);
      dst = new DiffSide(dstChange, dstChanges);
      diffLines = mergedLines(src, dst, mappingStores(srcChange, dstChange), graph);
      location = srcChange.getContextString();
    } else if (partnerWindow != null) {
      // The changes share their lines with code the other revision holds too, so those lines are printed against
      // the lines holding that code there
      boolean srcChanged = !srcChanges.isEmpty();
      DiffSide changedSide = srcChanged ? src : dst;
      int[] changedWindow = changedLines(changedSide);
      DiffSide partnerSide = new DiffSide(srcChanged ? dstContext : srcContext, Set.of(), partnerWindow[0], partnerWindow[1]);
      changedSide = new DiffSide(changedSide.context, srcChanged ? srcChanges : dstChanges, changedWindow[0], changedWindow[1]);
      src = srcChanged ? changedSide : partnerSide;
      dst = srcChanged ? partnerSide : changedSide;
      diffLines = mergedLines(src, dst, mappingStores(srcContext, dstContext), graph);
      location = MergeGroup.sortNodes(srcChanged ? srcChanges : dstChanges).get(0).getContextString();
    } else if (origin != null) {
      // The changes hold code moved from one place of the old revision, such as a body extracted to a new method, so
      // they read against the lines that code left there: what it keeps reads as unchanged, what it changes as edited
      int[] changedWindow = changedLines(dst);
      src = origin;
      dst = new DiffSide(dst.context, dstChanges, changedWindow[0], changedWindow[1]);
      diffLines = mergedMovedLines(src, dst, mappingStores(src.context, dst.context));
      location = MergeGroup.sortNodes(dstChanges).get(0).getContextString();
    } else if (contexts.locationContext()) {
      // The context only says where these changes live, so nothing of it is printed but them
      printsContext = false;
      diffLines = new ArrayList<>();
      if (src != null) {
        diffLines.addAll(src.changeLines(isWholeFile(srcContext, graph) ? unrepresentedLines(srcContext) : Set.of()));
      }
      if (dst != null) {
        diffLines.addAll(dst.changeLines(isWholeFile(dstContext, graph) ? unrepresentedLines(dstContext) : Set.of()));
      }

      Set<Node> located = srcChanges.isEmpty() ? dstChanges : srcChanges;
      location = located.isEmpty() ? "" : MergeGroup.sortNodes(located).get(0).getContextString();
    } else if (src != null && dst != null) {
      diffLines = mergedLines(src, dst, mappingStores(srcContext, dstContext), graph);
      location = diffLocation(srcContext, dstContext);
    } else {
      // One revision alone holds this context, so there is no counterpart to merge it against:
      // it is printed whole, and what its changes cover carries the marker
      DiffSide side = src != null ? src : dst;
      side.tagChanges(null, List.of(), Set.of());
      diffLines = side.taggedLines();
      location = side.context.getInclusiveContextString();
    }

    if (diffLines.stream().noneMatch(diffLine -> diffLine.prefix() != ' ')) {
      return null;
    }

    DiffNode diffNode = DiffNode.of(srcContext == null ? null : srcContext.getPath(),
            dstContext == null ? null : dstContext.getPath(),
            src == null ? List.of() : src.changes,
            dst == null ? List.of() : dst.changes,
            location, diffLines, length);

    // A location context printed around its changes, as a header or the code around a change updated in place,
    // shows the lines of its window; one printing nothing of itself but the changes shows no other code within it
    List<ShownLines> shownLines = new ArrayList<>();
    if (printsContext) {
      if (src != null) {
        shownLines.add(new ShownLines(src.context.getSrcDst(), src.context.getPath(), src.top, src.bottom));
      }
      if (dst != null) {
        shownLines.add(new ShownLines(dst.context.getSrcDst(), dst.context.getPath(), dst.top, dst.bottom));
      }
    }

    return new DiffBlock(diffNode, shownLines);
  }

  private static boolean isHeader(Node context, Set<Node> changes) {
    return !MergeGroup.declarationHeader(context).isEmpty() && changes.stream()
            .allMatch(change -> MergeGroup.headerChild(change.getTree(), context) != null);
  }

  // The first line of the header that its changes or the changes of the other revision are on, unless the declaration
  // itself starts before
  private static int headerTop(Node context, Set<Node> changes, Node otherContext, Set<Node> otherChanges,
                               List<MappingStore> mappingStores) {
    String content = context.getFileContent();
    List<Tree> header = MergeGroup.declarationHeader(context);
    int top = declarationStartLine(context);
    for (Node change : changes) {
      Tree child = MergeGroup.headerChild(change.getTree(), context);
      top = Math.min(top, TreeUtilFunctions.getLineRange(child, content).startLine());
    }
    for (Node otherChange : otherChanges) {
      Tree otherChild = MergeGroup.headerChild(otherChange.getTree(), otherContext);
      for (MappingStore mappingStore : mappingStores) {
        Tree counterpart = context.isSrc() ? mappingStore.getSrcForDst(otherChild) : mappingStore.getDstForSrc(otherChild);
        if (counterpart != null && header.stream().anyMatch(child -> child == counterpart)) {
          top = Math.min(top, TreeUtilFunctions.getLineRange(counterpart, content).startLine());
          break;
        }
      }
    }
    return top;
  }

  // The last line of the header, where the body of the type or of the method opens, or where a method without a body
  // ends
  private static int headerBottom(Node context) {
    Tree declaration = context.getTree();
    String content = context.getFileContent();
    List<Tree> header = MergeGroup.declarationHeader(context);
    if (context.getRM().declaredOperation() != null) {
      List<Tree> children = declaration.getChildren();
      return header.size() < children.size() ? TreeUtilFunctions.getLineRange(children.get(children.size() - 1), content).startLine()
              : TreeUtilFunctions.getLineRange(declaration, content).endLine();
    }
    return TreeUtilFunctions.getLineRange(header.get(header.size() - 1), content).endLine();
  }

  // The lines of the old revision that the code of the changes came from, when that code is all of the lines it spans
  // in one file there: nothing it is printed against there stays there
  @Nullable
  private static DiffSide movedOrigin(Set<Node> changes, Graph<Node, Edge> graph) {
    List<Node> origins = new ArrayList<>();
    for (Node change : changes) {
      for (Node counterpart : change.getMappingSources(graph)) {
        if (!origins.contains(counterpart)) {
          origins.add(counterpart);
        }
      }
    }
    if (origins.isEmpty() || origins.stream().anyMatch(counterpart -> !counterpart.getPath().equals(origins.get(0).getPath()))) {
      return null;
    }

    String content = origins.get(0).getFileContent();
    String[] lines = content.split("\n", -1);
    boolean[] covered = new boolean[lines.length + 1];
    int top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
    for (Node counterpart : origins) {
      TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(counterpart.getTree(), content);
      top = Math.min(top, range.startLine());
      bottom = Math.max(bottom, range.endLine());
      for (int line = range.startLine(); line <= range.endLine(); line++) {
        covered[line] = true;
      }
    }
    for (int line = top; line <= bottom; line++) {
      if (!covered[line] && !lines[line - 1].isBlank()) {
        return null;
      }
    }

    // Code of those lines going elsewhere, as when a body is split among several methods, is not printed against these
    // changes, nor a line all of whose code is removed: each line of code there brings some of its code here
    boolean[] holdsCode = new boolean[lines.length + 1];
    boolean[] brings = new boolean[lines.length + 1];
    for (Node counterpart : origins) {
      List<Tree> trees = new ArrayList<>(counterpart.getTree().getDescendants());
      trees.add(counterpart.getTree());
      for (Tree leaf : trees) {
        if (!leaf.getChildren().isEmpty()) {
          continue;
        }

        boolean arrived = false;
        for (ASTDiff diff : counterpart.getDiffs()) {
          Tree leafCounterpart = diff.getAllMappings().getMonoMappingStore().getDstForSrc(leaf);
          if (leafCounterpart == null) {
            continue;
          }
          if (changes.stream().noneMatch(change -> change.getTree() == leafCounterpart || leafCounterpart.getParents().contains(change.getTree()))) {
            return null;
          }
          arrived = true;
        }

        TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(leaf, content);
        for (int line = range.startLine(); line <= range.endLine(); line++) {
          holdsCode[line] = true;
          brings[line] |= arrived;
        }
      }
    }
    for (int line = top; line <= bottom; line++) {
      if (holdsCode[line] && !brings[line]) {
        return null;
      }
    }

    int originTop = top, originBottom = bottom;
    Optional<Node> holder = Context.get(graph, origins.get(0)).stream()
            .filter(context -> origins.stream().allMatch(counterpart -> counterpart.isDescendantOf(context))).findFirst();
    return holder.map(context -> new DiffSide(context, Set.of(), originTop, originBottom)).orElse(null);
  }

  private static int[] changedLines(DiffSide side) {
    int top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
    for (Node change : side.changes) {
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(change.getTree(), change.getFileContent());
      top = Math.min(top, lineRange.startLine());
      bottom = Math.max(bottom, lineRange.endLine());
    }
    return new int[]{top, bottom};
  }

  // The lines of the other revision holding the code that shares its lines with the changes of one revision only,
  // unless that code is spread there over more lines than here
  @Nullable
  private static int[] partnerWindow(DiffContexts contexts) {
    if (contexts.srcContext() == null || contexts.dstContext() == null
            || contexts.srcChanges().isEmpty() == contexts.dstChanges().isEmpty()) {
      return null;
    }

    boolean srcChanged = !contexts.srcChanges().isEmpty();
    Node context = srcChanged ? contexts.srcContext() : contexts.dstContext();
    Set<Node> changes = srcChanged ? contexts.srcChanges() : contexts.dstChanges();
    DiffSide side = new DiffSide(context, changes);
    int[] lines = changedLines(side);

    List<MappingStore> mappingStores = mappingStores(contexts.srcContext(), contexts.dstContext());
    Node partnerContext = srcChanged ? contexts.dstContext() : contexts.srcContext();
    List<Integer> partnerLines = new ArrayList<>();
    collectPartnerLines(context.getTree(), side, changes, lines, mappingStores, partnerContext, partnerLines);
    if (partnerLines.isEmpty() || Collections.max(partnerLines) - Collections.min(partnerLines) > lines[1] - lines[0]) {
      return null;
    }

    return new int[]{Collections.min(partnerLines), Collections.max(partnerLines)};
  }

  private static void collectPartnerLines(Tree tree, DiffSide side, Set<Node> changes, int[] lines, List<MappingStore> mappingStores,
                                          Node partnerContext, List<Integer> partnerLines) {
    TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, side.context.getFileContent());
    if (lineRange.endLine() < lines[0] || lines[1] < lineRange.startLine()
            || changes.stream().anyMatch(change -> change.getTree() == tree || tree.getParents().contains(change.getTree()))) {
      return;
    }

    if (tree.getChildren().isEmpty()) {
      for (MappingStore mappingStore : mappingStores) {
        Tree counterpart = side.context.isSrc() ? mappingStore.getDstForSrc(tree) : mappingStore.getSrcForDst(tree);
        if (counterpart != null && counterpart.getParents().contains(partnerContext.getTree())) {
          partnerLines.add(TreeUtilFunctions.getLineRange(counterpart, partnerContext.getFileContent()).startLine());
          break;
        }
      }
      return;
    }

    for (Tree child : tree.getChildren()) {
      collectPartnerLines(child, side, changes, lines, mappingStores, partnerContext, partnerLines);
    }
  }

  private static List<DiffNode.DiffLine> mergedLines(DiffSide src, DiffSide dst, List<MappingStore> mappingStores,
                                                    Graph<Node, Edge> graph) {
    List<int[]> anchors = buildAnchors(src, dst, mappingStores);
    Set<List<Integer>> inPlace = inPlaceLines(src, dst, anchors);
    src.tagChanges(dst, mappingStores, inPlace);
    dst.tagChanges(src, mappingStores, inPlace);
    tagChanges(src, dst, mappingStores, inPlace, graph);
    tagChanges(dst, src, mappingStores, inPlace, graph);

    closeChangedLines(src, dst, mappingStores);
    propagateChanges(src, dst, anchors);
    return mergeSides(src, dst, meetingPoints(src, dst, anchors));
  }

  // Moved code is changed where it holds code the other side does not hold as it is; the statement holding an edit is
  // not changed but by the edit
  private static List<DiffNode.DiffLine> mergedMovedLines(DiffSide src, DiffSide dst, List<MappingStore> mappingStores) {
    List<int[]> anchors = buildAnchors(src, dst, mappingStores);
    src.tagMoved(src.context.getTree(), dst, mappingStores);
    dst.tagMoved(dst.context.getTree(), src, mappingStores);

    closeChangedLines(src, dst, mappingStores);
    propagateChanges(src, dst, anchors);
    return mergeSides(src, dst, meetingPoints(src, dst, anchors));
  }

  // A changed line printed without the lines holding the counterparts of its code would read as adding that code:
  // the lines of the other side holding code of a changed line are changed too
  private static void closeChangedLines(DiffSide src, DiffSide dst, List<MappingStore> mappingStores) {
    List<int[]> leafLines = new ArrayList<>();
    collectLeafLines(src.context.getTree(), src, dst, mappingStores, leafLines);

    boolean closed = false;
    while (!closed) {
      closed = true;
      for (int[] leafLine : leafLines) {
        if (src.changed.contains(leafLine[0]) && dst.changed.add(leafLine[1])) {
          closed = false;
        }
        if (dst.changed.contains(leafLine[1]) && src.changed.add(leafLine[0])) {
          closed = false;
        }
      }
    }
  }

  private static void collectLeafLines(Tree tree, DiffSide src, DiffSide dst, List<MappingStore> mappingStores, List<int[]> leafLines) {
    TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, src.context.getFileContent());
    if (lineRange.endLine() < src.top || src.bottom < lineRange.startLine()) {
      return;
    }

    if (!tree.getChildren().isEmpty()) {
      for (Tree child : tree.getChildren()) {
        collectLeafLines(child, src, dst, mappingStores, leafLines);
      }
      return;
    }

    for (MappingStore mappingStore : mappingStores) {
      Tree counterpart = mappingStore.getDstForSrc(tree);
      if (counterpart == null) {
        continue;
      }

      int dstLine = TreeUtilFunctions.getLineRange(counterpart, dst.context.getFileContent()).startLine();
      if (src.inWindow(lineRange.startLine()) && dst.inWindow(dstLine)
              && (counterpart == dst.context.getTree() || counterpart.getParents().contains(dst.context.getTree()))) {
        leafLines.add(new int[]{lineRange.startLine(), dstLine});
      }
      return;
    }
  }

  // The pairs of same lines that keep their order in both revisions: the heaviest increasing chain of them, so a line
  // moving across the others is not part of it. Of code moving across other code, the code of the changes is what moves:
  // a line holding none weighs more
  private static Set<List<Integer>> inPlaceLines(DiffSide src, DiffSide dst, List<int[]> anchors) {
    List<int[]> same = anchors.stream().filter(anchor -> sameLine(src, anchor[0], dst, anchor[1])).toList();
    Set<Integer> srcChangeLines = src.changeLineSet();
    Set<Integer> dstChangeLines = dst.changeLineSet();
    int[] chainLength = new int[same.size()];
    int[] previous = new int[same.size()];
    int last = -1;
    for (int i = 0; i < same.size(); i++) {
      int weight = srcChangeLines.contains(same.get(i)[0]) || dstChangeLines.contains(same.get(i)[1]) ? 1 : 2;
      chainLength[i] = weight;
      previous[i] = -1;
      for (int j = 0; j < i; j++) {
        if (same.get(j)[1] < same.get(i)[1] && chainLength[j] + weight > chainLength[i]) {
          chainLength[i] = chainLength[j] + weight;
          previous[i] = j;
        }
      }
      if (last == -1 || chainLength[i] > chainLength[last]) {
        last = i;
      }
    }

    Set<List<Integer>> inPlace = new HashSet<>();
    for (int i = last; i != -1; i = previous[i]) {
      inPlace.add(List.of(same.get(i)[0], same.get(i)[1]));
    }
    return inPlace;
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

  private static void tagChanges(DiffSide side, DiffSide other, List<MappingStore> mappingStores, Set<List<Integer>> inPlace,
                                 Graph<Node, Edge> graph) {
    for (Node node : graph.vertexSet()) {
      if (!node.isBase() || !node.getSrcDst().equals(side.context.getSrcDst()) || !node.getPath().equals(side.context.getPath())) {
        continue;
      }
      side.tag(node.getTree(), other, mappingStores, inPlace);
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
      // The mappings of a diff may reach trees of other files, whose lines are not the ones of the other side
      if (alternative == null || (alternative != dst.context.getTree() && !alternative.getParents().contains(dst.context.getTree()))) {
        continue;
      }

      voteAnchor(votes, src, tree, dst, alternative);
      for (int[] linePair : leafLinePairs(src, tree, dst, alternative)) {
        voteLine(votes, src, linePair[0], dst, linePair[1]);
      }
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

  // The lines an updated leaf keeps: a label spanning lines, such as a comment, may change on some of them only
  private static List<int[]> leafLinePairs(DiffSide src, Tree srcTree, DiffSide dst, Tree dstTree) {
    if (!srcTree.getChildren().isEmpty() || !dstTree.getChildren().isEmpty()) {
      return List.of();
    }

    TreeUtilFunctions.LineRange srcRange = TreeUtilFunctions.getLineRange(srcTree, src.context.getFileContent());
    TreeUtilFunctions.LineRange dstRange = TreeUtilFunctions.getLineRange(dstTree, dst.context.getFileContent());
    int srcCount = srcRange.endLine() - srcRange.startLine() + 1;
    int dstCount = dstRange.endLine() - dstRange.startLine() + 1;
    if (srcCount == 1 && dstCount == 1) {
      return List.of();
    }

    int[][] common = new int[srcCount + 1][dstCount + 1];
    for (int i = srcCount - 1; i >= 0; i--) {
      for (int j = dstCount - 1; j >= 0; j--) {
        common[i][j] = sameLine(src, srcRange.startLine() + i, dst, dstRange.startLine() + j)
                ? common[i + 1][j + 1] + 1 : Math.max(common[i + 1][j], common[i][j + 1]);
      }
    }

    List<int[]> linePairs = new ArrayList<>();
    int i = 0, j = 0;
    while (i < srcCount && j < dstCount) {
      if (sameLine(src, srcRange.startLine() + i, dst, dstRange.startLine() + j)) {
        linePairs.add(new int[]{srcRange.startLine() + i, dstRange.startLine() + j});
        i++;
        j++;
      } else if (common[i + 1][j] >= common[i][j + 1]) {
        i++;
      } else {
        j++;
      }
    }

    return linePairs;
  }

  private static boolean sameLine(DiffSide src, int srcLine, DiffSide dst, int dstLine) {
    return src.line(srcLine).trim().equals(dst.line(dstLine).trim());
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
    String srcLocation = srcContext.getInclusiveContextString();
    String dstLocation = dstContext.getInclusiveContextString();

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
    private final int[] lineOffsets;
    private final List<Node> changes = new ArrayList<>();
    private final Set<Integer> changed = new HashSet<>();
    private final char prefix;

    private DiffSide(Node context, Set<Node> changeNodes) {
      this(context, changeNodes, TreeUtilFunctions.getLineRange(context.getTree(), context.getFileContent()).startLine(),
              TreeUtilFunctions.getLineRange(context.getTree(), context.getFileContent()).endLine());
    }

    // A window of the lines of the context only
    private DiffSide(Node context, Set<Node> changeNodes, int top, int bottom) {
      this.context = context;
      this.top = top;
      this.bottom = bottom;
      this.prefix = context.isSrc() ? '-' : '+';
      this.lines = context.getFileContent().split("\n", -1);
      this.lineOffsets = new int[lines.length];
      for (int i = 1; i < lines.length; i++) {
        lineOffsets[i] = lineOffsets[i - 1] + lines[i - 1].length() + 1;
      }
      changes.addAll(MergeGroup.sortNodes(changeNodes));
    }

    private void tagChanges(@Nullable DiffSide other, List<MappingStore> mappingStores, Set<List<Integer>> inPlace) {
      for (Node change : changes) {
        tag(change.getTree(), other, mappingStores, inPlace);
      }
    }

    // A line is changed by the tree when it holds code of the tree that is not carried over as it is to the other
    // side: a change may enclose code that only got a new parent, which stays in place in both revisions
    private void tag(Tree tree, @Nullable DiffSide other, List<MappingStore> mappingStores, Set<List<Integer>> inPlace) {
      List<Tree> carriedOver = new ArrayList<>();
      List<int[]> keptCode = new ArrayList<>();
      Set<Integer> keptLines = new HashSet<>();
      if (other != null) {
        collectCarriedOver(tree, other, mappingStores, inPlace, carriedOver, keptCode);
        keptLines.addAll(keptLines(tree, other, mappingStores));
      }

      Set<Integer> inPlaceLines = new HashSet<>();
      for (List<Integer> linePair : inPlace) {
        inPlaceLines.add(context.isSrc() ? linePair.get(0) : linePair.get(1));
      }

      String content = context.getFileContent();
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, content);
      for (int line = Math.max(lineRange.startLine(), top); line <= Math.min(lineRange.endLine(), bottom); line++) {
        if (keptLines.contains(line)) {
          continue;
        }

        int from = Math.max(lineOffsets[line - 1], tree.getPos());
        int to = Math.min(lineOffsets[line - 1] + lines[line - 1].length(), tree.getEndPos());
        for (int offset = from; offset < to; offset++) {
          if (!Character.isWhitespace(content.charAt(offset)) && !within(offset, carriedOver) && !withinRanges(offset, keptCode)
                  && !(inPlaceLines.contains(line) && isTerminator(tree, offset))) {
            changed.add(line);
            break;
          }
        }
      }
    }

    // Whether the offset is the semicolon ending a statement within the tree. On a line both revisions hold as it is, it
    // is no code of its own: a statement re-wrapped around code it carries over, as a creation returned before and
    // assigned now, ends on the same `);`
    private boolean isTerminator(Tree tree, int offset) {
      if (context.getFileContent().charAt(offset) != ';') {
        return false;
      }

      Tree holding = tree;
      for (boolean deeper = true; deeper; ) {
        deeper = false;
        for (Tree child : holding.getChildren()) {
          if (child.getPos() <= offset && offset < child.getEndPos()) {
            holding = child;
            deeper = true;
            break;
          }
        }
      }
      return holding.getEndPos() == offset + 1;
    }

    // A line is changed by moved code when it holds a leaf the other side holds with another label or not at all, or
    // the code of a tree the other side does not hold around the children of that tree
    private void tagMoved(Tree tree, DiffSide other, List<MappingStore> mappingStores) {
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, context.getFileContent());
      if (lineRange.endLine() < top || bottom < lineRange.startLine()) {
        return;
      }

      Tree counterpart = counterpartWithin(tree, other, mappingStores);
      if (tree.getChildren().isEmpty()) {
        if (counterpart == null || !counterpart.getLabel().equals(tree.getLabel())) {
          tagCode(tree.getPos(), tree.getEndPos());
        }
        return;
      }

      if (counterpart == null) {
        int from = tree.getPos();
        for (Tree child : tree.getChildren()) {
          tagCode(from, child.getPos());
          from = child.getEndPos();
        }
        tagCode(from, tree.getEndPos());
      }
      for (Tree child : tree.getChildren()) {
        tagMoved(child, other, mappingStores);
      }
    }

    private void tagCode(int from, int to) {
      String content = context.getFileContent();
      for (int offset = from; offset < to; offset++) {
        if (!Character.isWhitespace(content.charAt(offset))) {
          int line = lineOf(offset);
          if (inWindow(line)) {
            changed.add(line);
          }
        }
      }
    }

    private int lineOf(int offset) {
      int index = Arrays.binarySearch(lineOffsets, offset);
      return (index >= 0 ? index : -index - 2) + 1;
    }

    private Set<Integer> keptLines(Tree tree, DiffSide other, List<MappingStore> mappingStores) {
      Tree counterpart = counterpartWithin(tree, other, mappingStores);
      if (counterpart == null) {
        return Set.of();
      }

      Set<Integer> keptLines = new HashSet<>();
      List<int[]> linePairs = context.isSrc() ? leafLinePairs(this, tree, other, counterpart) : leafLinePairs(other, counterpart, this, tree);
      for (int[] linePair : linePairs) {
        keptLines.add(context.isSrc() ? linePair[0] : linePair[1]);
      }
      return keptLines;
    }

    @Nullable
    private Tree counterpartWithin(Tree tree, DiffSide other, List<MappingStore> mappingStores) {
      for (MappingStore mappingStore : mappingStores) {
        Tree counterpart = context.isSrc() ? mappingStore.getDstForSrc(tree) : mappingStore.getSrcForDst(tree);
        if (counterpart == null) {
          continue;
        }

        Tree otherTree = other.context.getTree();
        return counterpart == otherTree || counterpart.getParents().contains(otherTree) ? counterpart : null;
      }

      return null;
    }

    // The code of a tree the other side holds too, between its children, is kept on the lines that stay in place within
    // it there, such as the commas of the arguments of a call that moves into a new statement
    private void collectCarriedOver(Tree tree, DiffSide other, List<MappingStore> mappingStores, Set<List<Integer>> inPlace,
                                    List<Tree> carriedOver, List<int[]> keptCode) {
      if (isCarriedOver(tree, other, mappingStores, inPlace)) {
        carriedOver.add(tree);
        return;
      }

      Tree counterpart = tree.getChildren().isEmpty() ? null : counterpartWithin(tree, other, mappingStores);
      if (counterpart != null) {
        TreeUtilFunctions.LineRange counterpartRange = TreeUtilFunctions.getLineRange(counterpart, other.context.getFileContent());
        int from = tree.getPos();
        for (Tree child : tree.getChildren()) {
          keepInPlace(from, child.getPos(), counterpartRange, inPlace, keptCode);
          from = child.getEndPos();
        }
        keepInPlace(from, tree.getEndPos(), counterpartRange, inPlace, keptCode);
      }

      for (Tree child : tree.getChildren()) {
        collectCarriedOver(child, other, mappingStores, inPlace, carriedOver, keptCode);
      }
    }

    private void keepInPlace(int from, int to, TreeUtilFunctions.LineRange counterpartRange, Set<List<Integer>> inPlace,
                             List<int[]> keptCode) {
      for (int offset = from; offset < to; offset++) {
        int line = lineOf(offset);
        boolean stays = false;
        for (int counterpartLine = counterpartRange.startLine(); counterpartLine <= counterpartRange.endLine() && !stays; counterpartLine++) {
          stays = inPlace.contains(linePair(line, counterpartLine));
        }
        if (stays) {
          keptCode.add(new int[]{offset, offset + 1});
        }
      }
    }

    private static boolean withinRanges(int offset, List<int[]> ranges) {
      for (int[] range : ranges) {
        if (range[0] <= offset && offset < range[1]) {
          return true;
        }
      }

      return false;
    }

    private boolean isCarriedOver(Tree tree, DiffSide other, List<MappingStore> mappingStores, Set<List<Integer>> inPlace) {
      Tree counterpart = counterpartWithin(tree, other, mappingStores);
      if (counterpart == null) {
        return false;
      }

      String text = context.getFileContent().substring(tree.getPos(), tree.getEndPos());
      if (!Node.sameIgnoringIndentation(text, other.context.getFileContent().substring(counterpart.getPos(), counterpart.getEndPos()))) {
        return false;
      }

      TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(tree, context.getFileContent());
      TreeUtilFunctions.LineRange counterpartRange = TreeUtilFunctions.getLineRange(counterpart, other.context.getFileContent());
      return inPlace.contains(linePair(range.startLine(), counterpartRange.startLine()))
              && inPlace.contains(linePair(range.endLine(), counterpartRange.endLine()));
    }

    private List<Integer> linePair(int line, int counterpartLine) {
      return context.isSrc() ? List.of(line, counterpartLine) : List.of(counterpartLine, line);
    }

    private static boolean within(int offset, List<Tree> trees) {
      for (Tree tree : trees) {
        if (tree.getPos() <= offset && offset < tree.getEndPos()) {
          return true;
        }
      }

      return false;
    }

    private Set<Integer> changeLineSet() {
      Set<Integer> changeLines = new HashSet<>();
      for (Node change : changes) {
        TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(change.getTree(), context.getFileContent());
        for (int line = range.startLine(); line <= range.endLine(); line++) {
          changeLines.add(line);
        }
      }
      return changeLines;
    }

    private List<int[]> changeRanges(Set<Integer> bridged) {
      List<int[]> ranges = new ArrayList<>();
      for (Node change : changes) {
        TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(change.getTree(), context.getFileContent());
        int startLine = lineRange.startLine();
        int endLine = lineRange.endLine();

        // Changes apart only by blank lines are printed with those lines, as they lie in the file, and so are the lines
        // bridged, which hold code not represented that changes with them
        if (!ranges.isEmpty() && blankBetween(ranges.get(ranges.size() - 1)[1], startLine, bridged)) {
          ranges.get(ranges.size() - 1)[1] = Math.max(ranges.get(ranges.size() - 1)[1], endLine);
          continue;
        }

        ranges.add(new int[]{startLine, endLine});
      }

      return ranges;
    }

    private boolean blankBetween(int endLine, int startLine, Set<Integer> bridged) {
      for (int line = endLine + 1; line < startLine; line++) {
        if (!line(line).isBlank() && !bridged.contains(line)) {
          return false;
        }
      }
      return true;
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

    public List<DiffNode.DiffLine> changeLines(Set<Integer> bridged) {
      List<DiffNode.DiffLine> diffLines = new ArrayList<>();
      for (int[] range : this.changeRanges(bridged)) {
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

  private record DiffBlock(DiffNode diffNode, List<ShownLines> shownLines) implements Block {
  }

  // The lines of a revision of a file that a diff prints
  private record ShownLines(SrcDst srcDst, String path, int top, int bottom) {
  }
}
