package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.actions.classifier.ExtendedTreeClassifier;
import org.refactoringminer.astDiff.graph.cluster.traverse.Util;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class HunkNetwork {

  private final Graph<Node, Edge> graph;
  private final Util util;
  private final Map<String, Node> idNodeMap = new HashMap<>();
  private final UMLModelDiff modelDiff;
  private final RMIndex rmIndex;
  private final RMLinks rmLinks;
  private final ContentsContexts contentsContexts;
  private final Set<ImportTree> srcTrees = new HashSet<>();
  private final Set<ImportTree> dstTrees = new HashSet<>();
  private final Set<ASTDiff> diffs = new HashSet<>();
  private final Set<String> pairedSrcPaths = new HashSet<>();
  private final Set<String> pairedDstPaths = new HashSet<>();
  @Nullable
  private final MemberUsageFinder memberUsageFinder;
  @Nullable
  private final ReceiverTypeResolver receiverTypeResolver;

  public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents, Map<String, String> dstContents) {
    this(modelDiff, srcContents, dstContents, null);
  }

  public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents, Map<String, String> dstContents,
                     @Nullable RepositoryStateFiles repositoryStateFiles) {
    graph = GraphTypeBuilder.<Node, Edge>directed().allowingMultipleEdges(true)
            .allowingSelfLoops(true).edgeClass(Edge.class).weighted(true).buildGraph();
    this.util = new Util(graph);
    this.modelDiff = modelDiff;
    Map<String, String> networkSrcContents = new LinkedHashMap<>(srcContents);
    Map<String, String> networkDstContents = new LinkedHashMap<>(dstContents);
    this.contentsContexts = new ContentsContexts(networkSrcContents, networkDstContents,
            modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap());
    this.rmIndex = new RMIndex(modelDiff, contentsContexts);
    this.rmLinks = new RMLinks(modelDiff);
    this.receiverTypeResolver = repositoryStateFiles == null ? null
            : new ReceiverTypeResolver(modelDiff, repositoryStateFiles, contentsContexts);
    this.memberUsageFinder = receiverTypeResolver == null ? null : new MemberUsageFinder(receiverTypeResolver,
            (umlModel, location) -> overlapsChange(umlModel == modelDiff.getParentModel() ? SrcDst.SRC : SrcDst.DST, location));
  }

  public void importFiles() {
    for (Entry<String, TreeContext> srcContext : contentsContexts.getSrcContexts().entrySet()) {
      String path = srcContext.getKey();
      if (pairedSrcPaths.contains(path)) {
        continue;
      }

      srcTrees.addAll(getValidTrees(path, srcContext.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DELETION, path, SrcDst.SRC)).toList());
    }
    for (Entry<String, TreeContext> dstContext : contentsContexts.getDstContexts().entrySet()) {
      String path = dstContext.getKey();
      if (pairedDstPaths.contains(path)) {
        continue;
      }

      dstTrees.addAll(getValidTrees(path, dstContext.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.ADDITION, path, SrcDst.DST)).toList());
    }
  }

  public void importDiff(ASTDiff diff) {
    importDiff(diff, true);
  }

  public void importDiff(ASTDiff diff, boolean authoritative) {
    this.diffs.add(diff);

    TreeClassifier classifier = diff.createRootNodesClassifier();
    String srcPath = diff.getSrcPath();
    String dstPath = diff.getDstPath();
    boolean complete = authoritative && mapsWholeFilePair(diff);
    if (complete) {
      pairedSrcPaths.add(srcPath);
      pairedDstPaths.add(dstPath);
    }

    srcTrees.addAll(getValidTrees(srcPath, classifier.getMovedSrcs()).stream()
            .map(tree -> new ImportTree(tree, NodeType.SRC_MOVE, srcPath, SrcDst.SRC)).toList());
    srcTrees.addAll(getValidTrees(srcPath, multiMovedTrees(diff, SrcDst.SRC)).stream()
            .map(tree -> new ImportTree(tree, NodeType.SRC_MOVE, srcPath, SrcDst.SRC)).toList());
    if (complete) {
      srcTrees.addAll(getValidTrees(srcPath, classifier.getDeletedSrcs()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DELETION, srcPath, SrcDst.SRC)).toList());
      srcTrees.addAll(getValidTrees(srcPath, classifier.getUpdatedSrcs()).stream()
              .map(tree -> new ImportTree(tree, NodeType.SRC_UPDATE, srcPath, SrcDst.SRC)).toList());
    }

    dstTrees.addAll(getValidTrees(dstPath, classifier.getMovedDsts()).stream()
            .map(tree -> new ImportTree(tree, NodeType.DST_MOVE, dstPath, SrcDst.DST)).toList());
    dstTrees.addAll(getValidTrees(dstPath, multiMovedTrees(diff, SrcDst.DST)).stream()
            .map(tree -> new ImportTree(tree, NodeType.DST_MOVE, dstPath, SrcDst.DST)).toList());
    if (complete) {
      dstTrees.addAll(getValidTrees(dstPath, classifier.getInsertedDsts()).stream()
              .map(tree -> new ImportTree(tree, NodeType.ADDITION, dstPath, SrcDst.DST)).toList());
      dstTrees.addAll(getValidTrees(dstPath, classifier.getUpdatedDsts()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DST_UPDATE, dstPath, SrcDst.DST)).toList());
    }
  }

  private static Set<Tree> multiMovedTrees(ASTDiff diff, SrcDst srcDst) {
    Set<Tree> trees = new HashSet<>();

    boolean src = srcDst.equals(SrcDst.SRC);
    ExtendedTreeClassifier classifier = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
    MappingStore mappingStore = diff.getAllMappings().getMonoMappingStore();
    Tree root = src ? diff.src.getRoot() : diff.dst.getRoot();
    for (Tree tree : (src ? classifier.getMultiMapSrc() : classifier.getMultiMapDst()).keySet()) {
      Tree counterpart = src ? mappingStore.getDstForSrc(tree) : mappingStore.getSrcForDst(tree);
      if (counterpart == null && (tree == root || tree.getParents().contains(root))) {
        trees.add(tree);
      }
    }

    return trees;
  }

  private static boolean mapsWholeFilePair(ASTDiff diff) {
    Tree mappedRoot = diff.getAllMappings().getMonoMappingStore().getDstForSrc(diff.src.getRoot());
    return mappedRoot != null && mappedRoot.equals(diff.dst.getRoot());
  }

  private Set<Tree> getValidTrees(String path, Collection<Tree> trees) {
    Constants constants = new Constants(path);

    return trees.stream().filter(addition -> {
      HashSet<Tree> precedents = new HashSet<>(addition.getParents());
      precedents.add(addition);

      return precedents.stream().noneMatch(precedent -> {
        String treeType = precedent.getType().name;
        return constants.isSemanticallyInsignificant(treeType);
      });
    }).collect(Collectors.toSet());
  }

  // A tree both moved and updated is classified as both, but it is one change, of which the update is the content
  private static Set<ImportTree> distinctTrees(Set<ImportTree> importTrees) {
    Map<Tree, ImportTree> distinct = new IdentityHashMap<>();
    for (ImportTree importTree : importTrees) {
      distinct.merge(importTree.tree(), importTree, (existing, other) -> isUpdate(other.type()) ? other : existing);
    }

    return new HashSet<>(distinct.values());
  }

  private static boolean isUpdate(NodeType nodeType) {
    return nodeType.equals(NodeType.SRC_UPDATE) || nodeType.equals(NodeType.DST_UPDATE);
  }

  private HashMap<ImportTree, Set<ImportTree>> aggregateTrees(Set<ImportTree> importTrees) {
    HashMap<ImportTree, Set<ImportTree>> result = new HashMap<>();

    for (ImportTree subject : importTrees) {
      boolean isParent = true;

      for (ImportTree object : importTrees) {
        if (subject.equals(object)) {
          continue;
        }

        if (Node.isDescendantOf(subject.srcDst, subject.path, subject.tree, object.srcDst, object.path, object.tree)) {
          isParent = false;
          break;
        }
      }

      if (isParent) {
        result.put(subject, new HashSet<>());
      }
    }

    for (ImportTree parent : result.keySet()) {
      for (ImportTree importTree : importTrees) {
        if (parent.equals(importTree)) {
          continue;
        }

        if (Node.isDescendantOf(importTree.srcDst, importTree.path, importTree.tree, parent.srcDst, parent.path, parent.tree)) {
          result.get(parent).add(importTree);
        }
      }
    }

    return result;
  }

  // The trees are imported in the order of their files and positions, which the order of the nodes of the graph follows
  private void importTrees(HashMap<ImportTree, Set<ImportTree>> trees) {
    trees.entrySet().stream().sorted(Comparator.comparing((Entry<ImportTree, Set<ImportTree>> entry) -> entry.getKey().srcDst())
            .thenComparing(entry -> entry.getKey().path()).thenComparingInt(entry -> entry.getKey().tree().getPos())
            .thenComparingInt(entry -> -entry.getKey().tree().getEndPos())).map(entry -> {
      ImportTree parent = entry.getKey();
      Set<ImportTree> subs = entry.getValue();
      Set<Node> subsNode = subs.stream()
              .map(sub -> new Node(contentsContexts, rmIndex, sub.path, parent.srcDst, sub.tree, null, sub.type)).collect(Collectors.toSet());

      return new Node(contentsContexts, rmIndex, parent.path, parent.srcDst, parent.tree, subsNode, parent.type);
    }).forEach(this::addNode);
  }

  private Node addExtensionNode(Tree extensionTree, Node extendedNode) {
    String path = contentsContexts.getPath(extendedNode.getSrcDst(), extensionTree);
    Node node = new Node(contentsContexts, rmIndex, path, extendedNode.getSrcDst(), extensionTree, null, NodeType.EXTENSION);
    return addNode(node);
  }

  private Node addNode(Node node) {
    if (idNodeMap.containsKey(node.getId())) {
      Node existingNode = idNodeMap.get(node.getId());

      if (existingNode.getSubs() != null && node.getSubs() != null) {
        for (Node sub : node.getSubs()) {
          Optional<Node> foundSub = existingNode.getSubs().stream().filter(existingSub -> existingSub.getId().equals(sub.getId())).findFirst();
          if (foundSub.isEmpty()) {
            existingNode.getSubs().add(sub);
          }
        }
      }

      return existingNode;
    }

    graph.addVertex(node);
    idNodeMap.put(node.getId(), node);
    node.setUMLs(rmIndex.umls(node.getSrcDst(), node.getPath(), node.getTree(), false));

    addNodeContexts(node);

    return node;
  }

  private void addNodeContexts(Node node) {
    String path = node.getPath();
    SrcDst srcDst = node.getSrcDst();

    List<Pair<Tree, NodeType>> contexts = Context.get(node.getPath(), node.getTree(), node.isBase(), node.getFileContent());
    for (Pair<Tree, NodeType> context : contexts) {
      String potentialContextId = Node.formatId(path, srcDst, context.second, context.first);

      if (!idNodeMap.containsKey(potentialContextId)) {
        Node contextNode = new Node(contentsContexts, rmIndex, path, srcDst, context.first, null, context.second);
        graph.addVertex(contextNode);
        idNodeMap.put(contextNode.getId(), contextNode);
      }

      Node contextNode = idNodeMap.get(potentialContextId);
      contextNode.setUMLs(rmIndex.umls(contextNode.getSrcDst(), contextNode.getPath(), contextNode.getTree(), true));
      injectContextNode(contextNode);
    }

    injectContextNode(node);
  }

  private void injectContextNode(Node contextNode) {
    List<Node> descendantNodes = graph.vertexSet().stream().filter(node -> node.isDescendantOf(contextNode)).toList();
    List<Node> immediateDescendants = descendantNodes.stream().filter(
            subject -> descendantNodes.stream().noneMatch(subject::isDescendantOf)).toList();
    for (Node immediateDescendant : immediateDescendants) {
      Optional<Edge> contextEdge = graph.outgoingEdgesOf(immediateDescendant).stream()
              .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).findFirst();
      contextEdge.ifPresent(graph::removeEdge);

      Optional<Edge> existingEdge = graph.getAllEdges(immediateDescendant, contextNode).stream()
              .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).findFirst();
      if (existingEdge.isEmpty()) {
        addEdge(immediateDescendant, contextNode, EdgeType.CONTEXT);
      }
    }

    List<Node> predecessors = graph.vertexSet().stream().filter(contextNode::isDescendantOf).toList();
    Optional<Node> immediatePredecessor = predecessors.stream().filter(
            subject -> predecessors.stream().noneMatch(object -> object.isDescendantOf(subject))).findFirst();
    if (immediatePredecessor.isPresent()) {
      Optional<Edge> existingEdge = graph.getAllEdges(contextNode, immediatePredecessor.get())
              .stream()
              .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).findFirst();
      if (existingEdge.isEmpty()) {
        addEdge(contextNode, immediatePredecessor.get(), EdgeType.CONTEXT);
      }
    }
  }

  private void addEdge(Node node1, Node node2, EdgeType edgeType) {
    if (node1.equals(node2)) {
      return;
    }

    graph.addEdge(node1, node2, new Edge(edgeType));
  }

  public Graph<Node, Edge> getGraph() {
    return graph;
  }

  public void process() {
    importTrees(aggregateTrees(distinctTrees(srcTrees)));
    importTrees(aggregateTrees(distinctTrees(dstTrees)));

    if (memberUsageFinder != null) {
      memberUsageFinder.bindChanges(modelDiff.getParentModel());
      memberUsageFinder.bindChanges(modelDiff.getChildModel());
    }

    processDefUse();
    processClassLevelRelations();
    processOverrides();

    processExtensions(SrcDst.SRC);
    processExtensions(SrcDst.DST);

    processDiffs();
    processContextAlternative();
    processMapping();

    processSuccession();

    assignPromptIds();
  }

  private boolean overlapsChange(SrcDst srcDst, LocationInfo location) {
    return !findReachedChanges(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset()).isEmpty();
  }

  // Code reaches the changes it overlaps, not only the one enclosing it: an argument changed in an unchanged call
  // relies on the parameter it is passed to. The body of a lambda or an anonymous class within the code is not used by
  // it, so the changes inside such a body are not reached
  private List<Node> findReachedChanges(String path, SrcDst srcDst, int startOffset, int endOffset) {
    return util.findOverlappingNodes(path, srcDst, startOffset, endOffset, n -> !n.isContext() && !n.isExtension()
            && n.getTree().getPos() < endOffset && startOffset < n.getTree().getEndPos()
            && !isInBodyWithin(srcDst, path, n.getTree(), startOffset, endOffset));
  }

  private List<Node> editingNodes(List<Node> changes, LocationInfo use, boolean editing) {
    if (!editing) {
      return changes;
    }
    return changes.stream().filter(change -> !carriesOver(change, use.getStartOffset(), use.getEndOffset())).toList();
  }

  // Whether the use lies in code the change carries over as it is, such as a statement out of a removed try, a chain
  // extended by a new call or an expression passed to a new call: that code is unchanged where it is, so it uses
  // nothing anew. The code that is the whole change carries over every use overlapping it.
  private boolean carriesOver(Node change, int startOffset, int endOffset) {
    Tree tree = change.getTree();
    if (isCarriedOver(change, tree)) {
      return true;
    }

    while (tree.getPos() <= startOffset && endOffset <= tree.getEndPos()) {
      Tree holding = null;
      for (Tree child : tree.getChildren()) {
        if (child.getPos() <= startOffset && endOffset <= child.getEndPos()) {
          holding = child;
          break;
        }
      }
      if (holding == null) {
        return false;
      }
      if (isCarriedOver(change, holding)) {
        return true;
      }
      tree = holding;
    }

    return false;
  }

  // Code is carried over when the other revision holds it as it is (but its layout) under another parent, within the
  // code that holds it in both revisions below the member it is in: it got a new parent in place. It is either whole
  // statements, code staying in its statement, or code RM gives as staying in its statement in place (see isRestated);
  // code moved to another member or taken out to a new statement is not, as what it uses then works with other code.
  // Nor is code a new or a removed parent runs on a condition, in a loop or later (a lambda, an anonymous class): what
  // it uses then runs differently, which the change relies on.
  private boolean isCarriedOver(Node change, Tree tree) {
    for (ASTDiff diff : diffs) {
      boolean src = change.isSrc();
      if (!(src ? diff.getSrcPath() : diff.getDstPath()).equals(change.getPath())) {
        continue;
      }

      MappingStore mappingStore = diff.getAllMappings().getMonoMappingStore();
      Function<Tree, Tree> counterpartOf = src ? mappingStore::getDstForSrc : mappingStore::getSrcForDst;
      Tree counterpart = counterpartOf.apply(tree);
      // The mappings of a diff may reach trees of other files, whose content is not the one of its other path
      Tree counterpartRoot = src ? diff.dst.getRoot() : diff.src.getRoot();
      if (counterpart == null || (counterpart != counterpartRoot && !counterpart.getParents().contains(counterpartRoot))) {
        continue;
      }

      // Code re-wrapped over other lines, as a chain a new step is appended to, is the same code
      if (!tree.isIsomorphicTo(counterpart)) {
        return false;
      }

      Constants constants = new Constants(change.getPath());
      SrcDst srcDst = src ? SrcDst.SRC : SrcDst.DST;
      SrcDst counterpartSrcDst = src ? SrcDst.DST : SrcDst.SRC;
      String counterpartPath = src ? diff.getDstPath() : diff.getSrcPath();
      if (isRestated(tree, counterpart, counterpartOf, srcDst, change.getPath(), counterpartSrcDst, counterpartPath)) {
        return true;
      }

      Tree holder = tree.getParent();
      Tree holderCounterpart = holder == null ? null : counterpartOf.apply(holder);
      if (holderCounterpart == counterpart.getParent()) {
        return false;
      }
      while (holder != null && holderCounterpart == null) {
        holder = holder.getParent();
        holderCounterpart = holder == null ? null : counterpartOf.apply(holder);
      }

      if (holder == null || constants.isType(holder.getType().name) || constants.isRoot(holder.getType().name)
              || !counterpart.getParents().contains(holderCounterpart)
              || runsDifferently(srcDst, change.getPath(), tree, holder)
              || runsDifferently(counterpartSrcDst, counterpartPath, counterpart, holderCounterpart)) {
        return false;
      }

      // A whole statement, or code staying in the statement RM gives as the same in both revisions
      RMLinks.Statement statement = rmLinks.held(srcDst, change.getPath(), tree);
      return statement != null && (statement.isWhole(tree)
              || statement.isSameAs(rmLinks.held(counterpartSrcDst, counterpartPath, counterpart)));
    }

    return false;
  }

  // Whether RM gives the statements holding the tree and its counterpart as the same statement although the AST diff
  // does not map them, at the same place: a statement mapped to one of another kind, as a returned creation assigned to a
  // variable instead or the expression body of a lambda becoming the returned value of its block body, or the value of
  // an extracted (inlined) variable and a statement it is replaced in. The value is computed where it was, only kept or
  // handed on differently
  private boolean isRestated(Tree tree, Tree counterpart, Function<Tree, Tree> counterpartOf, SrcDst srcDst, String path,
                             SrcDst counterpartSrcDst, String counterpartPath) {
    RMLinks.Statement statement = rmLinks.held(srcDst, path, tree);
    RMLinks.Statement counterpartStatement = rmLinks.held(counterpartSrcDst, counterpartPath, counterpart);
    if (statement == null || counterpartStatement == null || !statement.isSameAs(counterpartStatement)) {
      return false;
    }

    Tree statementTree = statement.outermostWithin(tree);
    Tree counterpartStatementTree = counterpartStatement.outermostWithin(counterpart);
    if (counterpartOf.apply(statementTree) == counterpartStatementTree) {
      return false;
    }

    Tree holder = statementTree.getParent();
    Tree holderCounterpart = holder == null ? null : counterpartOf.apply(holder);
    while (holder != null && holderCounterpart == null) {
      holder = holder.getParent();
      holderCounterpart = holder == null ? null : counterpartOf.apply(holder);
    }
    return holder != null && counterpartStatementTree.getParents().contains(holderCounterpart)
            && !runsDifferently(srcDst, path, tree, holder)
            && !runsDifferently(counterpartSrcDst, counterpartPath, counterpart, holderCounterpart);
  }

  // Whether code below the holder, as RM gives it, runs the tree on a condition, in a loop or later
  private boolean runsDifferently(SrcDst srcDst, String path, Tree tree, Tree holder) {
    for (RMIndex.RunScope scope : rmIndex.runScopes(srcDst, path)) {
      if (scope.holdsBelow(tree.getPos(), tree.getEndPos()) && holder.getPos() <= scope.start() && scope.end() <= holder.getEndPos()
              && (holder.getPos() != scope.start() || holder.getEndPos() != scope.end())) {
        return true;
      }
    }
    return false;
  }

  // Whether the tree is in the body of a lambda or an anonymous class, as RM gives them, within the code at the range
  private boolean isInBodyWithin(SrcDst srcDst, String path, Tree tree, int startOffset, int endOffset) {
    for (RMIndex.RunScope scope : rmIndex.runScopes(srcDst, path)) {
      if (scope.later() && scope.holdsBelow(tree.getPos(), tree.getEndPos())
              && (startOffset < scope.start() || scope.end() < endOffset)) {
        return true;
      }
    }
    return false;
  }

  private void assignPromptIds() {
    for (int length = Node.PROMPT_ID_LENGTH; length <= Node.MAX_PROMPT_ID_LENGTH; length++) {
      Set<String> promptIds = new HashSet<>();
      boolean collision = false;

      for (Node node : graph.vertexSet()) {
        node.assignPromptId(length);
        if (!promptIds.add(node.getPromptId())) {
          collision = true;
          break;
        }
      }

      if (!collision) {
        return;
      }
    }

    throw new IllegalStateException("Failed to assign unique prompt ids");
  }

  private void processDiffs() {
    for (Node node : graph.vertexSet()) {
      for (ASTDiff diff : diffs) {
        if (node.isSrc() ? diff.getSrcPath().equals(node.getPath()) : diff.getDstPath().equals(node.getPath())) {
          node.addDiff(diff);

          if (node.getSubs() != null) {
            for (Node sub : node.getSubs()) {
              sub.addDiff(diff);
            }
          }
        }
      }
    }
  }

  private void processContextAlternative() {
    List<Node> contexts = graph.vertexSet().stream().filter(Node::isContext).toList();
    for (Node context : contexts) {
      Node alternative = context.getAlternative();
      if (alternative == null) {
        continue;
      }

      if (!idNodeMap.containsKey(alternative.getId())) {
        graph.addVertex(alternative);
        idNodeMap.put(alternative.getId(), alternative);
      }

      Node alternativeContextNode = idNodeMap.get(alternative.getId());
      for (ASTDiff diff : diffs) {
        if (alternativeContextNode.isSrc() ?
                diff.getSrcPath().equals(alternativeContextNode.getPath()) : diff.getDstPath().equals(alternativeContextNode.getPath())) {
          alternativeContextNode.addDiff(diff);
        }
      }

      injectContextNode(alternativeContextNode);
    }
  }

  private void processMapping() {
    List<Node> nodes = graph.vertexSet().stream().toList().stream().filter(node -> !node.isExtension()).toList();
    List<Node> srcNodes = nodes.stream().filter(Node::isSrc).toList();
    List<NodeTrees> dstNodes = nodes.stream().filter(Node::isDst).map(dstNode -> {
      Set<Tree> allTrees = new HashSet<>();
      allTrees.add(dstNode.getTree());
      if (dstNode.getSubs() != null) {
        allTrees.addAll(dstNode.getSubs().stream().map(Node::getTree).toList());
      }
      return new NodeTrees(dstNode, allTrees);
    }).toList();

    for (Node srcNode : srcNodes) {
      List<Node> srcOrSubs = new ArrayList<>();
      srcOrSubs.add(srcNode);
      if (srcNode.getSubs() != null) {
        srcOrSubs.addAll(srcNode.getSubs());
      }

      Set<Node> mappedDstNodes = new HashSet<>();
      for (Node srcOrSub : srcOrSubs) {
        // A tree no tree is mapped to alone may be mapped to several (see multiMovedTrees)
        List<Tree> dstTrees = new ArrayList<>();
        for (ASTDiff diff : srcOrSub.getDiffs()) {
          Tree dstTree = diff.getAllMappings().getMonoMappingStore().getDstForSrc(srcOrSub.getTree());
          if (dstTree != null) {
            dstTrees.add(dstTree);
          } else if (diff.getAllMappings().getDsts(srcOrSub.getTree()) != null) {
            dstTrees.addAll(diff.getAllMappings().getDsts(srcOrSub.getTree()));
          }
        }
        List<Node> srcOrSubMappedDstNodes = dstNodes.stream().filter(dstNode -> dstNode.trees.stream().anyMatch(dstTrees::contains))
                .map(dstNode -> dstNode.node).toList();
        if (srcNode.isContext()) {
          srcOrSubMappedDstNodes = srcOrSubMappedDstNodes.stream().filter(dstNode -> {
            if (!dstNode.isContext()) {
              return true;
            }
            return srcNode.getNodeType().equals(dstNode.getNodeType());
          }).toList();
        }

        mappedDstNodes.addAll(srcOrSubMappedDstNodes);
      }

      for (Node mappedDstNode : mappedDstNodes) {
        addEdge(srcNode, mappedDstNode, EdgeType.MAPPING);
      }
    }
  }

  private void processDefUse() {
    List<Node> nodes = graph.vertexSet().stream().filter(node -> !node.isExtension()).toList();
    for (Node node : nodes) {
      // This method is only for subclass relations (operations, attributes, and below), so the only LOCATION_CONTEXT
      // which could contribute to this method would be method which will be handled by the potential
      // SEMANTIC_CONTEXT with the same method tree. So no need to handle LOCATION_CONTEXT here
      if (node.getNodeType().equals(NodeType.LOCATION_CONTEXT)) {
        continue;
      }

      UMLs umls = node.getUMLs();
      if (umls == null) {
        continue;
      }

      Set<Node> defUseTargets = new HashSet<>();

      for (UMLOperation operation : umls.umlOperations) {
        node.addIdentifier(operation.getName());
        defUseTargets.addAll(getInvocationNodes(operation, node.getSrcDst(), false));
      }

      for (UMLAttribute attribute : umls.umlAttributes) {
        node.addIdentifier(attribute.getVariableDeclaration().getVariableName());
        defUseTargets.addAll(getAccessNodes(attribute, node.getSrcDst(), false));
      }

      for (VariableDeclaration variableDeclaration : umls.variableDeclarations) {
        node.addIdentifier(variableDeclaration.getVariableName());
        defUseTargets.addAll(findAccessNodes(variableDeclaration.getVariableName(),
                variableDeclaration.getScope().getStatementsInScopeUsingVariable(), node.getSrcDst(), false));
      }

      for (Entry<UMLOperation, Set<VariableDeclaration>> operationParameters : umls.operationParameters.entrySet()) {
        for (VariableDeclaration operationParameter : operationParameters.getValue()) {
          node.addIdentifier(operationParameter.getVariableName());
          defUseTargets.addAll(getArgumentNodes(operationParameters.getKey(), operationParameter, node.getSrcDst()));
        }
      }

      for (Node defUseTarget : defUseTargets) {
        addEdge(node, defUseTarget, EdgeType.DEF_USE);
      }
    }
  }

  // TODO: recursive extensions (from a non-extension to a non-extension OR to a terminal)
  // TODO: forward extensions (being used) in addition to the current backward extension (using)
  // TODO: noisy dependencies. Every declaration a change uses becomes a dependency printed whole, so the useful ones
  //  (e.g. the callee whose contract the change relies on) are buried among others which tell a reviewer nothing:
  //  - Trivial accessors and fluent APIs: getters returning a field (BrokerController#getMessageStore), builder
  //    steps and every overload of them used by a chain (Settings.builder(), the three Settings.Builder#put of one
  //    chain, RestClient#get/uri/build). Idea: print a declaration whose body is a single return of a field or of
  //    `this` by its signature only, and print the overloads of one name once.
  //  - Declarations far longer than what they tell: constructors and methods carrying long annotations
  //    (DateExtract's @FunctionInfo/@Param descriptions, ~30 lines before the signature) and multi-paragraph
  //    javadoc. Idea: drop annotation argument values and keep the first javadoc sentence, unless the change
  //    touches them.
  //  - Test framework helpers used by changed tests (ESTestCase#assertThat, randomFrom).
  //    Idea: leave out declarations of test-support code (test source sets of other modules, test frameworks)
  //    that the changes reach only from test code, unless that code is changed itself.
  //  - Dependencies of deleted code only (the metrics constants of removed latency recording, the old revision's
  //    log field). Idea: keep a dependency relied on by deleted code only when it is a callee or a member whose
  //    removal changes behavior (an invocation, a field written), and drop the constants and loggers it read.
  private void processExtensions(SrcDst srcDst) {
    HashMap<Node, Set<Node>> usesUsedNodes = new HashMap<>();

    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (UMLClass umlClass : umlModel.getClassList()) {
      // An enum constant is a field of its enum, as a case label naming it shows
      List<UMLAttribute> fieldDeclarations = new ArrayList<>(umlClass.getAttributes());
      fieldDeclarations.addAll(umlClass.getEnumConstants());
      for (UMLAttribute fieldDeclaration : fieldDeclarations) {
        LocationInfo declarationLocation = fieldDeclaration.getLocationInfo();
        Set<Node> accessNodes = getAccessNodes(fieldDeclaration, srcDst, false);
        Set<Node> editingAccessNodes = getAccessNodes(fieldDeclaration, srcDst, true);
        // A field is declared by its field declaration, which holds its type; an enum constant by itself
        pushUsedNodes(usesUsedNodes, accessNodes, editingAccessNodes, declarationLocation,
                fieldDeclaration instanceof UMLEnumConstant ? null : Tree::getParent, false, srcDst);
      }

      for (UMLOperation operation : umlClass.getOperations()) {
        LocationInfo operationLocation = operation.getLocationInfo();
        Set<Node> invocationNodes = getInvocationNodes(operation, srcDst, false);
        Set<Node> editingInvocationNodes = getInvocationNodes(operation, srcDst, true);
        pushUsedNodes(usesUsedNodes, invocationNodes, editingInvocationNodes, operationLocation, null, false, srcDst);

        pushLocalUsedNodes(usesUsedNodes, operation, umlModel, srcDst);
      }

      for (UMLInitializer initializer : umlClass.getInitializers()) {
        pushLocalUsedNodes(usesUsedNodes, initializer, umlModel, srcDst);
      }

      // The locals of the methods of anonymous classes, which the variables of the members declaring them leave out
      for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
        for (UMLOperation operation : anonymousClass.getOperations()) {
          pushLocalUsedNodes(usesUsedNodes, operation, umlModel, srcDst);
        }
      }
    }

    for (Entry<Node, Set<Node>> useUsedNodes : usesUsedNodes.entrySet()) {
      for (Node usedNode : useUsedNodes.getValue()) {
        boolean related = graph.getAllEdges(usedNode, useUsedNodes.getKey()).stream()
                .anyMatch(edge -> edge.getType().equals(EdgeType.DEF_USE));
        if (!related) {
          addEdge(usedNode, useUsedNodes.getKey(), EdgeType.DEF_USE);
        }
      }
    }
  }

  // A local is used by the statements RM finds in its scope, and by the code of anonymous classes that refers to it
  // deeper than the anonymous classes those statements create. An implicitly typed lambda parameter is a bare name,
  // which tells nothing of what it holds
  private void pushLocalUsedNodes(HashMap<Node, Set<Node>> usesUsedNodes, VariableDeclarationContainer container,
                                  UMLModel umlModel, SrcDst srcDst) {
    for (VariableDeclaration local : container.getAllVariableDeclarations()) {
      LocationInfo declarationLocation = local.getLocationInfo();
      Set<AbstractCodeFragment> fragments = local.getScope().getStatementsInScopeUsingVariable();
      Set<Node> accessNodes = findAccessNodes(local.getVariableName(), fragments, srcDst, false);
      Set<Node> editingAccessNodes = findAccessNodes(local.getVariableName(), fragments, srcDst, true);
      if (memberUsageFinder != null) {
        for (LeafExpression access : memberUsageFinder.findAnonymousClassAccesses(local, umlModel)) {
          LocationInfo accessLocation = access.getLocationInfo();
          List<Node> overlappingNodes = util.findOverlappingNodes(accessLocation.getFilePath(), srcDst, accessLocation.getStartOffset(),
                  accessLocation.getEndOffset(), n -> !n.isContext() && !n.isExtension());
          accessNodes.addAll(overlappingNodes);
          editingAccessNodes.addAll(editingNodes(overlappingNodes, accessLocation, true));
        }
      }
      String path = declarationLocation.getFilePath();
      pushUsedNodes(usesUsedNodes, accessNodes, editingAccessNodes, declarationLocation,
              tree -> rmIndex.declaringTree(srcDst, path, tree),
              rmIndex.isLambdaParameter(srcDst, path, local) && local.getType() == null, srcDst);
    }
  }

  // A change carrying code over as it is uses the declarations of that code anew only when the declaration is changed;
  // an unchanged declaration is an extension of the changes editing a use of it only, unless it is a bare name
  private void pushUsedNodes(HashMap<Node, Set<Node>> usesUsedNodes, Set<Node> useNodes, Set<Node> editingUseNodes,
                             LocationInfo usedLocation, Function<Tree, Tree> treeTransformer, boolean bareName, SrcDst srcDst) {
    if (useNodes.isEmpty()) {
      return;
    }

    Tree usedRootTree = contentsContexts.getRoot(srcDst, usedLocation.getFilePath());
    Tree usedTree = TreeUtilFunctions.findByLocationInfo(usedRootTree, usedLocation, new Constants(usedLocation.getFilePath()));
    if (usedTree != null && treeTransformer != null) {
      usedTree = treeTransformer.apply(usedTree);
    }
    int usedStart = usedTree == null ? usedLocation.getStartOffset() : usedTree.getPos();
    int usedEnd = usedTree == null ? usedLocation.getEndOffset() : usedTree.getEndPos();

    List<Node> usedNodes = util.findOverlappingNodes(usedLocation.getFilePath(), srcDst, usedStart, usedEnd,
            (n) -> !n.isContext() && !n.isExtension() && n.getTree().getPos() < usedEnd && usedStart < n.getTree().getEndPos());
    if (!usedNodes.isEmpty()) {
      // A changed declaration is no extension, but its uses outside it rely on its changes as they would on it
      for (Node useNode : useNodes) {
        if (useNode.getPath().equals(usedLocation.getFilePath()) && usedStart <= useNode.getTree().getPos()
                && useNode.getTree().getEndPos() <= usedEnd) {
          continue;
        }

        usesUsedNodes.computeIfAbsent(useNode, node -> new HashSet<>()).addAll(usedNodes);
      }
      return;
    }

    if (bareName) {
      return;
    }

    for (Node useNode : editingUseNodes) {
      Node usedExtensionNode = addExtensionNode(usedTree, useNode);

      usesUsedNodes.putIfAbsent(useNode, new HashSet<>());
      usesUsedNodes.get(useNode).add(usedExtensionNode);
    }
  }

  private Set<Node> getArgumentNodes(UMLOperation umlOperation, VariableDeclaration parameterDeclaration, SrcDst srcDst) {
    Set<Node> result = new HashSet<>();

    int parameterIndex = umlOperation.getParameterDeclarationList().indexOf(parameterDeclaration);

    for (AbstractCall invocation : findInvocations(umlOperation, srcDst)) {
      // Passing callback through method reference does not have arguments
      if (parameterIndex >= invocation.arguments().size()) {
        continue;
      }

      LocationInfo invocationLocation = invocation.getLocationInfo();

      String invocationFileContent = contentsContexts.getContent(srcDst, invocationLocation.getFilePath());
      String invocationStr = invocationFileContent.substring(invocationLocation.getStartOffset(), invocationLocation.getEndOffset());

      String parameterArgumentStr = invocation.arguments().get(parameterIndex);
      int argumentIndex = -1;
      for (int i = 0; i <= parameterIndex; i++) {
        String argument = invocation.arguments().get(i);
        if (argument.equals(parameterArgumentStr)) {
          argumentIndex = invocationStr.indexOf(parameterArgumentStr, argumentIndex + 1);
        }
      }

      int startOffset = invocationLocation.getStartOffset() + argumentIndex;
      int endOffset = startOffset + parameterArgumentStr.length();
      result.addAll(findReachedChanges(invocationLocation.getFilePath(), srcDst, startOffset, endOffset));
    }

    return result;
  }

  private void processClassLevelRelations() {
    // Class Instance Creation
    List<Node> nodes = graph.vertexSet().stream().filter(node -> !node.isContext() && !node.isExtension()).toList();
    for (Node node : nodes) {
      if (node.getUMLs() == null) {
        continue;
      }

      Set<AbstractCall> creations = new HashSet<>();

      for (UMLOperation umlOperation : node.getUMLs().umlOperations) {
        creations.addAll(umlOperation.getAllCreations());
      }
      for (UMLAttribute umlAttribute : node.getUMLs().umlAttributes) {
        creations.addAll(umlAttribute.getAllCreations());
      }

      Set<UMLAbstractClass> createdClasses = new HashSet<>();

      for (AbstractCall creation : creations) {
        String createdClassName = creation.getName();
        UMLAbstractClass createdClass = node.isSrc() ?
                modelDiff.findClassInParentModel(createdClassName) : modelDiff.findClassInChildModel(createdClassName);
        if (createdClass == null) {
          continue;
        }

        createdClasses.add(createdClass);
      }

      // A class literal hands the class to the code using it, as a creation does
      for (UMLOperation umlOperation : node.getUMLs().umlOperations) {
        if (umlOperation.getBody() == null) {
          continue;
        }
        for (AbstractCodeFragment statement : umlOperation.getBody().getCompositeStatement().getAllStatements()) {
          for (LeafExpression typeLiteral : statement.getTypeLiterals()) {
            String literalClassName = typeLiteral.getString().replaceFirst("\\.class$", "");
            UMLAbstractClass literalClass = node.isSrc() ?
                    modelDiff.findClassInParentModel(literalClassName) : modelDiff.findClassInChildModel(literalClassName);
            if (literalClass != null) {
              createdClasses.add(literalClass);
            }
          }
        }
      }

      Set<Node> classNodes = new HashSet<>();

      for (UMLAbstractClass createdClass : createdClasses) {
        Node classNode = getClassNode(createdClass, node.getSrcDst());
        if (classNode == null) {
          continue;
        }
        classNodes.add(classNode);
      }

      for (Node classNode : classNodes) {
        addEdge(classNode, node, EdgeType.DEF_USE);
      }
    }

    processCreations(SrcDst.SRC);
    processCreations(SrcDst.DST);

    // extends and implements
    processGeneralizations(SrcDst.SRC);
    processRealizations(SrcDst.SRC);
    processGeneralizations(SrcDst.DST);
    processRealizations(SrcDst.DST);
  }

  // A creation in changed code relies on the class it creates as a whole changed member creating it does, so a changed
  // class declaration is used by the changes holding a creation of it, such as removed code creating a removed record
  private void processCreations(SrcDst srcDst) {
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (UMLClass umlClass : umlModel.getClassList()) {
      List<AbstractCall> creations = new ArrayList<>();
      for (UMLOperation operation : umlClass.getOperations()) {
        creations.addAll(operation.getAllCreations());
      }
      for (UMLAttribute attribute : umlClass.getAttributes()) {
        creations.addAll(attribute.getAllCreations());
      }

      for (AbstractCall creation : creations) {
        UMLAbstractClass createdClass = srcDst.equals(SrcDst.SRC) ? modelDiff.findClassInParentModel(creation.getName())
                : modelDiff.findClassInChildModel(creation.getName());
        Node classNode = createdClass == null ? null : getClassNode(createdClass, srcDst);
        if (classNode == null || classNode.isContext()) {
          continue;
        }

        LocationInfo location = creation.getLocationInfo();
        for (Node creatingChange : findReachedChanges(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset())) {
          if (!creatingChange.isDescendantOf(classNode)) {
            addDefUse(classNode, creatingChange);
          }
        }
      }
    }
  }

  // A changed method of the new revision overriding or implementing a changed method relies on it as a caller does: it
  // fulfills the contract the overridden method declares. An old override relies on nothing the change needs.
  // TODO: an unchanged overridden method is no extension of the changes of the overriding one yet, though its contract
  //  (an interface method and its javadoc) is what a reviewer judges an implementation against
  private void processOverrides() {
    if (receiverTypeResolver == null) {
      return;
    }

    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    for (UMLClass umlClass : umlModel.getClassList()) {
      for (UMLOperation operation : umlClass.getOperations()) {
        List<Node> overridingChanges = declarationChanges(operation, srcDst);
        if (overridingChanges.isEmpty()) {
          continue;
        }

        for (UMLOperation overridden : receiverTypeResolver.findOverridden(operation, umlClass, umlModel)) {
          for (Node overriddenChange : declarationChanges(overridden, srcDst)) {
            for (Node overridingChange : overridingChanges) {
              addDefUse(overriddenChange, overridingChange);
            }
          }

          // A call of the overridden method may run the changed override, as a test calling it through the interface
          // its subject implements does; but a call the override relies on, as a template method of the class declaring
          // the hook it implements, reads before it
          for (Node invocationChange : getInvocationNodes(overridden, srcDst, false)) {
            for (Node overridingChange : overridingChanges) {
              if (graph.getAllEdges(invocationChange, overridingChange).stream().noneMatch(edge -> edge.getType().equals(EdgeType.DEF_USE))) {
                addDefUse(overridingChange, invocationChange);
              }
            }
          }
        }
      }
    }
  }

  private void addDefUse(Node used, Node using) {
    boolean related = graph.getAllEdges(used, using).stream().anyMatch(edge -> edge.getType().equals(EdgeType.DEF_USE));
    if (!related) {
      addEdge(used, using, EdgeType.DEF_USE);
    }
  }

  private List<Node> declarationChanges(UMLOperation operation, SrcDst srcDst) {
    LocationInfo location = operation.getLocationInfo();
    return util.findOverlappingNodes(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset(),
            n -> !n.isContext() && !n.isExtension() && n.getTree().getPos() < location.getEndOffset()
                    && location.getStartOffset() < n.getTree().getEndPos());
  }

  private void processGeneralizations(SrcDst srcDst) {
    UMLModel umlModel =
            srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (UMLGeneralization generalization : umlModel.getGeneralizationList()) {
      UMLClass child = generalization.getChild();

      Node childClassNode = getClassNode(child, srcDst);
      if (childClassNode == null) {
        continue;
      }

      UMLAbstractClass parentClass = srcDst.equals(SrcDst.SRC) ? modelDiff.findClassInParentModel(generalization.getParent())
              : modelDiff.findClassInChildModel(generalization.getParent());
      if (parentClass == null) {
        continue;
      }

      Node parentClassNode = getClassNode(parentClass, srcDst);
      if (parentClassNode == null) {
        continue;
      }

      addEdge(parentClassNode, childClassNode, EdgeType.DEF_USE);
    }
  }

  private void processRealizations(SrcDst srcDst) {
    UMLModel umlModel =
            srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (UMLRealization realization : umlModel.getRealizationList()) {
      UMLClass child = realization.getClient();

      Node childClassNode = getClassNode(child, srcDst);
      if (childClassNode == null) {
        continue;
      }

      UMLAbstractClass parentClass = srcDst.equals(SrcDst.SRC) ? modelDiff.findClassInParentModel(realization.getSupplier())
              : modelDiff.findClassInChildModel(realization.getSupplier());
      if (parentClass == null) {
        continue;
      }

      Node parentClassNode = getClassNode(parentClass, srcDst);
      if (parentClassNode == null) {
        continue;
      }

      addEdge(parentClassNode, childClassNode, EdgeType.DEF_USE);
    }
  }

  @Nullable
  private Node getClassNode(UMLAbstractClass umlClass, SrcDst srcDst) {
    Optional<Node> optionalClassNode = graph.vertexSet().stream().filter(node -> !node.isExtension()
            && node.getSrcDst().equals(srcDst) && node.getUMLs() != null && node.getUMLs().umlClasses.contains(umlClass)).findFirst();
    if (optionalClassNode.isEmpty()) {
      return null;
    }

    Node classNode = optionalClassNode.get();
    classNode.addIdentifier(umlClass.getNonQualifiedName());

    return classNode;
  }

  // The changes reached by the invocations of the operation; when editing, only those whose own code holds an invocation
  // (see isCarriedOver)
  private Set<Node> getInvocationNodes(UMLOperation operation, SrcDst srcDst, boolean editing) {
    Set<Node> result = new HashSet<>();

    for (AbstractCall invocation : findInvocations(operation, srcDst)) {
      LocationInfo invocationLocationInfo = invocation.getLocationInfo();
      result.addAll(editingNodes(findReachedChanges(invocationLocationInfo.getFilePath(), srcDst,
              invocationLocationInfo.getStartOffset(), invocationLocationInfo.getEndOffset()), invocationLocationInfo, editing));
    }

    return result;
  }

  private List<AbstractCall> findInvocations(UMLOperation operation, SrcDst srcDst) {
    if (memberUsageFinder == null) {
      return srcDst.equals(SrcDst.SRC) ? modelDiff.findInvocationsInParentModel(operation)
              : modelDiff.findInvocationsInChildModel(operation);
    }
    return memberUsageFinder.findInvocations(operation, srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel());
  }

  private Set<Node> getAccessNodes(UMLAttribute attribute, SrcDst srcDst, boolean editing) {
    if (memberUsageFinder == null) {
      return findAccessNodes(attribute.getName(), srcDst.equals(SrcDst.SRC) ? modelDiff.findFieldAccessesInParentModel(attribute)
              : modelDiff.findFieldAccessesInChildModel(attribute), srcDst, editing);
    }

    Set<Node> result = new HashSet<>();
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
      LocationInfo accessLocation = access.getLocationInfo();
      result.addAll(editingNodes(util.findOverlappingNodes(accessLocation.getFilePath(), srcDst, accessLocation.getStartOffset(),
              accessLocation.getEndOffset(), n -> !n.isContext() && !n.isExtension()), accessLocation, editing));
    }
    return result;
  }

  private Set<Node> findAccessNodes(String name, Set<AbstractCodeFragment> accessFragments, SrcDst srcDst, boolean editing) {
    Set<Node> result = new HashSet<>();

    for (AbstractCodeFragment accessFragment : accessFragments) {
      List<LeafExpression> useVariables =
              accessFragment.getVariables().stream().filter(variable -> variable.getString()
                      .contains(name)).toList();

      for (LeafExpression useVariable : useVariables) {
        LocationInfo useVariableLocation = useVariable.getLocationInfo();
        List<Node> overlappingNodes = util.findOverlappingNodes(useVariableLocation.getFilePath(),
                srcDst, useVariableLocation.getStartOffset(), useVariableLocation.getEndOffset(),
                n -> !n.isContext() && !n.isExtension());
        result.addAll(editingNodes(overlappingNodes, useVariableLocation, editing));
      }
    }

    return result;
  }

  private void processSuccession() {
    List<Node> nodes = graph.vertexSet().stream()
            .filter(node -> !node.isContext() && !node.isExtension()).toList();
    for (Node subject : nodes) {
      Tree rightSibling = subject.getRight();
      if (rightSibling == null) {
        continue;
      }

      for (Node object : nodes) {
        if (object.getTree().equals(rightSibling)) {
          addEdge(subject, object, EdgeType.SUCCESSION);
        }
      }
    }
  }

  private record ImportTree(Tree tree, NodeType type, String path, SrcDst srcDst) {
  }

  private record NodeTrees(Node node, Set<Tree> trees) {
  }
}
