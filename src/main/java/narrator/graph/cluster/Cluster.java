package narrator.graph.cluster;

import narrator.graph.Node;

public class Cluster extends GraphWrapper {
    public Cluster() {
    }

    Cluster(Node node) {
        addNode(node);
    }
}
