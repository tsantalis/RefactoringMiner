package org.refactoringminer.astDiff.utils;

import com.github.gumtreediff.tree.TreeContext;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.HunkNetwork;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

public class Driver {

    public static Graph<Node, Edge> getGraph(ProjectASTDiff projectASTDiff) {
        HunkNetwork network = new HunkNetwork(projectASTDiff.getModelDiff(),
                projectASTDiff.getFileContentsBefore(), projectASTDiff.getFileContentsAfter());

        Set<ASTDiff> authoritativeDiffs = new LinkedHashSet<>(projectASTDiff.getDiffSet());
        Set<ASTDiff> diffSet = new LinkedHashSet<>(authoritativeDiffs);
        diffSet.addAll(projectASTDiff.getMoveDiffSet());
        for (ASTDiff diff : diffSet) {
            network.importDiff(diff, authoritativeDiffs.contains(diff));
        }

        network.importFiles();

        network.process();

        return network.getGraph();
    }
}