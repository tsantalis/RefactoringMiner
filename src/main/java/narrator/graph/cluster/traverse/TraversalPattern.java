package narrator.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import narrator.graph.Node;
import narrator.graph.cluster.GraphWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TraversalPattern extends GraphWrapper {
    protected final Util util = new Util(getGraph());
    private String descriptionCache = null;

    protected void setDescriptionCache(String descriptionCache) {
        this.descriptionCache = descriptionCache;
    }

    public String textualRepresentation() {
        return null;
    }

    public String description() throws IOException {
        if (descriptionCache != null) {
            return descriptionCache;
        }

        return null;
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
