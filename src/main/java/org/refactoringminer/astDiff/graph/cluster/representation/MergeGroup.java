package org.refactoringminer.astDiff.graph.cluster.representation;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Context;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.EdgeType;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
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
    Set<Node> contexts = new HashSet<>();
    for (Node node : nodes) {
      List<Node> nodeContexts = node.getSemanticContexts(graph);
      if (nodeContexts.isEmpty()) {
        continue;
      }

      contexts.add(nodeContexts.get(0));
    }

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

    groups.addAll(mappingGroups(mains, graph));

    Set<Map<Node, Set<Node>>> contextMappingGroups = aggregateByContextMapping(mains, graph);
    groups.addAll(contextMappingGroups.stream()
            .map(contextAggregatedNodes ->
                    new MergeGroup(contextAggregatedNodes.values().stream().flatMap(Set::stream).collect(Collectors.toSet()))).toList());

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

  public static List<Map<Node, Set<Node>>> orderContextGroups(Set<Map<Node, Set<Node>>> contextGroups,
                                                              MergeGroup mergeGroup, List<TraversalPattern> leaves) {
    Map<Node, Integer> leafIndex = leafIndex(leaves);
    Map<Node, Integer> changeIndex = changeIndex(mergeGroup);

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

  private static Map<Node, Integer> changeIndex(MergeGroup mergeGroup) {
    List<Node> printedChanges = new ArrayList<>();
    printedChanges.addAll(mergeGroup.sources());
    printedChanges.addAll(mergeGroup.targets());

    Map<Node, Integer> changeIndex = new HashMap<>();
    for (int i = 0; i < printedChanges.size(); i++) {
      changeIndex.put(printedChanges.get(i), i);
    }
    return changeIndex;
  }

  public static Map<Integer, List<Node>> dependenciesIndex(@Nullable List<Node> localSides,
                                                           List<TraversalPattern> leaves, List<Map<Node, Set<Node>>> contextGroups) {
    Map<Integer, List<Node>> placed = new HashMap<>();
    if (localSides == null || localSides.isEmpty()) {
      return placed;
    }

    Set<Node> dependencies = new HashSet<>(localSides);
    Map<Node, Set<Node>> relyingChanges = new HashMap<>();
    for (TraversalPattern leaf : leaves) {
      List<Node> mains = leaf.getMains();
      for (Node side : leaf.getSides()) {
        if (dependencies.contains(side)) {
          relyingChanges.computeIfAbsent(side, s -> new HashSet<>()).addAll(mains);
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
    placed.replaceAll((index, deps) -> deps.stream().sorted(Node.COMPARATOR).toList());

    return placed;
  }

  public static Set<Node> changesOf(Map<Node, Set<Node>> contextGroup) {
    return contextGroup.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
  }
}
