package narrator.graph.cluster;

import narrator.graph.Node;

public class Cluster extends GraphWrapper {
    Cluster() {
    }

    Cluster(Node node) {
        addNode(node);
    }
}
