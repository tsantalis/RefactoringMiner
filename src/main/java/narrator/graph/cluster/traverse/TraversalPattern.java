package narrator.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonObject;
import narrator.graph.Node;
import narrator.graph.cluster.GraphWrapper;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class TraversalPattern extends GraphWrapper {
    protected final Util util = new Util(getGraph());

    public Node getLead() {
        List<Node> nodes = getGraph().vertexSet().stream().toList();

        Random random = new Random();
        int randomIndex = random.nextInt(nodes.size());

        return nodes.get(randomIndex);
    }

    public String getId() {
        Node lead = getLead();
        Tree tree = lead.getTree();
        return getClass().getSimpleName() + "-" + tree.getPos() + '-' + tree.getEndPos() + '-' + System.identityHashCode(this);
    }

    public JsonObject stringify() {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", getId());

        return nodeObj;
    }

    public Set<Node> vertexSet() {
        return getGraph().vertexSet();
    }
}
