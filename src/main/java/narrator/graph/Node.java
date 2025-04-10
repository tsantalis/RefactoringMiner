package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public class Node {
    private final String id;
    private final String path;
    private final String fileContent;
    private boolean active = true;
    private final Tree tree;
    private Set<Node> subNodes = null;
    private Set<Tree> subMoves = null;
    private final int startLine;
    private final int endLine;
    private NodeType nodeType;
    private List<ContentRange> contentRanges = null;
    private List<String> srcs = null;

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

    public Node(String fileContent, String path, Tree tree, Set<Node> subNodes, Set<Tree> subMoves) {
        this(fileContent, path, tree);
        this.subNodes = subNodes;
        this.subMoves = subMoves;
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

    private List<ContentRange> getContentRanges() {
        if (contentRanges != null) {
            return contentRanges;
        }

        List<ContentRange> ranges = new ArrayList<>();

        if (subNodes == null || subNodes.isEmpty()) {
            ranges.add(new ContentRange(tree.getPos(), tree.getEndPos(), true));
            return ranges;
        }

        List<Node> sortedSubNodes = new ArrayList<>(subNodes);
        sortedSubNodes.sort(Comparator.comparingInt(n -> n.getTree().getPos()));

        int processedPos = getTree().getPos();
        for (Node subNode : sortedSubNodes) {
            int pos = subNode.getTree().getPos();
            int startGap = pos - processedPos;
            if (startGap > 0) {
                ranges.addAll(getSubMoveContentRanges(processedPos, pos));
            }

            ranges.addAll(subNode.getContentRanges());

            processedPos = subNode.getTree().getEndPos();
        }

        ranges.addAll(getSubMoveContentRanges(processedPos, getTree().getEndPos()));

        List<ContentRange> mergedRanges = new ArrayList<>();
        ContentRange currentRange = null;
        for (ContentRange range : ranges) {
            if (currentRange == null || currentRange.isAddition != range.isAddition) {
                if (currentRange != null) {
                    mergedRanges.add(currentRange);
                }

                currentRange = new ContentRange(range.pos, range.endPos, range.isAddition);
                continue;
            }

            currentRange.endPos = range.endPos;
        }

        if (currentRange != null) {
            mergedRanges.add(currentRange);
        }

        contentRanges = mergedRanges;

        return mergedRanges;
    }

    private boolean getIsCensor() {
        List<ContentRange> contentRanges = getContentRanges();
        return contentRanges.stream().anyMatch(cr -> !cr.isAddition);
    }

    private List<ContentRange> getSubMoveContentRanges(int pos, int endPos) {
        List<ContentRange> ranges = new ArrayList<>();

        if (subMoves == null || subMoves.isEmpty()) {
            ranges.add(new ContentRange(pos, endPos, true));
            return ranges;
        }

        List<Pair<Integer, Integer>> overlaps = new ArrayList<>();
        for (Tree subMove : subMoves) {
            int maxPos = Math.max(pos, subMove.getPos());
            int minEndPos = Math.min(endPos, subMove.getEndPos());

            if (maxPos <= minEndPos) {
                overlaps.add(new Pair<>(maxPos, minEndPos));
            }
        }

        List<Pair<Integer, Integer>> sortedOverlaps = new ArrayList<>(overlaps);
        sortedOverlaps.sort(Comparator.comparingInt(n -> n.first));

        int processedPos = pos;
        for (Pair<Integer, Integer> overlap : sortedOverlaps) {
            int startGap = overlap.first - processedPos;
            if (startGap > 0) {
                ranges.add(new ContentRange(processedPos, overlap.first, true));
            }

            ranges.add(new ContentRange(overlap.first, overlap.second, false));

            processedPos = overlap.second;
        }

        int endGap = endPos - processedPos;
        if (endGap > 0) {
            ranges.add(new ContentRange(processedPos, endPos, true));
        }

        return ranges;
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

        StringBuilder result = new StringBuilder(fileContent.substring(tree.getPos(), tree.getEndPos()));
        if (srcs != null) {
            result.append("\nThe code above is the result of the change to the code below:\n");
            result.append(String.join("\n", srcs));
        }

        return result.toString();
    }

    private String getCensoredContent() {
        List<ContentRange> contentRanges = getContentRanges();

        if (contentRanges.size() == 1) {
            ContentRange contentRange = contentRanges.get(0);
            return fileContent.substring(contentRange.pos, contentRange.endPos);
        }

        StringBuilder result = new StringBuilder();

        for (ContentRange contentRange : contentRanges) {
            String rangeString = fileContent.substring(contentRange.pos, contentRange.endPos);

            if (!contentRange.isAddition) {
                rangeString = rangeString.replaceAll("[^\\s]", "*");
            }

            result.append(rangeString);
        }

        return result.toString();
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
            String type = tree.getType().name;
            return typeTextualRepresentation.containsKey(type) ?
                    typeTextualRepresentation.get(type) + " \"" + getContent() + "\"" : "\"" + getContent() + "\"";
        }

        return getContent();
    }
}

class ContentRange {
    int pos;
    int endPos;
    boolean isAddition;

    ContentRange(int pos, int endPos, boolean isAddition) {
        this.pos = pos;
        this.endPos = endPos;
        this.isAddition = isAddition;
    }
}
