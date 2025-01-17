package narrator.graph.cluster;

import com.github.gumtreediff.tree.Tree;
import narrator.graph.Node;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.*;

public class Cluster extends GraphWrapper {
    Cluster() {
    }

    Cluster(Node node) {
        addNode(node);
    }

    public String get() {
        List<Node> nodes = nodeMap.values().stream().toList();

        StringBuilder output = new StringBuilder();

        String currentPath = null;
        List<Node> batch = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (!node.getPath().equals(currentPath)) {
                if (currentPath != null) {
                    output.append("\n\n");
                }
                output.append(node.getPath()).append(":\n...");
                currentPath = node.getPath();
            }

            batch.add(node);

            String type = node.getTree().getType().name;
            if (type.equals(Constants.LINE_COMMENT) || type.equals(Constants.TEXT_ELEMENT)) {
                if (i != nodes.size() - 1) {
                    Tree clusterRightSiblingTree = nodes.get(i + 1).getTree();

                    Node rightSibling = node.getSiblings().getRight();
                    if (rightSibling != null) {
                        Tree rightSiblingTree = rightSibling.getTree();
                        if (clusterRightSiblingTree.equals(rightSiblingTree)) {
                            continue;
                        }
                    }
                }
            }

            output.append("\n");
            if (batch.get(0).getTree().getType().name.equals(Constants.LINE_COMMENT)) {
                output.append(ContentGenerator.getCommentBatchContent(batch));
            } else {
                output.append(ContentGenerator.getNodeContent(node));
            }
            output.append("\n...");

            batch.clear();
        }

        return output.toString();
    }
}
