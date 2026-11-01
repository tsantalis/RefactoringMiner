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
import org.refactoringminer.util.PathFileUtils;

// TODO: eliminate the ASTDiff layer, building the network and its representation from RM's model only (UMLModel,
//  UMLModelDiff, body mappers, refactorings, LocationInfo). RM's ASTDiff is derived from UMLModelDiff (its wrapper
//  matchers take member and statement correspondences from the body mappers; GumTree only runs inside statement
//  pairs RM mapped), so the code here converts back and forth: location -> tree (findByLocationInfo,
//  declarationTree) and tree -> RM element (RMIndex, RMLinks#held).
//  - profile (JFR, one RepresentationTest run, 2026-10-31): 43.5% is ExtendedMultiMappingStore#getMonoMappingStore
//    rebuilt on every call (Node#getAlternative 17.3%, Node#isOverloaded 14.6%, RawRepresentation#mappingStores
//    7.8%, RawRepresentation#movedOrigin 1.8%; the lookups themselves 0.1%); ProjectASTDiffer 11.2%; matching trees
//    to UML* (RMIndex, RMLinks, findByLocationInfo, Context) 5.6%; binding (ReceiverTypeResolver,
//    MemberUsageFinder) 15.1% and RM model + UMLModelDiff 10.5% stay either way
//  - step 0, independent quick win: share the cached mono stores (monoMappings) with Node and RawRepresentation
//  - nodes from RM elements (class, operation, attribute, statement, comment, doc, annotation): mapped statements
//    whose text differs are updates, getNonMappedLeavesT1/T2 and getNonMappedInnerNodesT1/T2 deletions/additions,
//    child mappers and class move/rename diffs moves; ids from file and offsets
//  - contexts from RM containment (class -> member -> composite -> statement), replacing Context's tree types and
//    Constants#isType/isRoot/isNamedBlock in one pass
//  - counterparts from RMLinks matches instead of mapping-store walks (counterpart, isMapped, isCarriedOver,
//    isRestated, movesUse, movesAsIs, counterpartChanges, processMapping, Node#getAlternative, Node#isOverloaded)
//  - RMIndex indexed by RM element, not looked up by tree
//  - rendering (RawRepresentation anchors, tag/collectCarriedOver/keptLines/tagMoved, closeChangedLines,
//    leafLinePairs, partnerWindow, movedOrigin) from statement pairs plus a line comparison inside each mapped pair:
//    rendering only needs which lines changed
//  - last, stop building ProjectASTDiffer (keep UMLModelDiff#getRefactorings)
//  - RM gaps to document, not work around: edits inside a statement have no location (Replacement holds strings),
//    so findReachedChanges/editingNodes/carriesOver/changesReceiverOnly become statement-granular (coarser def-use,
//    not missing); TypeScript exports and empty statements are not represented
//  Guard each step with RepresentationTest; justify every changed output.
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
  private final Map<SrcDst, Map<String, Set<String>>> memberUses = new EnumMap<>(SrcDst.class);
  // The mono mappings of each diff, which the store of a diff builds anew on each request
  private final Map<ASTDiff, MappingStore> monoMappings = new IdentityHashMap<>();
  private final MemberUsageFinder memberUsageFinder;
  private final ReceiverTypeResolver receiverTypeResolver;

  public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents, Map<String, String> dstContents,
                     RepositoryStateFiles repositoryStateFiles) {
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
    this.receiverTypeResolver = new ReceiverTypeResolver(modelDiff, repositoryStateFiles, contentsContexts);
    this.memberUsageFinder = new MemberUsageFinder(receiverTypeResolver,
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

  private MappingStore monoMappings(ASTDiff diff) {
    return monoMappings.computeIfAbsent(diff, key -> key.getAllMappings().getMonoMappingStore());
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

    memberUsageFinder.bindChanges(modelDiff.getParentModel());
    memberUsageFinder.bindChanges(modelDiff.getChildModel());

    processDefUse();
    processClassLevelRelations();
    processOverrides();

    processExtensions(SrcDst.SRC);
    processExtensions(SrcDst.DST);
    processUsages();
    processBridges();
    pruneContextDefUses();

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
    return editingNodes(changes, use, editing, null);
  }

  // The changes editing the use of the member (a local when none): the ones whose own code holds it, not carrying it over,
  // only renaming a variable in it or only changing the receiver the use is made on
  private List<Node> editingNodes(List<Node> changes, LocationInfo use, boolean editing, @Nullable String member) {
    if (!editing) {
      return changes;
    }
    return changes.stream().filter(change -> !carriesOver(change, use.getStartOffset(), use.getEndOffset())
            && !renamesVariable(change)
            && (member == null || !movesUse(change, use, member) && !changesReceiverOnly(change, use, member))).toList();
  }

  // Whether the change lies in the receiver of the use, a call or a field access binding the member in both revisions,
  // as a step inserted in a chain before the call: the use itself stays as it was, so it relies on nothing anew. The
  // receiver is the code of the use starting where the use starts, before its name and its arguments
  private boolean changesReceiverOnly(Node change, LocationInfo use, String member) {
    Tree useTree = declarationTree(change.getSrcDst(), use);
    // The use is the innermost of the trees at its range, not a wrapper of it, as the receiver of an enclosing call
    for (Tree child = useTree == null ? null : sameRangeChild(useTree); child != null; child = sameRangeChild(useTree)) {
      useTree = child;
    }
    Tree tree = change.getTree();
    if (useTree == null || !tree.getParents().contains(useTree)) {
      return false;
    }
    Tree part = tree;
    while (part.getParent() != useTree) {
      part = part.getParent();
    }
    if (part.getPos() != useTree.getPos()) {
      return false;
    }

    Counterpart counterpart = counterpart(change, useTree);
    SrcDst counterpartSrcDst = change.isSrc() ? SrcDst.DST : SrcDst.SRC;
    return counterpart != null && memberUses(counterpartSrcDst).getOrDefault(locationKey(counterpart.path(),
            counterpart.tree().getPos(), counterpart.tree().getEndPos()), Set.of()).contains(member);
  }

  @Nullable
  private static Tree sameRangeChild(Tree tree) {
    return tree.getChildren().stream().filter(child -> child.getPos() == tree.getPos() && child.getEndPos() == tree.getEndPos())
            .findFirst().orElse(null);
  }

  // Whether the change only renames a variable RM gives as renamed (Rename Variable, Rename Parameter), at its declaration
  // or at a reference to it: the code holding it reads the same value as before, so it uses nothing anew and gives its
  // users nothing new
  private boolean renamesVariable(Node change) {
    if (!change.getNodeType().equals(NodeType.SRC_UPDATE) && !change.getNodeType().equals(NodeType.DST_UPDATE)) {
      return false;
    }
    Counterpart counterpart = counterpart(change, change.getTree());
    if (counterpart == null) {
      return false;
    }
    return change.isSrc() ? rmLinks.isRename(change.getPath(), change.getTree(), counterpart.path(), counterpart.tree())
            : rmLinks.isRename(counterpart.path(), counterpart.tree(), change.getPath(), change.getTree());
  }

  // Whether the change is code RM gives as computing what an inlined (extracted) variable held where it was, only kept or
  // handed on differently (see RMLinks#restatesVariable)
  private boolean restatesVariable(Node change) {
    Counterpart counterpart = counterpart(change, change.getTree());
    return rmLinks.restatesVariable(change.getSrcDst(), change.getPath(), change.getTree(),
            counterpart == null || !counterpart.tree().isIsomorphicTo(change.getTree()) ? null : counterpart.tree());
  }

  // The tree of the other revision the AST diff of the change's file maps the tree to, within the file it pairs with
  @Nullable
  private Counterpart counterpart(Node change, Tree tree) {
    boolean src = change.isSrc();
    for (ASTDiff diff : diffs) {
      if (!(src ? diff.getSrcPath() : diff.getDstPath()).equals(change.getPath())) {
        continue;
      }
      MappingStore mappingStore = monoMappings(diff);
      Tree counterpart = src ? mappingStore.getDstForSrc(tree) : mappingStore.getSrcForDst(tree);
      Tree counterpartRoot = src ? diff.dst.getRoot() : diff.src.getRoot();
      if (counterpart == null || (counterpart != counterpartRoot && !counterpart.getParents().contains(counterpartRoot))) {
        continue;
      }
      return new Counterpart(src ? diff.getDstPath() : diff.getSrcPath(), counterpart);
    }
    return null;
  }

  private record Counterpart(String path, Tree tree) {
  }

  // Whether the use lies in code the change moves as it is to another member, such as a body extracted to a new method,
  // where it uses the member it used there: it relies on nothing anew. What it uses of the member it arrives in (a
  // parameter for a local it read) is code of the change, or binds it anew
  private boolean movesUse(Node change, LocationInfo use, String member) {
    // The moved code is the change, or code of it, as the statements a new method receives
    List<Node> moves = new ArrayList<>(List.of(change));
    if (change.getSubs() != null) {
      moves.addAll(change.getSubs());
    }
    // The innermost moved code holding the use, as a call moved into a new conditional within a moved statement
    Tree useTree = null;
    Node usingMove = null;
    for (Node move : moves) {
      if (!move.getNodeType().equals(NodeType.SRC_MOVE) && !move.getNodeType().equals(NodeType.DST_MOVE)
              || usingMove != null && !move.getTree().getParents().contains(usingMove.getTree())) {
        continue;
      }
      for (Tree tree : move.getTree().preOrder()) {
        if (tree.getPos() == use.getStartOffset() && tree.getEndPos() == use.getEndOffset()) {
          useTree = tree;
          usingMove = move;
          break;
        }
      }
    }
    // Code moved under a new (removed) condition, loop or lambda runs differently where it arrived (left), which the
    // change relies on as it does on code a new parent wraps in place
    if (useTree == null || runsUnderNewScope(usingMove)) {
      return false;
    }

    boolean src = change.isSrc();
    SrcDst counterpartSrcDst = src ? SrcDst.DST : SrcDst.SRC;
    for (ASTDiff diff : diffs) {
      if (!(src ? diff.getSrcPath() : diff.getDstPath()).equals(change.getPath())) {
        continue;
      }

      MappingStore mappingStore = monoMappings(diff);
      Tree counterpart = src ? mappingStore.getDstForSrc(useTree) : mappingStore.getSrcForDst(useTree);
      Tree counterpartRoot = src ? diff.dst.getRoot() : diff.src.getRoot();
      if (counterpart == null || (counterpart != counterpartRoot && !counterpart.getParents().contains(counterpartRoot))
              || !useTree.isIsomorphicTo(counterpart)) {
        continue;
      }

      String counterpartPath = src ? diff.getDstPath() : diff.getSrcPath();
      UMLOperation operation = rmIndex.locate(change.getSrcDst(), change.getPath(), useTree).operation();
      UMLOperation counterpartOperation = rmIndex.locate(counterpartSrcDst, counterpartPath, counterpart).operation();
      if (operation == null || counterpartOperation == null
              || rmLinks.isSameMember(src ? operation : counterpartOperation, src ? counterpartOperation : operation)) {
        return false;
      }

      return memberUses(counterpartSrcDst).getOrDefault(locationKey(counterpartPath, counterpart.getPos(), counterpart.getEndPos()), Set.of())
              .contains(member);
    }
    return false;
  }

  // Whether code of the revision of the move runs the moved code on a condition, in a loop or later where the other
  // revision has no such code, or the other revision runs it so where this one has none
  private boolean runsUnderNewScope(Node move) {
    Counterpart counterpart = counterpart(move, move.getTree());
    if (counterpart == null) {
      return false;
    }
    SrcDst counterpartSrcDst = move.isSrc() ? SrcDst.DST : SrcDst.SRC;
    return holdsInUnmappedScope(move.getSrcDst(), move.getPath(), move.getTree())
            || holdsInUnmappedScope(counterpartSrcDst, counterpart.path(), counterpart.tree());
  }

  // Whether a scope of the file holding the tree below itself, as RM gives it, is code the AST diff maps to nothing
  private boolean holdsInUnmappedScope(SrcDst srcDst, String path, Tree tree) {
    Tree root = tree;
    while (root.getParent() != null) {
      root = root.getParent();
    }
    for (RMIndex.RunScope scope : rmIndex.runScopes(srcDst, path)) {
      if (!scope.holdsBelow(tree.getPos(), tree.getEndPos())) {
        continue;
      }
      Tree scopeTree = TreeUtilFunctions.getTreeBetweenPositions(root, scope.start(), scope.end());
      for (Tree child = scopeTree == null ? null : sameRangeChild(scopeTree); child != null; child = sameRangeChild(scopeTree)) {
        scopeTree = child;
      }
      if (scopeTree != null && !isMapped(srcDst, path, scopeTree)) {
        return true;
      }
    }
    return false;
  }

  // Whether an AST diff of the file maps the tree to code of the file it pairs with
  private boolean isMapped(SrcDst srcDst, String path, Tree tree) {
    boolean src = srcDst.equals(SrcDst.SRC);
    for (ASTDiff diff : diffs) {
      if (!(src ? diff.getSrcPath() : diff.getDstPath()).equals(path)) {
        continue;
      }
      MappingStore mappingStore = monoMappings(diff);
      Tree counterpart = src ? mappingStore.getDstForSrc(tree) : mappingStore.getSrcForDst(tree);
      Tree counterpartRoot = src ? diff.dst.getRoot() : diff.src.getRoot();
      if (counterpart != null && (counterpart == counterpartRoot || counterpart.getParents().contains(counterpartRoot))) {
        return true;
      }
    }
    return false;
  }

  // The members each use of a revision binds to, by the location of the use
  private Map<String, Set<String>> memberUses(SrcDst srcDst) {
    return memberUses.computeIfAbsent(srcDst, side -> {
      Map<String, Set<String>> uses = new HashMap<>();
      UMLModel umlModel = side.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
      for (UMLClass umlClass : umlModel.getClassList()) {
        for (UMLOperation operation : umlClass.getOperations()) {
          for (AbstractCall invocation : findInvocations(operation, side)) {
            LocationInfo location = invocation.getLocationInfo();
            uses.computeIfAbsent(locationKey(location.getFilePath(), location.getStartOffset(), location.getEndOffset()),
                    key -> new HashSet<>()).add(memberKey(operation));
          }
        }
        List<UMLAttribute> attributes = new ArrayList<>(umlClass.getAttributes());
        attributes.addAll(umlClass.getEnumConstants());
        for (UMLAttribute attribute : attributes) {
          for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
            LocationInfo location = access.getLocationInfo();
            uses.computeIfAbsent(locationKey(location.getFilePath(), location.getStartOffset(), location.getEndOffset()),
                    key -> new HashSet<>()).add(memberKey(attribute));
          }
        }
      }
      return uses;
    });
  }

  private static String locationKey(String path, int start, int end) {
    return path + ":" + start + ":" + end;
  }

  // The member a use binds to, the same in both revisions when its class and its signature are
  private static String memberKey(Object member) {
    return member instanceof UMLOperation operation ? operation.getKey()
            : ((UMLAttribute) member).getClassName() + "." + ((UMLAttribute) member).getName();
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

      MappingStore mappingStore = monoMappings(diff);
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
      // Code leaving a holder that stays in the other revision, as a statement moved out of a try that stays, is held
      // there by the code the other revision gives as holding it, when that code holds the tree too
      if (holder != null && !counterpart.getParents().contains(holderCounterpart)) {
        Function<Tree, Tree> treeOf = src ? mappingStore::getSrcForDst : mappingStore::getDstForSrc;
        Tree counterpartHolder = counterpart.getParent();
        Tree counterpartHolderTree = counterpartHolder == null ? null : treeOf.apply(counterpartHolder);
        while (counterpartHolder != null && counterpartHolderTree == null) {
          counterpartHolder = counterpartHolder.getParent();
          counterpartHolderTree = counterpartHolder == null ? null : treeOf.apply(counterpartHolder);
        }
        if (counterpartHolderTree != null && tree.getParents().contains(counterpartHolderTree)) {
          holder = counterpartHolderTree;
          holderCounterpart = counterpartHolder;
        }
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
          Tree dstTree = monoMappings(diff).getDstForSrc(srcOrSub.getTree());
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

  // TODO: data and control flow around the changes. Dependencies, usages (processUsages), bridges (processBridges) and
  //  the local holding a field (localUsers) are special cases of how values move and of what runs around a use; one
  //  graph of both would subsume them and reach the gaps of the oracle's "data/control flow" todos:
  //  - value flow, from RM facts only: nodes are locals, parameters, fields, returns and creation sites; edges are
  //    assignments and initializers (getAssignments, as MemberUsageFinder.isAssigned), reads (VariableScope
  //    #getStatementsInScopeUsingVariable, bound field accesses), argument -> parameter of bound calls, return -> call
  //    site, and a lambda passed to a call running later in the caller (as RMIndex.RunScope#later)
  //  - control flow: a structured graph per member from RM's nesting (RMIndex units, bodies, catch/finally clauses) and
  //    its exits (return, throw, break, continue, yield), telling the guards of a statement (earlier statements exiting
  //    on some path), its handlers, the definitions reaching a read and what runs after it
  //  - queries, all starting at changes: paths of unchanged code between changes (bridges of any length; one member
  //    only today), effect slices forward from a changed value, argument slices backward from a call of changed code,
  //    and the creation sites reaching a receiver (dispatch of interface and inherited methods)
  //  - stopping rules, judged on the oracle: test code, code a printed diff or dependency shows, changes giving users
  //    nothing (changesBehavior, changesValue, movesAsIs), values only logged (rocketmq_8685), recursion through the
  //    changed member itself (es_117133), shared state flags (netty_14818 ioState)
  //  - stages, each with its oracle todos: 1. control flow in a member (es_110118 ensureOpen, bazel_25203 setupSuccess);
  //    2. data flow in a member (bazel_25203 spawnResults); 3. data flow across members (netty_14803
  //    initializeNewStream); 4. points-to and type flow for dispatch (netty_14332 handle.recycle, netty_14076 inherited
  //    duplicate/slice); 5. lambdas and futures (rocketmq_8593 thenApply); 6. ordering by uses on unchanged lines of
  //    printed diffs (netty_14031 continueReading)
  //  - out of its reach: users in files outside the model (es_114899 EsqlSession#fieldNames, es_120617
  //    LookupFromIndexService), framework links (bazel_25259 @Option converter; check UMLAnnotation#getMemberValuePairs
  //    type literals before calling it an RM gap), contracts between methods (netty_13903 hashCode and equals)
  //  Flow across members attaches more files (GitHub fetches into the test cache); always use the cached mono mappings
  //  (monoMappings), as getMonoMappingStore rebuilds them on every call.
  // TODO: forward extensions (being used, see processUsages) reach only the files of the model (the changed files and
  //  the unchanged files declaring what the changes use); unchanged users in other files of the repository are not found
  // TODO: noisy dependencies. Every declaration a change uses becomes a dependency printed whole, so the useful ones
  //  (e.g. the callee whose contract the change relies on) are buried among others which tell a reviewer nothing:
  //  - Fluent APIs: builder steps and every overload of them used by a chain (Settings.builder(), the three
  //    Settings.Builder#put of one chain, RestClient#get/uri/build). Idea: print the overloads of one name once. (A
  //    declaration whose body only returns a field or `this` already prints on the line of its signature, and an enum
  //    constant by its name and arguments, see RMIndex#briefs.)
  //  - Declarations far longer than what they tell: constructors and methods carrying long annotations
  //    (DateExtract's @FunctionInfo/@Param descriptions, ~30 lines before the signature) and multi-paragraph
  //    javadoc. Idea: drop annotation argument values and keep the first javadoc sentence, unless the change
  //    touches them.
  //  - Test framework helpers used by changed tests (ESTestCase#assertThat, randomFrom).
  //    Idea: leave out declarations of test-support code (test source sets of other modules, test frameworks)
  //    that the changes reach only from test code, unless that code is changed itself.
  //  - Dependencies of deleted code only (the metrics constants of removed latency recording, the old revision's
  //    log field). Idea: keep a dependency relied on by deleted code only when it is a callee or a member whose
  //    removal changes behavior (an invocation, a field written), and drop the constants and loggers it read. The code
  //    of a removed class already brings none (see isInRemovedClass), nor does code moved as it is to another member
  //    (see movesUse).
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
      for (LeafExpression access : memberUsageFinder.findAnonymousClassAccesses(local, umlModel)) {
        LocationInfo accessLocation = access.getLocationInfo();
        List<Node> overlappingNodes = util.findOverlappingNodes(accessLocation.getFilePath(), srcDst, accessLocation.getStartOffset(),
                accessLocation.getEndOffset(), n -> !n.isContext() && !n.isExtension());
        accessNodes.addAll(overlappingNodes);
        editingAccessNodes.addAll(editingNodes(overlappingNodes, accessLocation, true));
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
    if (usedNodes.isEmpty() && usedTree != null) {
      usedNodes = counterpartChanges(srcDst, usedLocation.getFilePath(), usedTree);
    }
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
      if (isInRemovedClass(useNode)) {
        continue;
      }

      Node usedExtensionNode = addExtensionNode(usedTree, useNode);

      usesUsedNodes.putIfAbsent(useNode, new HashSet<>());
      usesUsedNodes.get(useNode).add(usedExtensionNode);
    }
  }

  // Unchanged code of the new revision using a changed member is where its change takes effect, as a caller of a method
  // whose body changed or the code reading a field whose value changed: the statement holding the use is an extension
  // the changes of the member are used by. So is the code reading a field the changes assign a value to, which reads
  // what they assign, and a call a class makes on itself of a method a changed method overrides, as a template method
  // calling the hook a subclass implements: it runs the changed body of an override
  // TODO: a call of an overridden method on another receiver (a field holding an implementation of an interface) runs a
  //  changed override only when the receiver holds an instance of the overriding class (see the TODO on data and
  //  control flow, stage 4)
  private void processUsages() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    Map<UMLOperation, List<Node>> changedOperations = new IdentityHashMap<>();
    Map<UMLAttribute, List<Node>> changedAttributes = new IdentityHashMap<>();
    Map<UMLAttribute, List<Node>> writtenAttributes = new IdentityHashMap<>();
    Map<UMLOperation, List<Node>> overriddenChanges = new IdentityHashMap<>();
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      List<UMLAbstractClass> declaringClasses = new ArrayList<>(List.of(umlClass));
      declaringClasses.addAll(umlClass.getAnonymousClassList());
      for (UMLAbstractClass declaring : declaringClasses) {
        for (UMLOperation operation : declaring.getOperations()) {
          List<Node> changes = memberChanges(srcDst, operation.getLocationInfo());
          if (changes.stream().noneMatch(change -> changesBehavior(change, operation))) {
            continue;
          }
          if (declaring == umlClass) {
            changedOperations.put(operation, changes);
          }
          // A new override brings the contract it fulfills instead (see processOverrides)
          LocationInfo location = operation.getLocationInfo();
          boolean added = changes.stream().anyMatch(change -> change.getNodeType().equals(NodeType.ADDITION)
                  && change.getTree().getPos() <= location.getStartOffset() && location.getEndOffset() <= change.getTree().getEndPos());
          if (!added) {
            for (UMLOperation overridden : receiverTypeResolver.findAttachedOverridden(operation, declaring, umlModel)) {
              overriddenChanges.computeIfAbsent(overridden, key -> new ArrayList<>()).addAll(changes);
            }
          }
        }
      }
      List<UMLAttribute> attributes = new ArrayList<>(umlClass.getAttributes());
      attributes.addAll(umlClass.getEnumConstants());
      for (UMLAttribute attribute : attributes) {
        LocationInfo location = attribute.getFieldDeclarationLocationInfo() != null ? attribute.getFieldDeclarationLocationInfo()
                : attribute.getLocationInfo();
        List<Node> changes = memberChanges(srcDst, location);
        if (!changes.isEmpty() && changesValue(attribute)) {
          changedAttributes.put(attribute, changes);
        }
        // A field the changes assign a value to holds it for the code reading it, whether they change the target of the
        // assignment or only the value assigned
        for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
          LocationInfo accessLocation = access.getLocationInfo();
          if (memberUsageFinder.isWrite(access)) {
            LocationInfo assignment = memberUsageFinder.assignmentOf(access);
            String member = memberKey(attribute);
            List<Node> writing = util.findOverlappingNodes(assignment.getFilePath(), srcDst, assignment.getStartOffset(),
                    assignment.getEndOffset(), n -> !n.isContext() && !n.isExtension()).stream()
                    .filter(change -> !carriesOver(change, assignment.getStartOffset(), assignment.getEndOffset())
                            && !renamesVariable(change) && !movesUse(change, accessLocation, member)).toList();
            if (!writing.isEmpty()) {
              writtenAttributes.computeIfAbsent(attribute, key -> new ArrayList<>()).addAll(writing);
            }
          }
        }
      }
    }
    if (changedOperations.isEmpty() && changedAttributes.isEmpty() && writtenAttributes.isEmpty() && overriddenChanges.isEmpty()) {
      return;
    }

    Set<UMLOperation> usedOperations = Collections.newSetFromMap(new IdentityHashMap<>());
    usedOperations.addAll(changedOperations.keySet());
    usedOperations.addAll(overriddenChanges.keySet());
    Set<UMLAttribute> usedAttributes = Collections.newSetFromMap(new IdentityHashMap<>());
    usedAttributes.addAll(changedAttributes.keySet());
    usedAttributes.addAll(writtenAttributes.keySet());
    memberUsageFinder.bindUses(umlModel, usedOperations, usedAttributes);
    for (Entry<UMLOperation, List<Node>> changedOperation : changedOperations.entrySet()) {
      for (AbstractCall invocation : memberUsageFinder.findUnchangedInvocations(changedOperation.getKey(), umlModel)) {
        addUsage(srcDst, invocation.getLocationInfo(), changedOperation.getKey().getLocationInfo(), changedOperation.getValue(), false);
      }
    }
    for (Entry<UMLOperation, List<Node>> overridden : overriddenChanges.entrySet()) {
      LocationInfo declaring = declaringClassLocation(overridden.getKey(), umlModel);
      for (AbstractCall invocation : memberUsageFinder.findUnchangedInvocations(overridden.getKey(), umlModel)) {
        String receiver = invocation.getExpression();
        String thisDot = PathFileUtils.getLang(invocation.getLocationInfo().getFilePath()).THIS_DOT;
        if (declaring != null && declaring.subsumes(invocation.getLocationInfo()) && (receiver == null || thisDot.equals(receiver + "."))) {
          addUsage(srcDst, invocation.getLocationInfo(), overridden.getKey().getLocationInfo(), overridden.getValue(), false);
        }
      }
    }
    for (Entry<UMLAttribute, List<Node>> changedAttribute : changedAttributes.entrySet()) {
      for (LeafExpression access : memberUsageFinder.findUnchangedAccesses(changedAttribute.getKey(), umlModel)) {
        addUsage(srcDst, access.getLocationInfo(), changedAttribute.getKey().getLocationInfo(), changedAttribute.getValue(), true);
      }
    }
    for (Entry<UMLAttribute, List<Node>> writtenAttribute : writtenAttributes.entrySet()) {
      for (LeafExpression access : memberUsageFinder.findUnchangedAccesses(writtenAttribute.getKey(), umlModel)) {
        if (!memberUsageFinder.isWrite(access)) {
          addUsage(srcDst, access.getLocationInfo(), writtenAttribute.getKey().getLocationInfo(), writtenAttribute.getValue(), true);
        }
      }
    }
  }

  @Nullable
  private static LocationInfo declaringClassLocation(UMLOperation operation, UMLModel umlModel) {
    for (UMLClass umlClass : umlModel.getClassList()) {
      if (umlClass.getOperations().contains(operation)) {
        return umlClass.getLocationInfo();
      }
    }
    return null;
  }

  // Unchanged code a change uses (a dependency) which holds a usage of other changes bridges them: the change relies on
  // those changes through it, as a caller relies on the state a changed method sets that the callee it calls reads. The
  // dependency uses the changes its usages use. A dependency which is a usage itself, as a field initialized by calling
  // a changed constructor, has these edges already
  // TODO: a bridge of several unchanged members (a change calling unchanged code that calls other unchanged code using
  //  a change) is not found, as unchanged code is no use of other unchanged code (see the TODO on data and control flow)
  private void processBridges() {
    List<Node> extensions = graph.vertexSet().stream().filter(node -> node.isExtension() && node.isDst()).toList();
    for (Node dependency : extensions) {
      if (graph.outgoingEdgesOf(dependency).stream().noneMatch(edge -> edge.getType().equals(EdgeType.DEF_USE)
              && !graph.getEdgeTarget(edge).isExtension())) {
        continue;
      }
      // A local is printed as the code declaring it
      Tree dependencyTree = dependency.getDeclaration().getTree();
      for (Node usage : extensions) {
        Tree usageTree = usage.getTree();
        if (usage == dependency || !usage.getPath().equals(dependency.getPath())
                || usageTree.getPos() < dependencyTree.getPos() || dependencyTree.getEndPos() < usageTree.getEndPos()) {
          continue;
        }
        for (Edge edge : new ArrayList<>(graph.incomingEdgesOf(usage))) {
          Node used = graph.getEdgeSource(edge);
          if (edge.getType().equals(EdgeType.DEF_USE) && !used.isExtension()) {
            addDefUse(used, dependency);
          }
        }
      }
    }
  }

  // A semantic context (a changed declaration) is relied on as the changes it holds are: an edge of its own to code the
  // changes it holds already relate to repeats theirs, and would print the changed declaration again whole, as if
  // unchanged, as a dependency of that code
  private void pruneContextDefUses() {
    List<Node> contexts = graph.vertexSet().stream().filter(node -> node.getNodeType().equals(NodeType.SEMANTIC_CONTEXT)).toList();
    for (Node context : contexts) {
      List<Node> held = graph.vertexSet().stream().filter(node -> !node.isContext() && !node.isExtension() && node.isDescendantOf(context))
              .toList();
      for (Edge edge : new ArrayList<>(graph.outgoingEdgesOf(context))) {
        Node target = graph.getEdgeTarget(edge);
        if (edge.getType().equals(EdgeType.DEF_USE) && held.stream().anyMatch(change -> graph.getAllEdges(change, target).stream()
                .anyMatch(changeEdge -> changeEdge.getType().equals(EdgeType.DEF_USE)))) {
          graph.removeEdge(edge);
        }
      }
    }
  }

  // The changes of a member, in its code or, when only the other revision changes it, in the code of its counterpart;
  // none when they only move the member as it is, as a member reordered in its class, which changes nothing for its users
  private List<Node> memberChanges(SrcDst srcDst, LocationInfo location) {
    List<Node> changes = util.findOverlappingNodes(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset(),
            n -> !n.isContext() && !n.isExtension() && n.getTree().getPos() < location.getEndOffset()
                    && location.getStartOffset() < n.getTree().getEndPos());
    if (!changes.isEmpty()) {
      return changes.stream().allMatch(change -> change.getTree().getPos() <= location.getStartOffset()
              && location.getEndOffset() <= change.getTree().getEndPos() && movesAsIs(change)) ? List.of() : changes;
    }
    // A member moving as it is has changes in this revision too
    Tree declaration = declarationTree(srcDst, location);
    return declaration == null ? List.of() : counterpartChanges(srcDst, location.getFilePath(), declaration);
  }

  // Whether the change of an operation changes what its callers get: code of its signature or its body, not its doc
  // comment, annotations (its own or those of its parameters), modifiers (a visibility its callers already comply with)
  // or comments, nor code that computes the same as before: a variable renamed, inlined or extracted, or a value no
  // longer kept in a removed field
  private boolean changesBehavior(Node change, UMLOperation operation) {
    if (renamesVariable(change) || restatesVariable(change) || writesRemovedField(change)) {
      return false;
    }
    // A change of the other revision lies in the counterpart of the operation
    UMLOperation changed = change.getSrcDst().equals(SrcDst.DST) ? operation : change.getRM().operation();
    if (changed == null) {
      return true;
    }
    List<LocationInfo> passive = new ArrayList<>();
    if (changed.getJavadoc() != null) {
      passive.add(changed.getJavadoc().getLocationInfo());
    }
    changed.getAnnotations().forEach(annotation -> passive.add(annotation.getLocationInfo()));
    changed.getParameterDeclarationList().forEach(parameter ->
            parameter.getAnnotations().forEach(annotation -> passive.add(annotation.getLocationInfo())));
    changed.getModifiers().forEach(modifier -> passive.add(modifier.getLocationInfo()));
    changed.getComments().forEach(comment -> passive.add(comment.getLocationInfo()));
    Tree tree = change.getTree();
    return passive.stream().noneMatch(location -> location.getStartOffset() <= tree.getPos() && tree.getEndPos() <= location.getEndOffset());
  }

  // Whether the change removes a statement only assigning a field RM gives as removed a value computed without calls or
  // creations: no code reads that field any more, and computing the value had no effect of its own
  private boolean writesRemovedField(Node change) {
    if (!change.getNodeType().equals(NodeType.DELETION)) {
      return false;
    }
    AbstractStatement statement = rmIndex.statementAt(SrcDst.SRC, change.getPath(), change.getTree());
    if (statement == null || statement.getAssignments().size() != 1 || !statement.getMethodInvocations().isEmpty()
            || !statement.getCreations().isEmpty()) {
      return false;
    }
    LocationInfo assignment = statement.getAssignments().get(0).getLocationInfo();
    for (UMLAttribute attribute : rmLinks.removedAttributes()) {
      for (LeafExpression access : memberUsageFinder.findAccesses(attribute, modelDiff.getParentModel())) {
        LocationInfo written = memberUsageFinder.isWrite(access) ? memberUsageFinder.assignmentOf(access) : null;
        if (written != null && written.getFilePath().equals(change.getPath())
                && written.getStartOffset() == assignment.getStartOffset() && written.getEndOffset() == assignment.getEndOffset()) {
          return true;
        }
      }
    }
    return false;
  }

  // Whether the field (or enum constant) holds another value than it did, or is new: RM gives no field of the old
  // revision as the same, or that field has another type or initializer
  private boolean changesValue(UMLAttribute attribute) {
    UMLAttribute matched = rmLinks.matchedAttribute(attribute);
    if (matched == null) {
      return true;
    }
    if (attribute.getType() != null && matched.getType() != null && !attribute.getType().toString().equals(matched.getType().toString())) {
      return true;
    }
    AbstractExpression initializer = attribute.getVariableDeclaration().getInitializer();
    AbstractExpression matchedInitializer = matched.getVariableDeclaration().getInitializer();
    if (initializer == null || matchedInitializer == null) {
      return initializer != matchedInitializer;
    }
    return !initializer.getString().equals(matchedInitializer.getString());
  }

  // Whether the change moves its code to the other revision as it is
  private boolean movesAsIs(Node change) {
    List<Node> moves = new ArrayList<>(List.of(change));
    if (change.getSubs() != null) {
      moves.addAll(change.getSubs());
    }
    if (moves.stream().anyMatch(move -> !move.getNodeType().equals(NodeType.SRC_MOVE) && !move.getNodeType().equals(NodeType.DST_MOVE))) {
      return false;
    }

    boolean src = change.isSrc();
    for (ASTDiff diff : diffs) {
      if (!(src ? diff.getSrcPath() : diff.getDstPath()).equals(change.getPath())) {
        continue;
      }
      MappingStore mappingStore = monoMappings(diff);
      Tree counterpart = src ? mappingStore.getDstForSrc(change.getTree()) : mappingStore.getSrcForDst(change.getTree());
      if (counterpart != null) {
        return change.getTree().isIsomorphicTo(counterpart);
      }
    }
    return false;
  }

  // The statement holding the use, or the field declaration when it is no statement of a body. A field read into a local
  // is the field under another name, so the statements using the local use it too; the result of a call kept in a local
  // is a value derived from it, which the code using the local works on further
  private void addUsage(SrcDst srcDst, LocationInfo use, LocationInfo member, List<Node> changes, boolean fieldRead) {
    if (member.getFilePath().equals(use.getFilePath()) && member.subsumes(use)) {
      return;
    }
    Tree useTree = declarationTree(srcDst, use);
    if (useTree == null) {
      return;
    }
    Tree usingTree = rmIndex.innermostStatement(srcDst, use.getFilePath(), useTree);
    if (usingTree == null) {
      RMInfo rm = rmIndex.locate(srcDst, use.getFilePath(), useTree);
      usingTree = rm.simpleStatement() != null ? rm.simpleStatement() : useTree;
    }

    List<Tree> usingTrees = new ArrayList<>(List.of(usingTree));
    if (fieldRead) {
      usingTrees.addAll(localUsers(srcDst, use.getFilePath(), usingTree));
    }
    for (Tree tree : usingTrees) {
      Node usage = addNode(new Node(contentsContexts, rmIndex, use.getFilePath(), srcDst, tree, null, NodeType.EXTENSION));
      // The statement holding the use reads by the code of it holding the use; a statement using the local the use is
      // kept in reads whole
      if (tree == usingTree) {
        usage.addUse(useTree);
      }
      for (Node change : changes) {
        addDefUse(change, usage);
      }
    }
  }

  // The value a statement keeps in a local it declares takes effect where the local is used: the statements RM gives as
  // using it in its scope, or the condition of a composite statement using it in its condition, as its body is not
  // where the value is used
  private List<Tree> localUsers(SrcDst srcDst, String path, Tree statementTree) {
    AbstractStatement statement = rmIndex.statementAt(srcDst, path, statementTree);
    if (statement == null || statement instanceof CompositeStatementObject) {
      return List.of();
    }

    List<Tree> users = new ArrayList<>();
    for (VariableDeclaration local : statement.getVariableDeclarations()) {
      for (AbstractCodeFragment fragment : local.getScope().getStatementsInScopeUsingVariable()) {
        List<AbstractCodeFragment> using = fragment instanceof CompositeStatementObject composite
                ? composite.getExpressions().stream().filter(expression -> expression.getVariables().stream()
                        .anyMatch(variable -> variable.getString().equals(local.getVariableName())
                        || variable.getString().startsWith(local.getVariableName() + "."))).map(expression -> (AbstractCodeFragment) expression).toList()
                : List.of(fragment);
        for (AbstractCodeFragment user : using) {
          Tree userTree = declarationTree(srcDst, user.getLocationInfo());
          if (userTree != null && userTree != statementTree) {
            users.add(userTree);
          }
        }
      }
    }
    return users;
  }

  // Whether the change lies in a class RM gives as removed, not moved, renamed or merged: what such code used tells
  // nothing of the revision without it (what still uses it does), so it brings no unchanged declaration
  private boolean isInRemovedClass(Node change) {
    if (!change.isSrc()) {
      return false;
    }

    for (UMLClass removedClass : modelDiff.getRemovedClasses()) {
      LocationInfo location = removedClass.getLocationInfo();
      if (location.getFilePath().equals(change.getPath()) && location.getStartOffset() <= change.getTree().getPos()
              && change.getTree().getEndPos() <= location.getEndOffset()) {
        return true;
      }
    }
    return false;
  }

  // The changes of the declaration in the other revision, when only that revision changes it, as a field whose private
  // modifier is removed: the declaration is changed all the same
  private List<Node> counterpartChanges(SrcDst srcDst, String path, Tree declaration) {
    boolean src = srcDst.equals(SrcDst.SRC);
    for (ASTDiff diff : diffs) {
      if (!(src ? diff.getSrcPath() : diff.getDstPath()).equals(path)) {
        continue;
      }

      MappingStore mappingStore = monoMappings(diff);
      Tree counterpart = src ? mappingStore.getDstForSrc(declaration) : mappingStore.getSrcForDst(declaration);
      // The mappings of a diff may reach trees of other files, whose content is not the one of its other path
      Tree counterpartRoot = src ? diff.dst.getRoot() : diff.src.getRoot();
      if (counterpart == null || (counterpart != counterpartRoot && !counterpart.getParents().contains(counterpartRoot))) {
        continue;
      }

      int start = counterpart.getPos();
      int end = counterpart.getEndPos();
      return util.findOverlappingNodes(src ? diff.getDstPath() : diff.getSrcPath(), src ? SrcDst.DST : SrcDst.SRC, start, end,
              n -> !n.isContext() && !n.isExtension() && n.getTree().getPos() < end && start < n.getTree().getEndPos());
    }
    return List.of();
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
  // fulfills the contract the overridden method declares. An old override relies on nothing the change needs. A new
  // method overriding unchanged ones relies on the nearest of them, whose contract (an interface method and its javadoc,
  // a hook and its default) is what a reviewer judges it against.
  // TODO: an unchanged overridden method is no extension of the changes of an existing overriding one yet
  private void processOverrides() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    // Finding what a method overrides attaches the files declaring it to the model
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      for (UMLOperation operation : umlClass.getOperations()) {
        List<Node> overridingChanges = declarationChanges(operation, srcDst);
        if (overridingChanges.isEmpty()) {
          continue;
        }

        List<UMLOperation> allOverridden = receiverTypeResolver.findAttachedOverridden(operation, umlClass, umlModel);
        List<Node> newDeclarations = overridingChanges.stream().filter(change -> change.getNodeType().equals(NodeType.ADDITION)
                && change.getRM().declaredOperation() == operation).toList();
        for (UMLOperation overridden : nearestOverridden(allOverridden, umlModel)) {
          if (declarationChanges(overridden, srcDst).isEmpty()) {
            Tree overriddenTree = declarationTree(srcDst, overridden.getLocationInfo());
            for (Node newDeclaration : overriddenTree == null ? List.<Node>of() : newDeclarations) {
              addDefUse(addExtensionNode(overriddenTree, newDeclaration), newDeclaration);
            }
          }
        }

        for (UMLOperation overridden : allOverridden) {
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

  // The overridden methods no other of them overrides, as the method of a class rather than the interface method the
  // class implements
  private List<UMLOperation> nearestOverridden(List<UMLOperation> overridden, UMLModel umlModel) {
    List<UMLOperation> nearest = new ArrayList<>(overridden);
    for (UMLOperation operation : overridden) {
      for (UMLClass declaring : umlModel.getClassList()) {
        if (declaring.getOperations().contains(operation)) {
          nearest.removeAll(receiverTypeResolver.findOverridden(operation, declaring, umlModel));
        }
      }
    }
    return nearest;
  }

  @Nullable
  private Tree declarationTree(SrcDst srcDst, LocationInfo location) {
    // A file the model holds without its code, as one only its declarations are read from, declares no tree
    if (!(srcDst.equals(SrcDst.SRC) ? contentsContexts.getSrcContexts() : contentsContexts.getDstContexts()).containsKey(location.getFilePath())) {
      return null;
    }
    Tree root = contentsContexts.getRoot(srcDst, location.getFilePath());
    return TreeUtilFunctions.findByLocationInfo(root, location, new Constants(location.getFilePath()));
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
              invocationLocationInfo.getStartOffset(), invocationLocationInfo.getEndOffset()), invocationLocationInfo, editing,
              memberKey(operation)));
    }

    return result;
  }

  private List<AbstractCall> findInvocations(UMLOperation operation, SrcDst srcDst) {
    return memberUsageFinder.findInvocations(operation, srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel());
  }

  private Set<Node> getAccessNodes(UMLAttribute attribute, SrcDst srcDst, boolean editing) {
    Set<Node> result = new HashSet<>();
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
      LocationInfo accessLocation = access.getLocationInfo();
      result.addAll(editingNodes(util.findOverlappingNodes(accessLocation.getFilePath(), srcDst, accessLocation.getStartOffset(),
              accessLocation.getEndOffset(), n -> !n.isContext() && !n.isExtension()), accessLocation, editing, memberKey(attribute)));
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
