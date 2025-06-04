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
        writeCommit("https://github.com/Netflix/eureka/commit/f6212a7e474f812f31ddbce6d4f7a7a0d498b751");
    }

    public static Graph<Node, Edge> getPullRequestGraph(String url, int id) throws Exception {
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtPullRequest(url, id, 1000);
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
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            network.importHunks(astDiff.getDstPath(), astDiff.getSrcPath(), astDiff.getAddedDstTrees(),
                    astDiff.getAllMappings().getMonoMappingStore());
        }

        network.process();

        return network.getGraph();
    }

    public static void writeJson() throws FileNotFoundException {
        Gson gson = new Gson();
        FileReader reader = new FileReader("json/cases.json");
        Case[] cases = gson.fromJson(reader, Case[].class);
        for (Case c : cases) {
            String commitUrl = c.repo.replace(".git", "") + "/commit/" + c.commit;

            System.out.println(commitUrl);
            try {
                writeCommit(commitUrl);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void writeCommit(String url) throws IOException {
        List<Cluster> clusters = getClusters(url);
        List<List<TraversalPattern>> clustersComponents =
                clusters.stream().map(TraversalEngine::new).map(TraversalEngine::getComponents).toList();

        JsonObject stringifiedCommit = Stringifier.hierarchy(clustersComponents);
        writeFile(url, null, stringifiedCommit.toString());
    }

    public static List<Cluster> getClusters(String url) {
        Graph<Node, Edge> graph = getCommitGraph(url);
        Clusterer clusterer = new Clusterer(graph);
        return clusterer.getClusters();
    }

    private static String getFileName(String url, String metadata) {
        String ownerAndRepo = URLHelper.getOwnerAndRepo(url);
        String[] splitOwnerRepo = ownerAndRepo.split("/");

        String owner = splitOwnerRepo[0];
        String repo = splitOwnerRepo[1];
        String sha = URLHelper.getCommit(url);

        StringBuilder result = new StringBuilder(String.format("%s_%s_%s", owner, repo, sha));
        if (metadata != null) {
            result.append("-").append(metadata);
        }
        result.append(".json");

        return result.toString();
    }

    private static void purgeOutputDir() {
        File directory = new File("./json");
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                file.delete();
            }
        } else {
            directory.mkdirs();
        }
    }

    private static void writeFile(String url, String metadata, String content) throws IOException {
        FileWriter writer = new FileWriter("./json/" + getFileName(url, metadata));
        writer.write(content);
        writer.close();
    }
}

class Case {
    String repo;
    String commit;
}
