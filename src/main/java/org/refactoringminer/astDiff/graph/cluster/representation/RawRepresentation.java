package org.refactoringminer.astDiff.graph.cluster.representation;

import org.refactoringminer.util.PathFileUtils;
import gr.uom.java.xmi.LocationInfo;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.*;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class RawRepresentation {
  public static String specification() {
    StringBuilder spec = new StringBuilder();

    spec.append("### CHANGE REPRESENTATION\n")
            .append("- The chapter is a set of <diff>, <dependency> and <usage> elements.\n")
            .append("- A <diff> reads as a unified diff, but it is produced from an accurate AST differentiation between two revisions, ")
            .append("which finds edits at expression and statement granularity and reports them at line granularity.\n")
            .append("- Each <diff> presents the construct the edits sit in, not only a fixed number of lines around them, so it begins and ends where that construct does.\n")
            .append("- A <diff> within the body of a member also shows, around that construct, the code deciding whether and how often it runs: ")
            .append("the headers and the ends of the conditionals, loops and catch clauses holding it (an else branch with the condition of its if), the case labels it runs on, ")
            .append("the signature of the member (or the header of the lambda) declaring a parameter it reads, ")
            .append("the start of the try and its catch and finally clauses when it adds a throw within the body of that try, ")
            .append("and the checks before it that leave the member (or the loop) when their condition holds, each by its condition and the first line of its return, throw, break or continue, ")
            .append("or by a call of a method that throws, when the check reads a value the construct reads; a line of ... stands for the lines left out. ")
            .append("A changed condition of an if whose branch always leaves reads with the code after that if, which runs when the condition does not hold. ")
            .append("The code after the construct reading the values it gives its locals is shown too, where those values go. ")
            .append("Diffs of one member sharing those lines read as one diff.\n")
            .append("- A comment a <diff> edits, but a doc comment, shows the paragraphs holding the lines it changes, and the first and last lines of the comment; ")
            .append("a line of ... stands for its other lines.\n")
            .append("- Each <diff> carries id=\"#XXXXX\", which identifies it uniquely within the pull request, and location=\"<file>::<Type>#<member>\", ")
            .append("which reads \"<before> -> <after>\" when the location is changed.\n")
            .append("- For code leaving some blocks and arriving in some others, the corresponding diffs are mutually linked by carrying moved_to and moved_from, ")
            .append("each having a comma-separated list of the ids of the diffs its code moved to or came from.\n")
            .append("- A <diff> carrying moved_from whose code all came from one place shows it against the lines it left there: ")
            .append("code that arrived as it was reads as unchanged lines, and what changed on the way as removed and added lines.\n")
            .append("- A <diff> whose code is relied by other diffs in the chapter carries serves, a comma-separated list of the ids of those relying diffs.\n")
            .append("- Each <dependency> element provides code outside the chapter's own <diff> elements to resolve references to identifiers in edits, ")
            .append("and the methods and fields giving the values of the locals the edits read anew, ")
            .append("and the types the edits rely on for what they are (the class whose supertype a removed override falls back to, an exception a catch or a throws clause names, with its superclasses), ")
            .append("which read by their header alone, a line of ... standing for their body, ")
            .append("and carries location=\"<file>::<Type>#<member>\" and serves, a comma-separated list of the ids of the diffs in the chapter which rely on it.\n")
            .append("- A <dependency> whose code is changed by the pull request outside the chapter shows that change exactly as a <diff> does, but carries no id.\n")
            .append("- A <dependency> whose unchanged code uses code that diffs of the chapter change bridges them to the diffs it serves, which rely on those changes through it, ")
            .append("and carries uses, a comma-separated list of the ids of those diffs. They read before it unless they rely on it themselves.\n")
            .append("- Each <usage> element shows unchanged code using code that diffs of the chapter change, where those changes take effect, ")
            .append("and carries location=\"<file>::<Type>#<member>\" and uses, a comma-separated list of the ids of those diffs. ")
            .append("It shows the statements of the member using the changed code, with the lines of the code holding them (the signature of the member, ")
            .append("the headers and the ends of the enclosing statements), of the checks before them that leave when their condition holds and bear on what they read, ")
            .append("of the definitions of the values the enclosing conditions read, and of the statements reading the value they keep in a local, a line of ... standing for the lines left out. ")
            .append("Unchanged code a <diff> or a <dependency> already shows is not repeated in a <usage>, but where the <usage> needs it to read.\n")
            .append("- The elements are ordered by dependency in which a <diff> appears after all <diff> elements which it builds upon, and all the <dependency> elements which it relies on. ")
            .append("A <dependency> element sits as close as possible before the <diff> elements that rely on it. A <diff> element sits as close as possible before the <diff> elements that build upon it. ")
            .append("A <usage> element sits right after the last <diff> element it uses.\n\n");

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

    Set<Graph<Node, Edge>> graphs = Collections.newSetFromMap(new IdentityHashMap<>());
    elements.forEach(element -> graphs.add(element.getGraph()));
    Map<DiffNode, List<UsageBlock>> usagesAfter = usageBlocks(graphs, diffNodes, changeDiffNodes, diffBlocks, printed, sameCodeNodes,
            showingServes);

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
        List<Node> dependencyNodes = sameCodeNodes.getOrDefault(dependency, List.of(dependency));
        renderDependency(rendered, dependency, dependencyNodes, allChangeDiffNodes, renderedChangeDependencies, dependencies,
                changeDiffNodes, diffNodes, getOrderedIds(bridgedDiffNodes(dependencyNodes, graphs, changeDiffNodes), diffNodes));
      } else if (block instanceof DiffBlock diffBlock) {
        rendered.add(diffBlock.diffNode().render());
        for (UsageBlock usageBlock : usagesAfter.getOrDefault(diffBlock.diffNode(), List.of())) {
          rendered.add(usage(usageBlock.usages().get(0).getContextString(), getOrderedIds(usageBlock.used(), diffNodes),
                  usageBody(usageBlock.usages())));
        }
      }
    }

    // TODO: show group by indenting in a <sub_chapter>?
    return new RepresentedUnit(String.join("\n", rendered), new LinkedHashSet<>(diffNodes));
  }

  // The unchanged code using the changes of the unit, read right after the last diff it uses, one element per member using
  // them. Code a diff of the unit already shows is not printed again: that diff is served by the diffs it uses, when it
  // reads after them. Nor is a member whose usages a dependency all shows (the dependency uses the changes instead, see
  // bridgedDiffNodes), or test code, which exercises the changes rather than takes them into effect
  private static Map<DiffNode, List<UsageBlock>> usageBlocks(Set<Graph<Node, Edge>> graphs, List<DiffNode> diffNodes,
                                                             Map<Node, DiffNode> changeDiffNodes, List<DiffBlock> diffBlocks,
                                                             List<Block> printed, Map<Node, List<Node>> sameCodeNodes,
                                                             Map<DiffNode, Set<DiffNode>> showingServes) {
    Map<Node, Set<DiffNode>> usedDiffs = new HashMap<>();
    for (Graph<Node, Edge> graph : graphs) {
      for (Map.Entry<Node, DiffNode> changeDiffNode : changeDiffNodes.entrySet()) {
        if (!graph.containsVertex(changeDiffNode.getKey())) {
          continue;
        }
        for (Edge edge : graph.outgoingEdgesOf(changeDiffNode.getKey())) {
          Node target = graph.getEdgeTarget(edge);
          if (edge.getType().equals(EdgeType.DEF_USE) && target.isExtension()) {
            usedDiffs.computeIfAbsent(target, usage -> new HashSet<>()).add(changeDiffNode.getValue());
          }
        }
      }
    }

    // A dependency stands for the unchanged code of both revisions printed as one (see sameCodeNodes), which a usage of
    // the new revision lies in whichever of them is printed
    List<Node> printedDependencies = new ArrayList<>();
    for (Block block : printed) {
      if (block instanceof DependencyBlock dependencyBlock) {
        for (Node node : sameCodeNodes.getOrDefault(dependencyBlock.dependency(), List.of(dependencyBlock.dependency()))) {
          printedDependencies.add(node.getDeclaration());
          Node alternative = node.getAlternative();
          if (alternative != null) {
            printedDependencies.add(alternative.getDeclaration());
          }
        }
      }
    }
    Map<String, UsageBlock> memberUsages = new LinkedHashMap<>();
    for (Node usage : usedDiffs.keySet().stream().sorted(Node.COMPARATOR).toList()) {
      Set<DiffNode> used = usedDiffs.get(usage);
      Optional<DiffBlock> showing = diffBlocks.stream()
              .filter(diffBlock -> diffBlock.shownLines().stream().anyMatch(shown -> usageWithinLines(usage, shown))).findFirst();
      if (showing.isPresent()) {
        int showingIndex = diffNodes.indexOf(showing.get().diffNode());
        for (DiffNode usedDiff : used) {
          if (diffNodes.indexOf(usedDiff) < showingIndex) {
            showingServes.computeIfAbsent(usedDiff, diffNode -> new HashSet<>()).add(showing.get().diffNode());
          }
        }
        continue;
      }
      if (TraversalPattern.isTest(usage.getPath())) {
        continue;
      }

      Node.Enclosing enclosing = usage.getEnclosing();
      String member = usage.getPath() + ":" + enclosing.memberStart() + ":" + enclosing.memberEnd();
      UsageBlock usageBlock = memberUsages.computeIfAbsent(member, key -> new UsageBlock(new ArrayList<>(), new HashSet<>()));
      usageBlock.usages().add(usage);
      usageBlock.used().addAll(used);
    }
    // Code a dependency shows is not repeated, but a usage of the member it is read with keeps it, as the declaration of
    // the local holding a field whose uses the usage shows
    memberUsages.values().removeIf(usageBlock -> usageBlock.usages().stream()
            .allMatch(usage -> printedDependencies.stream().anyMatch(dependency -> holds(dependency, usage))));

    Map<DiffNode, List<UsageBlock>> usagesAfter = new HashMap<>();
    for (UsageBlock usageBlock : memberUsages.values()) {
      DiffNode last = usageBlock.used().stream().max(Comparator.comparingInt(diffNodes::indexOf)).orElseThrow();
      usagesAfter.computeIfAbsent(last, diffNode -> new ArrayList<>()).add(usageBlock);
    }
    return usagesAfter;
  }

  private static boolean holds(Node holder, Node node) {
    return holder.getSrcDst().equals(node.getSrcDst()) && holder.getPath().equals(node.getPath())
            && holder.getTree().getPos() <= node.getTree().getPos() && node.getTree().getEndPos() <= holder.getTree().getEndPos();
  }

  // The code of the usages with the code of their member holding them (see Node#getEnclosing), a line of ... standing for
  // the lines left out
  private static String usageBody(List<Node> usages) {
    String content = usages.get(0).getFileContent();
    List<Integer> lineStarts = new ArrayList<>(List.of(0));
    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n') {
        lineStarts.add(i + 1);
      }
    }
    String[] lines = content.split("\n", -1);

    TreeSet<Integer> shown = new TreeSet<>();
    Set<Integer> hidden = new HashSet<>();
    for (Node usage : usages) {
      Node.Enclosing enclosing = usage.getEnclosing();
      for (int[] part : enclosing.parts()) {
        addLines(shown, lineStarts, part[0], part[1]);
      }
      for (int[] body : enclosing.hidden()) {
        // A body holding another usage of the member is shown for that usage
        if (usages.stream().anyMatch(other -> body[0] <= other.getTree().getPos() && other.getTree().getEndPos() - 1 <= body[1])) {
          continue;
        }
        for (int line = lineOf(lineStarts, body[0]) + 1; line < lineOf(lineStarts, body[1]); line++) {
          hidden.add(line);
        }
      }
    }
    shown.removeAll(hidden);
    // A usage whose uses are known reads by the code holding them (see Node#getEnclosing), others whole
    for (Node usage : usages) {
      if (!usage.hasUses()) {
        addLines(shown, lineStarts, usage.getRM().signatureStart().getPos(), usage.getTree().getEndPos() - 1);
      }
    }

    int indentation = shown.stream().filter(line -> !lines[line].isBlank())
            .mapToInt(line -> lines[line].length() - lines[line].stripLeading().length()).min().orElse(0);
    List<String> body = new ArrayList<>();
    Integer previous = null;
    for (int line : shown) {
      if (previous != null && line > previous + 1) {
        for (int skipped = previous + 1; skipped < line; skipped++) {
          if (!lines[skipped].isBlank()) {
            String skippedLine = lines[skipped];
            int skippedIndentation = skippedLine.length() - skippedLine.stripLeading().length();
            body.add(" ".repeat(Math.max(0, skippedIndentation - indentation)) + "...");
            break;
          }
        }
      }
      body.add(lines[line].length() < indentation ? lines[line].strip() : lines[line].substring(indentation).stripTrailing());
      previous = line;
    }
    return String.join("\n", body);
  }

  private static void addLines(Set<Integer> shown, List<Integer> lineStarts, int start, int end) {
    int startLine = lineOf(lineStarts, start);
    int endLine = lineOf(lineStarts, Math.max(start, end));
    for (int line = startLine; line <= endLine; line++) {
      shown.add(line);
    }
  }

  private static int lineOf(List<Integer> lineStarts, int offset) {
    int index = Collections.binarySearch(lineStarts, offset);
    return index >= 0 ? index : -index - 2;
  }

  private static String usage(String location, List<String> usedIds, String body) {
    return "<usage location=\"" + location + "\" uses=\"" + String.join(", ", usedIds) + "\">\n" + body + "\n</usage>";
  }

  private static String printedCode(Node dependency) {
    return dependency.getContextString() + "\n" + dependency.getDeclaration().dependencyContent();
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

  // A usage a diff shows: its statement within the window and the code of it using the changes printed (see
  // revealRelated)
  private static boolean usageWithinLines(Node usage, ShownLines shown) {
    if (usage.getUses().isEmpty() || shown.hidden().isEmpty()) {
      return withinLines(usage, shown);
    }
    if (!usage.getSrcDst().equals(shown.srcDst()) || !usage.getPath().equals(shown.path())) {
      return false;
    }
    TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(usage.getTree(), usage.getFileContent());
    return shown.top() <= range.startLine() && range.endLine() <= shown.bottom() && usage.getUses().stream().allMatch(use -> {
      TreeUtilFunctions.LineRange useRange = TreeUtilFunctions.getLineRange(use, usage.getFileContent());
      return shown.shows(useRange.startLine(), useRange.endLine());
    });
  }

  private static boolean withinLines(Node node, ShownLines shown) {
    if (!node.getSrcDst().equals(shown.srcDst()) || !node.getPath().equals(shown.path())) {
      return false;
    }

    TreeUtilFunctions.LineRange nodeRange = TreeUtilFunctions.getLineRange(node.getTree(), node.getFileContent());
    return shown.shows(declarationStartLine(node), nodeRange.endLine());
  }

  // The doc comment and the annotations leading a declaration are not what a diff shows it by: a diff of a method body
  // shows the method from its signature on
  private static int declarationStartLine(Node node) {
    return TreeUtilFunctions.getLineRange(node.getRM().signatureStart(), node.getFileContent()).startLine();
  }

  // A body is printed from the start of the code it is the body of, as its header (a signature, a condition, a resource
  // list) cut at the line of its brace reads as broken code, unless it is printed as it is (see RMInfo#bodyFrom). An
  // expression is printed with the rest of the simple statement it is in, as a chain cut at its lines reads as broken
  // code too. Within that window the lines bearing on no change are left out, as in a usage (see hideApartBodies,
  // hideOtherItems, hideUnrelatedCode): an agent reads the change with what it relies on and what relies on it, and is
  // not handed the rest of a long member or list to tell from it
  private static DiffSide contextSide(Node context, Set<Node> changes) {
    String content = context.getFileContent();
    TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(context.getTree(), content);
    RMInfo rm = context.getRM();
    if (!rm.body()) {
      Tree statement = rm.simpleStatement();
      if (statement == null) {
        // A changed condition of an if whose branch leaves on every path decides the code after it too, which runs
        // instead of the branch as an else would
        int elseEnd = context.getImplicitElseEnd();
        if (elseEnd >= 0 && changes.stream().anyMatch(change -> context.getConditions().stream()
                .anyMatch(condition -> change.getTree().getPos() < condition.getEndOffset()
                        && condition.getStartOffset() < change.getTree().getEndPos()))) {
          int elseEndLine = (int) content.substring(0, elseEnd).chars().filter(c -> c == '\n').count() + 1;
          return new DiffSide(context, changes, range.startLine(), Math.max(range.endLine(), elseEndLine));
        }
        DiffSide side = new DiffSide(context, changes);
        hideOtherItems(side, context.getTree(), changes);
        return side;
      }

      TreeUtilFunctions.LineRange statementRange = TreeUtilFunctions.getLineRange(statement, content);
      DiffSide side = new DiffSide(context, changes, Math.min(statementRange.startLine(), range.startLine()),
              Math.max(statementRange.endLine(), range.endLine()));
      // Within a statement, what runs apart is what bears on no change: the bodies of its lambdas and anonymous classes
      if (!context.getApartBodies(statement).isEmpty()) {
        hideUnrelated(side, statement, side.top, side.bottom, changes);
      }
      hideOtherItems(side, statement, changes);
      return side;
    }
    if (rm.bodyFrom() == null) {
      return new DiffSide(context, changes);
    }

    int headerStart = TreeUtilFunctions.getLineRange(rm.bodyFrom(), content).startLine();
    DiffSide side = new DiffSide(context, changes, Math.min(headerStart, range.startLine()), range.endLine());
    if (context.isMemberBody()) {
      // The body keeps the signature and the lines down to its brace, and its end, the code the changes affect and the
      // definitions of what they read
      hideUnrelated(side, context.getTree(), range.startLine(), range.endLine(), changes);
      List<Integer> lineStarts = lineStarts(content);
      List<Tree> targets = changes.stream().map(Node::getTree).toList();
      for (int[] part : Stream.concat(context.getEffectParts(targets).stream(), context.getDefinitionParts(targets).stream()).toList()) {
        for (int line = lineOf(lineStarts, part[0]) + 1; line <= lineOf(lineStarts, part[1]) + 1; line++) {
          side.hidden.remove(line);
        }
      }
    }
    return side;
  }

  private static List<Integer> lineStarts(String content) {
    List<Integer> lineStarts = new ArrayList<>(List.of(0));
    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n') {
        lineStarts.add(i + 1);
      }
    }
    return lineStarts;
  }

  private static void addLines(Set<Integer> lines, TreeUtilFunctions.LineRange range) {
    for (int line = range.startLine(); line <= range.endLine(); line++) {
      lines.add(line);
    }
  }

  // The code of the window (a member's body, a statement) bearing on none of its changes, read as ...: the window keeps
  // its first lines down to the given one and its last lines from the given one, and for each change:
  // - the statement holding it, but the bodies running apart within that statement (block lambdas, anonymous classes),
  //   when the change lies in a lambda or an anonymous class of the statement itself: they are other callbacks than the
  //   changed one, as the success listener beside a changed failure handler (es_103817), and a body holding another
  //   change keeps what that change keeps of it; a change of the call itself, as the period a task is scheduled at
  //   (rocketmq_8626), bears on the bodies the call runs with it
  // - the first and last lines of the code holding it within the window, as the call and the lambda a statement lies in,
  //   or the anonymous class and the method, which tell where the change runs
  // - the statements next to the one holding it, which tell where it is in its block, as the check a new statement is
  //   placed before (bazel_25203 setExitCode before the cache hit check): a simple statement whole, as the arguments of
  //   the calls new assertions count (es_116075), a composite one by its first and last lines
  // - the control parts of its statement (see Node#getStatementControlParts): the conditionals, loops and catch clauses
  //   holding it, the guards bearing on what it reads and the readers of the values it gives
  // - in a member's body, the code what the changes write reaches through names, the state of objects included (see
  //   Node#getEffectParts), and the definitions of the locals they read, followed back (see Node#getDefinitionParts)
  // A statement of the window related to no change by place, control or data flow tells nothing of it, and printing a
  // member whole for one statement (onStart of 170 lines, a static block of 100) buries the change. The code a usage
  // prints of an unchanged member is chosen the same way. The dependencies and the usages within the window are shown in
  // place too (see revealRelated)
  private static void hideUnrelated(DiffSide side, Tree window, int keptTop, int keptBottom, Set<Node> changes) {
    String content = side.context.getFileContent();
    List<Integer> lineStarts = lineStarts(content);
    Set<Integer> kept = new HashSet<>();
    for (int line = side.top; line <= keptTop; line++) {
      kept.add(line);
    }
    for (int line = keptBottom; line <= side.bottom; line++) {
      kept.add(line);
    }
    for (Node change : changes) {
      Tree tree = change.getTree();
      addLines(kept, TreeUtilFunctions.getLineRange(tree, content));
      RMInfo rm = change.getRM();
      Tree statement = rm.statementTree();
      if (statement != null && window.getPos() <= statement.getPos() && statement.getEndPos() <= window.getEndPos()
              && statement.getPos() <= tree.getPos() && tree.getEndPos() <= statement.getEndPos()) {
        Set<Integer> statementLines = new HashSet<>();
        addLines(statementLines, TreeUtilFunctions.getLineRange(statement, content));
        // A body holding another change keeps what that change keeps of it, as any body does
        if (inCallback(rm.statement(), tree)) {
          for (int[] body : change.getApartBodies(statement)) {
            if (body[0] <= tree.getPos() && tree.getEndPos() <= body[1] + 1) {
              continue;
            }
            for (int line = lineOf(lineStarts, body[0]) + 2; line < lineOf(lineStarts, body[1]) + 1; line++) {
              statementLines.remove(line);
            }
          }
        }
        kept.addAll(statementLines);
        Tree parent = statement.getParent();
        if (parent != null && statement != window) {
          List<Tree> siblings = parent.getChildren();
          int index = siblings.indexOf(statement);
          for (int neighbour : new int[]{index - 1, index + 1}) {
            if (neighbour >= 0 && neighbour < siblings.size()) {
              Tree sibling = siblings.get(neighbour);
              TreeUtilFunctions.LineRange lines = TreeUtilFunctions.getLineRange(sibling, content);
              if (change.isCompositeStatement(sibling)) {
                kept.add(lines.startLine());
                kept.add(lines.endLine());
              } else {
                addLines(kept, lines);
              }
            }
          }
        }
      }
      for (Tree holder = tree.getParent(); holder != null && holder != window.getParent(); holder = holder.getParent()) {
        TreeUtilFunctions.LineRange lines = TreeUtilFunctions.getLineRange(holder, content);
        kept.add(lines.startLine());
        kept.add(lines.endLine());
      }
      Node.Enclosing control = change.getStatementControlParts();
      Set<Integer> controlLines = new HashSet<>();
      for (int[] part : control.parts()) {
        for (int line = lineOf(lineStarts, part[0]) + 1; line <= lineOf(lineStarts, Math.max(part[0], part[1])) + 1; line++) {
          controlLines.add(line);
        }
      }
      // A lambda or an anonymous class within that code reads by its first and last lines, as in a usage
      for (int[] apart : control.hidden()) {
        for (int line = lineOf(lineStarts, apart[0]) + 2; line < lineOf(lineStarts, apart[1]) + 1; line++) {
          controlLines.remove(line);
        }
      }
      kept.addAll(controlLines);
    }
    for (int line = side.top; line <= side.bottom; line++) {
      if (!kept.contains(line)) {
        side.hidden.add(line);
      }
    }
  }

  // Whether the tree lies in a lambda or an anonymous class of the statement, as RM gives them
  private static boolean inCallback(@Nullable gr.uom.java.xmi.decomposition.AbstractStatement statement, Tree tree) {
    if (statement == null) {
      return false;
    }
    List<LocationInfo> callbacks = new ArrayList<>();
    statement.getLambdas().forEach(lambda -> callbacks.add(lambda.getLocationInfo()));
    statement.getAnonymousClassDeclarations().forEach(anonymous -> callbacks.add(anonymous.getLocationInfo()));
    return callbacks.stream().anyMatch(callback -> callback.getStartOffset() < tree.getPos() && tree.getEndPos() <= callback.getEndOffset());
  }

  // The dependencies and the usages of the changes lying within the window of the side are shown in place, as the local
  // a change reads declared above it or an unchanged statement of the member reading a field a change writes (netty_14803
  // the assert on the id the new assignment sets), or calling a changed method (netty_14031 the read loops calling
  // shouldStopReading): within the member they read where they run, rather than apart. A usage shows by the code of it
  // using the changes and its first line, as its statement may be a whole loop
  private static void revealRelated(DiffSide side, Graph<Node, Edge> graph) {
    if (side.hidden.isEmpty()) {
      return;
    }
    String content = side.context.getFileContent();
    for (Node node : graph.vertexSet()) {
      if (!node.isExtension() || !node.getSrcDst().equals(side.context.getSrcDst()) || !node.getPath().equals(side.context.getPath())) {
        continue;
      }
      TreeUtilFunctions.LineRange lines = TreeUtilFunctions.getLineRange(node.getTree(), content);
      if (lines.startLine() < side.top || side.bottom < lines.endLine()) {
        continue;
      }
      boolean related = graph.edgesOf(node).stream().anyMatch(edge -> edge.getType().equals(EdgeType.DEF_USE)
              && !(graph.getEdgeSource(edge) == node ? graph.getEdgeTarget(edge) : graph.getEdgeSource(edge)).isExtension());
      if (!related) {
        continue;
      }
      List<TreeUtilFunctions.LineRange> revealed = new ArrayList<>();
      if (node.getUses().isEmpty()) {
        revealed.add(lines);
      } else {
        revealed.add(new TreeUtilFunctions.LineRange(lines.startLine(), 0, lines.startLine(), 0));
        node.getUses().forEach(use -> revealed.add(TreeUtilFunctions.getLineRange(use, content)));
      }
      for (TreeUtilFunctions.LineRange range : revealed) {
        for (int line = range.startLine(); line <= range.endLine(); line++) {
          side.hidden.remove(line);
        }
      }
    }
  }

  // Every line the diff prints reads within the code holding it: a construct of the window holding a printed line keeps its
  // first and last lines, as the loop and the else branch a kept statement lies in (bazel_23374), so that what is left out
  // never breaks the nesting the printed lines read by
  private static void keepStructure(DiffSide side) {
    if (side.hidden.isEmpty()) {
      return;
    }
    String content = side.context.getFileContent();
    Tree root = side.context.getTree();
    while (root.getParent() != null) {
      TreeUtilFunctions.LineRange parent = TreeUtilFunctions.getLineRange(root.getParent(), content);
      if (parent.startLine() < side.top || side.bottom < parent.endLine()) {
        break;
      }
      root = root.getParent();
    }
    Set<Integer> printed = new HashSet<>();
    for (int line = side.top; line <= side.bottom; line++) {
      if (!side.hidden.contains(line)) {
        printed.add(line);
      }
    }
    Set<Integer> structure = new HashSet<>();
    Deque<Tree> pending = new ArrayDeque<>(List.of(root));
    while (!pending.isEmpty()) {
      Tree tree = pending.pop();
      TreeUtilFunctions.LineRange lines = TreeUtilFunctions.getLineRange(tree, content);
      if (lines.endLine() - lines.startLine() < 2) {
        continue;
      }
      boolean holdsPrinted = false;
      for (int line = lines.startLine() + 1; line < lines.endLine() && !holdsPrinted; line++) {
        holdsPrinted = printed.contains(line) && !side.hidden.contains(line);
      }
      if (holdsPrinted) {
        // A list of items is no construct of its own: its lines are those of its items (see hideOtherItems)
        if (!isItemList(tree, side.context)) {
          structure.add(lines.startLine());
          structure.add(lines.endLine());
        }
        tree.getChildren().forEach(pending::push);
      }
    }
    side.hidden.removeAll(structure);
  }

  // A comment right above a line the diff prints reads with it, as the comment explaining a statement kept next to a
  // change (netty_14031: why doBeginRead sets the read flag, next to the forced read it no longer makes)
  private static void keepLeadingComments(DiffSide side) {
    if (side.hidden.isEmpty()) {
      return;
    }
    List<Integer> lineStarts = lineStarts(side.context.getFileContent());
    List<int[]> comments = side.context.getFileComments().stream()
            .map(comment -> new int[]{lineOf(lineStarts, comment[0]) + 1, lineOf(lineStarts, comment[1]) + 1})
            .filter(lines -> side.top <= lines[0] && lines[1] <= side.bottom).sorted(Comparator.comparingInt(lines -> -lines[0])).toList();
    boolean kept = true;
    while (kept) {
      kept = false;
      for (int[] lines : comments) {
        int next = lines[1] + 1;
        if (side.hidden.contains(lines[0]) && next <= side.bottom && !side.hidden.contains(next)) {
          for (int line = lines[0]; line <= lines[1]; line++) {
            kept |= side.hidden.remove(line);
          }
        }
      }
    }
  }

  // The items of a list of the code the changes do not touch, as the other settings of a list of 390 a change registers
  // one more in (es_109236), read as ...: a changed item reads with the code holding the list, which tells what it is (see
  // keepStructure), and with the items next to it, which tell where it is; the others are items of their own. A list is
  // the children of one tree of the same kind, each on lines of its own, but the members of an annotation
  // Whether the tree is a list of items: two expressions or more of one kind; the statements of a block run in order, and
  // the members of an annotation each have a role of their own, as the name of an option beside its converter, so they
  // are none
  private static boolean isItemList(Tree tree, Node context) {
    List<Tree> items = tree.getChildren();
    return items.size() >= 2 && items.stream().map(item -> item.getType().name).distinct().count() == 1
            && !context.isInLeadingPart(tree) && items.stream().noneMatch(context::isStatement);
  }

  private static void hideOtherItems(DiffSide side, Tree statement, Set<Node> changes) {
    String content = side.context.getFileContent();
    // A side with no change of its own leaves to its counterpart what is printed of the lines both hold (see
    // unchangedLine): it keeps the first and the last lines of its window only
    if (changes.isEmpty()) {
      for (int line = side.top + 1; line < side.bottom; line++) {
        side.hidden.add(line);
      }
      return;
    }
    for (Node change : changes) {
      for (Tree list = change.getTree().getParent(); list != null && list != statement.getParent(); list = list.getParent()) {
        List<Tree> items = list.getChildren();
        if (!isItemList(list, side.context)) {
          continue;
        }
        Set<Integer> touched = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
          Tree item = items.get(i);
          if (changes.stream().anyMatch(other -> item.getPos() < other.getTree().getEndPos() && other.getTree().getPos() < item.getEndPos())) {
            touched.add(i);
          }
        }
        if (touched.isEmpty()) {
          continue;
        }
        Set<Integer> keptLines = new HashSet<>();
        List<TreeUtilFunctions.LineRange> others = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
          TreeUtilFunctions.LineRange lines = TreeUtilFunctions.getLineRange(items.get(i), content);
          int index = i;
          if (touched.stream().anyMatch(j -> Math.abs(j - index) <= 1)) {
            addLines(keptLines, lines);
          } else {
            others.add(lines);
          }
        }
        for (TreeUtilFunctions.LineRange lines : others) {
          for (int line = lines.startLine(); line <= lines.endLine(); line++) {
            if (!keptLines.contains(line)) {
              side.hidden.add(line);
            }
          }
        }
      }
    }
  }

  // A dependency changed by another unit is shown as the diff presenting that change there, once for all
  // of its changes the unit relies on
  private static void renderDependency(List<String> rendered, Node dependency, List<Node> dependencyNodes,
                                       Map<Node, DiffNode> allChangeDiffNodes, Set<DiffNode> renderedChangeDependencies,
                                       UnitDependencies dependencies, Map<Node, DiffNode> changeDiffNodes, List<DiffNode> diffNodes,
                                       List<String> usedIds) {
    DiffNode changeDependency = allChangeDiffNodes.get(dependency);
    if (changeDependency == null) {
      rendered.add(dependency(dependency.getContextString(),
              getOrderedIds(servedDiffNodes(dependencyNodes, dependencies, changeDiffNodes), diffNodes), usedIds,
              dependency.getDeclaration().dependencyContent()));
      return;
    }

    if (!renderedChangeDependencies.add(changeDependency)) {
      return;
    }

    List<Node> changes = new ArrayList<>();
    changes.addAll(changeDependency.getSrcChanges());
    changes.addAll(changeDependency.getDstChanges());
    List<String> servedIds = getOrderedIds(servedDiffNodes(changes, dependencies, changeDiffNodes), diffNodes);
    rendered.add(dependency(changeDependency.getLocation(), servedIds, List.of(), changeDependency.renderBody()));
  }

  // The diffs of the unit whose changes unchanged code printed as a dependency uses, making it a bridge between them and
  // the diffs it serves (see HunkNetwork#processBridges)
  private static Set<DiffNode> bridgedDiffNodes(List<Node> dependencyNodes, Set<Graph<Node, Edge>> graphs,
                                                Map<Node, DiffNode> changeDiffNodes) {
    Set<DiffNode> bridged = new HashSet<>();
    for (Graph<Node, Edge> graph : graphs) {
      for (Node dependencyNode : dependencyNodes) {
        if (!graph.containsVertex(dependencyNode)) {
          continue;
        }
        for (Edge edge : graph.incomingEdgesOf(dependencyNode)) {
          DiffNode used = changeDiffNodes.get(graph.getEdgeSource(edge));
          if (edge.getType().equals(EdgeType.DEF_USE) && used != null) {
            bridged.add(used);
          }
        }
      }
    }
    return bridged;
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
    blocks = addControlLines(mergeOverlappingDiffs(blocks, length), graph, length);
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
        Set<Integer> hidden = new HashSet<>(shown.hidden());
        hidden.addAll(other.hidden());
        shownLines.add(new ShownLines(shown.srcDst(), shown.path(), shown.top(), other.bottom(), hidden));
      }
      List<Control> controls = new ArrayList<>(first.controls());
      controls.addAll(second.controls());
      List<Control> counterparts = new ArrayList<>(first.counterparts());
      counterparts.addAll(second.counterparts());
      DiffBlock mergedBlock = new DiffBlock(DiffNode.of(firstNode.getSrcPath(), firstNode.getDstPath(), srcChanges, dstChanges,
              firstNode.getLocation(), lines, length), shownLines, controls, counterparts);

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
    // A diff leaving out lines its windows share prints them on neither side alike
    for (ShownLines shown : first.shownLines()) {
      ShownLines other = second.shownLines().stream()
              .filter(side -> side.srcDst().equals(shown.srcDst()) && side.path().equals(shown.path())).findFirst().orElse(null);
      if (other != null && !(shown.shows(other.top(), shown.bottom()) && other.shows(other.top(), shown.bottom()))) {
        return 0;
      }
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
  // that diff rather than printed again by one of their own. A diff with no context in the revision of a change, as a
  // new file printed against the lines its moved code left, does not take that change: it reads on its own, linked to
  // the diff as where the code went
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

        DiffContexts contexts = resolveContexts(groups.get(i), graph);
        for (int j = 0; j < groups.size(); j++) {
          Set<Node> changes = MergeGroup.changesOf(groups.get(j));
          if (i == j || changes.isEmpty() || !changes.stream().allMatch(change -> showing.shownLines().stream()
                  .anyMatch(shown -> changeWithinLines(change, shown)))
                  || changes.stream().anyMatch(change -> (change.isSrc() ? contexts.srcContext() : contexts.dstContext()) == null)) {
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

    // The changes of a declaration sharing a line with changes printed at a location, as a field whose comment moved onto
    // its line, are printed there with them: the line reads once, with every change of it
    List<Map<Node, Set<Node>>> merged = mergeMovesWithin(mergeSharingLines(located), graph);
    for (Map<Node, Set<Node>> group : new ArrayList<>(isolated)) {
      Map<Node, Set<Node>> sharing = merged.stream().filter(other -> shareLine(group, other)).findFirst().orElse(null);
      Map<Node, Set<Node>> relocated = sharing == null ? null : relocate(group, sharing);
      if (relocated != null) {
        isolated.remove(group);
        located.add(relocated);
        merged = mergeSharingLines(located);
      }
    }
    isolated.addAll(merged);
    return isolated;
  }

  // Code moved within one location, as fields reordered in their class, reads as one diff with the code it left: apart,
  // the diff of the old lines and the one of the new lines each print the lines of the other, so a line reads as removed
  // twice, or as removed and unchanged
  private static List<Map<Node, Set<Node>>> mergeMovesWithin(List<Map<Node, Set<Node>>> groups, Graph<Node, Edge> graph) {
    List<Map<Node, Set<Node>>> merged = new ArrayList<>(groups);
    boolean widened = true;
    while (widened) {
      widened = false;
      search:
      for (Map<Node, Set<Node>> group : merged) {
        for (Map<Node, Set<Node>> other : merged) {
          if (group != other && sameLocation(group, other) && MergeGroup.changesOf(group).stream()
                  .anyMatch(change -> change.getMappingTargets(graph).stream().anyMatch(MergeGroup.changesOf(other)::contains))) {
            merged.remove(other);
            merged.remove(group);
            Map<Node, Set<Node>> joined = new HashMap<>(group);
            for (Map.Entry<Node, Set<Node>> context : other.entrySet()) {
              Node sameSide = joined.keySet().stream().filter(candidate -> candidate.getSrcDst().equals(context.getKey().getSrcDst()))
                      .findFirst().orElse(context.getKey());
              Set<Node> changes = new HashSet<>(joined.getOrDefault(sameSide, Set.of()));
              changes.addAll(context.getValue());
              joined.put(sameSide, changes);
            }
            merged.add(joined);
            widened = true;
            break search;
          }
        }
      }
    }
    return merged;
  }

  // Whether the location contexts of both groups declare the same type, in either revision
  private static boolean sameLocation(Map<Node, Set<Node>> group, Map<Node, Set<Node>> other) {
    Set<String> types = new HashSet<>();
    for (Node context : group.keySet()) {
      if (!context.getNodeType().equals(NodeType.LOCATION_CONTEXT) || context.getRM().declaredClass() == null) {
        return false;
      }
      types.add(context.getRM().declaredClass().getName());
    }
    for (Node context : other.keySet()) {
      if (!context.getNodeType().equals(NodeType.LOCATION_CONTEXT) || context.getRM().declaredClass() == null
              || !types.contains(context.getRM().declaredClass().getName())) {
        return false;
      }
    }
    return true;
  }

  // The changes of the group by the location contexts of the other group on their sides; none when the other group has
  // no location context on a side the group changes
  @Nullable
  private static Map<Node, Set<Node>> relocate(Map<Node, Set<Node>> group, Map<Node, Set<Node>> other) {
    Map<Node, Set<Node>> relocated = new HashMap<>();
    for (Map.Entry<Node, Set<Node>> context : group.entrySet()) {
      Node location = other.keySet().stream().filter(candidate -> candidate.getNodeType().equals(NodeType.LOCATION_CONTEXT)
              && candidate.getSrcDst().equals(context.getKey().getSrcDst())).findFirst().orElse(null);
      if (location == null) {
        return null;
      }
      relocated.computeIfAbsent(location, key -> new HashSet<>()).addAll(context.getValue());
    }
    return relocated;
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

  // Whether a change of the group and one of the other are printed on one line (see MergeGroup#shareLine)
  private static boolean shareLine(Map<Node, Set<Node>> group, Map<Node, Set<Node>> other) {
    for (Node change : MergeGroup.changesOf(group)) {
      for (Node otherChange : MergeGroup.changesOf(other)) {
        if (MergeGroup.shareLine(change, otherChange)) {
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
      revealRelated(src, graph);
      keepLeadingComments(src);
      keepStructure(src);
    }
    if (dstContext != null) {
      dst = contextSide(dstContext, dstChanges);
      revealRelated(dst, graph);
      keepLeadingComments(dst);
      keepStructure(dst);
    }

    List<DiffNode.DiffLine> diffLines;
    String location;
    boolean printsContext = true;
    DiffSide controlled = null;
    DiffSide counterpart = null;
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
      location = changesLocation(srcChanges, dstChanges);
    } else if (contexts.locationContext() && srcChanges.size() == 1 && dstChanges.size() == 1
            && sharesLinesWithin(src, dst, srcContext, dstContext, graph)) {
      // A change updated in place reads against its counterpart, which nothing of the context is printed around
      Node srcChange = srcChanges.iterator().next();
      Node dstChange = dstChanges.iterator().next();
      src = new DiffSide(srcChange, srcChanges);
      dst = new DiffSide(dstChange, dstChanges);
      diffLines = mergedLines(src, dst, mappingStores(srcChange, dstChange), graph);
      location = changesLocation(srcChanges, dstChanges);
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
      // A changed line holding code the other revision holds too, as a field whose initializer moves away, is printed
      // with the lines holding that code there, or it would read as removed (added) whole
      boolean bothChanged = !srcChanges.isEmpty() && !dstChanges.isEmpty();
      List<MappingStore> mappingStores = bothChanged ? mappingStores(srcContext, dstContext) : List.of();
      if (src != null) {
        diffLines.addAll(src.changeLines(isWholeFile(srcContext, graph) ? unrepresentedLines(srcContext) : Set.of(),
                bothChanged ? partnerLines(dst, src, mappingStores, graph) : Set.of()));
      }
      if (dst != null) {
        diffLines.addAll(dst.changeLines(isWholeFile(dstContext, graph) ? unrepresentedLines(dstContext) : Set.of(),
                bothChanged ? partnerLines(src, dst, mappingStores, graph) : Set.of()));
      }

      Set<Node> located = srcChanges.isEmpty() ? dstChanges : srcChanges;
      location = located.isEmpty() ? "" : MergeGroup.sortNodes(located).get(0).getContextString();
    } else if (src != null && dst != null) {
      diffLines = mergedLines(src, dst, mappingStores(srcContext, dstContext), graph);
      location = diffLocation(srcContext, dstContext);
      controlled = dst;
      counterpart = src;
    } else {
      // One revision alone holds this context, so there is no counterpart to merge it against:
      // it is printed whole, and what its changes cover carries the marker
      DiffSide side = src != null ? src : dst;
      side.tagChanges(null, List.of(), Set.of());
      diffLines = side.taggedLines();
      location = side.context.getInclusiveContextString();
      controlled = side;
    }

    diffLines = collapseHidden(diffLines);
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
        shownLines.add(new ShownLines(src.context.getSrcDst(), src.context.getPath(), src.top, src.bottom, Set.copyOf(src.hidden)));
      }
      if (dst != null) {
        shownLines.add(new ShownLines(dst.context.getSrcDst(), dst.context.getPath(), dst.top, dst.bottom, Set.copyOf(dst.hidden)));
      }
    }

    return new DiffBlock(diffNode, shownLines,
            controlled == null ? List.of() : List.of(new Control(controlled.context, controlled.top, controlled.bottom)),
            counterpart == null ? List.of() : List.of(new Control(counterpart.context, counterpart.top, counterpart.bottom)));
  }

  // Each diff printed with the code deciding whether and how often the code of its window runs, above and below that
  // window (see controlLines). Applied once diffs sharing lines are merged, as one window then reads with it once. Diffs
  // of one member printed one after the other whose control lines hold the window of another, or which print the same
  // control lines, as two edits of one loop, read as one diff, printing that code once; what the later ones rely on is
  // printed before it
  private static List<Block> addControlLines(List<Block> blocks, Graph<Node, Edge> graph, int length) {
    List<Block> result = new ArrayList<>();
    List<Controlled> group = new ArrayList<>();
    List<Block> held = new ArrayList<>();
    for (Block block : blocks) {
      Controlled controlled = block instanceof DiffBlock diffBlock ? controlled(diffBlock, graph) : null;
      if (block instanceof DependencyBlock) {
        (group.isEmpty() ? result : held).add(block);
        continue;
      }
      if (!group.isEmpty() && (controlled == null || group.stream().noneMatch(member -> member.readsWith(controlled)))) {
        result.add(render(group, length));
        result.addAll(held);
        group.clear();
        held.clear();
      }
      if (controlled == null) {
        result.add(block);
        continue;
      }
      result.addAll(held);
      held.clear();
      group.add(controlled);
    }
    if (!group.isEmpty()) {
      result.add(render(group, length));
    }
    result.addAll(held);
    return result;
  }

  // A diff with the window of the side its control lines are of, and those lines
  private record Controlled(DiffBlock block, Node context, int top, int bottom, Set<Integer> lines,
                            Map<Node, Set<Integer>> counterpartLines) {
    // Whether the two diffs read as one: they show one member of one revision, and the control lines of one hold lines
    // of the window of the other, or both print the same control lines, as two edits of one loop
    boolean readsWith(Controlled other) {
      return context.getSrcDst().equals(other.context.getSrcDst()) && context.getPath().equals(other.context.getPath())
              && block.diffNode().getLocation().equals(other.block.diffNode().getLocation())
              && (lines.stream().anyMatch(line -> other.top <= line && line <= other.bottom)
              || other.lines.stream().anyMatch(line -> top <= line && line <= bottom)
              || lines.stream().anyMatch(other.lines::contains));
    }
  }

  @Nullable
  private static Controlled controlled(DiffBlock diffBlock, Graph<Node, Edge> graph) {
    if (diffBlock.controls().isEmpty()) {
      return null;
    }
    Control first = diffBlock.controls().get(0);
    int top = diffBlock.controls().stream().mapToInt(Control::top).min().orElseThrow();
    int bottom = diffBlock.controls().stream().mapToInt(Control::bottom).max().orElseThrow();
    // The changes of the diff in that revision, which tell the cases whose condition they edit
    DiffNode diffNode = diffBlock.diffNode();
    List<Tree> targets = (first.context().isSrc() ? diffNode.getSrcChanges() : diffNode.getDstChanges()).stream()
            .map(Node::getTree).toList();
    Set<Integer> lines = new TreeSet<>();
    for (Control control : diffBlock.controls()) {
      if (control.context().getSrcDst().equals(first.context().getSrcDst()) && control.context().getPath().equals(first.context().getPath())) {
        lines.addAll(controlLines(control.context(), top, bottom, graph, targets));
      }
    }
    // A signature the changes edit is printed by the diff of its own, which shows the parameters it declares: printed
    // again above the body, with ... for its changed lines, it would repeat that diff and hide what it changes
    Set<Integer> changed = changeLines(first.context(), graph);
    Set<Integer> signature = partLines(first.context(), first.context().getMemberSignature(), List.of());
    if (signature.stream().anyMatch(changed::contains)) {
      lines.removeAll(signature);
    }
    // The code of the other revision reading a parameter, as a removed statement, reads with the signature of the member
    // too, which the lines of this revision print: a signature both revisions hold as it is, printed once, shows the
    // parameters of both (see render)
    Map<Node, Set<Integer>> counterpartLines = new LinkedHashMap<>();
    for (Control counterpart : diffBlock.counterparts()) {
      List<int[]> parameterParts = counterpart.context().getParameterParts();
      if (parameterParts.isEmpty() || signature.stream().anyMatch(changed::contains)) {
        continue;
      }
      counterpartLines.computeIfAbsent(counterpart.context(), context -> new TreeSet<>())
              .addAll(partLines(counterpart.context(), parameterParts, List.of()));
      Set<Integer> shownSignature = new TreeSet<>(signature);
      shownSignature.removeIf(line -> top <= line && line <= bottom);
      lines.addAll(shownSignature);
    }
    return new Controlled(diffBlock, first.context(), top, bottom, lines, counterpartLines);
  }

  // The diffs as one, their windows in the order of their lines with the control lines above, between and below them, a
  // line of ... standing for the lines left out
  private static DiffBlock render(List<Controlled> group, int length) {
    if (group.size() == 1 && group.get(0).lines().isEmpty()) {
      return group.get(0).block();
    }
    List<Controlled> windows = group.stream().sorted(Comparator.comparingInt(Controlled::top)).toList();
    TreeSet<Integer> control = new TreeSet<>();
    windows.forEach(window -> control.addAll(window.lines()));
    control.removeIf(line -> windows.stream().anyMatch(window -> window.top() <= line && line <= window.bottom()));

    Node context = windows.get(0).context();
    String[] fileLines = context.getFileContent().split("\n", -1);
    List<DiffNode.DiffLine> lines = new ArrayList<>();
    Integer previous = null;
    for (Controlled window : windows) {
      for (int line : previous == null ? control.headSet(window.top()) : control.subSet(previous, false, window.top(), false)) {
        addGap(lines, fileLines, previous, line);
        lines.add(new DiffNode.DiffLine(' ', fileLines[line - 1]));
        previous = line;
      }
      addGap(lines, fileLines, previous, window.top());
      lines.addAll(window.block().diffNode().getDiffLines());
      previous = window.bottom();
    }
    for (int line : control.tailSet(previous, false)) {
      addGap(lines, fileLines, previous, line);
      lines.add(new DiffNode.DiffLine(' ', fileLines[line - 1]));
      previous = line;
    }

    DiffNode first = windows.get(0).block().diffNode();
    List<Node> srcChanges = new ArrayList<>();
    List<Node> dstChanges = new ArrayList<>();
    List<ShownLines> shownLines = new ArrayList<>();
    List<Control> controls = new ArrayList<>();
    for (Controlled window : windows) {
      srcChanges.addAll(window.block().diffNode().getSrcChanges());
      dstChanges.addAll(window.block().diffNode().getDstChanges());
      shownLines.addAll(window.block().shownLines());
      controls.addAll(window.block().controls());
    }
    for (int line : control) {
      shownLines.add(new ShownLines(context.getSrcDst(), context.getPath(), line, line));
    }
    // A line of the other revision reading as a printed control line, as the signature both revisions hold, is shown by it
    Set<String> printed = new HashSet<>();
    control.forEach(line -> printed.add(fileLines[line - 1].trim()));
    List<Control> counterparts = new ArrayList<>();
    for (Controlled window : windows) {
      counterparts.addAll(window.block().counterparts());
      for (Map.Entry<Node, Set<Integer>> counterpart : window.counterpartLines().entrySet()) {
        String[] counterpartLines = counterpart.getKey().getFileContent().split("\n", -1);
        for (int line : counterpart.getValue()) {
          String text = counterpartLines[line - 1].trim();
          if (!text.isEmpty() && printed.contains(text)) {
            shownLines.add(new ShownLines(counterpart.getKey().getSrcDst(), counterpart.getKey().getPath(), line, line));
          }
        }
      }
    }
    return new DiffBlock(DiffNode.of(first.getSrcPath(), first.getDstPath(), srcChanges, dstChanges, first.getLocation(), lines, length),
            shownLines, controls, counterparts);
  }

  // The lines outside the window holding the code that decides whether and how often the code of the context runs (see
  // Node#getControlParts), but the lines holding changes, which their own diffs show
  private static Set<Integer> controlLines(Node context, int top, int bottom, Graph<Node, Edge> graph, List<Tree> targets) {
    Node.Enclosing control = context.getControlParts(targets);
    Set<Integer> lines = partLines(context, control.parts(), control.hidden());
    lines.removeIf(line -> top <= line && line <= bottom);
    lines.removeAll(changeLines(context, graph));
    return lines;
  }

  // The lines of the parts, as ranges of offsets of the file of the context, without the lines within the hidden bodies
  // but their first and last: a lambda or an anonymous class within that code reads by them, as in a usage
  private static Set<Integer> partLines(Node context, List<int[]> parts, List<int[]> hidden) {
    List<Integer> lineStarts = lineStarts(context.getFileContent());
    TreeSet<Integer> lines = new TreeSet<>();
    for (int[] part : parts) {
      for (int line = lineOf(lineStarts, part[0]) + 1; line <= lineOf(lineStarts, Math.max(part[0], part[1])) + 1; line++) {
        lines.add(line);
      }
    }
    for (int[] body : hidden) {
      for (int line = lineOf(lineStarts, body[0]) + 2; line < lineOf(lineStarts, body[1]) + 1; line++) {
        lines.remove(line);
      }
    }
    return lines;
  }

  // The lines the changes of the revision of the context lie on in its file, which their own diffs show
  private static Set<Integer> changeLines(Node context, Graph<Node, Edge> graph) {
    String content = context.getFileContent();
    Set<Integer> lines = new HashSet<>();
    for (Node node : graph.vertexSet()) {
      if (node.isContext() || node.isExtension() || !node.getSrcDst().equals(context.getSrcDst())
              || !node.getPath().equals(context.getPath())) {
        continue;
      }
      TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(node.getTree(), content);
      for (int line = range.startLine(); line <= range.endLine(); line++) {
        lines.add(line);
      }
    }
    return lines;
  }

  private static void addGap(List<DiffNode.DiffLine> lines, String[] fileLines, @Nullable Integer previous, int next) {
    if (previous == null || next <= previous + 1) {
      return;
    }
    for (int skipped = previous + 1; skipped < next; skipped++) {
      String skippedLine = fileLines[skipped - 1];
      if (!skippedLine.isBlank()) {
        lines.add(new DiffNode.DiffLine(' ', skippedLine.substring(0, skippedLine.length() - skippedLine.stripLeading().length()) + "..."));
        return;
      }
    }
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
    Set<Tree> arrivedLeaves = Collections.newSetFromMap(new IdentityHashMap<>());
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
          arrivedLeaves.add(leaf);
        }

        TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(leaf, content);
        for (int line = range.startLine(); line <= range.endLine(); line++) {
          holdsCode[line] = true;
          brings[line] |= arrived;
        }
      }
    }
    // The code around what arrives, which stays or goes elsewhere, as the header and the end of a try the moved call
    // leaves, is not printed against the changes either: its own diff shows it removed. The lines kept end with the
    // trees holding arriving code that start within them, as the call closing on lines of no code of their own
    while (top <= bottom && !brings[top]) {
      top++;
    }
    if (top > bottom) {
      return null;
    }
    int arrivingBottom = Integer.MIN_VALUE;
    for (Node counterpart : origins) {
      List<Tree> trees = new ArrayList<>(counterpart.getTree().getDescendants());
      trees.add(counterpart.getTree());
      for (Tree tree : trees) {
        TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(tree, content);
        if (range.startLine() >= top && range.endLine() > arrivingBottom
                && (tree.getChildren().isEmpty() ? arrivedLeaves.contains(tree) : tree.getDescendants().stream().anyMatch(arrivedLeaves::contains))) {
          arrivingBottom = range.endLine();
        }
      }
    }
    bottom = Math.min(bottom, arrivingBottom);
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

  // Whether the code each change shares its lines with lies on the lines of the other change, as when a change is updated
  // in place, and not elsewhere, as a field declaration whose initializer moves away
  private static boolean sharesLinesWithin(DiffSide src, DiffSide dst, Node srcContext, Node dstContext, Graph<Node, Edge> graph) {
    List<MappingStore> mappingStores = mappingStores(srcContext, dstContext);
    int[] srcLines = changedLines(src);
    int[] dstLines = changedLines(dst);
    return partnerLines(src, dst, mappingStores, graph).stream().allMatch(line -> dstLines[0] <= line && line <= dstLines[1])
            && partnerLines(dst, src, mappingStores, graph).stream().allMatch(line -> srcLines[0] <= line && line <= srcLines[1]);
  }

  // The lines of the partner holding the code the changes of the changed side share their lines with, but the ones
  // holding changes, which their own diffs print
  private static Set<Integer> partnerLines(DiffSide changed, DiffSide partner, List<MappingStore> mappingStores, Graph<Node, Edge> graph) {
    Set<Node> changes = new HashSet<>(changed.changes);
    List<Integer> partnerLines = new ArrayList<>();
    for (Node change : changed.changes) {
      TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(change.getTree(), change.getFileContent());
      collectPartnerLines(changed.context.getTree(), changed, changes, new int[]{range.startLine(), range.endLine()}, mappingStores,
              partner.context, partnerLines);
    }

    Set<Integer> lines = new HashSet<>(partnerLines);
    for (Node node : graph.vertexSet()) {
      if (node.isBase() && node.getSrcDst().equals(partner.context.getSrcDst()) && node.getPath().equals(partner.context.getPath())) {
        TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(node.getTree(), node.getFileContent());
        lines.removeIf(line -> range.startLine() <= line && line <= range.endLine());
      }
    }
    return lines;
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

    closeChangedLines(src, dst, mappingStores, inPlace);
    propagateChanges(src, dst, anchors, inPlace);
    hideUnchangedCommentLines(src, dst, inPlace);
    return mergeSides(src, dst, meetingPoints(src, dst, anchors));
  }

  // A comment the changes edit reads by the paragraphs holding the lines they change (see hideUnchangedLines), and by its
  // first and last lines, which tell it is a comment and where it ends; its other lines read as ... (see collapseHidden). A
  // copyright year bumped in a license header printed whole is fifteen lines of a text every file holds around one line
  // that changes, and a reviewer has to compare them line by line to find it. A comment is edited on a side when it holds
  // a changed line there, or when it shares a line in place with one edited on the other side (text added on one side
  // only). A comment the changes do not touch is kept as it is (see keepLeadingComments), as it explains the code it is on
  private static void hideUnchangedCommentLines(DiffSide src, DiffSide dst, Set<List<Integer>> inPlace) {
    List<int[]> srcComments = windowComments(src);
    List<int[]> dstComments = windowComments(dst);
    List<int[]> srcEdited = srcComments.stream().filter(lines -> holdsChangedLine(src, lines)).toList();
    List<int[]> dstEdited = dstComments.stream().filter(lines -> holdsChangedLine(dst, lines)).toList();
    for (int[] lines : srcComments) {
      if (srcEdited.contains(lines) || inPlace.stream().anyMatch(pair -> lines[0] <= pair.get(0) && pair.get(0) <= lines[1]
              && dstEdited.stream().anyMatch(edited -> edited[0] <= pair.get(1) && pair.get(1) <= edited[1]))) {
        hideUnchangedLines(src, lines);
      }
    }
    for (int[] lines : dstComments) {
      if (dstEdited.contains(lines) || inPlace.stream().anyMatch(pair -> lines[0] <= pair.get(1) && pair.get(1) <= lines[1]
              && srcEdited.stream().anyMatch(edited -> edited[0] <= pair.get(0) && pair.get(0) <= edited[1]))) {
        hideUnchangedLines(dst, lines);
      }
    }
  }

  // The comments of the file spanning lines within the window of the side, by their first and last lines. A doc comment
  // is none: it is the contract of its declaration, which an edit of it reads against whole, as a deprecation added
  // under a description it makes stale (netty_14031 getEpollMode, still saying the default is edge-triggered)
  private static List<int[]> windowComments(DiffSide side) {
    List<Integer> lineStarts = lineStarts(side.context.getFileContent());
    return side.context.getFileComments().stream()
            .map(comment -> new int[]{lineOf(lineStarts, comment[0]) + 1, lineOf(lineStarts, comment[1]) + 1})
            .filter(lines -> lines[0] < lines[1] && side.top <= lines[0] && lines[1] <= side.bottom).toList();
  }

  private static boolean holdsChangedLine(DiffSide side, int[] lines) {
    return side.changed.stream().anyMatch(line -> lines[0] <= line && line <= lines[1]);
  }

  // The lines of the comment out of the paragraphs holding its changed lines, but its first and last lines. A comment
  // reads in paragraphs, which a line holding nothing but the markers of the comment ends (as RM gives the text of the
  // comment without them), as a description, the block of its tags or a plan a test expects: a changed line reads with
  // the rest of its paragraph, which tells what it is part of (the parents of a changed node of a plan, the other
  // authors of a list one more joins), while another paragraph tells nothing of it
  private static void hideUnchangedLines(DiffSide side, int[] lines) {
    Set<Integer> kept = new HashSet<>();
    Set<Integer> blank = side.context.getBlankCommentLines();
    for (int line = lines[0] + 1; line < lines[1]; line++) {
      if (!side.changed.contains(line)) {
        continue;
      }
      kept.add(line);
      for (int above = line - 1; above > lines[0] && !blank.contains(above); above--) {
        kept.add(above);
      }
      for (int below = line + 1; below < lines[1] && !blank.contains(below); below++) {
        kept.add(below);
      }
    }
    for (int line = lines[0] + 1; line < lines[1]; line++) {
      if (!kept.contains(line)) {
        side.hidden.add(line);
      }
    }
  }


  // Moved code is changed where it holds code the other side does not hold as it is; the statement holding an edit is
  // not changed but by the edit
  private static List<DiffNode.DiffLine> mergedMovedLines(DiffSide src, DiffSide dst, List<MappingStore> mappingStores) {
    List<int[]> anchors = buildAnchors(src, dst, mappingStores);
    src.tagMoved(src.context.getTree(), dst, mappingStores);
    dst.tagMoved(dst.context.getTree(), src, mappingStores);

    closeChangedLines(src, dst, mappingStores, Set.of());
    propagateChanges(src, dst, anchors, Set.of());
    return mergeSides(src, dst, meetingPoints(src, dst, anchors));
  }

  // A changed line printed without the lines holding the counterparts of its code would read as adding that code:
  // the lines of the other side holding code of a changed line are changed too, but a line both revisions hold as it is
  // in place, which reads the same in both (see DiffSide#tag)
  private static void closeChangedLines(DiffSide src, DiffSide dst, List<MappingStore> mappingStores, Set<List<Integer>> inPlace) {
    List<int[]> leafLines = new ArrayList<>();
    collectLeafLines(src.context.getTree(), src, dst, mappingStores, leafLines);
    Set<Integer> srcSameLines = sameLines(src, dst, inPlace, true);
    Set<Integer> dstSameLines = sameLines(src, dst, inPlace, false);

    boolean closed = false;
    while (!closed) {
      closed = true;
      for (int[] leafLine : leafLines) {
        if (src.changed.contains(leafLine[0]) && !dstSameLines.contains(leafLine[1]) && dst.changed.add(leafLine[1])) {
          closed = false;
        }
        if (dst.changed.contains(leafLine[1]) && !srcSameLines.contains(leafLine[0]) && src.changed.add(leafLine[0])) {
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

  // The line holding the code of a changed line on the other side is changed too, but a line both revisions hold as it is
  // in place, as the line of an else-if branch now following the closing brace of the branch before the removed one
  private static void propagateChanges(DiffSide src, DiffSide dst, List<int[]> anchors, Set<List<Integer>> inPlace) {
    Set<Integer> srcSameLines = sameLines(src, dst, inPlace, true);
    Set<Integer> dstSameLines = sameLines(src, dst, inPlace, false);
    for (int[] anchor : anchors) {
      if (src.changed.contains(anchor[0]) && !dstSameLines.contains(anchor[1])) {
        dst.changed.add(anchor[1]);
      }
      if (dst.changed.contains(anchor[1]) && !srcSameLines.contains(anchor[0])) {
        src.changed.add(anchor[0]);
      }
    }
  }

  // The lines of one side that the in-place chain pairs with a line of the same code on the other side
  private static Set<Integer> sameLines(DiffSide src, DiffSide dst, Set<List<Integer>> inPlace, boolean srcSide) {
    Set<Integer> sameLines = new HashSet<>();
    for (List<Integer> linePair : inPlace) {
      if (sameLine(src, linePair.get(0), dst, linePair.get(1))) {
        sameLines.add(srcSide ? linePair.get(0) : linePair.get(1));
      }
    }
    return sameLines;
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
      merged.add(unchangedLine(src, anchor[0], dst, anchor[1]));

      srcLine = anchor[0] + 1;
      dstLine = anchor[1] + 1;
    }
    mergeSegment(merged, src, src.bottom, dst, dst.bottom, srcLine, dstLine);

    return merged;
  }

  private static void mergeSegment(List<DiffNode.DiffLine> merged, DiffSide src, int srcTo, DiffSide dst, int dstTo,
                                   int srcFrom, int dstFrom) {
    while (srcFrom <= srcTo && dstFrom <= dstTo && pairs(src, srcFrom, dst, dstFrom)) {
      merged.add(unchangedLine(src, srcFrom, dst, dstFrom));
      srcFrom++;
      dstFrom++;
    }

    List<DiffNode.DiffLine> tail = new ArrayList<>();
    while (srcTo >= srcFrom && dstTo >= dstFrom && pairs(src, srcTo, dst, dstTo)) {
      tail.add(0, unchangedLine(src, srcTo, dst, dstTo));
      srcTo--;
      dstTo--;
    }

    // Within the rest, a line neither side changes that both hold alike is code in place between changed lines, as an
    // unchanged callback between two edited arguments: it pairs with its counterpart, in order (a longest common
    // subsequence), rather than reading as removed and added
    int srcCount = Math.max(0, srcTo - srcFrom + 1);
    int dstCount = Math.max(0, dstTo - dstFrom + 1);
    int[][] common = new int[srcCount + 1][dstCount + 1];
    for (int i = srcCount - 1; i >= 0; i--) {
      for (int j = dstCount - 1; j >= 0; j--) {
        common[i][j] = pairs(src, srcFrom + i, dst, dstFrom + j) && !src.line(srcFrom + i).isBlank()
                ? common[i + 1][j + 1] + 1 : Math.max(common[i + 1][j], common[i][j + 1]);
      }
    }
    int i = 0;
    int j = 0;
    List<Integer> removed = new ArrayList<>();
    List<Integer> added = new ArrayList<>();
    while (i < srcCount || j < dstCount) {
      if (i < srcCount && j < dstCount && pairs(src, srcFrom + i, dst, dstFrom + j) && !src.line(srcFrom + i).isBlank()
              && common[i][j] == common[i + 1][j + 1] + 1) {
        flushUnpaired(merged, src, removed, dst, added);
        merged.add(unchangedLine(src, srcFrom + i, dst, dstFrom + j));
        i++;
        j++;
      } else if (j >= dstCount || i < srcCount && common[i + 1][j] >= common[i][j + 1]) {
        removed.add(srcFrom + i);
        i++;
      } else {
        added.add(dstFrom + j);
        j++;
      }
    }
    flushUnpaired(merged, src, removed, dst, added);
    merged.addAll(tail);
  }

  // The lines between two paired lines: a blank line on each side alone is the same blank line in place
  private static void flushUnpaired(List<DiffNode.DiffLine> merged, DiffSide src, List<Integer> removed, DiffSide dst,
                                    List<Integer> added) {
    if (removed.size() == 1 && added.size() == 1 && pairs(src, removed.get(0), dst, added.get(0))
            && src.line(removed.get(0)).isBlank()) {
      merged.add(unchangedLine(src, removed.get(0), dst, added.get(0)));
    } else {
      removed.forEach(line -> merged.add(new DiffNode.DiffLine('-', src.line(line))));
      added.forEach(line -> merged.add(new DiffNode.DiffLine('+', dst.line(line))));
    }
    removed.clear();
    added.clear();
  }

  // A line both revisions hold, left out when both sides leave it out (see DiffSide#hidden)
  private static DiffNode.DiffLine unchangedLine(DiffSide src, int srcLine, DiffSide dst, int dstLine) {
    return new DiffNode.DiffLine(src.hidden.contains(srcLine) && dst.hidden.contains(dstLine) ? HIDDEN : ' ', dst.line(dstLine));
  }

  // The prefix marking a line left out, until the lines left out are collapsed (see collapseHidden)
  private static final char HIDDEN = '\0';

  // The lines left out of a diff read as one line of ... at the indentation of the first of them, as the gaps of a usage
  // and of the control lines do. The line of ... takes a line of its own, so fewer than three lines left out are printed:
  // leaving out two lines would save one and hide code a reviewer reads in passing, as the body of a check next to a
  // change
  private static List<DiffNode.DiffLine> collapseHidden(List<DiffNode.DiffLine> lines) {
    List<DiffNode.DiffLine> collapsed = new ArrayList<>();
    for (int i = 0; i < lines.size(); ) {
      if (lines.get(i).prefix() != HIDDEN) {
        collapsed.add(lines.get(i++));
        continue;
      }
      int end = i;
      while (end < lines.size() && lines.get(end).prefix() == HIDDEN) {
        end++;
      }
      List<DiffNode.DiffLine> run = lines.subList(i, end);
      List<DiffNode.DiffLine> nonBlank = run.stream().filter(line -> !line.text().isBlank()).toList();
      if (nonBlank.size() < 3) {
        run.forEach(line -> collapsed.add(new DiffNode.DiffLine(' ', line.text())));
      } else {
        String first = nonBlank.get(0).text();
        collapsed.add(new DiffNode.DiffLine(' ', first.substring(0, first.length() - first.stripLeading().length()) + "..."));
      }
      i = end;
    }
    return collapsed;
  }

  private static boolean pairs(DiffSide src, int srcLine, DiffSide dst, int dstLine) {
    return !src.changed.contains(srcLine) && !dst.changed.contains(dstLine)
            && src.line(srcLine).trim().equals(dst.line(dstLine).trim());
  }

  private static String diffLocation(Node srcContext, Node dstContext) {
    return diffLocation(srcContext.getInclusiveContextString(), dstContext.getInclusiveContextString());
  }

  // Where the first changes of each revision live, which reads "<before> -> <after>" when the code is elsewhere in the
  // new revision, as a field pulled up to a superclass
  private static String changesLocation(Set<Node> srcChanges, Set<Node> dstChanges) {
    String srcLocation = srcChanges.isEmpty() ? "" : MergeGroup.sortNodes(srcChanges).get(0).getContextString();
    String dstLocation = dstChanges.isEmpty() ? "" : MergeGroup.sortNodes(dstChanges).get(0).getContextString();
    return diffLocation(srcLocation, dstLocation);
  }

  private static String diffLocation(String srcLocation, String dstLocation) {
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
    // The lines of the window left out, as they bear on no change (see contextSide); a line both revisions hold is left
    // out only when both sides leave it out, and a changed line never is
    private final Set<Integer> hidden = new HashSet<>();
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
      // A line both revisions hold as it is, in place, holds no code of its own: the code of a new or a removed parent
      // on it, as the else of an if a new else-if branch now holds, is the same code in both
      Set<Integer> sameLines = new HashSet<>();
      for (List<Integer> linePair : inPlace) {
        int line = context.isSrc() ? linePair.get(0) : linePair.get(1);
        inPlaceLines.add(line);
        if (other != null && line(line).trim().equals(other.line(context.isSrc() ? linePair.get(1) : linePair.get(0)).trim())) {
          sameLines.add(line);
        }
      }

      String content = context.getFileContent();
      TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, content);
      for (int line = Math.max(lineRange.startLine(), top); line <= Math.min(lineRange.endLine(), bottom); line++) {
        if (keptLines.contains(line) || sameLines.contains(line)) {
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

    // Whether the offset is the terminator ending a statement within the tree, the statement termination of the language
    // as RM's constants give it (none where a line ends a statement). On a line both revisions hold as it is, it is no
    // code of its own: a statement re-wrapped around code it carries over, as a creation returned before and assigned
    // now, ends on the same `);`
    private boolean isTerminator(Tree tree, int offset) {
      String terminator = PathFileUtils.getLang(context.getPath()).STATEMENT_TERMINATION.trim();
      if (terminator.isEmpty() || !context.getFileContent().startsWith(terminator, offset)) {
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

    private List<int[]> changeRanges(Set<Integer> bridged, Set<Integer> partnerLines) {
      List<int[]> changeRanges = new ArrayList<>();
      for (Node change : changes) {
        TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(change.getTree(), context.getFileContent());
        changeRanges.add(new int[]{lineRange.startLine(), lineRange.endLine()});
      }
      for (int partnerLine : partnerLines) {
        changeRanges.add(new int[]{partnerLine, partnerLine});
      }
      changeRanges.sort(Comparator.comparingInt(range -> range[0]));

      List<int[]> ranges = new ArrayList<>();
      for (int[] changeRange : changeRanges) {
        int startLine = changeRange[0];
        int endLine = changeRange[1];

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
        diffLines.add(new DiffNode.DiffLine(changed.contains(line) ? prefix : hidden.contains(line) ? HIDDEN : ' ', line(line)));
      }

      return diffLines;
    }

    public List<DiffNode.DiffLine> changeLines(Set<Integer> bridged, Set<Integer> partnerLines) {
      List<DiffNode.DiffLine> diffLines = new ArrayList<>();
      for (int[] range : this.changeRanges(bridged, partnerLines)) {
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

  private static String dependency(String location, List<String> servedIds, List<String> usedIds, String body) {
    StringBuilder sb = new StringBuilder("<dependency");

    if (!location.isEmpty()) {
      sb.append(" location=\"").append(location).append("\"");
    }
    if (!servedIds.isEmpty()) {
      sb.append(" serves=\"").append(String.join(", ", servedIds)).append("\"");
    }
    if (!usedIds.isEmpty()) {
      sb.append(" uses=\"").append(String.join(", ", usedIds)).append("\"");
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

  // The window of a side of a diff whose code the code deciding whether it runs is printed around (see addControlLines)
  private record Control(Node context, int top, int bottom) {
  }

  // A diff with the windows its control lines are printed around (see addControlLines), of one revision, and the windows
  // of the other revision, whose code reads with those lines
  private record DiffBlock(DiffNode diffNode, List<ShownLines> shownLines, List<Control> controls, List<Control> counterparts)
          implements Block {
  }

  // The usages of the changes in one member, with the diffs of the changes they use
  private record UsageBlock(List<Node> usages, Set<DiffNode> used) {
  }

  // The lines of a revision of a file that a diff prints: its window but the lines it leaves out within it (see
  // DiffSide#hidden)
  private record ShownLines(SrcDst srcDst, String path, int top, int bottom, Set<Integer> hidden) {
    private ShownLines(SrcDst srcDst, String path, int top, int bottom) {
      this(srcDst, path, top, bottom, Set.of());
    }

    // Whether the lines are printed: within the window and none left out
    private boolean shows(int from, int to) {
      if (from < top || bottom < to) {
        return false;
      }
      for (int line = from; line <= to; line++) {
        if (hidden.contains(line)) {
          return false;
        }
      }
      return true;
    }
  }
}
