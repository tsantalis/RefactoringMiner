package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import narrator.graph.Node;
import narrator.graph.NodeType;

import java.util.List;

public class SingularPattern extends TraversalPattern {
    private Node node;

    SingularPattern(Node node) {
        this.node = node;
        addNode(node);
    }

    @Override
    public Node getLead() {
        return node;
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("nodeType", NodeType.SINGULAR.name());

        return result;
    }
}
