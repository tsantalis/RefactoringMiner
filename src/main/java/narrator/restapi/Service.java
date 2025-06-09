package narrator.restapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import narrator.Driver;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.json.Stringifier;
import org.jgrapht.Graph;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// TODO: send a version to invalidate browser storage
@RestController
@RequestMapping("/api")
public class Service {
    @GetMapping(value = "/hierarchy/commit", produces = MediaType.APPLICATION_JSON_VALUE)
    public String commitHierarchy(@RequestParam String url) {
        Graph<Node, Edge> graph = Driver.getCommitGraph(url);
        return respondHierarchy(graph);
    }

    @GetMapping(value = "/hierarchy/pull-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public String pullRequestHierarchy(@RequestParam String url) throws Exception {
        Graph<Node, Edge> graph = Driver.getPullRequestGraph(url);
        return respondHierarchy(graph);
    }

    private String respondHierarchy(Graph<Node, Edge> graph) {
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();
        List<List<TraversalPattern>> clustersComponents =
                clusters.stream().map(TraversalEngine::new).map(TraversalEngine::getComponents).toList();

        JsonObject stringifiedCommit = Stringifier.hierarchy(clustersComponents);
        return stringifiedCommit.toString();
    }

    @GetMapping(value = "/clusters/commit", produces = MediaType.APPLICATION_JSON_VALUE)
    public String commitClusters(@RequestParam String url) {
        Graph<Node, Edge> graph = Driver.getCommitGraph(url);
        return respondClusters(graph);
    }

    @GetMapping(value = "/clusters/pull-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public String pullRequestClusters(@RequestParam String url) throws Exception {
        Graph<Node, Edge> graph = Driver.getPullRequestGraph(url);
        return respondClusters(graph);
    }

    private static String respondClusters(Graph<Node, Edge> graph) {
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();

        JsonArray stringifiedClusters = new JsonArray();
        clusters.forEach(cluster -> stringifiedClusters.add(Stringifier.graph(cluster.getGraph())));
        return stringifiedClusters.toString();
    }

    @GetMapping("/health")
    public String health() {
        return "Service is up and running";
    }
}
