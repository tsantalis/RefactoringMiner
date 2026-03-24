package narrator;

import com.github.gumtreediff.tree.TreeContext;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import narrator.graph.Edge;
import narrator.graph.HunkNetwork;
import narrator.graph.Node;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

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
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo,
                commit,
                1000);

        return getGraph(projectASTDiff);
    }

    private static Graph<Node, Edge> getGraph(ProjectASTDiff projectASTDiff) {
        Map<String, TreeContext> srcContexts = projectASTDiff.getParentContextMap();
        Map<String, TreeContext> dstContexts = projectASTDiff.getChildContextMap();
        HunkNetwork network = new HunkNetwork(projectASTDiff.getModelDiff(),
                projectASTDiff.getFileContentsBefore(),
                projectASTDiff.getFileContentsAfter(), srcContexts, dstContexts);

        Set<ASTDiff> diffSet = projectASTDiff.getDiffSet();
        diffSet.addAll(projectASTDiff.getMoveDiffSet());

        for (ASTDiff diff : diffSet) {
            network.importDiff(diff);
        }

        Set<String> diffSrcPaths = diffSet.stream().map(ASTDiff::getSrcPath)
                .collect(Collectors.toSet());
        List<Entry<String, TreeContext>> deletedFiles = srcContexts.entrySet().stream()
                .filter(entry -> !diffSrcPaths.contains(entry.getKey())).toList();
        Set<String> diffDstPaths = diffSet.stream().map(ASTDiff::getDstPath)
                .collect(Collectors.toSet());
        List<Entry<String, TreeContext>> addedFiles = dstContexts.entrySet().stream()
                .filter(entry -> !diffDstPaths.contains(entry.getKey())).toList();
        network.importFiles(deletedFiles, addedFiles);

//            network.importHunks(new HashSet<>(addedTree.getChildren()), null);

        network.process();

        return network.getGraph();
    }
}
