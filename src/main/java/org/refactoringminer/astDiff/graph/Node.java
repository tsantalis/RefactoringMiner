package org.refactoringminer.astDiff.graph;

import static org.refactoringminer.astDiff.graph.NodeType.ADDITION;
import static org.refactoringminer.astDiff.graph.NodeType.DELETION;
import static org.refactoringminer.astDiff.graph.NodeType.EXTENSION;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import javax.annotation.Nullable;
import org.refactoringminer.astDiff.graph.cluster.traverse.Util;
import org.jgrapht.Graph;
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
  private final Set<Node> subs;
  private final Set<String> identifiers = new HashSet<>();
  private final NodeType nodeType;
  private final Set<ASTDiff> diffs = new HashSet<>();

  public Node(String fileContent, String path, SrcDst srcDst, Tree tree,
      @Nullable Set<Node> subs, NodeType nodeType) {
    this.id = formatId(path, srcDst, nodeType, tree);
    this.promptId = generateShortId();
    this.fileContent = fileContent;
    this.path = path;
    this.constants = new Constants(path);
    this.srcDst = srcDst;
    this.tree = tree;
    this.subs = subs;
    this.nodeType = nodeType;
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

  public void addDiffs(Set<ASTDiff> diffs) {
    this.diffs.addAll(diffs.stream().filter(Objects::nonNull).toList());
  }

  public void addDiff(ASTDiff diff) {
    if (diff == null) {
      return;
    }
    this.diffs.add(diff);
  }

  public Set<ASTDiff> getDiffs() {
    return diffs;
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
    return nodeType.equals(NodeType.LOCATION_CONTEXT) || nodeType.equals(NodeType.SEMANTIC_CONTEXT);
  }

  public Tree getTree() {
    return tree;
  }

  @Nullable
  public Set<Node> getSubs() {
    return this.subs;
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

    Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lineRange = TreeUtilFunctions.getLineRange(tree, fileContent);
    int baseIndentLen = lineRange.first.second;

    return " ".repeat(baseIndentLen) + getContent();
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

    if (this.getSubs() != null) {
      JsonArray subsArr = new JsonArray();
      for (Node sub : this.getSubs()) {
        JsonObject exceptionObj = new JsonObject();

        Tree subTree = sub.getTree();
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

  public String base(Graph<Node, Edge> graph) {
    String basePrompt = "{ id: " + this.getPromptId() + ", type: " + getPromptType(graph);
    String contextString = getContextString(graph);
    if (!contextString.isEmpty()) {
      basePrompt += ", location: " + contextString;
    }
    basePrompt += " }\n" + this.normalizeContent();

    return basePrompt;
  }

  public String baseXml(Graph<Node, Edge> graph) {
    String xmlPrompt = "<" + this.getPromptType(graph) + " id=\"" + getPromptId() + "\"";

    String contextString = getContextString(graph);
    if (!contextString.isEmpty()) {
      xmlPrompt += " location=\"" + contextString + "\"";
    }

    xmlPrompt += ">\n    ";
    xmlPrompt += this.normalizeContent().replace("\n", "\n    ");
    xmlPrompt += "\n</" + this.getPromptType(graph) + ">";

    return xmlPrompt;
  }

  public List<String> getOperations(Graph<Node, Edge> graph) {
    List<String> operations = new ArrayList<>();

    List<Node> alts = new ArrayList<>();
    alts.addAll(this.getMappingSources(graph));
    alts.addAll(this.getMappingTargets(graph));
    Node alt = alts.isEmpty() ? null : alts.get(0);

    if (alt == null) {
      return operations;
    }

    String thisContextString = this.getContextString(graph);
    String altContextString = alt.getContextString(graph);
    if (!thisContextString.equals(altContextString)) {
      if (!(thisContextString.endsWith(this.getContent()) && altContextString.endsWith(
          alt.getContent()))) {
        operations.add("move");
      }
    }
    if (!this.getContent().equals(alt.getContent())) {
      operations.add("change");
    }

    if (operations.isEmpty()) {
      operations.add("move");
    }

    return operations;
  }

  private String getPromptType(Graph<Node, Edge> graph) {
    String type = switch (this.getNodeType()) {
      case EXTENSION -> "unchanged";
      case DELETION -> "deleted";
      case ADDITION -> "added";
      default -> this.getNodeType().name();
    };

    if (!graph.vertexSet().contains(this)) {
      return type;
    }

    List<String> operations = getOperations(graph);
    if (operations.isEmpty()) {
      return type;
    }

    if (!this.getMappingSources(graph).isEmpty()) {
      return "after_" + String.join("_and_", operations);
    }
    if (!this.getMappingTargets(graph).isEmpty()) {
      return "before_" + String.join("_and_", operations);
    }
    return type;
  }

  public List<Node> getSemanticContexts(Graph<Node, Edge> graph) {
    List<Node> contexts = Context.get(graph, this);
    List<Node> semanticContexts = new ArrayList<>();
    for (Node contextNode : contexts) {
      if (contextNode.getNodeType().equals(NodeType.SEMANTIC_CONTEXT)) {
        semanticContexts.add(contextNode);
      }
    }
    return semanticContexts;
  }

  private String getContextString(Graph<Node, Edge> graph) {
    List<Node> contexts = Context.get(graph, this);
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

  public String mapping(Graph<Node, Edge> graph) {
    String basePrompt = base(graph);

    List<Node> sources = getMappingSources(graph);
    List<Node> targets = getMappingTargets(graph);

    if (sources.isEmpty() && targets.isEmpty()) {
      return basePrompt;
    }

    List<String> operations = getOperations(graph);

    if (!sources.isEmpty()) {
      List<String> sourcePrompts = new ArrayList<>();
      for (Node source : sources) {
        sourcePrompts.add(source.base(graph));
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
      targetPrompts.add(target.base(graph));
    }
    String targetPrompt = String.join("\n", targetPrompts);
    return basePrompt
        + "\n\n"
        + operations.stream().map(op -> op + "d")
        .collect(java.util.stream.Collectors.joining(" and "))
        + " to:\n\n"
        + targetPrompt;
  }

  public String mappingXml(Graph<Node, Edge> graph) {
    String basePrompt = baseXml(graph);

    List<Node> sources = getMappingSources(graph);
    List<Node> targets = getMappingTargets(graph);

    if (sources.isEmpty() && targets.isEmpty()) {
      return basePrompt;
    }

    if (!sources.isEmpty()) {
      List<String> sourcePrompts = new ArrayList<>();
      for (Node source : sources) {
        sourcePrompts.add(source.baseXml(graph));
      }
      String sourcePrompt = String.join("\n", sourcePrompts);
      return sourcePrompt + "\n" + basePrompt;
    }

    // targets.length > 0
    List<String> targetPrompts = new ArrayList<>();
    for (Node target : targets) {
      targetPrompts.add(target.baseXml(graph));
    }
    String targetPrompt = String.join("\n", targetPrompts);
    return basePrompt + "\n" + targetPrompt;
  }

  public List<Node> getMappingSources(Graph<Node, Edge> graph) {
    return new Util(graph).getMappingSources(this);
  }

  public List<Node> getMappingTargets(Graph<Node, Edge> graph) {
    return new Util(graph).getMappingTargets(this);
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
