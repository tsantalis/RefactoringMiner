package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.actions.classifier.ExtendedTreeClassifier;
import org.refactoringminer.astDiff.graph.cluster.traverse.TraversalPattern;
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
//  - profile (JFR, one RepresentationTest run, 2026-10-31, shares of a run still rebuilding the mono mapping stores on
//    every call): ProjectASTDiffer 11.2%; matching trees to UML* (RMIndex, RMLinks, findByLocationInfo, Context) 5.6%;
//    binding (ReceiverTypeResolver, MemberUsageFinder) 15.1% and RM model + UMLModelDiff 10.5% stay either way
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
  // The statements whose cases are their own statements, each running under the case labels before it
  private static final Set<LocationInfo.CodeElementType> SWITCHES = EnumSet.of(LocationInfo.CodeElementType.SWITCH_STATEMENT,
          LocationInfo.CodeElementType.WHEN_STATEMENT);
  private final Set<ImportTree> srcTrees = new HashSet<>();
  private final Set<ImportTree> dstTrees = new HashSet<>();
  private final Set<ASTDiff> diffs = new HashSet<>();
  private final Set<String> pairedSrcPaths = new HashSet<>();
  private final Set<String> pairedDstPaths = new HashSet<>();
  private final Map<SrcDst, Map<String, Set<String>>> memberUses = new EnumMap<>(SrcDst.class);
  // The files the change touches, in either revision
  private final Set<String> changedPaths = new HashSet<>();
  // The mono mappings of each diff, which the store of a diff builds anew on each request
  private final Map<ASTDiff, MappingStore> monoMappings = new IdentityHashMap<>();
  private final MemberUsageFinder memberUsageFinder;
  private final ReceiverTypeResolver receiverTypeResolver;
  private final TestSupport testSupport;
  private final LogStatements logStatements;
  private final PointsTo pointsTo;
  private final FieldWrites fieldWrites;
  private final ValueRelays valueRelays;

  public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents, Map<String, String> dstContents,
                     RepositoryStateFiles repositoryStateFiles) {
    graph = GraphTypeBuilder.<Node, Edge>directed().allowingMultipleEdges(true)
            .allowingSelfLoops(true).edgeClass(Edge.class).weighted(true).buildGraph();
    this.util = new Util(graph);
    this.modelDiff = modelDiff;
    changedPaths.addAll(srcContents.keySet());
    changedPaths.addAll(dstContents.keySet());
    Map<String, String> networkSrcContents = new LinkedHashMap<>(srcContents);
    Map<String, String> networkDstContents = new LinkedHashMap<>(dstContents);
    this.contentsContexts = new ContentsContexts(networkSrcContents, networkDstContents,
            modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap());
    this.rmIndex = new RMIndex(modelDiff, contentsContexts);
    this.rmLinks = new RMLinks(modelDiff);
    // Names in the code before the change are resolved among the files of that revision, with the ones the change deletes
    this.receiverTypeResolver = new ReceiverTypeResolver(modelDiff, repositoryStateFiles,
            repositoryStateFiles == null ? null : repositoryStateFiles.revisionBefore(srcContents, dstContents),
            RepositoryStateFiles.differingPaths(srcContents, dstContents), contentsContexts);
    this.memberUsageFinder = new MemberUsageFinder(receiverTypeResolver,
            (umlModel, location) -> overlapsChange(umlModel == modelDiff.getParentModel() ? SrcDst.SRC : SrcDst.DST, location));
    this.testSupport = new TestSupport(modelDiff, rmIndex, receiverTypeResolver, changedPaths);
    this.logStatements = new LogStatements(modelDiff, rmIndex, receiverTypeResolver);
    this.pointsTo = new PointsTo(rmIndex, receiverTypeResolver, memberUsageFinder);
    this.fieldWrites = new FieldWrites(rmIndex, receiverTypeResolver, memberUsageFinder, pointsTo);
    this.valueRelays = new ValueRelays(modelDiff, rmIndex, rmLinks, receiverTypeResolver);
    Map<AbstractCall, Boolean> throwingCalls = new IdentityHashMap<>();
    rmIndex.setThrowingCall((srcDst, call) -> throwingCalls.computeIfAbsent(call, key -> callsThrowing(srcDst, key)));
    rmIndex.setLogStatement(logStatements::isLogStatement);
  }

  // Whether the call binds to a method of the model that throws on some path (see ControlFlow#throwsOnSomePath), as a
  // check does. The call is bound without attaching the file of its target: a method no file of the model declares is
  // not looked into
  private boolean callsThrowing(SrcDst srcDst, AbstractCall call) {
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    UMLAbstractClass context = rmIndex.ownerOf(srcDst, call.getLocationInfo());
    if (context == null) {
      return false;
    }
    UMLOperation target = receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
    return target != null && receiverTypeResolver.isInModelDiff(target.getLocationInfo().getFilePath())
            && ControlFlow.throwsOnSomePath(target);
  }

  public void importFiles() {
    for (Entry<String, TreeContext> srcContext : contentsContexts.getSrcContexts().entrySet()) {
      String path = srcContext.getKey();
      if (pairedSrcPaths.contains(path)) {
        continue;
      }

      srcTrees.addAll(getValidTrees(path, SrcDst.SRC, srcContext.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DELETION, path, SrcDst.SRC)).toList());
    }
    for (Entry<String, TreeContext> dstContext : contentsContexts.getDstContexts().entrySet()) {
      String path = dstContext.getKey();
      if (pairedDstPaths.contains(path)) {
        continue;
      }

      dstTrees.addAll(getValidTrees(path, SrcDst.DST, dstContext.getValue().getRoot().getChildren()).stream()
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

    srcTrees.addAll(getValidTrees(srcPath, SrcDst.SRC, classifier.getMovedSrcs()).stream()
            .map(tree -> new ImportTree(tree, NodeType.SRC_MOVE, srcPath, SrcDst.SRC)).toList());
    srcTrees.addAll(getValidTrees(srcPath, SrcDst.SRC, multiMovedTrees(diff, SrcDst.SRC)).stream()
            .map(tree -> new ImportTree(tree, NodeType.SRC_MOVE, srcPath, SrcDst.SRC)).toList());
    if (complete) {
      srcTrees.addAll(getValidTrees(srcPath, SrcDst.SRC, classifier.getDeletedSrcs()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DELETION, srcPath, SrcDst.SRC)).toList());
      srcTrees.addAll(getValidTrees(srcPath, SrcDst.SRC, classifier.getUpdatedSrcs()).stream()
              .map(tree -> new ImportTree(tree, NodeType.SRC_UPDATE, srcPath, SrcDst.SRC)).toList());
    }

    dstTrees.addAll(getValidTrees(dstPath, SrcDst.DST, classifier.getMovedDsts()).stream()
            .map(tree -> new ImportTree(tree, NodeType.DST_MOVE, dstPath, SrcDst.DST)).toList());
    dstTrees.addAll(getValidTrees(dstPath, SrcDst.DST, multiMovedTrees(diff, SrcDst.DST)).stream()
            .map(tree -> new ImportTree(tree, NodeType.DST_MOVE, dstPath, SrcDst.DST)).toList());
    if (complete) {
      dstTrees.addAll(getValidTrees(dstPath, SrcDst.DST, classifier.getInsertedDsts()).stream()
              .map(tree -> new ImportTree(tree, NodeType.ADDITION, dstPath, SrcDst.DST)).toList());
      dstTrees.addAll(getValidTrees(dstPath, SrcDst.DST, classifier.getUpdatedDsts()).stream()
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

  // The trees of code the network holds: all but those within the package declaration or an import of the file, as RM
  // gives them with its classes, which serve only as links to what they name. RM gap: the empty statements and the
  // TypeScript exports are not represented, so they are code like any other here (see the TODO above the class)
  private Set<Tree> getValidTrees(String path, SrcDst srcDst, Collection<Tree> trees) {
    List<LocationInfo> fileParts = rmIndex.fileParts(srcDst, path);
    return trees.stream().filter(tree -> fileParts.stream()
                    .noneMatch(part -> part.getStartOffset() <= tree.getPos() && tree.getEndPos() <= part.getEndOffset()))
            .collect(Collectors.toSet());
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
    processFieldDefinitions();
    processDispatchDependencies();
    processPassedValues();
    processRemovedOverrides();
    processTypeHeaders();
    processUsages();
    processBridges();
    processShownUsages();
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
  // only renaming a variable in it, only changing the receiver the use is made on or a call nested in its arguments, nor
  // being a comment
  // A use in a statement only logging relies on nothing for the behavior of the change (see LogStatements#isLogStatement)
  private List<Node> editingNodes(List<Node> changes, LocationInfo use, boolean editing, @Nullable String member) {
    if (!editing) {
      return changes;
    }
    if (!changes.isEmpty() && logStatements.isLogStatement(changes.get(0).getSrcDst(), rmIndex.statementHolding(changes.get(0).getSrcDst(), use))) {
      return List.of();
    }
    return changes.stream().filter(change -> !carriesOver(change, use.getStartOffset(), use.getEndOffset())
            && !renamesVariable(change) && !isComment(change)
            && (member == null || !movesUse(change, use, member) && !changesReceiverOnly(change, use, member)
            && !changesNestedCallOnly(change, use, member))).toList();
  }

  // Whether the change is a comment RM gives, which no code relies on: a note added among the arguments of a call edits
  // no use of it
  private boolean isComment(Node change) {
    Tree tree = change.getTree();
    return rmIndex.comments(change.getSrcDst(), change.getPath()).stream()
            .anyMatch(comment -> comment[0] <= tree.getPos() && tree.getEndPos() - 1 <= comment[1]);
  }

  // Whether the change lies within a call nested in an argument of the use, which binds the member as it did: that call
  // receives the change and the use only gets its result, as the constructor of a request gets the version a stricter
  // valueOf now parses. The use relies on nothing anew; the nested call relies on what it calls. A change replacing the
  // whole argument hands the use's parameter another value, which still relies on the parameter
  private boolean changesNestedCallOnly(Node change, LocationInfo use, String member) {
    AbstractStatement statement = rmIndex.statementHolding(change.getSrcDst(), use);
    if (statement == null) {
      return false;
    }
    List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
            ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
    Tree tree = change.getTree();
    boolean nested = false;
    for (AbstractCodeFragment fragment : fragments) {
      List<AbstractCall> calls = new ArrayList<>(fragment.getMethodInvocations());
      calls.addAll(fragment.getCreations());
      for (AbstractCall call : calls) {
        LocationInfo location = call.getLocationInfo();
        // A call starting where the use starts is its receiver (see changesReceiverOnly)
        if (use.subsumes(location) && location.getStartOffset() > use.getStartOffset()
                && location.getStartOffset() <= tree.getPos() && tree.getEndPos() <= location.getEndOffset()
                && (location.getStartOffset() != tree.getPos() || location.getEndOffset() != tree.getEndPos())) {
          nested = true;
        }
      }
    }
    if (!nested) {
      return false;
    }
    Tree useTree = declarationTree(change.getSrcDst(), use);
    for (Tree child = useTree == null ? null : sameRangeChild(useTree); child != null; child = sameRangeChild(useTree)) {
      useTree = child;
    }
    Counterpart counterpart = useTree == null ? null : counterpart(change, useTree);
    SrcDst counterpartSrcDst = change.isSrc() ? SrcDst.DST : SrcDst.SRC;
    return counterpart != null && member.equals(boundMember(counterpartSrcDst, counterpart.path(), counterpart.tree()));
  }

  // The member the call at the tree binds to: as the uses of the changes give it, or bound anew for code no change holds
  @Nullable
  private String boundMember(SrcDst srcDst, String path, Tree tree) {
    Set<String> members = memberUses(srcDst).getOrDefault(locationKey(path, tree.getPos(), tree.getEndPos()), Set.of());
    if (!members.isEmpty()) {
      return members.size() == 1 ? members.iterator().next() : null;
    }
    AbstractStatement statement = rmIndex.statementHolding(srcDst, path, tree);
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    if (statement == null) {
      return null;
    }
    List<AbstractCodeFragment> fragments = statement instanceof CompositeStatementObject composite
            ? new ArrayList<>(composite.getExpressions()) : List.of(statement);
    for (AbstractCodeFragment fragment : fragments) {
      List<AbstractCall> calls = new ArrayList<>(fragment.getMethodInvocations());
      calls.addAll(fragment.getCreations());
      for (AbstractCall call : calls) {
        LocationInfo location = call.getLocationInfo();
        if (location.getStartOffset() == tree.getPos() && location.getEndOffset() == tree.getEndPos()) {
          UMLAbstractClass context = rmIndex.ownerOf(srcDst, location);
          UMLOperation bound = context == null ? null : receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
          return bound == null ? null : memberKey(bound);
        }
      }
    }
    return null;
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
    // change relies on as it does on code a new parent wraps in place. Code a new jump skips brings no dependencies: it
    // is mostly not printed, so they would serve code no element shows (es_123438)
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
      // Code staying under its parent is carried over only as a whole statement the change reorders in its block: it
      // runs as it did, among other statements. Code within a moved block runs as the block does, code reordered
      // within a statement, as swapped arguments, hands its value on differently, and a statement reordered among the
      // cases of a switch runs under the case labels it now follows
      if (holderCounterpart == counterpart.getParent()) {
        AbstractStatement statement = rmIndex.statementAt(src ? SrcDst.SRC : SrcDst.DST, change.getPath(), tree);
        return tree == change.getTree() && holder != null && statement != null
                && (statement.getParent() == null || !SWITCHES.contains(statement.getParent().getLocationInfo().getCodeElementType()));
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

      if (holder == null || rmIndex.declaresClass(srcDst, change.getPath(), holder) || constants.isRoot(holder.getType().name)
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
  //  graph of both would subsume them and reach the gaps of the oracle's "data/control flow" todos. The cases of each
  //  gap, with what a reviewer misses and what reaching it takes, are the todos and notes of representations.json
  //  naming this TODO:
  //  - value flow, from RM facts only: nodes are locals, parameters, fields, returns and creation sites; edges are
  //    assignments and initializers (getAssignments, as MemberUsageFinder.isAssigned), reads (VariableScope
  //    #getStatementsInScopeUsingVariable, bound field accesses), argument -> parameter of bound calls, return -> call
  //    site, and a lambda passed to a call running later in the caller (as RMIndex.RunScope#later)
  //  - queries, all starting at changes: paths of unchanged code between changes of any length (processBridges finds
  //    one member only), and effect slices forward from a changed value and argument slices backward from a call of
  //    changed code across members (RMIndex#effectParts and #definitionParts stay within a member). The forward slice
  //    follows a name through the receivers of calls and cannot tell a setter from a reader of an object, so a builder
  //    chain after a changed step stays whole (bazel_25203)
  //  - stopping rules, judged on the oracle: test code, code a printed diff or dependency shows, and changes giving users
  //    nothing whose values are equal only through their creations (es_119580: keepsValue follows a constructor
  //    parameter through super(...) only, not the value flow through every creation of both revisions) or except for a
  //    null default (es_107947: telling that no creation passes null)
  //  - stages:
  //    1. control flow in a member: the alternatives of an edited case, a candidate rule waiting for more cases where a
  //       change edits the condition of an if returning the member's result (es_104911)
  //    2. data flow in a member: the parts of a value, the elements of a list (bazel_25203)
  //    3. data flow across members: argument -> parameter for the definitions a condition decides on, with callers
  //       outside the model (es_106413), the calls a callee makes on what the change hands it (spring_32034), what a
  //       getter the change compares returns on each path (rocketmq_9214), a test value reaching changed code through
  //       deserialization (es_107947 TEST_CFG), settings reaching changed test support through unchanged overloads
  //       (es_110603), the parts of a value a call reads (rocketmq_8593), a callback the changed code invokes rather
  //       than passes on (es_123438), an unchanged check a new caller hits (rocketmq_8290), and the calls of a method a
  //       check calls that throws on some path (ControlFlow, not followed)
  //    4. points-to and type flow for dispatch: values held in collections and maps (netty_13903, bazel_23301; see
  //       processUsages), and what a delegate holds when its class is not in the model (netty_14031)
  //    5. lambdas and futures: chains of stages, a value through the result of a function (bazel_23374)
  //    6. ordering by uses on unchanged lines of printed diffs: two members using each other's changes on unchanged
  //       lines read in one order, so one of the two uses stays unserved (netty_14818); the orders processShownUsages
  //       changed are not judged yet (rocketmq_8290, 8403, 8685, netty_14291, 14818)
  //  - values passed through files or other state, which no data flow over code follows (bazel_24714)
  //  - out of its reach: users in files outside the model (es_114899, es_120617), framework links (bazel_25259; check
  //    UMLAnnotation#getMemberValuePairs type literals before calling it an RM gap), contracts between methods
  //    (netty_13903 hashCode and equals), configuration steering an engine rather than a value the changed code reads
  //    (es_118370)
  //  Flow across members attaches more files (GitHub fetches into the test cache).
  // TODO: forward extensions (being used, see processUsages) reach only the files of the model (the changed files and
  //  the unchanged files declaring what the changes use); unchanged users in other files of the repository are not found
  //  (netty_14141, es_114899, es_110603, es_115624, es_119580, netty_14188 in representations.json). Searching the files
  //  a changed file imports, or every file read to resolve names, was measured on es_119580 and es_109236 (see the todo
  //  of es_119580): wider reach waits for stopping rules on which users a change gives something
  // TODO: noisy dependencies. Every declaration a change uses becomes a dependency printed whole, so the useful ones
  //  (e.g. the callee whose contract the change relies on) are buried among others which tell a reviewer nothing:
  //  - Fluent APIs: builder steps used by production code are dependencies (isFluentStep leaves them out of test code
  //    only), and so are the ends of a chain (Settings.builder(), Settings.Builder#build(): es_109236, es_110251,
  //    es_118370), steps returning a self type through a cast (AbstractBootstrap#option returning self(): netty_14031),
  //    and steps declared without a body (RestClient#get/uri of an interface: spring_32034). Idea: print the overloads
  //    of one name once
  //  - Trivial accessors (rocketmq_9153)
  //  - Multi-paragraph javadoc. Idea: keep the first javadoc sentence, unless the change touches it
  //  - Brief annotations hide a member telling a caller something (es_107947 `optional = true`)
  //  - Dependencies of deleted code only: a declaration the change leaves without users is not marked as such
  //    (netty_14031)
  private void processExtensions(SrcDst srcDst) {
    HashMap<Node, Set<Node>> usesUsedNodes = new HashMap<>();

    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    // Binding what the definitions of the locals use attaches files to the model (see pushDefiningNodes)
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      // An enum constant is a field of its enum, as a case label naming it shows
      List<UMLAttribute> fieldDeclarations = new ArrayList<>(umlClass.getAttributes());
      fieldDeclarations.addAll(umlClass.getEnumConstants());
      for (UMLAttribute fieldDeclaration : fieldDeclarations) {
        LocationInfo declarationLocation = fieldDeclaration.getLocationInfo();
        Set<Node> accessNodes = getAccessNodes(fieldDeclaration, srcDst, false);
        // Code of the revision before reading an unchanged field, as a constant or a logger, is code the change takes
        // away: what it read tells nothing of the revision after, while the field it wrote held a state other code read
        Set<Node> editingAccessNodes = srcDst.equals(SrcDst.SRC) ? getWritingAccessNodes(fieldDeclaration)
                : getAccessNodes(fieldDeclaration, srcDst, true, testSupport.isTestSupport(declarationLocation.getFilePath())
                        ? access -> testSupport.tellsOfTest(access.getLocationInfo(), null, null, srcDst) : access -> true);
        // A field is declared by its field declaration, which holds its type; an enum constant by itself
        pushUsedNodes(usesUsedNodes, accessNodes, editingAccessNodes, declarationLocation,
                fieldDeclaration instanceof UMLEnumConstant ? null : Tree::getParent, false, srcDst);
      }

      for (UMLOperation operation : umlClass.getOperations()) {
        LocationInfo operationLocation = operation.getLocationInfo();
        Set<Node> invocationNodes = getInvocationNodes(operation, srcDst, false);
        // The return type of test support is looked into only for a call keeping its result, as resolving it may read the
        // repository's file names. Test support only filling in defaults for another of its methods (a pure delegate, see
        // delegatedTo) gives the test what that method gives: the call shows what the test passes, and the delegate's body
        // only the defaults it adds (a null filter), which tell a reviewer nothing of the object the test keeps
        boolean ofTestSupport = testSupport.isTestSupport(operationLocation.getFilePath());
        Set<Node> editingInvocationNodes = getInvocationNodes(operation, srcDst, true, ofTestSupport
                ? call -> testSupport.tellsOfTest(call.getLocationInfo(), call, operation, srcDst)
                || rmIndex.keptInLocal(srcDst, call.getLocationInfo().getFilePath(), call.getLocationInfo()) && testSupport.givesCodeUnderTest(operation, umlClass)
                && delegatedTo(operation, srcDst, umlModel) == null
                : call -> testSupport.tellsOfTest(call.getLocationInfo(), call, operation, srcDst) || !testSupport.isFluentStep(operation, umlClass, umlModel));
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
      pushDefiningNodes(usesUsedNodes, local, fragments, umlModel, srcDst);
    }
  }

  // A change of the new revision reading a local anew relies on the value it reads, which the definitions reaching it
  // give (see DataFlow#definitionsReaching): the member a definition takes the value from is used by the change too,
  // the method whose result it is and the field or the local it calls that method on, as the semaphore a lock tries.
  // A change only restating the local (an extracted or inlined variable) reads nothing anew; what the method or its
  // arguments use in turn is not followed (stage 3)
  private void pushDefiningNodes(HashMap<Node, Set<Node>> usesUsedNodes, VariableDeclaration local, Set<AbstractCodeFragment> fragments,
                                 UMLModel umlModel, SrcDst srcDst) {
    if (srcDst.equals(SrcDst.SRC)) {
      return;
    }
    // The reads by each statement: its own, or those of an expression lambda it holds, which RM gives as code of its own
    Map<AbstractStatement, List<LocationInfo>> readsByReader = new IdentityHashMap<>();
    List<AbstractStatement> readers = new ArrayList<>();
    Set<LocationInfo> seenReads = new HashSet<>();
    for (AbstractCodeFragment fragment : fragments) {
      AbstractStatement holder = fragment instanceof AbstractStatement statement ? statement
              : rmIndex.statementHolding(srcDst, fragment.getLocationInfo());
      if (holder == null) {
        continue;
      }
      for (LocationInfo read : DataFlow.readsAt(fragment, local)) {
        if (seenReads.add(read)) {
          readsByReader.computeIfAbsent(holder, key -> {
            readers.add(key);
            return new ArrayList<>();
          }).add(read);
        }
      }
    }
    for (AbstractStatement reader : readers) {
      // A statement the other revision holds as it is, moved or in place, reads nothing anew
      Tree readerTree = declarationTree(srcDst, reader.getLocationInfo());
      Set<Node> reading = new HashSet<>();
      // A value of test support a condition reads decides which code of the test runs (see TestSupport#tellsOfTest)
      boolean readInCondition = false;
      for (LocationInfo read : readsByReader.get(reader)) {
        List<Node> readingChanges = readingAnew(srcDst, read, readerTree);
        reading.addAll(readingChanges);
        readInCondition |= !readingChanges.isEmpty() && rmIndex.isInCondition(srcDst, read.getFilePath(), read);
      }
      if (reading.isEmpty()) {
        continue;
      }
      for (AbstractStatement definition : rmIndex.definitionsReaching(srcDst, reader, local)) {
        UMLAbstractClass context = rmIndex.ownerOf(srcDst, definition.getLocationInfo());
        if (context == null) {
          continue;
        }
        int[] value = RMCode.valueOf(definition, local);
        if (value == null) {
          continue;
        }
        List<LocationInfo> used = new ArrayList<>();
        // The methods of test support giving the test an object of the code under test, which the local keeps
        Set<LocationInfo> givingCodeUnderTest = new HashSet<>();
        // Fluent steps giving the value of test code (see TestSupport#isFluentStep)
        Set<LocationInfo> fluentSteps = new HashSet<>();
        LocationInfo readerLocation = reader.getLocationInfo();
        boolean takesCode = false;
        // Whether every call giving the value draws it at random (see TestSupport#drawsAtRandom)
        boolean drawnAtRandom = true;
        for (int[] branch : RMCode.valueBranches(definition, value)) {
          // The call whose result the value is: the outermost one ending where the value does
          AbstractCall producing = null;
          for (AbstractCall call : definition.getMethodInvocations()) {
            LocationInfo location = call.getLocationInfo();
            if (location.getEndOffset() == branch[1] && branch[0] <= location.getStartOffset()
                    && (producing == null || location.getStartOffset() < producing.getLocationInfo().getStartOffset())) {
              producing = call;
            }
          }
          if (producing == null) {
            continue;
          }
          UMLOperation target = receiverTypeResolver.resolveInvocationTarget(producing, context, umlModel);
          takesCode |= testSupport.runsTestCode(srcDst, readerLocation.getFilePath(), producing, target);
          drawnAtRandom &= target != null && testSupport.drawsAtRandom(target, srcDst, umlModel);
          // A new condition comparing the value with constants relies on the returns giving those constants, which a pure
          // delegate takes from the method it delegates to (see delegatedTo); the delegate itself is a dependency of the
          // call already
          UMLOperation delegated = target == null || !readInCondition ? null : delegatedTo(target, srcDst, umlModel);
          if (delegated != null && valueRelays.returnsConstantOf(delegated, reader, context, srcDst, umlModel)) {
            used.add(delegated.getLocationInfo());
          }
          if (target != null) {
            used.add(target.getLocationInfo());
            UMLAbstractClass declaring = TraversalPattern.isTest(readerLocation.getFilePath())
                    ? rmIndex.ownerOf(srcDst, target.getLocationInfo()) : null;
            if (declaring != null && testSupport.isTestSupport(target.getLocationInfo().getFilePath()) && testSupport.givesCodeUnderTest(target, declaring)) {
              givingCodeUnderTest.add(target.getLocationInfo());
            }
            if (declaring != null && testSupport.isFluentStep(target, declaring, umlModel)) {
              fluentSteps.add(target.getLocationInfo());
            }
          }
          // The field the method is called on, by the name the call starts with
          for (LeafExpression variable : definition.getVariables()) {
            LocationInfo location = variable.getLocationInfo();
            if (location.getStartOffset() == producing.getLocationInfo().getStartOffset() && producing.getExpression() != null
                    && producing.getExpression().equals(variable.getString())) {
              UMLAttribute field = receiverTypeResolver.resolveAccessTarget(variable, context, umlModel);
              if (field != null) {
                used.add(field.getLocationInfo());
              }
            }
          }
        }
        // Test support giving a value test code only reads on tells nothing beyond its name (see TestSupport#tellsOfTest), nor does
        // a fluent step building it; nor does one drawing at random a value a condition reads (see TestSupport#drawsAtRandom)
        boolean testCodeOnly = TraversalPattern.isTest(readerLocation.getFilePath()) && (!readInCondition || drawnAtRandom)
                && !takesCode;
        used.removeIf(member -> testCodeOnly && (testSupport.isTestSupport(member.getFilePath()) && !givingCodeUnderTest.contains(member)
                || fluentSteps.contains(member)));
        for (LocationInfo member : used) {
          // A field is declared by its field declaration, which holds its type
          boolean field = umlModel.getClassList().stream().anyMatch(umlClass -> umlClass.getAttributes().stream()
                  .anyMatch(attribute -> attribute.getLocationInfo() == member));
          pushUsedNodes(usesUsedNodes, reading, reading, member, field ? Tree::getParent : null, false, srcDst);
        }
      }
    }
  }

  // A new condition deciding on the value of a field, read by its name or through a getter returning it, relies on where
  // that value comes from (stage 3 of the TODO on data and control flow), as a condition reading a local relies on the
  // definitions of the local (see RMIndex#conditionDefinitions): a null check on a field a constructor always assigns
  // guards nothing in production code. The definitions of a field are its initializer, which its declaration shows, and
  // the unchanged assignments to it in other members (a constructor, a setter). A definition names the call or the creation it
  // assigns the result of; the body of that method lies a step further (its code took 20 to 80 lines in the oracle,
  // burying the definition), unlike the method a local takes its value from in the member a change reads it in. A
  // definition relaying the parameters of its member tells nothing of the value, which its callers give (see
  // relaysParameters); a field left with no definition of its own reads by the getter returning it. Judged on the
  // oracle against each PR (2026-11-03): the definitions kept give the value itself (a creation, a literal, a value of
  // the member's own state or locals), the ones left out relayed constructor arguments.
  // A field read only as the object a condition calls a method on (`in.getTransportVersion().onOrAfter(...)`,
  // `indexSettings.getMode().getDefaultMapping()`) decides by what that method gives for the object, not by where the
  // object comes from: its definitions (the default stream version, the MODE setting) tell a reviewer nothing of the
  // condition and only bury the method it calls. They bear on it when the changes alter what the method gives: the method
  // is changed, or it reads a field of its class the changes write, directly or through a method they call anew
  // (rocketmq_8130: the new getReserveTime reads the reserveTime the changes set on the TopicMetadata the flat file keeps,
  // so the assignment caching that object is where the value comes from; see bearsOnObject). Which object a write
  // reaches is judged by the class of the field, not by points-to.
  // A new assignment of a field under an unchanged check of its value decides on the field as well: the check tells what
  // the field must hold when the assignment replaces it, which the other definitions give (see addCheckedWrite). A local
  // a definition assigns as it is reads with its own definitions, as the value is theirs (see RMCode#assignedLocal)
  private void processFieldDefinitions() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    Map<UMLAttribute, Set<Node>> deciding = new IdentityHashMap<>();
    Map<UMLAttribute, List<LocationInfo>> decidingReads = new IdentityHashMap<>();
    // For each read, the call the condition makes on the value read, if it does nothing else with it
    Map<UMLAttribute, List<AbstractCall>> calledOn = new IdentityHashMap<>();
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      for (UMLAttribute attribute : umlClass.getAttributes()) {
        for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
          if (!memberUsageFinder.isWrite(access)) {
            addDecidingRead(deciding, decidingReads, calledOn, attribute, access.getLocationInfo(), access.getString(), srcDst);
          } else {
            addCheckedWrite(deciding, decidingReads, calledOn, attribute, access, srcDst, umlModel);
          }
        }
      }
      for (UMLOperation operation : umlClass.getOperations()) {
        UMLAttribute returned = null;
        for (AbstractCall call : memberUsageFinder.findInvocations(operation, umlModel)) {
          LocationInfo location = call.getLocationInfo();
          if (!rmIndex.isInCondition(srcDst, location.getFilePath(), location)) {
            continue;
          }
          returned = returned != null ? returned : valueRelays.returnedField(operation, umlClass, umlModel);
          if (returned == null) {
            break;
          }
          addDecidingRead(deciding, decidingReads, calledOn, returned, location, call.getString(), srcDst);
        }
      }
    }
    if (deciding.isEmpty()) {
      return;
    }

    HashMap<Node, Set<Node>> usesUsedNodes = new HashMap<>();
    Map<UMLAttribute, List<MemberUsageFinder.Write>> writes = memberUsageFinder.findWrites(umlModel, deciding.keySet());
    for (Entry<UMLAttribute, Set<Node>> decided : deciding.entrySet()) {
      UMLAttribute field = decided.getKey();
      Set<Node> readers = decided.getValue();
      if (calledOn.get(field).stream().allMatch(call -> call != null && !bearsOnObject(field, call, umlModel, srcDst))) {
        continue;
      }
      List<LocationInfo> definitions = new ArrayList<>();
      for (MemberUsageFinder.Write write : writes.getOrDefault(field, List.of())) {
        LocationInfo assignment = write.assignment();
        Tree assignmentTree = declarationTree(srcDst, assignment);
        AbstractStatement definition = assignmentTree == null ? null
                : rmIndex.statementHolding(srcDst, assignment.getFilePath(), assignmentTree);
        VariableDeclarationContainer member = RMCode.memberOf(write.context(), assignment);
        // A changed definition is a diff of its own, which shows where the value comes from
        if (definition == null || member == null || overlapsChange(srcDst, assignment)
                || valueRelays.relaysParameters(write, member, umlModel)
                || decidingReads.get(field).stream().anyMatch(read -> read.getFilePath().equals(assignment.getFilePath())
                && member.getLocationInfo().subsumes(read))) {
          continue;
        }
        definitions.add(definition.getLocationInfo());
        // A local assigned as it is gives the value its definitions give: the assignment alone names a value whose
        // origin it does not show (initializeNewStream assigning the id of the stream it will create, netty_14803)
        VariableDeclaration local = RMCode.assignedLocal(write, member);
        if (local != null) {
          rmIndex.definitionsReaching(srcDst, definition, local).forEach(reaching -> definitions.add(reaching.getLocationInfo()));
        }
      }
      // The field is declared by its field declaration, which holds its type, whether it is final and its initializer;
      // with no definition giving a value of its own, it tells nothing a getter returning it does not
      if (field.getVariableDeclaration().getInitializer() == null && definitions.isEmpty()) {
        continue;
      }
      pushUsedNodes(usesUsedNodes, readers, readers, field.getLocationInfo(), Tree::getParent, false, srcDst);
      for (LocationInfo definition : definitions) {
        pushUsedNodes(usesUsedNodes, readers, readers, definition, null, false, srcDst);
      }
    }
    for (Entry<Node, Set<Node>> useUsedNodes : usesUsedNodes.entrySet()) {
      for (Node usedNode : useUsedNodes.getValue()) {
        addDefUse(usedNode, useUsedNodes.getKey());
      }
    }
  }

  // Records the changes a new condition reading the field at the location (the code given) decides by its value with,
  // and the call the condition makes on the value read when that is all it does with it (null otherwise)
  private void addDecidingRead(Map<UMLAttribute, Set<Node>> deciding, Map<UMLAttribute, List<LocationInfo>> decidingReads,
                               Map<UMLAttribute, List<AbstractCall>> calledOn, UMLAttribute field, LocationInfo read, String code,
                               SrcDst srcDst) {
    String path = read.getFilePath();
    Tree readTree = declarationTree(srcDst, read);
    if (readTree == null || !rmIndex.isInCondition(srcDst, path, read)) {
      return;
    }
    AbstractStatement reader = rmIndex.statementHolding(srcDst, path, readTree);
    List<Node> readingChanges = readingAnew(srcDst, read, reader == null ? null : declarationTree(srcDst, reader.getLocationInfo()));
    if (!readingChanges.isEmpty()) {
      deciding.computeIfAbsent(field, key -> new LinkedHashSet<>()).addAll(readingChanges);
      decidingReads.computeIfAbsent(field, key -> new ArrayList<>()).add(read);
      calledOn.computeIfAbsent(field, key -> new ArrayList<>()).add(reader == null ? null : RMCode.callOn(reader, read, code));
    }
  }

  // Records the changes assigning the field at the write when an unchanged check of the value the field holds runs before
  // the assignment (stage 3 of the TODO on data and control flow): an assert before it, the condition of the code holding
  // it, or an earlier check leaving before it (see ControlFlow#checkExitsOf). Such a check states what the field holds
  // when the new assignment replaces it, as `assert id == -1 || stream.id() == id;` above the new `this.id = stream.id();`
  // of netty_14803: whether the assignment keeps the check true, or overwrites a value other code relies on, depends on
  // the values the other members assign the field (initializeNewStream giving a frame stream the id of the stream it
  // will create before onStreamAdded sets that stream). Without them a reviewer sees a check on a value whose origin no
  // element shows, and must take the assignment on trust or search the class for the other writes. The changes then rely
  // on those definitions as a new condition deciding on the field does
  private void addCheckedWrite(Map<UMLAttribute, Set<Node>> deciding, Map<UMLAttribute, List<LocationInfo>> decidingReads,
                               Map<UMLAttribute, List<AbstractCall>> calledOn, UMLAttribute field, LeafExpression write,
                               SrcDst srcDst, UMLModel umlModel) {
    LocationInfo assignment = memberUsageFinder.assignmentOf(write);
    if (assignment == null) {
      return;
    }
    String path = assignment.getFilePath();
    List<Node> writing = util.findOverlappingNodes(path, srcDst, assignment.getStartOffset(), assignment.getEndOffset(),
                    n -> !n.isContext() && !n.isExtension()).stream()
            .filter(change -> !carriesOver(change, assignment.getStartOffset(), assignment.getEndOffset()) && !renamesVariable(change))
            .toList();
    AbstractStatement statement = writing.isEmpty() ? null : rmIndex.statementHolding(srcDst, assignment);
    UMLAbstractClass context = statement == null ? null : rmIndex.ownerOf(srcDst, assignment);
    if (context == null) {
      return;
    }
    // The names of the field the checks read, bound as RM gives them; a check the changes edit is a change of its own
    for (AbstractStatement check : checksBefore(statement, srcDst)) {
      for (AbstractCodeFragment code : RMCode.ownCode(check)) {
        for (LeafExpression read : code.getVariables()) {
          LocationInfo location = read.getLocationInfo();
          if (!RMCode.namesField(read, code, field.getName()) || overlapsChange(srcDst, location)
                  || receiverTypeResolver.resolveAccessTarget(read, context, umlModel) != field) {
            continue;
          }
          deciding.computeIfAbsent(field, key -> new LinkedHashSet<>()).addAll(writing);
          decidingReads.computeIfAbsent(field, key -> new ArrayList<>()).add(location);
          calledOn.computeIfAbsent(field, key -> new ArrayList<>()).add(RMCode.callOn(check, location, read.getString()));
        }
      }
    }
  }

  // The checks the statement runs after, within the body of its member or lambda: the asserts and the checks leaving
  // (see ControlFlow#checkExitsOf) before it in the bodies holding it, and the conditions of the statements holding it
  private List<AbstractStatement> checksBefore(AbstractStatement statement, SrcDst srcDst) {
    List<AbstractStatement> checks = new ArrayList<>();
    AbstractStatement current = statement;
    while (current.getParent() != null) {
      CompositeStatementObject parent = current.getParent();
      if (ControlFlow.decidesOnCondition(parent)) {
        checks.add(parent);
      }
      if (parent.isBlock()) {
        for (AbstractStatement earlier : parent.getStatements()) {
          if (earlier == current) {
            break;
          }
          if (earlier.getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.ASSERT_STATEMENT
                  || !ControlFlow.checkExitsOf(earlier, call -> callsThrowing(srcDst, call)).isEmpty()) {
            checks.add(earlier);
          }
        }
      }
      current = parent;
    }
    return checks;
  }

  // Whether the definitions of the field bear on what the call made on its value gives: the object they give decides which
  // implementation runs (an interface method, an overridden one, which the classes the definitions create tell: the
  // lookup NestedLookup.build makes from the nested mappers, es_110251), or the changes alter what the call gives for the
  // object: the method it binds is changed, or reads a field of its class that the changes write, by a changed assignment
  // or in a method they call anew. A method of a library, which the model does not tell, may give anything, so it bears
  // on it too
  private boolean bearsOnObject(UMLAttribute field, AbstractCall call, UMLModel umlModel, SrcDst srcDst) {
    LocationInfo location = call.getLocationInfo();
    UMLAbstractClass context = rmIndex.ownerOf(srcDst, location);
    UMLOperation target = context == null ? null : receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
    if (target == null) {
      return true;
    }
    if (!memberChanges(srcDst, target.getLocationInfo()).isEmpty()) {
      return true;
    }
    for (UMLAbstractClass runtime : pointsTo.fieldClasses(field, umlModel)) {
      UMLOperation runs = receiverTypeResolver.findAttachedDispatchTarget(target, runtime, umlModel);
      if (runs != null && runs != target) {
        return true;
      }
    }
    UMLClass declaring = umlModel.getClassList().stream().filter(umlClass -> umlClass.getOperations().contains(target))
            .findFirst().orElse(null);
    // A class the model does not hold is one the changes neither write nor call into (binding them attaches it)
    if (declaring == null || target.getBody() == null) {
      return false;
    }
    Set<UMLAttribute> read = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AbstractStatement statement : target.getBody().getCompositeStatement().getAllStatements()) {
      for (LeafExpression variable : statement.getVariables()) {
        UMLAttribute readField = receiverTypeResolver.resolveAccessTarget(variable, declaring, umlModel);
        if (readField != null && declaring.getAttributes().contains(readField)) {
          read.add(readField);
        }
      }
    }
    for (List<MemberUsageFinder.Write> writes : memberUsageFinder.findWrites(umlModel, read).values()) {
      for (MemberUsageFinder.Write write : writes) {
        if (overlapsChange(srcDst, write.assignment())) {
          return true;
        }
        VariableDeclarationContainer member = RMCode.memberOf(write.context(), write.assignment());
        if (member instanceof UMLOperation writer && !getInvocationNodes(writer, srcDst, true).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  // The changes reading the code at the location (a name, a call) anew: it is new code, which the other revision holds
  // no counterpart of (a renamed or a moved name has one), of a change neither restating a variable nor holding the
  // statement reading it as before, moved or in place
  private List<Node> readingAnew(SrcDst srcDst, LocationInfo read, @Nullable Tree readerTree) {
    List<Node> changes = util.findOverlappingNodes(read.getFilePath(), srcDst, read.getStartOffset(), read.getEndOffset(),
            n -> !n.isContext() && !n.isExtension());
    Tree readTree = declarationTree(srcDst, read);
    if (changes.isEmpty() || readTree == null || counterpart(changes.get(0), readTree) != null) {
      return List.of();
    }
    return editingNodes(changes, read, true).stream().filter(change -> !restatesVariable(change)
            && !holdsAsBefore(change, readerTree)).toList();
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
  // A call of an overridden method on a receiver that may hold an instance of a class running a changed override may run
  // it too (see processDispatch)
  // TODO: a receiver holding values out of a collection or a map (netty_13903 frame streams) has no creation site
  //  reaching it (see the TODO on data and control flow, stage 4)
  private void processUsages() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    Map<UMLOperation, List<Node>> changedOperations = new IdentityHashMap<>();
    Map<UMLOperation, UMLAbstractClass> changedDeclaring = new IdentityHashMap<>();
    Map<UMLAttribute, List<Node>> changedAttributes = new IdentityHashMap<>();
    Map<UMLAttribute, List<Node>> writtenAttributes = new IdentityHashMap<>();
    // The flags the changes set or clear in a field holding a set of them, unless a change writes it otherwise
    Map<UMLAttribute, Set<UMLAttribute>> writtenFlags = new IdentityHashMap<>();
    Set<UMLAttribute> wholeWrites = Collections.newSetFromMap(new IdentityHashMap<>());
    Map<UMLOperation, List<Node>> overriddenChanges = new IdentityHashMap<>();
    // The changes of the overrides of each overridden method, by the class declaring the override
    Map<UMLOperation, Map<UMLAbstractClass, List<Node>>> overridingChanges = new IdentityHashMap<>();
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      List<UMLAbstractClass> declaringClasses = new ArrayList<>(List.of(umlClass));
      declaringClasses.addAll(umlClass.getAnonymousClassList());
      for (UMLAbstractClass declaring : declaringClasses) {
        for (UMLOperation operation : declaring.getOperations()) {
          List<Node> changes = memberChanges(srcDst, operation.getLocationInfo());
          if (changes.stream().noneMatch(change -> changesBehavior(change, operation)) && !removesBehavior(operation, changes)) {
            continue;
          }
          if (declaring == umlClass) {
            changedOperations.put(operation, changes);
            changedDeclaring.put(operation, declaring);
          }
          // A new override brings the contract it fulfills instead (see processOverrides)
          boolean added = isNewOperation(operation, changes);
          // A call on a receiver holding an instance of the overriding class runs it, new or not (see processDispatch)
          for (UMLOperation overridden : receiverTypeResolver.findAttachedOverridden(operation, declaring, umlModel)) {
            if (!added) {
              overriddenChanges.computeIfAbsent(overridden, key -> new ArrayList<>()).addAll(changes);
            }
            overridingChanges.computeIfAbsent(overridden, key -> new IdentityHashMap<>())
                    .computeIfAbsent(declaring, key -> new ArrayList<>()).addAll(changes);
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
        // assignment or only the value assigned; so does an atomic field the changes update (see FieldWrites#atomicUpdate)
        for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
          LocationInfo accessLocation = access.getLocationInfo();
          LocationInfo assignment = memberUsageFinder.isWrite(access) ? memberUsageFinder.assignmentOf(access)
                  : fieldWrites.atomicUpdate(access, attribute);
          if (assignment != null) {
            String member = memberKey(attribute);
            List<Node> writing = util.findOverlappingNodes(assignment.getFilePath(), srcDst, assignment.getStartOffset(),
                    assignment.getEndOffset(), n -> !n.isContext() && !n.isExtension()).stream()
                    .filter(change -> !carriesOver(change, assignment.getStartOffset(), assignment.getEndOffset())
                            && !renamesVariable(change) && !movesUse(change, accessLocation, member)).toList();
            if (!writing.isEmpty() && !valueRelays.storesCreationArgument(accessLocation, attribute, umlClass, umlModel)) {
              writtenAttributes.computeIfAbsent(attribute, key -> new ArrayList<>()).addAll(writing);
              Set<UMLAttribute> flags = fieldWrites.flagsAssigned(access, umlModel);
              if (flags == null) {
                wholeWrites.add(attribute);
              } else {
                writtenFlags.computeIfAbsent(attribute, key -> Collections.newSetFromMap(new IdentityHashMap<>())).addAll(flags);
              }
            }
          }
        }
      }
    }
    Map<UMLAttribute, List<Node>> copyingAttributes = copyingFields(changedAttributes, umlModel);
    if (changedOperations.isEmpty() && changedAttributes.isEmpty() && writtenAttributes.isEmpty() && overridingChanges.isEmpty()) {
      return;
    }

    Set<UMLOperation> usedOperations = Collections.newSetFromMap(new IdentityHashMap<>());
    usedOperations.addAll(changedOperations.keySet());
    usedOperations.addAll(overriddenChanges.keySet());
    usedOperations.addAll(overridingChanges.keySet());
    Set<UMLAttribute> usedAttributes = Collections.newSetFromMap(new IdentityHashMap<>());
    usedAttributes.addAll(changedAttributes.keySet());
    usedAttributes.addAll(writtenAttributes.keySet());
    usedAttributes.addAll(copyingAttributes.keySet());
    memberUsageFinder.bindUses(umlModel, usedOperations, usedAttributes);
    for (Entry<UMLOperation, List<Node>> changedOperation : changedOperations.entrySet()) {
      UMLOperation changed = changedOperation.getKey();
      for (AbstractCall invocation : memberUsageFinder.findUnchangedInvocations(changed, umlModel)) {
        if (recursesThrough(changed, changedDeclaring.get(changed), invocation.getLocationInfo(), umlModel)) {
          continue;
        }
        addUsage(srcDst, invocation.getLocationInfo(), changed.getLocationInfo(), changedOperation.getValue());
      }
    }
    for (Entry<UMLOperation, List<Node>> overridden : overriddenChanges.entrySet()) {
      LocationInfo declaring = declaringClassLocation(overridden.getKey(), umlModel);
      for (AbstractCall invocation : memberUsageFinder.findUnchangedInvocations(overridden.getKey(), umlModel)) {
        String receiver = invocation.getExpression();
        String thisDot = PathFileUtils.getLang(invocation.getLocationInfo().getFilePath()).THIS_DOT;
        if (declaring != null && declaring.subsumes(invocation.getLocationInfo()) && (receiver == null || thisDot.equals(receiver + "."))) {
          addUsage(srcDst, invocation.getLocationInfo(), overridden.getKey().getLocationInfo(), overridden.getValue());
        }
      }
    }
    processDispatch(overridingChanges, umlModel);
    for (Entry<UMLAttribute, List<Node>> changedAttribute : changedAttributes.entrySet()) {
      for (LeafExpression access : memberUsageFinder.findUnchangedAccesses(changedAttribute.getKey(), umlModel)) {
        addUsage(srcDst, access.getLocationInfo(), changedAttribute.getKey().getLocationInfo(), changedAttribute.getValue());
      }
    }
    // A field holding a value the changes give is read by the code reading it; code assigning it replaces that value
    List<Map<UMLAttribute, List<Node>>> holdingAttributes = List.of(writtenAttributes, copyingAttributes);
    for (Map<UMLAttribute, List<Node>> holding : holdingAttributes) {
      for (Entry<UMLAttribute, List<Node>> holdingAttribute : holding.entrySet()) {
        Set<UMLAttribute> flags = holding == writtenAttributes && !wholeWrites.contains(holdingAttribute.getKey())
                ? writtenFlags.get(holdingAttribute.getKey()) : null;
        for (LeafExpression access : memberUsageFinder.findUnchangedAccesses(holdingAttribute.getKey(), umlModel)) {
          if (!memberUsageFinder.isWrite(access) && fieldWrites.atomicUpdate(access, holdingAttribute.getKey()) == null
                  && !(flags != null && fieldWrites.testsOtherFlags(access, flags, umlModel))) {
            addUsage(srcDst, access.getLocationInfo(), holdingAttribute.getKey().getLocationInfo(), holdingAttribute.getValue());
          }
        }
      }
    }
  }

  // Whether the call lies in a method that only the changed method calls, as the planner of a node type that a
  // dispatcher calls and that calls the dispatcher back for the child of the node: the call is the recursion of the
  // changed method, which the change reaches only through that method. Its own callers are where the change takes
  // effect. A method called from elsewhere too, as the dispatcher a changed planner calls back, is an entry of its own
  private boolean recursesThrough(UMLOperation changed, UMLAbstractClass declaring, LocationInfo call, UMLModel umlModel) {
    UMLAbstractClass owner = rmIndex.ownerOf(SrcDst.DST, call);
    UMLOperation caller = null;
    for (UMLOperation operation : owner == null ? List.<UMLOperation>of() : owner.getOperations()) {
      if (operation.getLocationInfo().subsumes(call) && (caller == null || caller.getLocationInfo().subsumes(operation.getLocationInfo()))) {
        caller = operation;
      }
    }
    if (caller == null || caller == changed) {
      return false;
    }
    boolean called = false;
    for (AbstractCall callee : changed.getAllOperationInvocations()) {
      if (callee.getName().equals(caller.getName()) && receiverTypeResolver.bindInvocationTarget(callee, declaring, umlModel) == caller) {
        called = true;
        break;
      }
    }
    return called && !calledOutside(caller, changed, umlModel);
  }

  // A call of an overridden method may run a changed override when its receiver may be an instance of a class running
  // that override (stage 4 of the TODO on data and control flow, the creation sites reaching a receiver, see PointsTo#classesOf):
  // the call binds to the method the override implements, as a store calling the fetcher its constructor creates
  // through the fetcher's interface, but the classes of the receiver decide which body runs, so the call is a usage of
  // the changed override
  private void processDispatch(Map<UMLOperation, Map<UMLAbstractClass, List<Node>>> overridingChanges, UMLModel umlModel) {
    SrcDst srcDst = SrcDst.DST;
    for (Entry<UMLOperation, Map<UMLAbstractClass, List<Node>>> overridden : overridingChanges.entrySet()) {
      for (AbstractCall invocation : memberUsageFinder.findUnchangedInvocations(overridden.getKey(), umlModel)) {
        List<Node> changes = new ArrayList<>();
        for (UMLAbstractClass runtime : pointsTo.receiverClasses(invocation, umlModel)) {
          UMLOperation runs = receiverTypeResolver.findAttachedDispatchTarget(overridden.getKey(), runtime, umlModel);
          UMLAbstractClass declaring = runs == null ? null : rmIndex.ownerOf(srcDst, runs.getLocationInfo());
          List<Node> overriding = declaring == null ? null : overridden.getValue().get(declaring);
          if (overriding != null) {
            overriding.stream().filter(change -> !changes.contains(change)).forEach(changes::add);
          }
        }
        if (changes.isEmpty()) {
          continue;
        }
        // Changes of the statement holding the call work on what it gives, as a new step chained on its result: they
        // rely on the override as the changes holding a call of it do (see processOverrides)
        LocationInfo call = invocation.getLocationInfo();
        Tree callTree = declarationTree(srcDst, call);
        Tree statementTree = callTree == null ? null : rmIndex.innermostStatement(srcDst, call.getFilePath(), callTree);
        List<Node> statementChanges = statementTree == null ? List.of() : util.findOverlappingNodes(call.getFilePath(), srcDst,
                statementTree.getPos(), statementTree.getEndPos(), n -> !n.isContext() && !n.isExtension()
                        && n.getTree().getPos() < statementTree.getEndPos() && statementTree.getPos() < n.getTree().getEndPos());
        if (statementChanges.isEmpty()) {
          addUsage(srcDst, call, overridden.getKey().getLocationInfo(), changes);
          continue;
        }
        for (Node statementChange : statementChanges) {
          for (Node change : changes) {
            if (change != statementChange) {
              addDefUse(change, statementChange);
            }
          }
        }
      }
    }
  }

  // A call of the changes binding to a method with no body (an interface or an abstract method, or a method of a library
  // interface) shows no code it runs; the code it runs is the implementation each class reaching its receiver runs (see
  // classesOf), overriding or inherited, as the final release a derived buffer the changed duplicate returns inherits,
  // or the fake clock a test passes as the supplier of the time. The changes holding the call rely on those
  // implementations, besides the declaration they bind to
  private void processDispatchDependencies() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    HashMap<Node, Set<Node>> usesUsedNodes = new HashMap<>();
    List<UMLAbstractClass> classes = new ArrayList<>();
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      classes.add(umlClass);
      classes.addAll(umlClass.getAnonymousClassList());
    }
    for (UMLAbstractClass context : classes) {
      for (UMLOperation operation : context.getOperations()) {
        for (AbstractCall call : operation.getAllOperationInvocations()) {
          LocationInfo location = call.getLocationInfo();
          String receiver = call.getExpression();
          if (!overlapsChange(srcDst, location)) {
            continue;
          }
          pushPassedDispatch(usesUsedNodes, call, context, umlModel);
          if (receiver == null || receiver.equals(context.getLANG().THIS)) {
            continue;
          }
          UMLOperation target = receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
          if (target != null && (target.getBody() != null || target.isStatic() || target.isConstructor())) {
            continue;
          }
          Set<UMLAbstractClass> runtimes = pointsTo.receiverClasses(call, umlModel);
          if (runtimes.isEmpty()) {
            continue;
          }
          List<Node> changes = util.findOverlappingNodes(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset(),
                  n -> !n.isContext() && !n.isExtension());
          Set<Node> calling = new HashSet<>(editingNodes(changes, location, true));
          boolean setsUpTest = testSupport.setsUpTest(call, operation, context, srcDst);
          for (UMLAbstractClass runtime : runtimes) {
            UMLOperation runs = target != null ? receiverTypeResolver.findAttachedDispatchTarget(target, runtime, umlModel)
                    : receiverTypeResolver.findAttachedDispatchTarget(call.getName(), call.arguments().size(), runtime, umlModel);
            UMLAbstractClass runsClass = runs == null || !setsUpTest ? null : rmIndex.ownerOf(srcDst, runs.getLocationInfo());
            if (runsClass != null && testSupport.isFluentStep(runs, runsClass, umlModel) && !holdsChanges(srcDst, runsClass)) {
              continue;
            }
            if (runs != null) {
              pushUsedNodes(usesUsedNodes, calling, calling, runs.getLocationInfo(), null, false, srcDst);
            }
          }
        }
      }
    }
    for (Entry<Node, Set<Node>> useUsedNodes : usesUsedNodes.entrySet()) {
      for (Node usedNode : useUsedNodes.getValue()) {
        addDefUse(usedNode, useUsedNodes.getKey());
      }
    }
  }

  // Whether the declaration of the class holds changes of the revision
  private boolean holdsChanges(SrcDst srcDst, UMLAbstractClass umlClass) {
    LocationInfo location = umlClass.getLocationInfo();
    return !util.findOverlappingNodes(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset(),
            n -> !n.isContext() && !n.isExtension()).isEmpty();
  }

  // An override a class that stays no longer declares leaves its calls to the method the class now inherits (stage 4 of
  // the TODO on data and control flow, dispatch): an instance of the class runs that method instead, as the read-only
  // unsafe buffer now runs the changed duplicate and slice of the read-only buffer it extends. An unchanged inherited
  // method is code the removal relies on; a
  // changed one is a diff of its own, which the removal serves: it hands the calls of its class to that method, which
  // reads after it, as the call site a body moves from reads before the method it moves to (es_120617)
  private void processRemovedOverrides() {
    UMLModel parentModel = modelDiff.getParentModel();
    UMLModel childModel = modelDiff.getChildModel();
    List<UMLAbstractClassDiff> classDiffs = new ArrayList<>(modelDiff.getCommonClassDiffList());
    classDiffs.addAll(modelDiff.getClassMoveDiffList());
    classDiffs.addAll(modelDiff.getInnerClassMoveDiffList());
    classDiffs.addAll(modelDiff.getClassRenameDiffList());
    for (UMLAbstractClassDiff classDiff : classDiffs) {
      UMLAbstractClass srcClass = classDiff.getOriginalClass();
      UMLAbstractClass dstClass = classDiff.getNextClass();
      // Whether RM gives the code as removed or as moved elsewhere, the class no longer declares the method; a method RM
      // matches to one of the class (renamed in place) is still the class's own, related to what calls it
      Set<String> declared = new HashSet<>();
      dstClass.getOperations().forEach(operation -> declared.add(operation.getName() + operation.getParameterTypeList()));
      Set<VariableDeclarationContainer> matched = Collections.newSetFromMap(new IdentityHashMap<>());
      classDiff.getOperationBodyMapperList().forEach(mapper -> matched.add(mapper.getContainer1()));
      for (UMLOperation removed : srcClass.getOperations()) {
        if (declared.contains(removed.getName() + removed.getParameterTypeList()) || matched.contains(removed)
                || removed.isStatic() || removed.isConstructor() || removed.getBody() == null
                || receiverTypeResolver.findOverridden(removed, srcClass, parentModel).isEmpty()) {
          continue;
        }
        UMLOperation inherited = receiverTypeResolver.findAttachedDispatchTarget(removed.getName(),
                removed.getParameterDeclarationList().size(), dstClass, childModel);
        UMLAbstractClass inheritedClass = inherited == null ? null : rmIndex.ownerOf(SrcDst.DST, inherited.getLocationInfo());
        List<Node> removal = memberChanges(SrcDst.SRC, removed.getLocationInfo());
        if (inheritedClass == null || inheritedClass == dstClass || removal.isEmpty()) {
          continue;
        }
        List<Node> inheritedChanges = memberChanges(SrcDst.DST, inherited.getLocationInfo()).stream()
                .filter(change -> changesBehavior(change, inherited)).toList();
        Node inheritedNode = inheritedChanges.isEmpty() ? newRevisionExtension(inherited.getLocationInfo()) : null;
        // The class falls back to the method of the supertype its header names, which tells why that method takes over
        for (Node header : typeHeaderNodes(dstClass, SrcDst.DST)) {
          removal.forEach(removing -> addDefUse(header, removing));
        }
        for (Node removing : removal) {
          if (inheritedNode != null) {
            addDefUse(inheritedNode, removing);
          }
          for (Node inheritedChange : inheritedChanges) {
            addDefUse(removing, inheritedChange);
          }
        }
      }
    }
  }

  // Types the changes rely on by what their headers say, read by their headers (see typeHeaderNodes):
  // - the type a catch clause the changes edit catches, and a type the throws clause of a method declares where the
  //   changes edit it, with the superclasses of that type the repository declares: they tell whether the exception is
  //   checked, and which exceptions the catch takes, as the reviewer asking whether a TaskCancelledException is a
  //   runtime exception (es_123438), down to the class of the platform the last of them extends. A member the changes
  //   write whole declares its exceptions anew, which no existing code throws or catches differently for: the compiler
  //   holds a new method to its checked exceptions, and their headers would only repeat `extends Exception` for every
  //   type of a throws clause copied from an overload; nor does a type the method declared before, as a throws clause
  //   only rewrapped (rocketmq_9153)
  // - a supertype a changed extends or implements clause names, when the changes add that type whole: the new class
  //   serves the classes now extending it, which read after it (es_107947 AbstractConfigurationFunctionTestCase). The
  //   header of an unchanged supertype tells nothing the clause does not, but its own supertypes
  // A method a class newly declares or no longer declares relies on its header too (see processRemovedOverrides)
  private void processTypeHeaders() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      List<UMLType> supertypes = new ArrayList<>();
      if (umlClass.getSuperclass() != null) {
        supertypes.add(umlClass.getSuperclass());
      }
      supertypes.addAll(umlClass.getImplementedInterfaces());
      for (UMLType supertype : supertypes) {
        LocationInfo reference = supertype.getLocationInfo();
        if (reference == null) {
          continue;
        }
        List<Node> referencing = findReachedChanges(reference.getFilePath(), srcDst, reference.getStartOffset(), reference.getEndOffset());
        if (referencing.isEmpty()) {
          continue;
        }
        UMLAbstractClass superClass = receiverTypeResolver.resolveAttachedClass(supertype, umlClass, umlModel);
        Node superClassNode = superClass == null ? null : getClassNode(superClass, srcDst);
        if (superClassNode == null || superClassNode.isContext()) {
          continue;
        }
        for (Node change : referencing) {
          if (change != superClassNode && !change.isDescendantOf(superClassNode)) {
            addDefUse(superClassNode, change);
          }
        }
      }
    }

    Map<LocationInfo, UMLAbstractClass> exceptionTypes = new LinkedHashMap<>();
    Map<LocationInfo, UMLType> writtenTypes = new HashMap<>();
    Map<LocationInfo, LocationInfo> members = new HashMap<>();
    // The methods of the revision before RM matches the methods of this one to, whose throws clauses tell what is new
    Map<VariableDeclarationContainer, VariableDeclarationContainer> before = new IdentityHashMap<>();
    List<UMLAbstractClassDiff> classDiffs = new ArrayList<>(modelDiff.getCommonClassDiffList());
    classDiffs.addAll(modelDiff.getClassMoveDiffList());
    classDiffs.addAll(modelDiff.getInnerClassMoveDiffList());
    classDiffs.addAll(modelDiff.getClassRenameDiffList());
    classDiffs.forEach(classDiff -> classDiff.getOperationBodyMapperList()
            .forEach(mapper -> before.put(mapper.getContainer2(), mapper.getContainer1())));
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      for (UMLOperation operation : umlClass.getOperations()) {
        Set<String> declaredBefore = new HashSet<>();
        if (before.get(operation) instanceof UMLOperation previous) {
          previous.getThrownExceptionTypes().forEach(thrown -> declaredBefore.add(thrown.getClassType()));
        }
        for (UMLType thrown : operation.getThrownExceptionTypes()) {
          if (declaredBefore.contains(thrown.getClassType())) {
            continue;
          }
          exceptionTypes.put(thrown.getLocationInfo(), umlClass);
          writtenTypes.put(thrown.getLocationInfo(), thrown);
          members.put(thrown.getLocationInfo(), operation.getLocationInfo());
        }
      }
      String path = umlClass.getLocationInfo().getFilePath();
      for (CompositeStatementObject clause : rmIndex.catchClauses(srcDst, path)) {
        for (VariableDeclaration caught : clause.getVariableDeclarations()) {
          UMLType type = caught.getType();
          if (type == null || !umlClass.getLocationInfo().subsumes(clause.getLocationInfo())) {
            continue;
          }
          UMLAbstractClass context = rmIndex.ownerOf(srcDst, clause.getLocationInfo());
          LocationInfo member = umlClass.getOperations().stream().map(UMLOperation::getLocationInfo)
                  .filter(location -> location.subsumes(clause.getLocationInfo())).findFirst().orElse(umlClass.getLocationInfo());
          for (UMLType alternative : type instanceof ListCompositeType union ? union.getTypes() : List.of(type)) {
            exceptionTypes.put(alternative.getLocationInfo(), context == null ? umlClass : context);
            writtenTypes.put(alternative.getLocationInfo(), alternative);
            members.merge(alternative.getLocationInfo(), member, (known, other) -> known.subsumes(other) ? other : known);
          }
        }
      }
    }
    for (Entry<LocationInfo, UMLAbstractClass> exceptionType : exceptionTypes.entrySet()) {
      LocationInfo written = exceptionType.getKey();
      if (written == null) {
        continue;
      }
      LocationInfo member = members.get(written);
      List<Node> relying = findReachedChanges(written.getFilePath(), srcDst, written.getStartOffset(), written.getEndOffset()).stream()
              .filter(change -> member == null || member.getStartOffset() < change.getTree().getPos()
                      || change.getTree().getEndPos() < member.getEndOffset()).toList();
      if (relying.isEmpty()) {
        continue;
      }
      UMLType type = writtenTypes.get(written);
      UMLAbstractClass context = exceptionType.getValue();
      Set<UMLAbstractClass> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      for (UMLAbstractClass exceptionClass = receiverTypeResolver.resolveAttachedClass(type, context, umlModel);
           exceptionClass != null && visited.add(exceptionClass); ) {
        for (Node header : typeHeaderNodes(exceptionClass, srcDst)) {
          for (Node change : relying) {
            if (header != change && !change.isDescendantOf(header)) {
              addDefUse(header, change);
            }
          }
        }
        UMLType superclass = exceptionClass.getSuperclass();
        exceptionClass = superclass == null ? null : receiverTypeResolver.resolveAttachedClass(superclass, exceptionClass, umlModel);
      }
    }
  }

  // The header of the type, by which code relying on what the type is reads: the changes of the header where the changes
  // edit it (a new class, a changed extends clause), whose diff shows it, else a dependency of the header alone, from its
  // signature to its body, which a node of its own spans: a node of the whole type would count as showing its members
  // too, swallowing the usages and the dependencies within them (netty_14332)
  private List<Node> typeHeaderNodes(UMLAbstractClass umlClass, SrcDst srcDst) {
    Tree declaration = declarationTree(srcDst, umlClass.getLocationInfo());
    if (declaration == null) {
      return List.of();
    }
    String path = umlClass.getLocationInfo().getFilePath();
    RMInfo rm = new Node(contentsContexts, rmIndex, path, srcDst, declaration, null, NodeType.EXTENSION).getRM();
    if (rm.declaredClass() == null || rm.header().isEmpty()) {
      return List.of();
    }
    int start = rm.signatureStart().getPos();
    int end = rm.header().get(rm.header().size() - 1).getEndPos();
    List<Node> headerChanges = util.findOverlappingNodes(path, srcDst, start, end, n -> !n.isContext() && !n.isExtension()
            && n.getTree().getPos() < end && start < n.getTree().getEndPos());
    if (!headerChanges.isEmpty()) {
      return headerChanges;
    }
    com.github.gumtreediff.tree.DefaultTree header = new com.github.gumtreediff.tree.DefaultTree(declaration.getType());
    header.setPos(start);
    header.setLength(end - start);
    header.setParent(declaration);
    Node headerNode = new Node(contentsContexts, rmIndex, path, srcDst, header, null, NodeType.EXTENSION);
    headerNode.markTypeHeader();
    Node added = addNode(headerNode);
    added.markTypeHeader();
    return List.of(added);
  }

  // An unchanged declaration of the new revision at the location, as a dependency
  @Nullable
  private Node newRevisionExtension(LocationInfo location) {
    String path = location.getFilePath();
    Tree root = contentsContexts.getRoot(SrcDst.DST, path);
    Tree tree = root == null ? null : TreeUtilFunctions.findByLocationInfo(root, location, new Constants(path));
    return tree == null ? null : addNode(new Node(contentsContexts, rmIndex, path, SrcDst.DST, tree, null, NodeType.EXTENSION));
  }

  // A value the changes make, kept in a local, goes where the local is read: a method of the model the code passes the
  // local to as it is runs on it (argument -> parameter, stage 3 of the TODO on data and control flow), as the graph walk
  // reading the node of each lookup key a changed lambda now builds, which is where a key naming no node fails. The
  // changes rely on what that method does with their value, as a changed call relies on what its callee does with the
  // value it passes (see pushPassedDispatch). Only a method of the class of the code goes on with its work on the value;
  // a method of another class is a service the code calls, read by its name as the other unchanged calls are (an accessor
  // reading a part of a changed state); a changed method is a diff of its own. A test handing its value on to its own
  // helpers builds its input, which the helpers' names tell, as for the other test support (see TestSupport#tellsOfTest)
  private void processPassedValues() {
    SrcDst srcDst = SrcDst.DST;
    UMLModel umlModel = modelDiff.getChildModel();
    HashMap<Node, Set<Node>> usesUsedNodes = new HashMap<>();
    Map<AbstractStatement, Set<Node>> definingChanges = new LinkedHashMap<>();
    Map<AbstractStatement, Set<VariableDeclaration>> definedLocals = new IdentityHashMap<>();
    for (Node change : graph.vertexSet()) {
      if (change.isContext() || change.isExtension() || !change.getSrcDst().equals(srcDst)) {
        continue;
      }
      List<VariableDeclaration> locals = rmIndex.valueLocals(srcDst, change.getPath(), change.getTree());
      AbstractStatement statement = locals.isEmpty() ? null : rmIndex.statementHolding(srcDst, change.getPath(), change.getTree());
      if (statement != null) {
        definingChanges.computeIfAbsent(statement, key -> new HashSet<>()).add(change);
        definedLocals.computeIfAbsent(statement, key -> Collections.newSetFromMap(new IdentityHashMap<>())).addAll(locals);
      }
    }
    for (Entry<AbstractStatement, Set<Node>> defining : definingChanges.entrySet()) {
      AbstractStatement definition = defining.getKey();
      for (VariableDeclaration local : definedLocals.get(definition)) {
        for (AbstractStatement reader : rmIndex.readersReached(definition, local)) {
          List<AbstractCodeFragment> fragments = reader instanceof CompositeStatementObject composite
                  ? new ArrayList<>(composite.getExpressions()) : List.of(reader);
          for (AbstractCodeFragment fragment : fragments) {
            for (AbstractCall call : fragment.getMethodInvocations()) {
              if (call.arguments().stream().noneMatch(argument -> argument.trim().equals(local.getVariableName()))) {
                continue;
              }
              UMLAbstractClass context = rmIndex.ownerOf(srcDst, call.getLocationInfo());
              if (context == null || context.getOperations().stream().noneMatch(operation -> operation.getName().equals(call.getName()))) {
                continue;
              }
              UMLOperation callee = receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
              if (callee == null || callee.getBody() == null || rmIndex.ownerOf(srcDst, callee.getLocationInfo()) != context
                      || TraversalPattern.isTest(callee.getLocationInfo().getFilePath())
                      || !memberChanges(srcDst, callee.getLocationInfo()).isEmpty()) {
                continue;
              }
              pushUsedNodes(usesUsedNodes, defining.getValue(), defining.getValue(), callee.getLocationInfo(), null, false, srcDst);
            }
          }
        }
      }
    }
    for (Entry<Node, Set<Node>> useUsedNodes : usesUsedNodes.entrySet()) {
      for (Node usedNode : useUsedNodes.getValue()) {
        addDefUse(usedNode, useUsedNodes.getKey());
      }
    }
  }

  // A call of the changes passing a value to a method relies on what that method runs on it (argument -> parameter
  // forward, stage 3 of the TODO on data and control flow): the calls the method makes on its parameter bound to a method
  // with no body run the implementations of the classes of the value passed (see PointsTo#classesOf), as a test passing the fake
  // clock it creates to the driver, which reads the time from it. Only the method called is followed, not what it
  // passes the parameter on to
  private void pushPassedDispatch(HashMap<Node, Set<Node>> usesUsedNodes, AbstractCall call, UMLAbstractClass context, UMLModel umlModel) {
    SrcDst srcDst = SrcDst.DST;
    LocationInfo location = call.getLocationInfo();
    UMLOperation callee = call.arguments().isEmpty() ? null : receiverTypeResolver.bindInvocationTarget(call, context, umlModel);
    UMLAbstractClass calleeClass = callee == null || callee.getBody() == null ? null : rmIndex.ownerOf(srcDst, callee.getLocationInfo());
    AbstractStatement statement = calleeClass == null ? null : rmIndex.statementHolding(srcDst, location);
    if (statement == null) {
      return;
    }
    Set<Node> calling = null;
    List<VariableDeclaration> parameters = callee.getParameterDeclarationList();
    for (int i = 0; i < parameters.size() && i < call.arguments().size(); i++) {
      String parameter = parameters.get(i).getVariableName();
      List<AbstractCall> callsOnParameter = callee.getAllOperationInvocations().stream()
              .filter(inner -> inner.getExpression() != null && inner.getExpression().trim().equals(parameter)).toList();
      if (callsOnParameter.isEmpty()) {
        continue;
      }
      PointsTo.Value argument = pointsTo.argumentValue(call, statement, i, context);
      Set<UMLAbstractClass> runtimes = argument == null ? Set.of() : pointsTo.classesOf(argument, umlModel);
      for (AbstractCall inner : runtimes.isEmpty() ? List.<AbstractCall>of() : callsOnParameter) {
        UMLOperation bound = receiverTypeResolver.bindInvocationTarget(inner, calleeClass, umlModel);
        if (bound != null && (bound.getBody() != null || bound.isStatic())) {
          continue;
        }
        for (UMLAbstractClass runtime : runtimes) {
          UMLOperation runs = bound != null ? receiverTypeResolver.findAttachedDispatchTarget(bound, runtime, umlModel)
                  : receiverTypeResolver.findAttachedDispatchTarget(inner.getName(), inner.arguments().size(), runtime, umlModel);
          if (runs == null) {
            continue;
          }
          if (calling == null) {
            List<Node> changes = util.findOverlappingNodes(location.getFilePath(), srcDst, location.getStartOffset(), location.getEndOffset(),
                    n -> !n.isContext() && !n.isExtension());
            calling = new HashSet<>(editingNodes(changes, location, true));
          }
          pushUsedNodes(usesUsedNodes, calling, calling, runs.getLocationInfo(), null, false, srcDst);
        }
      }
    }
  }

  // The method an unchanged pure delegate stands for (return -> call site, stage 3 of the TODO on data and control flow):
  // a method whose body only returns what a method of its class gives, called on the instance with its own parameters
  // as they are and literals, as an overload filling in defaults. What its callers get is what that method returns, so
  // the code they rely on is that method's; the delegate's body tells only the defaults it fills in. Delegates
  // delegating in turn are followed to the method doing the work; none for a method that is no such delegate
  @Nullable
  private UMLOperation delegatedTo(UMLOperation operation, SrcDst srcDst, UMLModel umlModel) {
    UMLOperation current = operation;
    Set<UMLOperation> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    while (visited.add(current) && memberChanges(srcDst, current.getLocationInfo()).isEmpty()) {
      UMLAbstractClass declaring = rmIndex.ownerOf(srcDst, current.getLocationInfo());
      UMLOperation next = declaring == null ? null : valueRelays.delegateOf(current, declaring, umlModel);
      if (next == null) {
        break;
      }
      current = next;
    }
    return current == operation ? null : current;
  }

  // The fields initialized with a changed field as it is, with the changes of that field: such a field holds the changed
  // value under another name, as a local a field is read into does (see addUsage), so the code reading it reads the
  // value the changes give, as the field it starts from (an initial target that later rounds adapt). A field
  // initialized with a value computed from a changed field holds something else, as a queue sized by a changed
  // capacity, whose readers take from the queue
  private Map<UMLAttribute, List<Node>> copyingFields(Map<UMLAttribute, List<Node>> changedAttributes, UMLModel umlModel) {
    Map<UMLAttribute, List<Node>> copying = new IdentityHashMap<>();
    Deque<UMLAttribute> pending = new ArrayDeque<>(changedAttributes.keySet());
    while (!pending.isEmpty()) {
      UMLAttribute changed = pending.pop();
      List<Node> changes = changedAttributes.containsKey(changed) ? changedAttributes.get(changed) : copying.get(changed);
      for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
        for (UMLAttribute attribute : umlClass.getAttributes()) {
          AbstractExpression initializer = attribute.getVariableDeclaration().getInitializer();
          if (initializer == null || changedAttributes.containsKey(attribute) || copying.containsKey(attribute)) {
            continue;
          }
          for (LeafExpression name : initializer.getVariables()) {
            if (name.getString().equals(initializer.getString()) && RMCode.namesField(name, initializer, changed.getName())
                    && receiverTypeResolver.resolveAccessTarget(name, umlClass, umlModel) == changed) {
              copying.put(attribute, changes);
              pending.push(attribute);
              break;
            }
          }
        }
      }
    }
    return copying;
  }

  // Whether code of the model outside the member calls the operation
  private boolean calledOutside(UMLOperation operation, UMLOperation member, UMLModel umlModel) {
    LocationInfo memberLocation = member.getLocationInfo();
    for (UMLClass umlClass : new ArrayList<>(umlModel.getClassList())) {
      List<UMLAbstractClass> classes = new ArrayList<>(List.of(umlClass));
      classes.addAll(umlClass.getAnonymousClassList());
      for (UMLAbstractClass context : classes) {
        List<VariableDeclarationContainer> containers = new ArrayList<>(context.getOperations());
        containers.addAll(context.getAttributes());
        for (VariableDeclarationContainer container : containers) {
          for (AbstractCall call : container.getAllOperationInvocations()) {
            LocationInfo location = call.getLocationInfo();
            if (call.getName().equals(operation.getName())
                    && !(location.getFilePath().equals(memberLocation.getFilePath()) && memberLocation.subsumes(location))
                    && receiverTypeResolver.bindInvocationTarget(call, context, umlModel) == operation) {
              return true;
            }
          }
        }
      }
    }
    return false;
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

  // A changed member calling a changed member on one of its unchanged lines relies on that change where its diff shows the
  // call (stage 6 of the TODO on data and control flow, ordering by uses on unchanged lines of printed diffs), as the
  // datagram read loop of netty_14031 does on the changed continueReading its `while` calls. The diff shows that line
  // (the representation reveals a usage lying within the window of a diff, see RawRepresentation#revealRelated), and a
  // diff showing a usage is served by the diffs it uses only when it reads after them; with no relation between the two
  // changes the callee's diff may read after the caller's, which then shows a call whose new behaviour it never explains:
  // a reviewer reads the loop on the old meaning of the call, and only an agent going back from the callee to every diff
  // calling it on lines nothing marks catches the effect. The caller relies on the callee as a change calling it anew
  // does, so the callee reads first and serves it. The window of a diff holds every change of the member, so the usage
  // is taken to be shown when it lies within the innermost code of the new revision holding all of them (a change of the
  // old revision by the code of the new revision it lies in); a usage outside it, which the diff may leave out, reads as a
  // usage element of its own instead. A member with no change in the new revision prints no new code a call could be on
  private void processShownUsages() {
    SrcDst srcDst = SrcDst.DST;
    List<Node> usages = graph.vertexSet().stream().filter(node -> node.isExtension() && node.isDst() && node.hasUses()).toList();
    for (Node usage : usages) {
      List<Node> used = graph.incomingEdgesOf(usage).stream().filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
              .map(graph::getEdgeSource).filter(node -> !node.isExtension() && !node.isContext()).toList();
      if (used.isEmpty()) {
        continue;
      }
      Node.Enclosing enclosing = usage.getEnclosing();
      int memberStart = enclosing.memberStart();
      int memberEnd = enclosing.memberEnd();
      String path = usage.getPath();
      List<Node> holding = util.findOverlappingNodes(path, srcDst, memberStart, memberEnd,
              node -> !node.isContext() && !node.isExtension() && memberStart <= node.getTree().getPos()
                      && node.getTree().getEndPos() <= memberEnd && !used.contains(node));
      if (holding.isEmpty()) {
        continue;
      }
      Tree root = contentsContexts.getRoot(srcDst, path);
      Tree member = root == null ? null : TreeUtilFunctions.getTreeBetweenPositions(root, memberStart, memberEnd);
      if (member == null) {
        continue;
      }
      List<Tree> changed = new ArrayList<>(holding.stream().map(Node::getTree).toList());
      for (Node oldChange : counterpartChanges(srcDst, path, member)) {
        Tree inNew = newCodeHolding(oldChange, path);
        if (inNew == null) {
          changed = null;
          break;
        }
        changed.add(inNew);
      }
      Tree window = changed == null ? null : innermostHolding(changed);
      Tree usageTree = usage.getTree();
      if (window == null || usageTree.getPos() < window.getPos() || window.getEndPos() < usageTree.getEndPos()) {
        continue;
      }
      // A member the callee already relies on, directly or through other code, reads before it: the order cannot
      // follow both, and the reliance of the callee on the caller is the one its own changes state. The changes of a
      // member read as one diff, so the reliance is judged between the changes of the two members
      List<Node> callerChanges = new ArrayList<>(holding);
      callerChanges.addAll(counterpartChanges(srcDst, path, member));
      for (Node callee : used) {
        if (reliesOn(memberChangesOf(callee), callerChanges)) {
          continue;
        }
        for (Node caller : holding) {
          addDefUse(callee, caller);
        }
      }
    }
  }

  // The changes of the member holding the change, in both revisions
  private List<Node> memberChangesOf(Node change) {
    Node.Enclosing enclosing = change.getEnclosing();
    int start = enclosing.memberStart();
    int end = enclosing.memberEnd();
    List<Node> changes = new ArrayList<>(util.findOverlappingNodes(change.getPath(), change.getSrcDst(), start, end,
            node -> !node.isContext() && !node.isExtension() && start <= node.getTree().getPos() && node.getTree().getEndPos() <= end));
    Tree root = contentsContexts.getRoot(change.getSrcDst(), change.getPath());
    Tree member = root == null ? null : TreeUtilFunctions.getTreeBetweenPositions(root, start, end);
    if (member != null) {
      changes.addAll(counterpartChanges(change.getSrcDst(), change.getPath(), member));
    }
    return changes;
  }

  // Whether one of the nodes relies on one of the others, through the code it uses (the uses of what it uses, and so on)
  private boolean reliesOn(Collection<Node> nodes, Collection<Node> others) {
    Set<Node> targets = Collections.newSetFromMap(new IdentityHashMap<>());
    targets.addAll(others);
    Set<Node> visited = new HashSet<>();
    Deque<Node> pending = new ArrayDeque<>(nodes);
    while (!pending.isEmpty()) {
      Node current = pending.pop();
      if (!visited.add(current)) {
        continue;
      }
      for (Edge edge : graph.incomingEdgesOf(current)) {
        if (edge.getType().equals(EdgeType.DEF_USE)) {
          Node used = graph.getEdgeSource(edge);
          if (targets.contains(used)) {
            return true;
          }
          pending.push(used);
        }
      }
    }
    return false;
  }

  // The code of the new revision a change of the old revision lies in: the innermost code holding it that the mappings
  // give a counterpart of, which lies in the file of the new revision at the path; none when that counterpart lies
  // elsewhere, as for code moved to another file
  @Nullable
  private Tree newCodeHolding(Node oldChange, String path) {
    for (Tree tree = oldChange.getTree(); tree != null; tree = tree.getParent()) {
      Counterpart counterpart = counterpart(oldChange, tree);
      if (counterpart != null) {
        return counterpart.path().equals(path) ? counterpart.tree() : null;
      }
    }
    return null;
  }

  // The innermost tree holding all the trees, which lie in one file
  @Nullable
  private static Tree innermostHolding(List<Tree> trees) {
    Tree holding = trees.get(0);
    while (holding != null) {
      Tree candidate = holding;
      if (trees.stream().allMatch(tree -> candidate.getPos() <= tree.getPos() && tree.getEndPos() <= candidate.getEndPos())) {
        return holding;
      }
      holding = holding.getParent();
    }
    return null;
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
    if (renamesVariable(change) || restatesVariable(change) || writesRemovedField(change) || logStatements.logsOnly(change)
            || valueRelays.keepsValue(change)) {
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

  // Whether a member whose changes of this revision give its callers nothing but a log line (with its doc comment or
  // annotations) changes by the code it no longer has, which only the revision before holds (memberChanges gives the
  // changes of one revision): a body emptied into a log line, as a setter made a no-op that logs it is not supported
  // (netty_14031 setEpollMode), takes away what its callers relied on. Only a log line stands for code in its place; the
  // old side of other changes giving callers nothing, as the declaration of an inlined local, is no removal of behavior
  // (rocketmq_8290 string2SocketAddress)
  private boolean removesBehavior(UMLOperation operation, List<Node> changes) {
    if (changes.stream().noneMatch(logStatements::logsOnly)) {
      return false;
    }
    LocationInfo location = operation.getLocationInfo();
    Tree declaration = declarationTree(SrcDst.DST, location);
    return declaration != null && counterpartChanges(SrcDst.DST, location.getFilePath(), declaration).stream()
            .anyMatch(change -> changesBehavior(change, operation));
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
  // is the field under another name, so the statements reading the local use it too; the result of a call kept in a local
  // is a value derived from it, which the code reading the local works on further (see localUsers)
  private void addUsage(SrcDst srcDst, LocationInfo use, LocationInfo member, List<Node> changes) {
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

    Map<Tree, List<Tree>> usingTrees = new LinkedHashMap<>();
    usingTrees.put(usingTree, List.of(useTree));
    localUsers(srcDst, use.getFilePath(), usingTree, use).forEach(usingTrees::putIfAbsent);
    for (Entry<Tree, List<Tree>> using : usingTrees.entrySet()) {
      Node usage = addNode(new Node(contentsContexts, rmIndex, use.getFilePath(), srcDst, using.getKey(), null, NodeType.EXTENSION));
      // The statement holding the use reads by the code of it holding the use, a statement reading the local the use is
      // kept in by the code of it holding the reads
      using.getValue().forEach(usage::addUse);
      for (Node change : changes) {
        addDefUse(change, usage);
      }
    }
  }

  // The value a statement keeps in a local takes effect where the local is read: the readers that definition reaches
  // (see DataFlow#readersReached), a composite statement by the parts of its header reading it, as its body is not where
  // the value is read. The local is one the statement declares with a value holding the use, or assigns such a value to
  // Each reader is given with the names of it reading the local, its uses
  private Map<Tree, List<Tree>> localUsers(SrcDst srcDst, String path, Tree statementTree, LocationInfo use) {
    AbstractStatement statement = rmIndex.statementAt(srcDst, path, statementTree);
    if (statement == null || statement instanceof CompositeStatementObject) {
      return Map.of();
    }

    Map<Tree, List<Tree>> users = new LinkedHashMap<>();
    for (VariableDeclaration local : RMCode.keepingLocals(statement, use)) {
      for (AbstractStatement reader : rmIndex.readersReached(statement, local)) {
        List<AbstractCodeFragment> reading = reader instanceof CompositeStatementObject composite
                ? composite.getExpressions().stream().filter(expression -> DataFlow.readsIn(expression, local))
                .map(expression -> (AbstractCodeFragment) expression).toList()
                : List.of(reader);
        for (AbstractCodeFragment user : reading) {
          Tree userTree = declarationTree(srcDst, user.getLocationInfo());
          if (userTree == null || userTree == statementTree) {
            continue;
          }
          List<Tree> reads = users.computeIfAbsent(userTree, key -> new ArrayList<>());
          List<LocationInfo> readLocations = new ArrayList<>(DataFlow.readsAt(user, local));
          readLocations.addAll(RMCode.stageValueReads(user, local));
          for (LocationInfo read : readLocations) {
            Tree readTree = declarationTree(srcDst, read);
            if (readTree != null && !reads.contains(readTree)) {
              reads.add(readTree);
            }
          }
        }
      }
    }
    return users;
  }

  // Whether the AST diff of the file of the change maps the tree to code of the other revision as it is
  private boolean holdsAsBefore(Node change, @Nullable Tree tree) {
    if (tree == null) {
      return false;
    }
    Counterpart counterpart = counterpart(change, tree);
    return counterpart != null && tree.isIsomorphicTo(counterpart.tree());
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

      LocationInfo argument = argumentLocation(invocation, parameterIndex);
      if (argument != null) {
        result.addAll(findReachedChanges(argument.getFilePath(), srcDst, argument.getStartOffset(), argument.getEndOffset()));
      }
    }

    return result;
  }

  // Where the argument of the call at the index lies, as RM records the code of the fragment holding the call: the code
  // whose text is the argument, lying in the call after its start (where its receiver lies) and within no other call,
  // creation or lambda in it; arguments of the same text take those codes in order. RM gives the arguments of a call as
  // text only, and leaves a name or a literal out of the arguments it records with their locations, so the code is
  // looked for among all the code RM records
  @Nullable
  private LocationInfo argumentLocation(AbstractCall call, int index) {
    AbstractCodeFragment fragment = memberUsageFinder.fragmentOf(call);
    if (fragment == null) {
      return null;
    }
    LocationInfo callLocation = call.getLocationInfo();
    List<LocationInfo> nested = new ArrayList<>();
    fragment.getMethodInvocations().forEach(other -> nested.add(other.getLocationInfo()));
    fragment.getCreations().forEach(other -> nested.add(other.getLocationInfo()));
    fragment.getLambdas().forEach(lambda -> nested.add(lambda.getLocationInfo()));
    nested.removeIf(other -> !callLocation.subsumes(other) || other.subsumes(callLocation));
    List<LeafExpression> codes = new ArrayList<>();
    for (List<? extends LeafExpression> kind : List.of(fragment.getArguments(), fragment.getVariables(), fragment.getStringLiterals(),
            fragment.getTextBlocks(), fragment.getCharLiterals(), fragment.getNumberLiterals(), fragment.getNullLiterals(),
            fragment.getBooleanLiterals(), fragment.getTypeLiterals(), fragment.getThisExpressions(), fragment.getInfixExpressions(),
            fragment.getArrayAccesses(), fragment.getCastExpressions(), fragment.getPrefixExpressions(), fragment.getPostfixExpressions(),
            fragment.getParenthesizedExpressions(), fragment.getMethodInvocations(), fragment.getCreations())) {
      codes.addAll(kind);
    }
    String text = call.arguments().get(index);
    TreeMap<Integer, LocationInfo> candidates = new TreeMap<>();
    for (LeafExpression code : codes) {
      LocationInfo location = code.getLocationInfo();
      if (code.getString().equals(text) && callLocation.subsumes(location) && location.getStartOffset() > callLocation.getStartOffset()
              && nested.stream().noneMatch(other -> other.subsumes(location) && !location.subsumes(other))) {
        candidates.putIfAbsent(location.getStartOffset(), location);
      }
    }
    int occurrence = (int) call.arguments().subList(0, index).stream().filter(text::equals).count();
    return occurrence < candidates.size() ? new ArrayList<>(candidates.values()).get(occurrence) : null;
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

      // A class literal hands the class to the code using it, as a creation does.
      // RM gap: RM gives a type literal as its text only, with no type, so the class is read from the Java form
      // `Name.class`; the literals of other languages (Kotlin `Name::class`) name no class here
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
        List<Node> newDeclarations = !isNewOperation(operation, overridingChanges) ? List.of() : overridingChanges.stream()
                .filter(change -> change.getRM().declaredOperation() == operation).toList();
        for (UMLOperation overridden : nearestOverridden(allOverridden, umlModel)) {
          if (declarationChanges(overridden, srcDst).isEmpty()) {
            Tree overriddenTree = declarationTree(srcDst, overridden.getLocationInfo());
            for (Node newDeclaration : overriddenTree == null ? List.<Node>of() : newDeclarations) {
              addDefUse(addExtensionNode(overriddenTree, newDeclaration), newDeclaration);
            }
          }
        }

        for (UMLOperation overridden : allOverridden) {
          List<Node> overriddenChanges = declarationChanges(overridden, srcDst);
          for (Node overriddenChange : overriddenChanges) {
            for (Node overridingChange : overridingChanges) {
              addDefUse(overriddenChange, overridingChange);
            }
          }

          // A call of the overridden method may run the changed override, as a test calling it through the interface
          // its subject implements does; but a call the override relies on, as a template method of the class declaring
          // the hook it implements, reads before it. Only the changes of what the override does for its callers run so,
          // not a comment or a doc comment. A changed overridden method is what its calls rely on, and the overrides are
          // its alternatives, which it serves: they read after it and the calls of it, as the enrich override of an
          // extracted hook after the hook and the call site it moved from (es_120617), not between them
          for (Node invocationChange : overriddenChanges.isEmpty() ? getInvocationNodes(overridden, srcDst, false) : Set.<Node>of()) {
            for (Node overridingChange : overridingChanges) {
              if (!changesBehavior(overridingChange, operation)) {
                continue;
              }
              if (graph.getAllEdges(invocationChange, overridingChange).stream().noneMatch(edge -> edge.getType().equals(EdgeType.DEF_USE))) {
                addDefUse(overridingChange, invocationChange);
              }
            }
          }
        }
      }
    }
  }

  // Whether the operation is new in this revision: a change adds its whole declaration, or its class did not declare it
  // before (see RMLinks#isNewOperation), as when the AST diff maps its statements as moved from removed code. A new override is
  // judged against the contract it fulfills, which a reviewer needs to see (the hashCode it refines and the removed
  // overrides now fall back to), and it runs for no caller of the template methods calling the overridden method before:
  // they ran the overridden method itself, so no code of theirs reads differently. Taking such an override for an
  // existing one hides its contract and prints those callers as if their behavior changed (es_107947 Node#transformDown)
  private boolean isNewOperation(UMLOperation operation, List<Node> changes) {
    LocationInfo location = operation.getLocationInfo();
    return rmLinks.isNewOperation(operation) || changes.stream().anyMatch(change -> change.getNodeType().equals(NodeType.ADDITION)
            && change.getTree().getPos() <= location.getStartOffset() && location.getEndOffset() <= change.getTree().getEndPos());
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
    return getInvocationNodes(operation, srcDst, editing, call -> true);
  }

  // The changes holding the calls of the operation the predicate keeps
  private Set<Node> getInvocationNodes(UMLOperation operation, SrcDst srcDst, boolean editing, Predicate<AbstractCall> kept) {
    Set<Node> result = new HashSet<>();

    for (AbstractCall invocation : findInvocations(operation, srcDst)) {
      if (!kept.test(invocation)) {
        continue;
      }
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
    return getAccessNodes(attribute, srcDst, editing, access -> true);
  }

  // The changes holding the names referring to the attribute the predicate keeps
  private Set<Node> getAccessNodes(UMLAttribute attribute, SrcDst srcDst, boolean editing, Predicate<LeafExpression> kept) {
    Set<Node> result = new HashSet<>();
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (LeafExpression access : memberUsageFinder.findAccesses(attribute, umlModel)) {
      if (!kept.test(access)) {
        continue;
      }
      LocationInfo accessLocation = access.getLocationInfo();
      result.addAll(editingNodes(util.findOverlappingNodes(accessLocation.getFilePath(), srcDst, accessLocation.getStartOffset(),
              accessLocation.getEndOffset(), n -> !n.isContext() && !n.isExtension()), accessLocation, editing, memberKey(attribute)));
    }
    return result;
  }

  // The changes of the revision before editing a write of the field
  private Set<Node> getWritingAccessNodes(UMLAttribute attribute) {
    Set<Node> result = new HashSet<>();
    for (LeafExpression access : memberUsageFinder.findAccesses(attribute, modelDiff.getParentModel())) {
      if (!memberUsageFinder.isWrite(access)) {
        continue;
      }
      LocationInfo accessLocation = access.getLocationInfo();
      result.addAll(editingNodes(util.findOverlappingNodes(accessLocation.getFilePath(), SrcDst.SRC, accessLocation.getStartOffset(),
              accessLocation.getEndOffset(), n -> !n.isContext() && !n.isExtension()), accessLocation, true, memberKey(attribute)));
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

  // Changes of successive code read together. A comment on lines of its own leads the code after it, which it documents,
  // and does not succeed the code before it: the removed `EMPTY` constant of es_110046 and the comment of the params
  // field below it are two changes of two declarations, and reading them as one put the constant's removal with the
  // field the comment moved onto, ahead of the constructor the constant relied on
  private void processSuccession() {
    List<Node> nodes = graph.vertexSet().stream()
            .filter(node -> !node.isContext() && !node.isExtension()).toList();
    for (Node subject : nodes) {
      Tree rightSibling = subject.getRight();
      if (rightSibling == null) {
        continue;
      }

      for (Node object : nodes) {
        if (object.getTree().equals(rightSibling) && !leadsNextCode(subject, object)) {
          addEdge(subject, object, EdgeType.SUCCESSION);
        }
      }
    }
  }

  // Whether the object is a comment starting on a line after the end of the subject, which it then does not describe;
  // a comment after a comment goes on with it, as the lines of one explanation
  private boolean leadsNextCode(Node subject, Node object) {
    if (!isComment(object) || isComment(subject)) {
      return false;
    }
    String content = subject.getFileContent();
    return TreeUtilFunctions.getLineRange(subject.getTree(), content).endLine()
            < TreeUtilFunctions.getLineRange(object.getTree(), content).startLine();
  }

  private record ImportTree(Tree tree, NodeType type, String path, SrcDst srcDst) {
  }

  private record NodeTrees(Node node, Set<Tree> trees) {
  }
}
