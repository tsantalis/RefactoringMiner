package narrator.restapi;

import com.google.gson.JsonObject;
import narrator.graph.CommitGraph;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.Clusterer;
import narrator.json.Stringifier;
import org.jgrapht.Graph;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class Service {
    @GetMapping(value = "/hierarchy", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hierarchy(@RequestParam String url) throws IOException {
        if (url == null) {
            return null;
        }

        Graph<Node, Edge> graph = CommitGraph.get(url);
        Clusterer clusterer = new Clusterer(graph);

        JsonObject stringifiedCommit = Stringifier.stringifyCommit(url, clusterer.getClusters());

        return stringifiedCommit.toString();
    }

    @GetMapping("/health")
    public String health() {
        return "Service is up and running";
    }
}
