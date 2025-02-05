package narrator;

import com.google.gson.JsonObject;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.excel.ExcelOperator;
import narrator.graph.CommitGraph;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.json.Stringifier;
import narrator.llm.GroqClient;
import narrator.llm.OpenAIClient;
import narrator.llm.Prompts;
import org.jgrapht.Graph;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GitHub;
import org.refactoringminer.astDiff.utils.URLHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class Driver {
    private static final int BATCH_SIZE = 10;

    public static void main(String[] args) throws Exception {
        String url = "https://github.com/termux/termux-app/commit/f80b46487df539c7e9214550002f461e5c66131c";
        stringifyCommit(url);
    }

    private static void writeClusters(String url) throws Exception {
        Graph<Node, Edge> graph = CommitGraph.get(url);
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();

        String ownerAndRepo = URLHelper.getOwnerAndRepo(url);
        String owner = ownerAndRepo.split("/")[0];
        String repo = ownerAndRepo.split("/")[1];
        String sha = URLHelper.getCommit(url);

        FileWriter writer = new FileWriter(String.format("./clusters/%s_%s_%s.txt", owner, repo, sha));
        writer.write(String.join("\n\n---\n\n", clusters.stream().map(Cluster::get).toList()));
        writer.close();
    }

    private static void stringifyCommitGraph(String url) throws Exception {
        Graph<Node, Edge> graph = CommitGraph.get(url);
        FileWriter writer = new FileWriter("./graph.json");
        writer.write(Stringifier.stringifyGraph(graph));
        writer.close();
    }

    private static void stringifyCommit(String url) throws Exception {
        Graph<Node, Edge> graph = CommitGraph.get(url);
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();

        File directory = new File("./json");
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                file.delete();
            }
        } else {
            directory.mkdirs();
        }

        for (int i = 0; i < clusters.size(); i++) {
            FileWriter writer = new FileWriter(String.format("./json/cluster-%s.json", i));
            writer.write(Stringifier.stringifyGraph(clusters.get(i).getGraph()));
            writer.close();
        }

        FileWriter writer = new FileWriter("./json/commit.json");
        writer.write(Stringifier.stringifyCommit(clusters));
        writer.close();
    }

    private static void stringifyContextGraph(String url) throws Exception {
        FileWriter writer = new FileWriter("./context.json");
        writer.write(Stringifier.stringifyContextGraph());
        writer.close();
    }

    private static void describeExcel() throws Exception {
        Properties prop = new Properties();
        InputStream input = new FileInputStream("github-oauth.properties");
        prop.load(input);
        GitHub github = GitHub.connectUsingOAuth(prop.getProperty("OAuthToken"));

        List<String> urls = ExcelOperator.readCommits("commits.xlsx");
        int index = 0;
        // 0.000277$ per addition
        // 0.026s per addition for cluster
        // 0.147s per addition overall
        for (String url : urls) {
            String ownerAndRepo = URLHelper.getOwnerAndRepo(url);

            String owner = ownerAndRepo.split("/")[0];
            String repo = ownerAndRepo.split("/")[1];
            String sha = URLHelper.getCommit(url);

            GHCommit commit = github.getRepository(ownerAndRepo).getCommit(sha);
            int additions = commit.getLinesAdded();
            int deletions = commit.getLinesDeleted();

            long start = System.nanoTime();

            Graph<Node, Edge> graph = CommitGraph.get(url);
            Clusterer clusterer = new Clusterer(graph);
            List<Cluster> clusters = clusterer.getClusters();

            long clusterTiming = System.nanoTime() - start;

            FileWriter writer = new FileWriter(String.format("./clusters/%s_%s_%s.txt", owner, repo, sha));
            writer.write(String.join("\n\n---\n\n", clusters.stream().map(Cluster::get).toList()));
            writer.close();

            List<String> allDescriptions = new ArrayList<>();
            for (int i = 0; i < clusters.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, clusters.size());
                List<Cluster> batch = clusters.subList(i, endIndex);

                List<CompletableFuture<String>> futures = batch.stream().map(cluster -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return OpenAIClient.getClusterDescription(cluster.get());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })).toList();
                CompletableFuture<List<String>> batchDescriptionsFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
                List<String> batchDescriptions = batchDescriptionsFuture.get();

                allDescriptions.addAll(batchDescriptions);
            }

            writer = new FileWriter(String.format("./descriptions/%s_%s_%s.txt", owner, repo, sha));
            writer.write(String.join("\n\n---\n\n", allDescriptions));
            writer.close();

            long overallTiming = System.nanoTime() - start;
            ExcelOperator.appendTiming(owner, repo, sha, additions, deletions, clusterTiming, overallTiming);

            System.out.printf("%s/%s - %s%n", ++index, urls.size(), clusters.size());

            Thread.sleep(clusters.size() * 500L);
        }
    }
}
