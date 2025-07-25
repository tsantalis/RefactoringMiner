package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;

import java.util.*;

public class SimilarityPattern extends TraversalPattern {
    Set<Node> similarNodes;

    SimilarityPattern(Set<Node> similarNodes) {
        for (Node node : similarNodes) {
            addNode(node);
        }
        for (Node source : similarNodes) {
            for (Node target : similarNodes) {
                if (source.equals(target)) {
                    continue;
                }

                addEdge(source, target, new Edge(EdgeType.SIMILARITY));
            }
        }

        this.similarNodes = similarNodes;
    }

    @Override
    public Node getLead() {
        if (cachedLead == null) {
            List<Node> nodes = similarNodes.stream().toList();
            int randomIndex = new Random().nextInt(nodes.size());
            cachedLead = nodes.get(randomIndex);
        }

        return cachedLead;
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("nodeType", NodeType.SIMILARITY.name());

        return result;
    }
}
