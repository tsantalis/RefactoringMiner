package narrator.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonObject;
import java.util.Set;
import narrator.graph.Node;
import narrator.graph.cluster.GraphWrapper;

public class TraversalPattern extends GraphWrapper {

    protected final Util util = new Util(getGraph());
    protected Node cachedLead = null;

    public Node getLead() {
        if (cachedLead == null) {
            cachedLead = getGraph().vertexSet().iterator().next();
        }

        return cachedLead;
    }

    public String getId() {
        Node lead = getLead();
        Tree tree = lead.getTree();
        return getClass().getSimpleName() + "-" + tree.getPos() + '-' + tree.getEndPos() + '-'
                + System.identityHashCode(this);
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
