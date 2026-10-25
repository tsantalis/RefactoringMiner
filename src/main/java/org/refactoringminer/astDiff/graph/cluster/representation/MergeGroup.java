package org.refactoringminer.astDiff.graph.cluster.representation;

import com.github.gumtreediff.tree.Tree;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Context;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.EdgeType;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
import org.refactoringminer.astDiff.graph.RMInfo;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MergeGroup {
  public Set<Node> nodes;

  public MergeGroup(Set<Node> nodes) {
    this.nodes = nodes;
  }

  public List<Node> sources() {
    return sortNodes(nodes.stream().filter(Node::isSrc).collect(Collectors.toSet()));
  }

  public List<Node> targets() {
    return sortNodes(nodes.stream().filter(Node::isDst).collect(Collectors.toSet()));
  }

  public static List<Node> sortNodes(Set<Node> nodes) {
    return nodes.stream().sorted(Node.COMPARATOR).toList();
  }

  public static Set<Map<Node, Set<Node>>> aggregateByContextMapping(List<Node> nodes, Graph<Node, Edge> graph) {
    Set<Node> contexts = new HashSet<>(statementContexts(nodes, graph).values());

    Set<Node> parentContexts = new HashSet<>();
    for (Node subject : contexts) {
      boolean isParent = true;

      List<Node> subjectAlternatives = subject.isSrc() ? subject.getMappingTargets(graph) : subject.getMappingSources(graph);
      Node subjectAlternative = subjectAlternatives.isEmpty() ? null : subjectAlternatives.get(0);
      for (Node object : contexts) {
        if (subject.equals(object)) {
          continue;
        }

        if (subject.isDescendantOf(object)) {
          isParent = false;
          break;
        }

        if (subject.getSrcDst().equals(object.getSrcDst())) {
          continue;
        }

        if (subjectAlternative == null) {
          continue;
        }

        if (subjectAlternative.isDescendantOf(object)) {
          isParent = false;
          break;
        }
      }

      if (isParent) {
        parentContexts.add(subject);
      }
    }

    Set<Map<Node, Set<Node>>> result = new HashSet<>();

    for (Node parentContext : parentContexts) {
      Map<Node, Set<Node>> parentContextResult = new HashMap<>();

      Set<Node> descendants = new HashSet<>();
      for (Node node : nodes) {
        if (node.isDescendantOf(parentContext)) {
          descendants.add(node);
        }

        for (Node source : node.getMappingSources(graph)) {
          if (nodes.contains(source) && source.isDescendantOf(parentContext)) {
            descendants.add(source);
          }
        }
        for (Node target : node.getMappingTargets(graph)) {
          if (nodes.contains(target) && target.isDescendantOf(parentContext)) {
            descendants.add(target);
          }
        }
      }
      if (!descendants.isEmpty()) {
        parentContextResult.put(parentContext, descendants);
      }

      List<Node> alternatives = parentContext.isSrc() ? parentContext.getMappingTargets(graph) : parentContext.getMappingSources(graph);
      Node alternative = alternatives.isEmpty() ? null : alternatives.get(0);
      if (alternative != null) {
        Set<Node> alternativeDescendants = new HashSet<>();
        for (Node node : nodes) {
          if (node.isDescendantOf(alternative)) {
            alternativeDescendants.add(node);
          }

          for (Node source : node.getMappingSources(graph)) {
            if (nodes.contains(source) && source.isDescendantOf(alternative)) {
              alternativeDescendants.add(source);
            }
          }
          for (Node target : node.getMappingTargets(graph)) {
            if (nodes.contains(target) && target.isDescendantOf(alternative)) {
              alternativeDescendants.add(target);
            }
          }
        }
        if (!alternativeDescendants.isEmpty()) {
          parentContextResult.put(alternative, alternativeDescendants);
        }
      }

      result.add(parentContextResult);
    }

    Set<Node> coveredNodes = result.stream().map(Map::values).flatMap(Collection::stream).flatMap(Collection::stream).collect(Collectors.toSet());
    List<Node> contextlessNodes = nodes.stream().filter(node -> !coveredNodes.contains(node)).toList();
    result.addAll(aggregateBySuccession(contextlessNodes, graph));

    return result;
  }

  // The nearest semantic context of each node, except that the changes of one statement whose nearest contexts are
  // apart meet at the nearest context they share within that statement, so the statement reads as one
  private static Map<Node, Node> statementContexts(List<Node> nodes, Graph<Node, Edge> graph) {
    Map<Node, List<Node>> chains = new LinkedHashMap<>();
    Map<Node, Node> contexts = new LinkedHashMap<>();
    for (Node node : nodes) {
      List<Node> nodeContexts = node.getSemanticContexts(graph);
      if (nodeContexts.isEmpty()) {
        continue;
      }

      chains.put(node, nodeContexts);
      contexts.put(node, nodeContexts.get(0));
    }

    Map<Tree, List<Node>> statementsNodes = new IdentityHashMap<>();
    for (Node node : contexts.keySet()) {
      Tree statement = node.getRM().statementTree();
      if (statement != null) {
        statementsNodes.computeIfAbsent(statement, tree -> new ArrayList<>()).add(node);
      }
    }

    for (Map.Entry<Tree, List<Node>> statementNodes : statementsNodes.entrySet()) {
      List<Node> located = statementNodes.getValue();
      boolean widened = true;
      while (widened) {
        widened = false;
        for (int i = 0; i < located.size(); i++) {
          for (int j = i + 1; j < located.size(); j++) {
            Node subject = located.get(i);
            Node object = located.get(j);
            Node subjectContext = contexts.get(subject);
            Node objectContext = contexts.get(object);
            if (subjectContext.equals(objectContext) || subjectContext.isDescendantOf(objectContext)
                    || objectContext.isDescendantOf(subjectContext)) {
              continue;
            }

            Node shared = sharedContext(chains.get(subject), subjectContext, chains.get(object), objectContext,
                    statementNodes.getKey());
            if (shared == null) {
              continue;
            }

            contexts.put(subject, shared);
            contexts.put(object, shared);
            widened = true;
          }
        }
      }
    }

    return contexts;
  }

  @Nullable
  private static Node sharedContext(List<Node> subjectChain, Node subjectContext, List<Node> objectChain, Node objectContext,
                                    Tree statement) {
    List<Node> objectCandidates = objectChain.subList(objectChain.indexOf(objectContext), objectChain.size());
    for (Node candidate : subjectChain.subList(subjectChain.indexOf(subjectContext), subjectChain.size())) {
      Tree tree = candidate.getTree();
      if (tree != statement && !tree.getParents().contains(statement)) {
        return null;
      }
      if (objectCandidates.contains(candidate)) {
        return candidate;
      }
    }

    return null;
  }

  public static Set<Map<Node, Set<Node>>> aggregateBySuccession(List<Node> nodes, Graph<Node, Edge> graph) {
    Set<Node> allowedNodes = new HashSet<>(nodes);

    Set<Map<Node, Set<Node>>> result = new HashSet<>();

    Set<Node> visited = new HashSet<>();
    for (Node node : nodes) {
      if (visited.contains(node)) continue;

      Set<Node> component = new HashSet<>();
      Queue<Node> queue = new LinkedList<>();
      queue.add(node);
      visited.add(node);

      while (!queue.isEmpty()) {
        Node curr = queue.poll();
        component.add(curr);

        for (Edge edge : graph.edgesOf(curr)) {
          if (!edge.getType().equals(EdgeType.SUCCESSION)) {
            continue;
          }

          Node neighbor = graph.getEdgeSource(edge).equals(curr) ? graph.getEdgeTarget(edge) : graph.getEdgeSource(edge);
          if (!allowedNodes.contains(neighbor) || visited.contains(neighbor)) {
            continue;
          }

          visited.add(neighbor);
          queue.add(neighbor);
        }
      }

      List<Node> groupNodes = sortNodes(component);
      Node context = Context.get(graph, groupNodes.get(0)).stream()
              .filter(contextNode -> contextNode.getNodeType().equals(NodeType.LOCATION_CONTEXT)).toList().get(0);

      Map<Node, Set<Node>> group = new HashMap<>();
      group.put(context, new HashSet<>(groupNodes));
      result.add(group);
    }

    return result;
  }

  public static Map<Set<Node>, Set<Node>> aggregateByMapping(List<Node> nodes, Graph<Node, Edge> graph) {
    Set<Node> allowedNodes = new HashSet<>(nodes);
    for (Node node : nodes) {
      allowedNodes.addAll(node.getMappingSources(graph));
      allowedNodes.addAll(node.getMappingTargets(graph));
    }

    Map<Set<Node>, Set<Node>> result = new HashMap<>();

    Set<Node> visited = new HashSet<>();
    for (Node node : nodes) {
      if (visited.contains(node)) continue;

      Set<Node> component = new HashSet<>();
      Queue<Node> queue = new LinkedList<>();
      queue.add(node);
      visited.add(node);

      while (!queue.isEmpty()) {
        Node curr = queue.poll();
        component.add(curr);

        for (Node src : curr.getMappingSources(graph)) {
          if (!visited.contains(src)) {
            visited.add(src);
            queue.add(src);
          }
        }
        for (Node dst : curr.getMappingTargets(graph)) {
          if (!visited.contains(dst)) {
            visited.add(dst);
            queue.add(dst);
          }
        }
      }

      Set<Node> srcNodes = new HashSet<>();
      Set<Node> dstNodes = new HashSet<>();
      for (Node n : component) {
        if (!allowedNodes.contains(n)) continue;
        if (n.isSrc()) srcNodes.add(n);
        else dstNodes.add(n);
      }

      if (!srcNodes.isEmpty() && !dstNodes.isEmpty()) {
        result.put(srcNodes, dstNodes);
      }
    }

    return result;
  }

  public static List<MergeGroup> mappingGroups(List<Node> nodes, Graph<Node, Edge> graph) {
    return aggregateByMapping(nodes, graph).entrySet().stream().map(e -> {
      Set<Node> mappedNodes = new HashSet<>();
      mappedNodes.addAll(e.getKey());
      mappedNodes.addAll(e.getValue());
      return new MergeGroup(mappedNodes);
    }).toList();
  }

  public static List<MergeGroup> getAllMergeGroups(List<Node> mains, Graph<Node, Edge> graph) {
    List<MergeGroup> groups = new ArrayList<>();

    List<MergeGroup> mappingGroups = mappingGroups(mains, graph);
    groups.addAll(mappingGroups);

    // A mapped source is not a main, but its context is printed with the changes of its group, so two groups
    // sharing that context would print it twice
    Set<Node> contextNodes = new LinkedHashSet<>(mains);
    for (MergeGroup mappingGroup : mappingGroups) {
      contextNodes.addAll(mappingGroup.nodes);
    }
    Set<Map<Node, Set<Node>>> contextMappingGroups = aggregateByContextMapping(new ArrayList<>(contextNodes), graph);
    groups.addAll(contextMappingGroups.stream()
            .map(contextAggregatedNodes ->
                    new MergeGroup(contextAggregatedNodes.values().stream().flatMap(Set::stream).collect(Collectors.toSet()))).toList());
    groups.addAll(memberGroups(new ArrayList<>(contextNodes)));
    groups.addAll(headerGroups(new ArrayList<>(contextNodes)));

    List<Set<Node>> mergedGroups = new ArrayList<>();
    for (MergeGroup group : groups) {
      Set<Node> merged = new HashSet<>(group.nodes);
      boolean widened = true;
      while (widened) {
        widened = mergedGroups.removeIf(mergedGroup -> {
          if (Collections.disjoint(mergedGroup, merged)) {
            return false;
          }

          merged.addAll(mergedGroup);
          return true;
        });
      }
      mergedGroups.add(merged);
    }

    return mergedGroups.stream().map(MergeGroup::new).collect(Collectors.toCollection(ArrayList::new));
  }

  // The changes of one method read together, one after the other, rather than wherever what they rely on puts each
  private static List<MergeGroup> memberGroups(List<Node> nodes) {
    Map<String, Set<Node>> memberNodes = new LinkedHashMap<>();
    for (Node node : nodes) {
      UMLOperation member = node.getRM().operation();
      if (member != null) {
        memberNodes.computeIfAbsent(node.getSrcDst() + node.getPath() + "@" + member.getLocationInfo().getStartOffset(),
                key -> new HashSet<>()).add(node);
      }
    }

    return memberNodes.values().stream().filter(group -> group.size() > 1).map(MergeGroup::new).toList();
  }

  // The changes of the header of one type read together too, as the changes of a signature do
  private static List<MergeGroup> headerGroups(List<Node> nodes) {
    Map<String, Set<Node>> headerNodes = new LinkedHashMap<>();
    for (Node node : nodes) {
      UMLClass type = node.getRM().type();
      if (type != null && RMInfo.headerChild(node.getTree(), node.getRM().typeHeader()) != null) {
        headerNodes.computeIfAbsent(node.getSrcDst() + node.getPath() + "@" + type.getLocationInfo().getStartOffset(),
                key -> new HashSet<>()).add(node);
      }
    }

    return headerNodes.values().stream().filter(group -> group.size() > 1).map(MergeGroup::new).toList();
  }

  // The header of a declaration, which reads as one diff ending at the line declaring it: the header of a class, or all
  // of an operation but its body (its doc comment, annotations, modifiers and signature), as RM gives them
  public static List<Tree> declarationHeader(Node declaration) {
    return declaration.getRM().header();
  }

  // The part of the header of the declaration that the tree lies in
  @Nullable
  public static Tree headerChild(Tree tree, Node declaration) {
    return RMInfo.headerChild(tree, declaration.getRM().header());
  }

  public static List<Map<Node, Set<Node>>> orderContextGroups(Set<Map<Node, Set<Node>>> contextGroups,
                                                              Set<Node> mains, List<TraversalPattern> leaves) {
    Map<Node, Integer> leafIndex = leafIndex(leaves);
    Map<Node, Integer> changeIndex = changeIndex(mains);

    Comparator<Map<Node, Set<Node>>> byLatestLeaf =
            Comparator.comparingInt(group -> changesOf(group).stream()
                    .mapToInt(node -> leafIndex.getOrDefault(node, -1))
                    .max().orElse(-1));
    Comparator<Map<Node, Set<Node>>> byEarliestChange =
            Comparator.comparingInt(group -> changesOf(group).stream()
                    .mapToInt(node -> changeIndex.getOrDefault(node, Integer.MAX_VALUE))
                    .min().orElse(Integer.MAX_VALUE));
    Comparator<Map<Node, Set<Node>>> byContextPosition =
            Comparator.comparing((Map<Node, Set<Node>> group) -> MergeGroup.sortNodes(group.keySet()).get(0),
                    Node.COMPARATOR);

    return contextGroups.stream().sorted(byLatestLeaf.thenComparing(byEarliestChange).thenComparing(byContextPosition)).toList();
  }

  private static Map<Node, Integer> leafIndex(List<TraversalPattern> leaves) {
    Map<Node, Integer> leafIndex = new HashMap<>();
    for (int i = 0; i < leaves.size(); i++) {
      for (Node main : leaves.get(i).getMains()) {
        leafIndex.merge(main, i, Math::max);
      }
    }

    return leafIndex;
  }

  private static Map<Node, Integer> changeIndex(Set<Node> mains) {
    List<Node> printedChanges = new ArrayList<>();
    printedChanges.addAll(sortNodes(mains.stream().filter(Node::isSrc).collect(Collectors.toSet())));
    printedChanges.addAll(sortNodes(mains.stream().filter(Node::isDst).collect(Collectors.toSet())));

    Map<Node, Integer> changeIndex = new HashMap<>();
    for (int i = 0; i < printedChanges.size(); i++) {
      changeIndex.put(printedChanges.get(i), i);
    }
    return changeIndex;
  }

  public static Map<Integer, List<Node>> dependenciesIndex(List<Node> localSides, Map<Node, Set<Node>> mainsToSides,
                                                           List<Map<Node, Set<Node>>> contextGroups) {
    Map<Integer, List<Node>> placed = new HashMap<>();
    if (localSides.isEmpty()) {
      return placed;
    }

    Set<Node> dependencies = new HashSet<>(localSides);
    Map<Node, Set<Node>> relyingChanges = new HashMap<>();
    for (Map.Entry<Node, Set<Node>> mainToSides : mainsToSides.entrySet()) {
      for (Node side : mainToSides.getValue()) {
        if (dependencies.contains(side)) {
          relyingChanges.computeIfAbsent(side, s -> new HashSet<>()).add(mainToSides.getKey());
        }
      }
    }

    List<Set<Node>> changesPerGroup = contextGroups.stream().map(MergeGroup::changesOf).toList();
    for (Node dependency : localSides) {
      Set<Node> relying = relyingChanges.getOrDefault(dependency, Set.of());

      int index = 0;
      for (int i = 0; i < changesPerGroup.size(); i++) {
        if (!Collections.disjoint(changesPerGroup.get(i), relying)) {
          index = i;
          break;
        }
      }

      placed.computeIfAbsent(index, i -> new ArrayList<>()).add(dependency);
    }
    placed.replaceAll((index, deps) -> deps.stream().sorted(Node.DEPENDENCY_COMPARATOR).toList());

    return placed;
  }

  public static Set<Node> changesOf(Map<Node, Set<Node>> contextGroup) {
    return contextGroup.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
  }
}
