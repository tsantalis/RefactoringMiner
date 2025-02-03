package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.HashMap;
import java.util.List;

public class Node {
    private String id;
    private String path;
    private String fileContent;
    private Tree tree;
    private NodeType nodeType;

    private boolean active = true;

    public Node(String fileContent, String path, Tree tree) {
        this.id = String.format("%s-%s-%s-%s", path, tree.getPos(), tree.getEndPos(), tree.getType().name);
        this.path = path;
        this.fileContent = fileContent;
        this.tree = tree;
        this.nodeType = NodeType.BASE;
    }

    public Node(String fileContent, String path, Tree tree, NodeType nodeType) {
        this(fileContent, path, tree);
        this.nodeType = nodeType;
    }

    public String getId() {
        return id;
    }

    public boolean isBase() {
        return nodeType.equals(NodeType.BASE);
    }

    public boolean isContext() {
        return nodeType.equals(NodeType.CONTEXT);
    }

    public boolean isExtension() {
        return nodeType.equals(NodeType.EXTENSION);
    }

    public Tree getTree() {
        return tree;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getContent() {
        if (isContext()) {
            String type = tree.getType().name;
            if (type.equals(Constants.TYPE_DECLARATION) || type.equals(Constants.METHOD_DECLARATION)) {
                Tree name = TreeUtilFunctions.findChildByType(tree, Constants.SIMPLE_NAME);
                if (name != null) {
                    return name.getLabel();
                }
            }

            if (type.equals(Constants.COMPILATION_UNIT)) {
                return path;
            }
        }
        return fileContent.substring(tree.getPos(), tree.getEndPos());
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public String getPath() {
        return path;
    }

    public Node getParent() {
        Tree parentTree = tree.getParent();
        if (parentTree == null) {
            return null;
        }

        return new Node(this.fileContent, this.path, parentTree, NodeType.CONTEXT);
    }

    public Node getParentUntilType(String type) {
        Tree parent = TreeUtilFunctions.getParentUntilType(tree, type);
        if (parent == null) {
            return null;
        }
        return new Node(this.fileContent, this.path, parent, NodeType.CONTEXT);
    }

    public Pair<Node, Node> getSiblings() {
        Tree parent = tree.getParent();
        if (parent == null) {
            return new ImmutablePair<>(null, null);
        }

        List<Tree> parentChildren = tree.getParent().getChildren();

        int nodeIndex = -1;
        for (int i = 0; i < parentChildren.size(); i++) {
            if (parentChildren.get(i) == tree) {
                nodeIndex = i;
                break;
            }
        }

        Node left = null, right = null;
        if (nodeIndex > 0) {
            left = new Node(this.fileContent, this.path, parentChildren.get(nodeIndex - 1), NodeType.CONTEXT);
        }
        if (nodeIndex < parentChildren.size() - 1) {
            right = new Node(this.fileContent, this.path, parentChildren.get(nodeIndex + 1), NodeType.CONTEXT);
        }

        return new ImmutablePair<>(left, right);
    }

    public List<Node> getContexts() {
        return Context.get(tree).stream()
                .map(context -> new Node(fileContent, path, context, NodeType.CONTEXT)).toList();
    }

    private HashMap<String, String> typeTextualRepresentation = new HashMap<>() {{
        put(Constants.VARIABLE_DECLARATION_STATEMENT, "VARIABLE");
        put(Constants.METHOD_DECLARATION, "METHOD");
        put(Constants.TYPE_DECLARATION, "TYPE");
        put(Constants.COMPILATION_UNIT, "FILE");
    }};

    public String textualRepresentation() {
        if (isContext()) {
            String type = tree.getType().name;
            return typeTextualRepresentation.containsKey(type) ? typeTextualRepresentation.get(type) + " \"" + getContent() + "\"" : "\"" + getContent() + "\"";
        }

        return getContent();
    }
}
