package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public class Node {
    private final String id;
    private final String path;
    private final String fileContent;
    private boolean active = true;
    private final Tree tree;
    private final int startLine;
    private final int endLine;
    private NodeType nodeType;
    private List<String> srcs = null;

    public String getSubAggregatorId(String aggregatorId) {
        String result = id;
        if (!aggregatorId.isEmpty()) {
            result += "-" + aggregatorId;
        }
        return result;
    }

    public JsonObject stringify(String aggregatorId) {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", getSubAggregatorId(aggregatorId));
        nodeObj.addProperty("hunkId", id);
        nodeObj.addProperty("path", path);
        nodeObj.addProperty("content", getContent());
        nodeObj.addProperty("startLine", startLine);
        nodeObj.addProperty("endLine", endLine);
        nodeObj.addProperty("nodeType", nodeType.name());

        if (srcs != null) {
            JsonArray srcsArr = new JsonArray();
            for (String src : srcs) {
                srcsArr.add(src);
            }
            nodeObj.add("srcs", srcsArr);
        }

        return nodeObj;
    }

    public void setSrcs(List<String> srcs) {
        this.srcs = srcs;
    }

    public static String formatId(String path, Tree tree) {
        return String.format("%s-%s-%s-%s", path, tree.getPos(), tree.getEndPos(), tree.getType().name);
    }

    public Node(String fileContent, String path, Tree tree) {
        this.id = formatId(path, tree);
        this.path = path;
        this.fileContent = fileContent;
        this.tree = tree;
        this.nodeType = NodeType.BASE;

        Pair<Integer, Integer> lineRange = TreeUtilFunctions.getLineRange(tree, fileContent);
        this.startLine = lineRange.first;
        this.endLine = lineRange.second;
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

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getContent() {
        if (isContext()) {
            String type = tree.getType().name;
            if (type.equals(Constants.TYPE_DECLARATION) || type.equals(Constants.METHOD_DECLARATION) || type.equals(Constants.ENUM_DECLARATION) || type.equals(Constants.RECORD_DECLARATION)) {
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

    public Pair<Node, Node> getSiblings() {
        Tree parent = tree.getParent();
        if (parent == null) {
            return new Pair<>(null, null);
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

        return new Pair<>(left, right);
    }

    public List<Node> getContexts() {
        return Context.get(tree).stream().map(context -> new Node(fileContent, path, context, NodeType.CONTEXT)).toList();
    }

    private final HashMap<String, String> typeTextualRepresentation = new HashMap<>() {{
        put(Constants.VARIABLE_DECLARATION_STATEMENT, "VARIABLE");
        put(Constants.METHOD_DECLARATION, "METHOD");
        put(Constants.TYPE_DECLARATION, "TYPE");
        put(Constants.COMPILATION_UNIT, "FILE");
    }};

    public String textualRepresentation() {
        if (isContext()) {
            String result = "";

            String type = tree.getType().name;
            if (typeTextualRepresentation.containsKey(type)) {
                result += typeTextualRepresentation.get(type) + " ";
            }

            result += "\"" + getContent() + "\"";

            return result;
        }

        return getContent();
    }
}
