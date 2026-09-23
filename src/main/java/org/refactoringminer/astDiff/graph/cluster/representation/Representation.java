package org.refactoringminer.astDiff.graph.cluster.representation;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.cluster.traverse.NarrativeElement;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;

import javax.annotation.Nullable;
import java.util.List;

public interface Representation {
  Representation DEFAULT = new RawRepresentation();

  String specification();
  NarrativeElement mergeGroup(MergeGroup mergeGroup, List<TraversalPattern> leaves, @Nullable List<Node> localSides, int length, Graph<Node, Edge> graph);
  NarrativeElement dependency(Node dependency, Graph<Node, Edge> graph);
}
