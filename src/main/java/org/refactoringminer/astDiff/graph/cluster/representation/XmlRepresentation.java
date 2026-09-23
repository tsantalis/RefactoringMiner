package org.refactoringminer.astDiff.graph.cluster.representation;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.*;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

class XmlRepresentation implements Representation {
  private static final String SEPARATOR = "...";

  public String specification() {
    StringBuilder spec = new StringBuilder();

    spec.append("### CHANGE REPRESENTATION\n")
            .append("- The chapter is not a textual diff. It is a structured view derived from an AST comparison of the two revisions, ")
            .append("in which related edits have already been grouped into <sub_chapter> elements.\n")
            .append("- A <sub_chapter> is one coherent unit of work: edits that are related either because they are the same code before and after — edited in place, relocated, or both — ")
            .append("or because they sit in a shared enclosing construct. Edits joined by either relation belong to the same <sub_chapter>; an edit joined by neither stands alone in one.\n")
            .append("- Edits are captured at expression and statement granularity, so a single logical change is routinely spread across several <change> blocks inside one <sub_chapter>.\n")
            .append("- A <change> holds one edit. A before_* element paired with an after_* element is the same code before and after that edit—one change, not two. ")
            .append("A lone <added> or <deleted> element is an insertion or a removal with no counterpart.\n")
            .append("- Element tags name the operation: <added>, <deleted>, <unchanged>, and the paired forms before_change/after_change (edited in place), ")
            .append("before_move/after_move (relocated intact), and before_move_and_change/after_move_and_change (both).\n")
            .append("- <context> shows the enclosing construct before and after the edit. It restates, in situ, the same code that the <change> elements isolate. ")
            .append("It is orientation, not additional change. A <context> heads the <change> elements that follow it and covers those only.\n")
            .append("- <dependency> contains code that the edits depend on, supplied so that identifiers resolve. It is not part of the change. ")
            .append("Inside a <sub_chapter> it sits immediately before the edits that rely on it; standing between <sub_chapter> elements, it serves the ones that follow it.\n")
            .append("- A `").append(SEPARATOR).append("` line separates one block from the next inside a <sub_chapter>; a block is either a <dependency> or a <context> together with its <change> elements.\n")
            .append("- Every element carries location=\"<file>::<Type>#<member>\", and changes have additional id=\"#XXXXX\", which identifies the change uniquely within the pull request.\n")
            .append("- The <sub_chapter> elements are ordered by dependency: each one appears after the code it builds on, so reading the chapter top to bottom follows the change from its foundations outward.\n")
            .append("- The blocks inside a <sub_chapter> follow the same rule, so an edit appears after the edits it relies on.\n")
            .append("- Neither order is the order of the code in the file, and the blocks carry no line numbers; the code inside <context> is where the real sequence is visible.\n\n");

    return spec.toString();
  }

  public NarrativeElement mergeGroup(MergeGroup mergeGroup, List<TraversalPattern> leaves, @Nullable List<Node> localSides,
                                     int length, Graph<Node, Edge> graph) {
    Set<Map<Node, Set<Node>>> groups = MergeGroup.aggregateByContextMapping(mergeGroup.nodes.stream().toList(), graph);
    List<Map<Node, Set<Node>>> contextGroups = MergeGroup.orderContextGroups(groups, mergeGroup, leaves);

    Map<Integer, List<Node>> indexDependencies = MergeGroup.dependenciesIndex(localSides, leaves, contextGroups);

    List<String> blocks = new ArrayList<>();
    for (int i = 0; i <= contextGroups.size(); i++) {
      for (Node dependency : indexDependencies.getOrDefault(i, List.of())) {
        blocks.add(dependency(dependency, graph).getContent());
      }

      if (i == contextGroups.size()) {
        break;
      }

      Map<Node, Set<Node>> contextGroup = contextGroups.get(i);
      Set<Node> changes = MergeGroup.changesOf(contextGroup);
      List<Node> sources = MergeGroup.sortNodes(changes.stream().filter(Node::isSrc).collect(Collectors.toSet()));
      List<Node> targets = MergeGroup.sortNodes(changes.stream().filter(Node::isDst).collect(Collectors.toSet()));
      if (sources.isEmpty() && targets.isEmpty()) {
        continue;
      }

      StringBuilder block = new StringBuilder();
      if (contextGroup.keySet().stream().allMatch(context -> context.getNodeType().equals(NodeType.SEMANTIC_CONTEXT))) {
        block.append(buildXmlContext(List.of(contextGroup.keySet()), graph)).append("\n");
      }
      block.append(buildXmlMappingHunk(sources, targets, graph));
      blocks.add(block.toString());
    }

    StringBuilder sb = new StringBuilder();
    sb.append("<sub_chapter>");
    for (int i = 0; i < blocks.size(); i++) {
      if (i > 0) {
        sb.append("\n    ").append(SEPARATOR);
      }
      sb.append("\n    ").append(blocks.get(i).replace("\n", "\n    "));
    }
    sb.append("\n</sub_chapter>");
    String content = sb.toString();

    Set<ReviewNode> anchoredNodes = new HashSet<>(mergeGroup.nodes);

    return new NarrativeElement(content, anchoredNodes, mergeGroup.nodes, graph);
  }

  private String buildXmlMappingHunk(List<Node> sources, List<Node> targets, Graph<Node, Edge> graph) {
    StringBuilder xmlOutput = new StringBuilder();
    xmlOutput.append("<change>");
    if (!sources.isEmpty()) {
      xmlOutput.append("\n    ");
      xmlOutput.append(String.join("\n    ", sources.stream()
              .map(n -> n.baseXml(graph).replace("\n", "\n    ")).toList()));
    }
    if (!targets.isEmpty()) {
      xmlOutput.append("\n    ");
      xmlOutput.append(String.join("\n    ", targets.stream()
              .map(n -> n.baseXml(graph).replace("\n", "\n    ")).toList()));
    }
    xmlOutput.append("\n</change>");
    return xmlOutput.toString();
  }

  private String buildXmlContext(Collection<Set<Node>> contextsSets, Graph<Node, Edge> graph) {
    return String.join("\n", contextsSets.stream().map(contexts -> {
      StringBuilder xmlOutput = new StringBuilder();
      xmlOutput.append("<context>");
      xmlOutput.append("\n    ").append(contexts.iterator().next().mappingXml(graph).replace("\n", "\n    "));
      xmlOutput.append("\n</context>");
      return xmlOutput.toString();
    }).toList());
  }

  public NarrativeElement dependency(Node dependency, Graph<Node, Edge> graph) {
    return new NarrativeElement("<dependency>\n    " + dependency.baseXml(graph).replace("\n", "\n    ") + "\n</dependency>",
            new HashSet<>(), Set.of(dependency), graph) ;
  }
}
