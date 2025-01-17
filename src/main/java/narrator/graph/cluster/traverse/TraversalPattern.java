package narrator.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import narrator.graph.Node;
import narrator.graph.cluster.GraphWrapper;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class TraversalPattern extends GraphWrapper {
    protected final Util util = new Util(getGraph());

    public String textualRepresentation() {
        return "";
    }

    public Node getLead() {
        List<Node> nodes = getGraph().vertexSet().stream().toList();

        Random random = new Random();
        int randomIndex = random.nextInt(nodes.size());

        return nodes.get(randomIndex);
    }

    public String getId() {
        Node lead = getLead();
        Tree tree = lead.getTree();
        return getClass().getSimpleName() + "-" + tree.getPos() + '-' + tree.getEndPos() + "-" + tree.getType().name;
    }

    public Set<Node> vertexSet() {
        return getGraph().vertexSet();
    }
}
