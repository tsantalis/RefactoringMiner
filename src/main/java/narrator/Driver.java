package narrator;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import narrator.graph.HunkNetwork;
import narrator.graph.Edge;
import narrator.graph.Node;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.*;

public class Driver {
    public static Graph<Node, Edge> getPullRequestGraph(String url) throws Exception {
        String repo = URLHelper.getRepo(url);
        String PR = URLHelper.getPRID(url);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtPullRequest(repo,
                Integer.parseInt(PR), 1000);
        return getGraph(projectASTDiff);
    }

    public static Graph<Node, Edge> getCommitGraph(String url) {
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);

        return getGraph(projectASTDiff);
    }

    private static Graph<Node, Edge> getGraph(ProjectASTDiff projectASTDiff) {
        Map<String, TreeContext> srcContexts = projectASTDiff.getParentContextMap();
        Map<String, TreeContext> dstContexts = projectASTDiff.getChildContextMap();
        Map<String, String> srcContents = projectASTDiff.getFileContentsBefore();
        Map<String, String> dstContents = projectASTDiff.getFileContentsAfter();
        HunkNetwork network = new HunkNetwork(projectASTDiff.getModelDiff(), srcContents, dstContents,
                srcContexts, dstContexts);

        Set<ASTDiff> diffSet = projectASTDiff.getDiffSet();

        for (ASTDiff diff : diffSet) {
            network.importHunks(diff.getAddedDstTrees(),
                    diff.getAllMappings().getMonoMappingStore());
        }

        List<String> diffDestinationPaths = diffSet.stream().map(ASTDiff::getDstPath).toList();
        List<String> addedPaths =
                dstContexts.keySet().stream().filter(path -> !diffDestinationPaths.contains(path)).toList();
        addedPaths.forEach(path -> {
            Tree addedTree = dstContexts.get(path).getRoot();
            network.importHunks(new HashSet<>(addedTree.getChildren()), null);
        });

        network.process();

        return network.getGraph();
    }
}
