package narrator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import narrator.graph.HunkNetwork;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.json.Stringifier;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.*;
import java.util.List;
import java.util.Map;

public class Driver {
    public static void main(String[] args) throws Exception {
        //        Graph<Node, Edge> graph = getCommitGraph("https://github.com/LMAX-Exchange/disruptor/commit" +
        //                "/6a93ff9e94a878cd2d7672b2a07b94a2307d41fe");
        Graph<Node, Edge> graph = getPullRequestGraph("https://github.com/JabRef/jabref/pull/13233");
        List<List<TraversalPattern>> components = getComponents(graph);
    }

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
        Map<String, String> dstContentsMap = projectASTDiff.getFileContentsAfter();
        Map<String, String> srcContentsMap = projectASTDiff.getFileContentsBefore();
        HunkNetwork network = new HunkNetwork(projectASTDiff.getModelDiff(), srcContentsMap, dstContentsMap,
                projectASTDiff.getChildContextMap());

        // TODO: support added files (https://github.com/LMAX-Exchange/disruptor/commit/6a93ff9e94a878cd2d7672b2a07b94a2307d41fe)
        //  fileContentsAfter includes the new file but diffSet is empty
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            network.importHunks(astDiff.getDstPath(), astDiff.getSrcPath(), astDiff.getAddedDstTrees(),
                    astDiff.getAllMappings().getMonoMappingStore());
        }

        network.process();

        return network.getGraph();
    }

    private static List<List<TraversalPattern>> getComponents(Graph<Node, Edge> graph) {
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();
        return clusters.stream().map(TraversalEngine::new).map(TraversalEngine::getComponents).toList();
    }
}
