package narrator.graph.cluster;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;

import narrator.graph.Node;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ContentGenerator {
    public static String getNodeContent(Node node) {
        StringBuilder nodeContent = new StringBuilder();
        nodeContent.append(node.getTree().getType().name).append("\n");
        nodeContent.append(node.getContent());

        boolean isExperimental = experimentalTypes.contains(node.getTree().getType().name);

        List<Node> contexts = node.getContexts();
        if (!contexts.isEmpty() || isExperimental) {
            nodeContent.append("\nIN\n");
            if (isExperimental) {
                nodeContent.append(experiment(node));
            } else {
                nodeContent.append(contexts.get(0).getContent());
            }
        }

        return nodeContent.toString();
    }

    public static String getCommentBatchContent(List<Node> nodes) {
        //        TODO: get the context for a comment.
        //         comments can be above an element or after it in the same line
        //         it may be a commented out code element
        //         for block comment, line comment, and java doc
        //         RM will give comments
        //        Node lastNode = nodes.get(nodes.size() - 1);
        //        if (lastNode.getTree().getType().name.equals(Constants.LINE_COMMENT)) {
        //            Node right = lastNode.getSiblings().getRight();
        //            if (right != null) {
        //                output.append("\nFor\n");
        //
        //                Tree rightTree = right.getTree();
        //                String rightType = rightTree.getType().name;
        //                Tree rightChild = rightTree.getChildren().get(0);
        //                output.append(contextPartition.containsKey(rightType) ? contextPartition.get(rightType)
        //                .apply(right,
        //                        new Node(right.getFileContent(), right.getPath(), rightChild)) : right.getContent());
        //            }
        //        }

        return String.join("\n", nodes.stream().map(Node::getContent).toList()); // abc
    }

    //    private static final HashMap<String, Function<Node, String>> typeToContent = new HashMap<>() {{
    //        put(Constants.FIELD_DECLARATION, (node -> ""));
    //    }};

    private static final List<String> experimentalTypes = new ArrayList<>() {{
        add(Constants.TAG_ELEMENT);
    }};


    private static String experiment(Node node) {
        List<String> output = new ArrayList<>();

        Node parent = node;
        for (int i = 0; i < 3; i++) {
            parent = parent.getParent();
            if (parent == null) {
                break;
            }

            output.add(parent.getTree().getType().name + "\n" + parent.getContent() + "\n" + parent.getTree().getChildren());
        }

        return String.join("\n---\n", output);
    }

    private static final Function<List<String>, BiFunction<Node, Node, String>> partitionContextByType =
            (types) -> (context, node) -> {
                Tree contextTree = context.getTree();
                List<Tree> children = contextTree.getChildren();
                int endPos = contextTree.getEndPos();
                for (Tree child : children) {
                    if (types.contains(child.getType().name)) {
                        endPos = child.getPos();
                        break;
                    }
                }

                return context.getContent().substring(0, endPos - contextTree.getPos());
            };

    //    TODO: revise extension. It may be small or huge
    //     these types of extensions proved not to be desirable
    private static final Function<Integer, BiFunction<Node, Node, String>> extendNodeInContext =
            (extensionSteps) -> (context, node) -> {
                Node right = node;
                for (int i = 0; i < extensionSteps; i++) {
                    Node next = right.getSiblings().getRight();
                    if (next == null) {
                        break;
                    }
                    right = next;
                }

                Node left = node;
                for (int i = 0; i < extensionSteps; i++) {
                    Node next = left.getSiblings().getLeft();
                    if (next != null) {
                        left = next;
                    }
                }

                if (left.equals(node) && right.equals(node)) {
                    return node.getContent().equals(context.getContent()) ? null : context.getContent(); // javadoc
                }

                int pos = left.getTree().getPos();
                int endPos = right.getTree().getEndPos();

                Tree contextTree = context.getTree();
                return context.getContent().substring(pos - contextTree.getPos(), endPos - contextTree.getPos());
            };

    private static final HashMap<String, BiFunction<Node, Node, String>> contextTypePartitioner = new HashMap<>() {{
        put(Constants.METHOD_DECLARATION, partitionContextByType.apply(List.of(Constants.BLOCK)));
        put(Constants.FOR_STATEMENT, partitionContextByType.apply(List.of(Constants.BLOCK)));
        put(Constants.ENHANCED_FOR_STATEMENT, partitionContextByType.apply(List.of(Constants.BLOCK)));
        put(Constants.TYPE_DECLARATION, partitionContextByType.apply(List.of(Constants.FIELD_DECLARATION,
                Constants.METHOD_DECLARATION, Constants.TYPE_DECLARATION)));
        put(Constants.RECORD_DECLARATION, partitionContextByType.apply(List.of(Constants.FIELD_DECLARATION,
                Constants.METHOD_DECLARATION, Constants.TYPE_DECLARATION)));
        put(Constants.ENUM_DECLARATION, partitionContextByType.apply(List.of(Constants.ENUM_CONSTANT_DECLARATION)));
        put(Constants.CLASS_INSTANCE_CREATION,
                partitionContextByType.apply(List.of(Constants.ANONYMOUS_CLASS_DECLARATION)));

        put(Constants.SWITCH_STATEMENT, extendNodeInContext.apply(2));
        put(Constants.BLOCK, extendNodeInContext.apply(1));
        put(Constants.JAVA_DOC, extendNodeInContext.apply(2));
    }};
}
