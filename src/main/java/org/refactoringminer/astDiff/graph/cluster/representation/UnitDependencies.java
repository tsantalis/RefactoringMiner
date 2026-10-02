package org.refactoringminer.astDiff.graph.cluster.representation;

import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;

import java.util.*;

class UnitDependencies {
  private final Set<Node> mains = new HashSet<>();
  private final Map<Node, Set<Node>> sidesToMains = new HashMap<>();
  private final List<List<Node>> globalSides = new ArrayList<>();
  private final List<List<Node>> localSides = new ArrayList<>();

  UnitDependencies(List<NarrativeElement> elements) {
    Map<Node, Integer> sideElementsCount = new HashMap<>();
    List<Set<Node>> elementsSides = new ArrayList<>();
    for (NarrativeElement element : elements) {
      Set<Node> elementSides = new HashSet<>();
      for (Map.Entry<Node, Set<Node>> mainToSides : element.getMainsToSides().entrySet()) {
        Node main = mainToSides.getKey();
        mains.add(main);
        for (Node side : mainToSides.getValue()) {
          sidesToMains.computeIfAbsent(side, s -> new HashSet<>()).add(main);
          elementSides.add(side);
        }
      }

      for (Node side : elementSides) {
        sideElementsCount.merge(side, 1, Integer::sum);
      }
      elementsSides.add(elementSides);
    }

    // A side of multiple elements is global and printed once, before the first element it exists in
    Set<Node> printedGlobalSides = new HashSet<>();
    for (Set<Node> elementSides : elementsSides) {
      List<Node> elementGlobalSides = new ArrayList<>();
      List<Node> elementLocalSides = new ArrayList<>();
      for (Node side : elementSides) {
        if (!isDependency(side)) {
          continue;
        }

        if (sideElementsCount.get(side) == 1) {
          elementLocalSides.add(side);
        } else if (printedGlobalSides.add(side)) {
          elementGlobalSides.add(side);
        }
      }

      globalSides.add(elementGlobalSides.stream().sorted(Node.COMPARATOR).toList());
      localSides.add(elementLocalSides.stream().sorted(Node.COMPARATOR).toList());
    }
  }

  // A changed side is already printed as a change when it is a main within the unit
  private boolean isDependency(Node side) {
    return !(side.isBase() && mains.contains(side));
  }

  List<Node> globalSidesBefore(int elementIndex) {
    return globalSides.get(elementIndex);
  }

  List<Node> localSidesOf(int elementIndex) {
    return localSides.get(elementIndex);
  }

  Set<Node> relyingMains(Node dependency) {
    return sidesToMains.getOrDefault(dependency, Set.of());
  }
}
