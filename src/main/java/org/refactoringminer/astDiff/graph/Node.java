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
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class Node implements ReviewNode {

  public static final Comparator<Node> COMPARATOR = Comparator.comparing(Node::getSrcDst)
      .thenComparing(Node::getPath)
      .thenComparingInt(node -> node.getTree().getPos());

  // Dependencies read as the code declaring them, whichever revision each is taken from
  public static final Comparator<Node> DEPENDENCY_COMPARATOR = Comparator.comparing(Node::getPath)
      .thenComparingInt(node -> node.getTree().getPos())
      .thenComparing(Node::getSrcDst);

  private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  public static final int PROMPT_ID_LENGTH = 5;
  public static final int MAX_PROMPT_ID_LENGTH = 12;
  public static final String PROMPT_ID_PREFIX = "#";
  public static final String PROMPT_ID_BODY_REGEX = "[0-9A-HJKMNP-TV-Z]{" + PROMPT_ID_LENGTH + ",}(?![0-9A-Z])";

  private final String id;
  private String promptId;
  private final String path;
  private final SrcDst srcDst;
  private final ContentsContexts contentsContexts;
  private final RMIndex rmIndex;
  private final Tree tree;
  @Nullable
  private final Set<Node> subs;
  private final Set<String> identifiers = new HashSet<>();
  private final NodeType nodeType;
  private final Set<ASTDiff> diffs = new HashSet<>();
  @Nullable
  private UMLs umls = null;
  @Nullable
  private RMInfo rm;

  public Node(ContentsContexts contentsContexts, RMIndex rmIndex, String path, SrcDst srcDst, Tree tree,
      @Nullable Set<Node> subs, NodeType nodeType) {
    this.id = formatId(path, srcDst, nodeType, tree);
    this.path = path;
    this.contentsContexts = contentsContexts;
    this.rmIndex = rmIndex;
    this.srcDst = srcDst;
    this.tree = tree;
    this.subs = subs;
    this.nodeType = nodeType;
    this.promptId = PROMPT_ID_PREFIX + shortId(this.id, PROMPT_ID_LENGTH);
  }

  // Code moved into a new block keeps its lines but takes the indentation of the block, as a diff ignoring whitespace
  // shows
  public static boolean sameIgnoringIndentation(String code, String other) {
    String[] lines = code.split("\n", -1);
    String[] otherLines = other.split("\n", -1);
    if (lines.length != otherLines.length) {
      return false;
    }

    for (int i = 0; i < lines.length; i++) {
      if (!lines[i].strip().equals(otherLines[i].strip())) {
        return false;
      }
    }
    return true;
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

  // Where the tree lies in what RM gives of its revision
  public RMInfo getRM() {
    if (rm == null) {
      rm = rmIndex.locate(srcDst, path, tree);
    }
    return rm;
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
    if (this.srcDst.equals(node.getSrcDst()) && this.path.equals(node.getPath()) && this.getTree().equals(node.getTree())
            && this.nodeType.equals(NodeType.SEMANTIC_CONTEXT) && node.getNodeType().equals(NodeType.LOCATION_CONTEXT)) {
      return true;
    }

    return Node.isDescendantOf(this.srcDst, this.path, this.tree, node.srcDst, node.path, node.tree);
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

  // Nodes are told apart by identity, but hashed by what they are, so that the sets and maps of nodes, and all that is
  // built by iterating them, come out in the same order on every run
  @Override
  public int hashCode() {
    return id.hashCode();
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

  // A location context reads as the name of the class or the operation it declares, or as the path of its file
  public String getContent() {
    if (nodeType.equals(NodeType.LOCATION_CONTEXT)) {
      RMInfo rm = getRM();
      if (rm.declaredOperation() != null) {
        return rm.declaredOperation().getName();
      }
      if (rm.declaredClass() != null) {
        return rm.declaredClass().getNonQualifiedName();
      }
      if (new Constants(path).isRoot(tree.getType().name)) {
        return path;
      }
    }

    return getFileContent().substring(tree.getPos(), tree.getEndPos());
  }

  // A local not holding its type, as a variable of a declaration statement, is printed as the code declaring it
  public Node getDeclaration() {
    Tree declaration = getRM().declaration();
    return declaration == null ? this : new Node(contentsContexts, rmIndex, path, srcDst, declaration, null, nodeType);
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

      Node alternativeNode =  new Node(contentsContexts, rmIndex, contentsContexts.getPath(dstSrc, alternativeTree), dstSrc,
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
    // The end of a doc comment stands between no two changes either: the last of its tags is followed by what follows
    // the comment, such as the annotations of the declaration it documents
    for (Tree current = tree; current.getParent() != null; current = current.getParent()) {
      Tree right = rightSibling(current);
      if (right != null || !withinDocComment(current.getParent())) {
        return right;
      }
    }
    return null;
  }

  private Tree rightSibling(Tree tree) {
    List<Tree> parentChildren = tree.getParent().getChildren();

    int nodeIndex = -1;
    for (int i = 0; i < parentChildren.size(); i++) {
      if (parentChildren.get(i) == tree) {
        nodeIndex = i;
        break;
      }
    }

    // Code not represented, such as the package and the imports between the header comment of a file and its type,
    // stands between no two changes
    for (int i = nodeIndex + 1; i < parentChildren.size(); i++) {
      Tree sibling = parentChildren.get(i);
      if (getRM().fileParts().stream().noneMatch(part -> part.getStartOffset() <= sibling.getPos() && sibling.getEndPos() <= part.getEndOffset())) {
        return sibling;
      }
    }
    return null;
  }

  private boolean withinDocComment(Tree tree) {
    return rmIndex.isInDocComment(srcDst, path, tree);
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
    return contextString(false);
  }

  // The context string naming this node too when it is a named block (a type or a method) itself, as when its code
  // is printed whole
  public String getInclusiveContextString() {
    return contextString(Context.isLocationContext(this.getPath(), this.getTree().getType().name));
  }

  private String contextString(boolean inclusive) {
    List<Pair<Tree, NodeType>> contexts = Context.get(this.getPath(), this.getTree());
    List<Pair<Tree, NodeType>> locationContexts = new ArrayList<>();
    if (inclusive) {
      locationContexts.add(new Pair<>(this.getTree(), NodeType.LOCATION_CONTEXT));
    }
    for (Pair<Tree, NodeType> context : contexts) {
      if (context.second.equals(NodeType.LOCATION_CONTEXT)) {
        locationContexts.add(context);
      }
    }
    Collections.reverse(locationContexts);

    List<Node> locationContextNodes = locationContexts.stream()
            .map(locationContext ->
                    new Node(contentsContexts, rmIndex, this.getPath(), this.getSrcDst(), locationContext.first, null, locationContext.second))
            .toList();
    StringBuilder sb = new StringBuilder();
    if (!locationContextNodes.isEmpty()) {
      sb.append(locationContextNodes.get(0).getContent());
      if (locationContextNodes.size() > 1) {
        sb.append("::").append(locationContextNodes.get(1).getContent());
        for (int i = 2; i < locationContextNodes.size(); i++) {
          Node n = locationContextNodes.get(i);
          boolean method = n.getRM().declaredOperation() != null;
          sb.append(method ? "#" : ".").append(n.getContent());
          if (method && isOverloaded(n.getTree(), n.getContent())) {
            sb.append(parameterTypes(n));
          }
        }
      }
    }
    return sb.toString();
  }

  // A method sharing its name with another method of its type, in either revision, is told apart by the types of its
  // parameters: a method gaining an overload keeps one location in both revisions
  private boolean isOverloaded(Tree method, String name) {
    Tree owner = method.getParent();
    if (owner == null) {
      return false;
    }
    if (rmIndex.operationsNamed(srcDst, path, owner, name) > 1) {
      return true;
    }

    for (ASTDiff diff : diffs) {
      MappingStore mappingStore = diff.getAllMappings().getMonoMappingStore();
      Tree counterpart = isSrc() ? mappingStore.getDstForSrc(owner) : mappingStore.getSrcForDst(owner);
      if (counterpart != null) {
        SrcDst otherSide = isSrc() ? SrcDst.DST : SrcDst.SRC;
        return rmIndex.operationsNamed(otherSide, contentsContexts.getPath(otherSide, counterpart), counterpart, name) > 1;
      }
    }
    return false;
  }

  // The types of the parameters of the operation the node declares, as its code writes them
  private String parameterTypes(Node operation) {
    List<String> types = new ArrayList<>();
    String content = operation.getFileContent();
    for (VariableDeclaration parameter : operation.getRM().declaredOperation().getParameterDeclarationList()) {
      if (parameter.getType() == null || parameter.getType().getLocationInfo() == null) {
        continue;
      }
      int start = parameter.getType().getLocationInfo().getStartOffset();
      int end = parameter.getLocationInfo().getEndOffset() - parameter.getVariableName().length();
      types.add(content.substring(start, Math.max(start, end)).strip().replaceAll("\\s+", " "));
    }
    return "(" + String.join(", ", types) + ")";
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
}
