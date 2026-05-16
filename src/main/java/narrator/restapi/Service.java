package narrator.restapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import narrator.Driver;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.json.Stringifier;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// TODO: send a version to invalidate browser storage
@RestController
@RequestMapping("/api")
public class Service {

    @Autowired
    @Qualifier("taskExecutor")
    private TaskExecutor taskExecutor;

    @GetMapping(value = "/clusters/commit-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter commitClustersSSE(@RequestParam String url) {
        SseEmitter emitter = new SseEmitter(0L);
        taskExecutor.execute(
                () -> { // Use @Async with ThreadPoolTaskExecutor (virtual threads recommended)
                    try {
                        Graph<Node, Edge> graph = Driver.getCommitGraph(url);
                        String result = respondClusters(graph);
                        emitter.send(SseEmitter.event().data(result));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    } finally {
                        emitter.complete();
                    }
                });
        return emitter;
    }

    @GetMapping(value = "/clusters/pull-request-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter pullRequestClustersSSE(@RequestParam String url) {
        SseEmitter emitter = new SseEmitter(0L);
        taskExecutor.execute(
                () -> { // Use @Async with ThreadPoolTaskExecutor (virtual threads recommended)
                    try {
                        Graph<Node, Edge> graph = Driver.getPullRequestGraph(url);
                        String result = respondClusters(graph);
                        emitter.send(SseEmitter.event().data(result));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    } finally {
                        emitter.complete();
                    }
                });
        return emitter;
    }

    private String respondClusters(Graph<Node, Edge> graph) {
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();

        JsonArray stringifiedClusters = new JsonArray();
        clusters.forEach(cluster -> stringifiedClusters.add(Stringifier.graph(cluster.getGraph())));
        return stringifiedClusters.toString();
    }

    @GetMapping(value = "/hierarchy/commit-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter commitHierarchySSE(@RequestParam String url) {
        SseEmitter emitter = new SseEmitter(0L);
        taskExecutor.execute(
                () -> { // Use @Async with ThreadPoolTaskExecutor (virtual threads recommended)
                    try {
                        Graph<Node, Edge> graph = Driver.getCommitGraph(url);
                        String result = respondHierarchy(graph);
                        emitter.send(SseEmitter.event().data(result));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    } finally {
                        emitter.complete();
                    }
                });
        return emitter;
    }

    @GetMapping(value = "/hierarchy/pull-request-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter pullRequestHierarchySSE(@RequestParam String url) {
        SseEmitter emitter = new SseEmitter(0L);
        taskExecutor.execute(
                () -> { // Use @Async with ThreadPoolTaskExecutor (virtual threads recommended)
                    try {
                        Graph<Node, Edge> graph = Driver.getPullRequestGraph(url);
                        String result = respondHierarchy(graph);
                        emitter.send(SseEmitter.event().data(result));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    } finally {
                        emitter.complete();
                    }
                });
        return emitter;
    }

    private String respondHierarchy(Graph<Node, Edge> graph) {
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();
        List<List<TraversalPattern>> clustersComponents =
                clusters.stream().map(TraversalEngine::new).map(TraversalEngine::getComponents)
                        .filter(components -> !components.isEmpty()).toList();

        JsonObject stringifiedCommit = Stringifier.hierarchy(clustersComponents);
        return stringifiedCommit.toString();
    }

    @GetMapping("/health")
    public String health() {
        return "Service is up and running";
    }
}
