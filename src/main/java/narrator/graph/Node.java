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
    private NodeType nodeType;
    private List<String> srcs = null;
    private List<Tree> dsts = null;

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
        nodeObj.addProperty("nodeType", nodeType.name());

        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lineRange = TreeUtilFunctions.getLineRange(this.tree,
                this.fileContent);
        Pair<Integer, Integer> startLineRange = lineRange.first;
        nodeObj.addProperty("startLine", startLineRange.first);
        nodeObj.addProperty("startLineOffset", startLineRange.second);
        Pair<Integer, Integer> endLineRange = lineRange.second;
        nodeObj.addProperty("endLine", endLineRange.first);
        nodeObj.addProperty("endLineOffset", endLineRange.second);

        if (srcs != null) {
            JsonArray srcsArr = new JsonArray();
            for (String src : srcs) {
                srcsArr.add(src);
            }
            nodeObj.add("srcs", srcsArr);
        }

        if (dsts != null) {
            JsonArray dstsArr = new JsonArray();
            for (Tree dst : dsts) {
                JsonObject dstObj = new JsonObject();

                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> dstLineRange =
                        TreeUtilFunctions.getLineRange(dst, this.fileContent);
                Pair<Integer, Integer> startDstRange = dstLineRange.first;
                dstObj.addProperty("startLine", startDstRange.first);
                dstObj.addProperty("startLineOffset", startDstRange.second);
                Pair<Integer, Integer> endDstRange = dstLineRange.second;
                dstObj.addProperty("endLine", endDstRange.first);
                dstObj.addProperty("endLineOffset", endDstRange.second);

                dstsArr.add(dstObj);
            }
            nodeObj.add("dsts", dstsArr);
        }

        return nodeObj;
    }

    public void setSrcs(List<String> srcs) {
        this.srcs = srcs;
    }

    public void setDsts(List<Tree> dsts) {
        this.dsts = dsts;
    }

    public static String formatId(String path, Tree tree) {
        return String.format("%s-%s-%s-%s", path, tree.getPos(), tree.getEndPos(), tree.getType().name);
    }

    public Node(String fileContent, String path, Tree tree) {
        this.id = formatId(path, tree);
        this.fileContent = fileContent;
        this.path = path;
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
        return nodeType.equals(NodeType.LOCATION_CONTEXT) || nodeType.equals(NodeType.SEMANTIC_CONTEXT);
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
            left = new Node(this.fileContent, this.path, parentChildren.get(nodeIndex - 1), NodeType.SEMANTIC_CONTEXT);
        }
        if (nodeIndex < parentChildren.size() - 1) {
            right = new Node(this.fileContent, this.path, parentChildren.get(nodeIndex + 1), NodeType.SEMANTIC_CONTEXT);
        }

        return new Pair<>(left, right);
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
