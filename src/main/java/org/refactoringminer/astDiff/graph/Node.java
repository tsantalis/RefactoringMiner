package org.refactoringminer.astDiff.graph;

import static org.refactoringminer.astDiff.graph.NodeType.ADDITION;
import static org.refactoringminer.astDiff.graph.NodeType.DELETION;
import static org.refactoringminer.astDiff.graph.NodeType.EXTENSION;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.annotation.Nullable;
import org.refactoringminer.astDiff.graph.cluster.traverse.Util;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class Node implements ReviewNode {

  public static final Comparator<Node> COMPARATOR = Comparator.comparing(Node::getSrcDst)
      .thenComparing(Node::getPath)
      .thenComparingInt(node -> node.getTree().getPos());

  private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  public static final int PROMPT_ID_LENGTH = 5;
  public static final int MAX_PROMPT_ID_LENGTH = 12;
  public static final String PROMPT_ID_PREFIX = "#";
  public static final String PROMPT_ID_BODY_REGEX = "[0-9A-HJKMNP-TV-Z]{" + PROMPT_ID_LENGTH + ",}(?![0-9A-Z])";

  private final String id;
  private String promptId;
  private final String path;
  private final Constants constants;
  private final SrcDst srcDst;
  private final ContentsContexts contentsContexts;
  private final Tree tree;
  @Nullable
  private final Set<Node> subs;
  private final Set<String> identifiers = new HashSet<>();
  private final NodeType nodeType;
  private final Set<ASTDiff> diffs = new HashSet<>();
  @Nullable
  private UMLs umls = null;

  public Node(ContentsContexts contentsContexts, String path, SrcDst srcDst, Tree tree,
      @Nullable Set<Node> subs, NodeType nodeType) {
    this.id = formatId(path, srcDst, nodeType, tree);
    this.path = path;
    this.constants = new Constants(path);
    this.contentsContexts = contentsContexts;
    this.srcDst = srcDst;
    this.tree = tree;
    this.subs = subs;
    this.nodeType = nodeType;
    this.promptId = PROMPT_ID_PREFIX + shortId(this.id, PROMPT_ID_LENGTH);
  }

  public static String formatId(String path, SrcDst srcDst, NodeType nodeType, Tree tree) {
    return String.format("%s-%s-%s-%s-%s-%s", path, srcDst, nodeType, tree.getPos(),
        tree.getEndPos(), tree.getType().name);
  }

  void assignPromptId(int length) {
    this.promptId = PROMPT_ID_PREFIX + shortId(this.id, length);
  }

  static String shortId(String formatId, int length) {
    if (length < 1 || length > MAX_PROMPT_ID_LENGTH) {
      throw new IllegalArgumentException("Unsupported prompt id length: " + length);
    }

    byte[] digest;
    try {
      digest = MessageDigest.getInstance("SHA-256")
          .digest(formatId.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }

    long value = 0;
    for (int i = 0; i < 8; i++) {
      value = (value << 8) | (digest[i] & 0xFFL);
    }

    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHABET.charAt((int) (value & 31)));
      value >>>= 5;
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

  public UMLs getUMLs() {
    return umls;
  }

  public void setUMLs(UMLs umls) {
    this.umls = umls;
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
    return (this.getTree().equals(node.getTree()) && this.nodeType.equals(NodeType.SEMANTIC_CONTEXT) && node.getNodeType().equals(NodeType.LOCATION_CONTEXT))
            || Node.isDescendantOf(this.srcDst, this.path, this.tree, node.srcDst, node.path, node.tree);
  }

  public static boolean isDescendantOf(SrcDst examineeSrcDst, String examineePath, Tree examineeTree,
                                       SrcDst ofSrcDst, String ofPath, Tree ofTree) {
    if (!examineeSrcDst.equals(ofSrcDst) || !examineePath.equals(ofPath)) {
      return false;
    }

    if ((ofTree.getPos() < examineeTree.getPos() && examineeTree.getEndPos() <= ofTree.getEndPos()) ||
            (ofTree.getPos() <= examineeTree.getPos() && examineeTree.getEndPos() < ofTree.getEndPos())) {
      return true;
    }

    return ofTree.getPos() == examineeTree.getPos() && examineeTree.getEndPos() == ofTree.getEndPos() && examineeTree.getParents().contains(ofTree);
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
    return nodeType.equals(DELETION) || nodeType.equals(NodeType.SRC_MOVE) || nodeType.equals(NodeType.SRC_UPDATE)
        || nodeType.equals(ADDITION) || nodeType.equals(NodeType.DST_MOVE) || nodeType.equals(NodeType.DST_UPDATE);
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

      if (constants.isNamedBlock(type)) {
        Tree name = TreeUtilFunctions.findChildByType(tree, constants.SIMPLE_NAME);
        if (name != null) {
          return name.getLabel();
        }
      }

      if (constants.isRoot(type)) {
        return path;
      }
    }

    return getFileContent().substring(tree.getPos(), tree.getEndPos());
  }

  public String dedentContent() {
    String content = getContent();
    if (content == null || nodeType.equals(NodeType.LOCATION_CONTEXT)) {
      return content;
    }

    TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(tree, getFileContent());
    String baseIndent = leadingWhitespace(lineAt(lineRange.startLine()));

    // The first line begins at the node, so it carries no indentation of its own to take off
    String[] lines = content.split("\n", -1);
    StringBuilder block = new StringBuilder(lines[0]);
    for (int i = 1; i < lines.length; i++) {
      block.append("\n").append(dedent(lines[i], baseIndent));
    }

    return block.toString();
  }

  private String lineAt(int line) {
    String[] lines = getFileContent().split("\n", -1);
    return line >= 1 && line <= lines.length ? lines[line - 1] : "";
  }

  private static String leadingWhitespace(String line) {
    int end = 0;
    while (end < line.length() && Character.isWhitespace(line.charAt(end))) {
      end++;
    }

    return line.substring(0, end);
  }

  private static String dedent(String line, String baseIndent) {
    if (line.startsWith(baseIndent)) {
      return line.substring(baseIndent.length());
    }

    return line.substring(Math.min(leadingWhitespace(line).length(), baseIndent.length()));
  }

  public String getFileContent() {
    return this.contentsContexts.getContent(this.getSrcDst(), this.getPath());
  }

  public Node getAlternative() {
    for (ASTDiff diff : getDiffs()) {
      MappingStore mappingStore = diff.getAllMappings().getMonoMappingStore();
      Tree alternativeTree = this.isSrc() ? mappingStore.getDstForSrc(this.getTree()) : mappingStore.getSrcForDst(this.getTree());
      if (alternativeTree == null) {
        continue;
      }

      SrcDst dstSrc = this.isSrc() ? SrcDst.DST : SrcDst.SRC;
      NodeType alternativeNodeType = NodeType.alternativeNodeType(this.nodeType);
      if (alternativeNodeType == null) {
        continue;
      }

      Node alternativeNode =  new Node(contentsContexts, contentsContexts.getPath(dstSrc, alternativeTree), dstSrc,
              alternativeTree, null, alternativeNodeType);
      alternativeNode.addDiff(diff);
      return alternativeNode;
    }

    return null;
  }

  public String getPath() {
    return path;
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

  // A hunk may be smaller than a line, so it is overlap in such cases
  @Override
  public boolean overlapLine(String path, String side, int line, @Nullable Integer startLine) {
    if (!this.getPath().equals(path)) {
      return false;
    }

    if (("LEFT".equals(side) && !this.isSrc()) || ("RIGHT".equals(side) && !this.isDst())) {
      return false;
    }

    TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(this.getTree(), this.getFileContent());
    return startLine == null ? line >= lineRange.startLine() && line <= lineRange.endLine() :
            startLine <= lineRange.endLine() && lineRange.startLine() <= line;
  }

  @Override
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

    TreeUtilFunctions.LineRange lineRange = TreeUtilFunctions.getLineRange(this.getTree(), this.getFileContent());
    nodeObj.addProperty("startLine", lineRange.startLine());
    nodeObj.addProperty("startLineOffset", lineRange.startLineOffset());
    nodeObj.addProperty("endLine", lineRange.endLine());
    nodeObj.addProperty("endLineOffset", lineRange.endLineOffset());
    nodeObj.addProperty("length", this.getTree().getEndPos() - this.getTree().getPos() + 1);

    if (this.getSubs() != null) {
      JsonArray subsArr = new JsonArray();
      for (Node sub : this.getSubs()) {
        JsonObject exceptionObj = new JsonObject();

        Tree subTree = sub.getTree();
        TreeUtilFunctions.LineRange exceptionLineRange = TreeUtilFunctions.getLineRange(subTree, this.getFileContent());
        exceptionObj.addProperty("startLine", exceptionLineRange.startLine());
        exceptionObj.addProperty("startLineOffset", exceptionLineRange.startLineOffset());
        exceptionObj.addProperty("endLine", exceptionLineRange.endLine());
        exceptionObj.addProperty("endLineOffset", exceptionLineRange.endLineOffset());
        exceptionObj.addProperty("length", subTree.getEndPos() - subTree.getPos() + 1);

        subsArr.add(exceptionObj);
      }
      nodeObj.add("subs", subsArr);
    }

    return nodeObj;
  }

  public String base(Graph<Node, Edge> graph) {
    String basePrompt = "{ id: " + this.getPromptId() + ", type: " + getPromptType(graph);
    String contextString = getContextString();
    if (!contextString.isEmpty()) {
      basePrompt += ", location: " + contextString;
    }
    basePrompt += " }\n" + this.dedentContent();

    return basePrompt;
  }

  public String baseXml(Graph<Node, Edge> graph) {
    String promptType = this.getPromptType(graph);
    String xmlPrompt = "<" + promptType;
    if (this.isBase()) {
      xmlPrompt +=  " id=\"" + getPromptId() + "\"";
    }

    String contextString = getContextString();
    if (!contextString.isEmpty()) {
      xmlPrompt += " location=\"" + contextString + "\"";
    }

    xmlPrompt += ">\n    ";
    xmlPrompt += this.dedentContent().replace("\n", "\n    ");
    xmlPrompt += "\n</" + promptType + ">";

    return xmlPrompt;
  }

  public List<String> getOperations(Graph<Node, Edge> graph) {
    List<String> operations = new ArrayList<>();

    List<Node> alts = new ArrayList<>();
    alts.addAll(this.getMappingSources(graph));
    alts.addAll(this.getMappingTargets(graph));
    if (alts.isEmpty()) {
      return operations;
    }

    if (!isOneToOneMapping(graph)) {
      operations.add("move");
      operations.add("change");
      return operations;
    }

    Node alt = alts.get(0);
    String thisContextString = this.getContextString();
    String altContextString = alt.getContextString();
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

  private boolean isOneToOneMapping(Graph<Node, Edge> graph) {
    Set<Node> component = new HashSet<>();
    Deque<Node> queue = new ArrayDeque<>();
    component.add(this);
    queue.add(this);

    while (!queue.isEmpty()) {
      Node current = queue.poll();
      for (Node source : current.getMappingSources(graph)) {
        if (component.add(source)) {
          queue.add(source);
        }
      }
      for (Node target : current.getMappingTargets(graph)) {
        if (component.add(target)) {
          queue.add(target);
        }
      }
    }

    return component.stream().filter(Node::isSrc).count() == 1
        && component.stream().filter(Node::isDst).count() == 1;
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

  public String getContextString() {
    List<Pair<Tree, NodeType>> contexts = Context.get(this.getPath(), this.getTree());
    List<Pair<Tree, NodeType>> locationContexts = new ArrayList<>();
    for (Pair<Tree, NodeType> context : contexts) {
      if (context.second.equals(NodeType.LOCATION_CONTEXT)) {
        locationContexts.add(context);
      }
    }
    Collections.reverse(locationContexts);

    List<Node> locationContextNodes = locationContexts.stream()
            .map(locationContext ->
                    new Node(contentsContexts, this.getPath(), this.getSrcDst(), locationContext.first, null, locationContext.second))
            .toList();
    StringBuilder sb = new StringBuilder();
    if (!locationContextNodes.isEmpty()) {
      sb.append(locationContextNodes.get(0).getContent());
      if (locationContextNodes.size() > 1) {
        sb.append("::").append(locationContextNodes.get(1).getContent());
        for (int i = 2; i < locationContextNodes.size(); i++) {
          Node n = locationContextNodes.get(i);
          String prefix = this.constants.isNamedMethod(n.getTree().getType().name) ? "#" : ".";
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
        .map(tree -> getFileContent().substring(tree.getPos(), tree.getEndPos())).toList();
  }
}
