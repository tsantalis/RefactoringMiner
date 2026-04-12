package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class Node {

    private final String id;
    private final String path;
    private final SrcDst srcDst;
    private final String fileContent;
    private final Tree tree;
    @Nullable
    private final Set<Tree> moveTrees;
    @Nullable
    private final Set<Tree> subTrees;
    private final Set<String> identifiers = new HashSet<>();
    private final NodeType nodeType;
    @Nullable
    private final ASTDiff diff;

    public Node(String fileContent, String path, SrcDst srcDst, Tree tree,
            @Nullable Set<Tree> subTrees, @Nullable Set<Tree> moveTrees, NodeType nodeType,
            @Nullable ASTDiff diff) {
        this.id = formatId(path, srcDst, nodeType, tree);
        this.fileContent = fileContent;
        this.path = path;
        this.srcDst = srcDst;
        this.tree = tree;
        this.subTrees = subTrees;
        this.moveTrees = moveTrees;
        this.nodeType = nodeType;
        this.diff = diff;
    }

    public static String formatId(String path, SrcDst srcDst, NodeType nodeType, Tree tree) {
        return String.format("%s-%s-%s-%s-%s-%s", path, srcDst, nodeType, tree.getPos(),
                tree.getEndPos(), tree.getType().name);
    }

    @Nullable
    public Set<Tree> getMoveTrees() {
        return moveTrees;
    }

    @Nullable
    public ASTDiff getDiff() {
        return diff;
    }

    public SrcDst getSrcDst() {
        return srcDst;
    }

    public boolean isSrc() {
        return srcDst.equals(SrcDst.SRC);
    }

    public boolean isDst() {
        return srcDst.equals(SrcDst.DST);
    }

    public JsonObject stringify() {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", id);
        nodeObj.addProperty("path", path);
        nodeObj.addProperty("srcDst", this.srcDst.name());
        nodeObj.addProperty("content", getContent());
        nodeObj.addProperty("nodeType", nodeType.name());

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

        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lineRange = TreeUtilFunctions.getLineRange(
                this.tree,
                this.fileContent);
        Pair<Integer, Integer> startLineRange = lineRange.first;
        nodeObj.addProperty("startLine", startLineRange.first);
        nodeObj.addProperty("startLineOffset", startLineRange.second);
        Pair<Integer, Integer> endLineRange = lineRange.second;
        nodeObj.addProperty("endLine", endLineRange.first);
        nodeObj.addProperty("endLineOffset", endLineRange.second);
        nodeObj.addProperty("length", this.tree.getEndPos() - this.tree.getPos() + 1);

        if (moveTrees != null) {
            JsonArray movesArr = new JsonArray();
            for (Tree move : moveTrees) {
                JsonObject moveObj = new JsonObject();

                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> dstLineRange =
                        TreeUtilFunctions.getLineRange(move, this.fileContent);
                Pair<Integer, Integer> startDstRange = dstLineRange.first;
                moveObj.addProperty("startLine", startDstRange.first);
                moveObj.addProperty("startLineOffset", startDstRange.second);
                Pair<Integer, Integer> endDstRange = dstLineRange.second;
                moveObj.addProperty("endLine", endDstRange.first);
                moveObj.addProperty("endLineOffset", endDstRange.second);
                moveObj.addProperty("length", move.getEndPos() - move.getPos() + 1);

                movesArr.add(moveObj);
            }
            nodeObj.add("moves", movesArr);
        }

        if (subTrees != null) {
            JsonArray subsArr = new JsonArray();
            for (Tree subTree : subTrees) {
                JsonObject exceptionObj = new JsonObject();

                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> exceptionLineRange =
                        TreeUtilFunctions.getLineRange(subTree, this.fileContent);
                Pair<Integer, Integer> startExceptionRange = exceptionLineRange.first;
                exceptionObj.addProperty("startLine", startExceptionRange.first);
                exceptionObj.addProperty("startLineOffset", startExceptionRange.second);
                Pair<Integer, Integer> endExceptionRange = exceptionLineRange.second;
                exceptionObj.addProperty("endLine", endExceptionRange.first);
                exceptionObj.addProperty("endLineOffset", endExceptionRange.second);
                exceptionObj.addProperty("length", subTree.getEndPos() - subTree.getPos() + 1);

                subsArr.add(exceptionObj);
            }
            nodeObj.add("subs", subsArr);
        }

        return nodeObj;
    }

    public void addIdentifier(String identifier) {
        this.identifiers.add(identifier);
    }

    public Set<String> getIdentifiers() {
        return this.identifiers;
    }

    public String getId() {
        return id;
    }

    public boolean isBase() {
        return nodeType.equals(NodeType.DELETION) || nodeType.equals(NodeType.SRC_MOVE)
                || nodeType.equals(NodeType.ADDITION) || nodeType.equals(NodeType.DST_MOVE);
    }

    public boolean isContext() {
        return nodeType.equals(NodeType.LOCATION_CONTEXT) || nodeType.equals(
                NodeType.SEMANTIC_CONTEXT);
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
        if (nodeType.equals(NodeType.LOCATION_CONTEXT)) {
            Constants constants = new Constants(this.getPath());
            String type = tree.getType().name;

            if (type.equals(constants.TYPE_DECLARATION) || type.equals(
                    constants.METHOD_DECLARATION) || type.equals(
                    constants.ENUM_DECLARATION) || type.equals(
                    constants.RECORD_DECLARATION)) {
                Tree name = TreeUtilFunctions.findChildByType(tree, constants.SIMPLE_NAME);
                if (name != null) {
                    return name.getLabel();
                }
            }

            if (type.equals(constants.COMPILATION_UNIT)) {
                return path;
            }
        }

        return fileContent.substring(tree.getPos(), tree.getEndPos());
    }

    public String getFileContent() {
        return fileContent;
    }

    public String getPath() {
        return path;
    }

    public Tree getRight() {
        Tree parent = tree.getParent();
        if (parent == null) {
            return null;
        }

        List<Tree> parentChildren = tree.getParent().getChildren();

        int nodeIndex = -1;
        for (int i = 0; i < parentChildren.size(); i++) {
            if (parentChildren.get(i) == tree) {
                nodeIndex = i;
                break;
            }
        }

        return nodeIndex < parentChildren.size() - 1 ? parentChildren.get(nodeIndex + 1) : null;
    }

    private List<String> getDescendantSimpleNames() {
        List<Tree> trees = new ArrayList<>(this.tree.getDescendants());
        trees.add(tree);
        List<Tree> simpleNameTrees = trees.stream()
                .filter(tree -> tree.getType().name.equals(
                        new Constants(this.getPath()).SIMPLE_NAME)).toList();
        return simpleNameTrees.stream()
                .map(tree -> fileContent.substring(tree.getPos(), tree.getEndPos())).toList();
    }
}
