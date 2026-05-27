package narrator.graph;

import static narrator.graph.NodeType.ADDITION;
import static narrator.graph.NodeType.DELETION;
import static narrator.graph.NodeType.EXTENSION;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.traverse.Util;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class Node {

  private static final java.util.Random RANDOM = new java.util.Random();
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  private final String id;
  private final String promptId;
  private final String path;
  private final Constants constants;
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
    this.promptId = generateShortId();
    this.fileContent = fileContent;
    this.path = path;
    this.constants = new Constants(path);
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

  private String generateShortId() {
    StringBuilder sb = new StringBuilder(4);
    for (int i = 0; i < 4; i++) {
      sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return sb.toString();
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

  public boolean isDescendantOf(Node node) {
    return (this.getTree().equals(node.getTree()) && this.nodeType.equals(NodeType.SEMANTIC_CONTEXT)
        && node.getNodeType().equals(NodeType.LOCATION_CONTEXT)) || this.getTree().getParents()
        .contains(node.getTree());
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

  public String getPromptId() {
    return promptId;
  }

  public boolean isExtension() {
    return nodeType.equals(EXTENSION);
  }

  public boolean isBase() {
    return nodeType.equals(DELETION) || nodeType.equals(NodeType.SRC_MOVE)
        || nodeType.equals(ADDITION) || nodeType.equals(NodeType.DST_MOVE);
  }

  public boolean isContext() {
    return nodeType.equals(NodeType.LOCATION_CONTEXT) || nodeType.equals(
        NodeType.SEMANTIC_CONTEXT);
  }

  public Tree getTree() {
    return tree;
  }

  @Nullable
  public Set<Tree> getSubTrees() {
    return this.subTrees;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public String getContent() {
    if (nodeType.equals(NodeType.LOCATION_CONTEXT)) {
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

  public String normalizeContent() {
    String content = getContent();
    if (content == null || !content.contains("\n")) {
      return content;
    }

    if (nodeType.equals(NodeType.LOCATION_CONTEXT)) {
      return content;
    }

    int lineStart = fileContent.lastIndexOf('\n', tree.getPos() - 1);
    String baseIndent = (lineStart == -1)
        ? fileContent.substring(0, tree.getPos())
        : fileContent.substring(lineStart + 1, tree.getPos());

    String[] lines = content.split("\n", -1);
    StringBuilder sb = new StringBuilder();
    sb.append(lines[0]);

    for (int i = 1; i < lines.length; i++) {
      sb.append("\n");
      String line = lines[i];
      if (line.startsWith(baseIndent)) {
        sb.append(line.substring(baseIndent.length()));
      } else {
        sb.append(line);
      }
    }
    return sb.toString();
  }

  public String getFileContent() {
    return fileContent;
  }

  public String getPath() {
    return path;
  }

  public Constants getConstants() {
    return constants;
  }

  public Tree getRight() {
    Tree parent = tree.getParent();
    if (parent == null) {
      return null;
    }

    List<Tree> parentChildren = parent.getChildren();

    int nodeIndex = -1;
    for (int i = 0; i < parentChildren.size(); i++) {
      if (parentChildren.get(i) == tree) {
        nodeIndex = i;
        break;
      }
    }

    return nodeIndex < parentChildren.size() - 1 ? parentChildren.get(nodeIndex + 1) : null;
  }

  public JsonObject stringify() {
    JsonObject nodeObj = new JsonObject();

    nodeObj.addProperty("id", this.getId());
    nodeObj.addProperty("path", this.getPath());
    nodeObj.addProperty("srcDst", this.getSrcDst().name());
    nodeObj.addProperty("content", this.getContent());
    nodeObj.addProperty("nodeType", this.getNodeType().name());
    nodeObj.addProperty("treeType", this.getTree().getType().name);

    List<String> descendantSimpleNames = this.getDescendantSimpleNames();
    JsonArray descendantSimpleNamesArr = new JsonArray();
    for (String descendantSimpleName : descendantSimpleNames) {
      descendantSimpleNamesArr.add(descendantSimpleName);
    }
    nodeObj.add("descendantSimpleNames", descendantSimpleNamesArr);

    if (!this.getIdentifiers().isEmpty()) {
      JsonArray identifiersArr = new JsonArray();
      for (String identifier : this.getIdentifiers()) {
        identifiersArr.add(identifier);
      }

      nodeObj.add("identifiers", identifiersArr);
    }

    Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lineRange = TreeUtilFunctions.getLineRange(
        this.getTree(),
        this.getFileContent());
    Pair<Integer, Integer> startLineRange = lineRange.first;
    nodeObj.addProperty("startLine", startLineRange.first);
    nodeObj.addProperty("startLineOffset", startLineRange.second);
    Pair<Integer, Integer> endLineRange = lineRange.second;
    nodeObj.addProperty("endLine", endLineRange.first);
    nodeObj.addProperty("endLineOffset", endLineRange.second);
    nodeObj.addProperty("length", this.getTree().getEndPos() - this.getTree().getPos() + 1);

    if (this.getMoveTrees() != null) {
      JsonArray movesArr = new JsonArray();
      for (com.github.gumtreediff.tree.Tree move : this.getMoveTrees()) {
        JsonObject moveObj = new JsonObject();

        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> dstLineRange =
            TreeUtilFunctions.getLineRange(move, this.getFileContent());
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

    if (this.getSubTrees() != null) {
      JsonArray subsArr = new JsonArray();
      for (com.github.gumtreediff.tree.Tree subTree : this.getSubTrees()) {
        JsonObject exceptionObj = new JsonObject();

        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> exceptionLineRange =
            TreeUtilFunctions.getLineRange(subTree, this.getFileContent());
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

  public String base(Cluster cluster) {
    String basePrompt = "{ id: " + this.getPromptId() + ", type: " + getPromptType(cluster);
    String contextString = getContextString(cluster);
    if (!contextString.isEmpty()) {
      basePrompt += ", location: " + contextString;
    }
    basePrompt += " }\n" + this.normalizeContent();

    return basePrompt;
  }

  public List<String> getOperations(Cluster cluster) {
    List<String> operations = new ArrayList<>();

    List<Node> alts = new ArrayList<>();
    alts.addAll(this.getMappingSources(cluster));
    alts.addAll(this.getMappingTargets(cluster));
    Node alt = alts.isEmpty() ? null : alts.get(0);

    if (alt == null) {
      return operations;
    }

    String thisContextString = this.getContextString(cluster);
    String altContextString = alt.getContextString(cluster);
    if (!thisContextString.equals(altContextString)) {
      if (!(thisContextString.endsWith(this.getContent()) && altContextString.endsWith(
          alt.getContent()))) {
        operations.add("Move");
      }
    }
    if (!this.getContent().equals(alt.getContent())) {
      operations.add("Change");
    }

    if (operations.isEmpty()) {
      operations.add("Move");
    }

    return operations;
  }

  private String getPromptType(Cluster cluster) {
    String type = switch (this.getNodeType()) {
      case EXTENSION -> "UNCHANGED";
      case DELETION -> "DELETED";
      case ADDITION -> "ADDED";
      default -> this.getNodeType().name();
    };

    if (!cluster.getGraph().vertexSet().contains(this)) {
      return type;
    }

    List<String> operations = getOperations(cluster);
    if (operations.isEmpty()) {
      return type;
    }

    List<String> upperCaseOperations = operations.stream().map(String::toUpperCase).toList();
    if (!this.getMappingSources(cluster).isEmpty()) {
      return "AFTER_" + String.join("_AND_", upperCaseOperations);
    }
    if (!this.getMappingTargets(cluster).isEmpty()) {
      return "BEFORE_" + String.join("_AND_", upperCaseOperations);
    }
    return type;
  }

  public List<Node> getSemanticContexts(Cluster cluster) {
    List<Node> contexts = Context.get(cluster.getGraph(), this);
    List<Node> semanticContexts = new ArrayList<>();
    for (Node contextNode : contexts) {
      if (contextNode.getNodeType().equals(NodeType.SEMANTIC_CONTEXT)) {
        semanticContexts.add(contextNode);
      }
    }
    return semanticContexts;
  }

  private String getContextString(Cluster cluster) {
    List<Node> contexts = Context.get(cluster.getGraph(), this);
    List<Node> locationContexts = new ArrayList<>();
    for (Node contextNode : contexts) {
      if (contextNode.getNodeType().equals(NodeType.LOCATION_CONTEXT)) {
        locationContexts.add(contextNode);
      }
    }
    Collections.reverse(locationContexts);

    StringBuilder sb = new StringBuilder();
    if (!locationContexts.isEmpty()) {
      sb.append(locationContexts.get(0).getContent());
      if (locationContexts.size() > 1) {
        sb.append("::").append(locationContexts.get(1).getContent());
        for (int i = 2; i < locationContexts.size(); i++) {
          Node n = locationContexts.get(i);
          String prefix =
              n.getTree().getType().name.equals(this.getConstants().METHOD_DECLARATION) ? "#" : ".";
          sb.append(prefix).append(n.getContent());
        }
      }
    }
    return sb.toString();
  }

  public String mapping(Cluster cluster) {
    String basePrompt = base(cluster);

    List<Node> sources = getMappingSources(cluster);
    List<Node> targets = getMappingTargets(cluster);

    if (sources.isEmpty() && targets.isEmpty()) {
      return basePrompt;
    }

    List<String> operations = getOperations(cluster);

    if (!sources.isEmpty()) {
      List<String> sourcePrompts = new ArrayList<>();
      for (Node source : sources) {
        sourcePrompts.add(source.base(cluster));
      }
      String sourcePrompt = String.join("\n", sourcePrompts);
      return sourcePrompt
          + "\n\n"
          + operations.stream().map(op -> op + "d")
          .collect(java.util.stream.Collectors.joining(" and "))
          + " to:\n\n"
          + basePrompt;
    }

    // targets.length > 0
    List<String> targetPrompts = new ArrayList<>();
    for (Node target : targets) {
      targetPrompts.add(target.base(cluster));
    }
    String targetPrompt = String.join("\n", targetPrompts);
    return basePrompt
        + "\n\n"
        + operations.stream().map(op -> op + "d")
        .collect(java.util.stream.Collectors.joining(" and "))
        + " to:\n\n"
        + targetPrompt;
  }

  public List<Node> getMappingSources(Cluster cluster) {
    return new Util(cluster.getGraph()).getMappingSources(this);
  }

  public List<Node> getMappingTargets(Cluster cluster) {
    return new Util(cluster.getGraph()).getMappingTargets(this);
  }

  public List<String> getDescendantSimpleNames() {
    List<Tree> trees = new ArrayList<>(this.tree.getDescendants());
    trees.add(tree);
    List<Tree> simpleNameTrees = trees.stream()
        .filter(tree -> tree.getType().name.equals(constants.SIMPLE_NAME)).toList();
    return simpleNameTrees.stream()
        .map(tree -> fileContent.substring(tree.getPos(), tree.getEndPos())).toList();
  }
}
