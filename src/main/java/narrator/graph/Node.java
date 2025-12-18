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
    private List<Node> srcs = null;
    private List<Tree> dsts = null;
    private Set<Tree> dstExceptions = null;
    private Set<String> identifiers = new HashSet<>();
    private Constants constants;

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

    public JsonObject stringify() {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", id);
        nodeObj.addProperty("path", path);
        nodeObj.addProperty("content", getContent());
        nodeObj.addProperty("nodeType", nodeType.name());
        nodeObj.addProperty("astType", tree.getType().name);

        List<String> descendantSimpleNames = getDescendantSimpleNames();
        JsonArray descendantSimpleNamesArr = new JsonArray();
        for (String descendantSimpleName : descendantSimpleNames) {
            descendantSimpleNamesArr.add(descendantSimpleName);
        }
        nodeObj.add("descendantSimpleNames", descendantSimpleNamesArr);

        if (!identifiers.isEmpty()) {
            JsonArray identifiersArr = new JsonArray();
            for (String identifier : identifiers) {
                identifiersArr.add(identifier);
            }

            nodeObj.add("identifiers", identifiersArr);
        }

        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lineRange = TreeUtilFunctions.getLineRange(this.tree,
                this.fileContent);
        Pair<Integer, Integer> startLineRange = lineRange.first;
        nodeObj.addProperty("startLine", startLineRange.first);
        nodeObj.addProperty("startLineOffset", startLineRange.second);
        Pair<Integer, Integer> endLineRange = lineRange.second;
        nodeObj.addProperty("endLine", endLineRange.first);
        nodeObj.addProperty("endLineOffset", endLineRange.second);
        nodeObj.addProperty("length", this.tree.getEndPos() - this.tree.getPos() + 1);

        if (srcs != null) {
            JsonArray srcsArr = new JsonArray();
            for (Node src : srcs) {
                JsonObject srcObj = new JsonObject();
                srcObj.addProperty("path", src.getPath());
                srcObj.addProperty("content", src.getContent());
                srcObj.addProperty("astType", src.tree.getType().name);

                JsonArray contextsArr = new JsonArray();
                List<Node> contexts =
                        Context.get(src.getTree()).stream().map(context -> new Node(src.getFileContent(),
                                src.getPath(), context.first, context.second)).toList();
                contexts.forEach(context -> {
                    JsonObject contextObj = new JsonObject();
                    contextObj.addProperty("content", context.getContent());
                    contextObj.addProperty("nodeType", context.nodeType.name());

                    contextsArr.add(contextObj);
                });
                srcObj.add("contexts", contextsArr);

                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> srcLineRange =
                        TreeUtilFunctions.getLineRange(src.getTree(), src.getFileContent());
                Pair<Integer, Integer> srcStartRange = srcLineRange.first;
                srcObj.addProperty("startLine", srcStartRange.first);
                srcObj.addProperty("startLineOffset", srcStartRange.second);
                Pair<Integer, Integer> srcEndRange = srcLineRange.second;
                srcObj.addProperty("endLine", srcEndRange.first);
                srcObj.addProperty("endLineOffset", srcEndRange.second);
                srcObj.addProperty("length", src.getTree().getEndPos() - src.getTree().getPos() + 1);

                srcsArr.add(srcObj);
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
                dstObj.addProperty("length", dst.getEndPos() - dst.getPos() + 1);

                dstsArr.add(dstObj);
            }
            nodeObj.add("dsts", dstsArr);
        }

        if (dstExceptions != null) {
            JsonArray exceptionsArr = new JsonArray();
            for (Tree exception : dstExceptions) {
                JsonObject exceptionObj = new JsonObject();

                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> exceptionLineRange =
                        TreeUtilFunctions.getLineRange(exception, this.fileContent);
                Pair<Integer, Integer> startExceptionRange = exceptionLineRange.first;
                exceptionObj.addProperty("startLine", startExceptionRange.first);
                exceptionObj.addProperty("startLineOffset", startExceptionRange.second);
                Pair<Integer, Integer> endExceptionRange = exceptionLineRange.second;
                exceptionObj.addProperty("endLine", endExceptionRange.first);
                exceptionObj.addProperty("endLineOffset", endExceptionRange.second);
                exceptionObj.addProperty("length", exception.getEndPos() - exception.getPos() + 1);

                exceptionsArr.add(exceptionObj);
            }
            nodeObj.add("dstExceptions", exceptionsArr);
        }

        return nodeObj;
    }

    public void addIdentifier(String identifier) {
        this.identifiers.add(identifier);
    }

    public void setSrcs(List<Node> srcs) {
        this.srcs = srcs;
    }

    public void setDsts(List<Tree> dsts) {
        this.dsts = dsts;
    }

    public void setDstExceptions(Set<Tree> dstExceptions) {
        this.dstExceptions = dstExceptions;
    }

    public static String formatId(String path, Tree tree) {
        return String.format("%s-%s-%s-%s", path, tree.getPos(), tree.getEndPos(), tree.getType().name);
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
            if (type.equals(Constants.get().TYPE_DECLARATION) || type.equals(Constants.get().METHOD_DECLARATION) || type.equals(Constants.get().ENUM_DECLARATION) || type.equals(Constants.get().RECORD_DECLARATION)) {
                Tree name = TreeUtilFunctions.findChildByType(tree, Constants.get().SIMPLE_NAME);
                if (name != null) {
                    return name.getLabel();
                }
            }

            if (type.equals(Constants.get().COMPILATION_UNIT)) {
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

    private List<String> getDescendantSimpleNames() {
        List<Tree> trees = new ArrayList<>(this.tree.getDescendants());
        trees.add(tree);
        return trees.stream().filter(tree -> tree.getType().name.equals(Constants.get().SIMPLE_NAME)).map(tree -> fileContent.substring(tree.getPos(), tree.getEndPos())).toList();
    }
}
