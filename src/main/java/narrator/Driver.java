package narrator;

import com.google.gson.Gson;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.CommitGraph;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.json.Stringifier;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.URLHelper;

import java.io.*;
import java.util.List;

public class Driver {
    public static void main(String[] args) throws Exception {

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

        List<String> stringifiedClusters = getStringifiedClusters(clusters);
        for (int i = 0; i < stringifiedClusters.size(); i++) {
            writeFile(url, "cluster_" + i, stringifiedClusters.get(i));
        }

        String stringifiedCommit = getStringifiedCommit(url, clusters);
        writeFile(url, null, stringifiedCommit);
    }

    public static List<Cluster> getClusters(String url) {
        Graph<Node, Edge> graph = CommitGraph.get(url);
        Clusterer clusterer = new Clusterer(graph);
        return clusterer.getClusters();
    }

    public static List<String> getStringifiedClusters(List<Cluster> clusters) {
        return clusters.stream().map(cluster -> Stringifier.stringifyGraph(cluster.getGraph())).toList();
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

    public static String getStringifiedCommit(String url, List<Cluster> clusters) throws IOException {
        return Stringifier.stringifyCommit(url, clusters);
    }
}

class Case {
    String repo;
    String commit;
}
