package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLGeneralization;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLRealization;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class HunkNetwork {

  private final Graph<Node, Edge> graph;
  private final HashMap<String, Node> nodeMap = new HashMap<>();
  private final UMLModelDiff modelDiff;
  private final Map<String, String> srcContents;
  private final Map<String, String> dstContents;
  private final Map<String, TreeContext> srcContexts;
  private final Map<String, TreeContext> dstContexts;
  private final Set<ImportTree> srcTrees = new HashSet<>();
  private final Set<ImportTree> dstTrees = new HashSet<>();

  public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents,
      Map<String, String> dstContents,
      Map<String, TreeContext> srcContexts, Map<String, TreeContext> dstContexts) {
    graph = GraphTypeBuilder.<Node, Edge>directed().allowingMultipleEdges(true)
        .allowingSelfLoops(true).edgeClass(Edge.class).weighted(true).buildGraph();
    this.modelDiff = modelDiff;
    this.srcContents = srcContents;
    this.dstContents = dstContents;
    this.srcContexts = srcContexts;
    this.dstContexts = dstContexts;
  }

  private TreeLocation localizeTree(Tree tree) {
    List<Tree> parents = tree.getParents();
    Tree root = parents.get(parents.size() - 1);
    return findContext(srcContexts, root, SrcDst.SRC)
        .or(() -> findContext(dstContexts, root, SrcDst.DST))
        .orElse(null);
  }

  private Optional<TreeLocation> findContext(Map<String, TreeContext> contexts, Tree root, SrcDst srcDst) {
    return contexts.entrySet().stream()
        .filter(e -> e.getValue().getRoot().equals(root))
        .map(e -> new TreeLocation(srcDst, e.getKey()))
        .findFirst();
  }

  private String getFileContent(SrcDst srcDst, String path) {
    return (srcDst.equals(SrcDst.SRC) ? srcContents : dstContents).get(path);
  }

  public void importFiles(List<Entry<String, TreeContext>> deletedFiles,
      List<Entry<String, TreeContext>> addedFiles) {
    for (Entry<String, TreeContext> deletedFile : deletedFiles) {
      String path = deletedFile.getKey();
      srcTrees.addAll(getValidTrees(path, deletedFile.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DELETION, null, path)).toList());
    }
    for (Entry<String, TreeContext> addedFile : addedFiles) {
      String path = addedFile.getKey();
      dstTrees.addAll(getValidTrees(path, addedFile.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.ADDITION, null, path)).toList());
    }
  }

  public void importDiff(ASTDiff diff) {
    TreeClassifier classifier = diff.createRootNodesClassifier();
    String srcPath = diff.getSrcPath();
    String dstPath = diff.getDstPath();

    srcTrees.addAll(getValidTrees(srcPath, classifier.getMovedSrcs()).stream()
            .map(tree -> new ImportTree(tree, NodeType.SRC_MOVE, diff, srcPath)).toList());
    if (srcPath.equals(dstPath)) {
      srcTrees.addAll(getValidTrees(srcPath, classifier.getDeletedSrcs()).stream()
          .map(tree -> new ImportTree(tree, NodeType.DELETION, diff, srcPath)).toList());
      srcTrees.addAll(getValidTrees(srcPath, classifier.getUpdatedSrcs()).stream()
          .map(tree -> new ImportTree(tree, NodeType.SRC_UPDATE, diff, srcPath)).toList());
    }

    dstTrees.addAll(getValidTrees(dstPath, classifier.getMovedDsts()).stream()
            .map(tree -> new ImportTree(tree, NodeType.DST_MOVE, diff, dstPath)).toList());
    if (srcPath.equals(dstPath)) {
      dstTrees.addAll(getValidTrees(dstPath, classifier.getInsertedDsts()).stream()
          .map(tree -> new ImportTree(tree, NodeType.ADDITION, diff, dstPath)).toList());
      dstTrees.addAll(getValidTrees(dstPath, classifier.getUpdatedDsts()).stream()
          .map(tree -> new ImportTree(tree, NodeType.DST_UPDATE, diff, dstPath)).toList());
    }
  }

  private Set<Tree> getValidTrees(String path, Collection<Tree> trees) {
    Constants constants = new Constants(path);
    Set<String> invalidTypes = new HashSet<>() {{
      add(constants.EMPTY_STATEMENT);
      add(constants.IMPORT_DECLARATION);
      add(constants.PACKAGE_DECLARATION);
    }};

    return trees.stream()
        .filter(addition -> {
          HashSet<Tree> precedents = new HashSet<>(addition.getParents());
          precedents.add(addition);

          return precedents.stream()
              .noneMatch(precedent -> invalidTypes.contains(precedent.getType().name));
        }).collect(
            Collectors.toSet());
  }

  private HashMap<ImportTree, Set<ImportTree>> aggregateTrees(Set<ImportTree> importTrees) {
    HashMap<ImportTree, Set<ImportTree>> result = new HashMap<>();

    for (ImportTree subject : importTrees) {
      boolean isParent = true;

      for (ImportTree object : importTrees) {
        if (subject.equals(object)) {
          continue;
        }

        if (object.path.equals(subject.path) && object.tree.getPos() <= subject.tree.getPos()
            && subject.tree.getEndPos() <= object.tree.getEndPos()) {
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

        if (parent.path.equals(importTree.path) && parent.tree.getPos() <= importTree.tree.getPos()
            && importTree.tree.getEndPos() <= parent.tree.getEndPos()) {
          result.get(parent).add(importTree);
        }
      }
    }

    return result;
  }

  private void importTrees(HashMap<ImportTree, Set<ImportTree>> trees) {
    trees.entrySet().stream().map(entry -> {
      ImportTree parent = entry.getKey();
      Set<ImportTree> subs = entry.getValue();
      TreeLocation treeLocation = localizeTree(parent.tree);
      String fileContent = getFileContent(treeLocation.srcDst, treeLocation.path);
      Set<Node> subsNode = subs.stream().map(sub -> {
        Node subNode = new Node(fileContent, sub.path, treeLocation.srcDst, sub.tree, null, sub.type);
        subNode.addDiff(sub.diff);
        return subNode;
      }).collect(Collectors.toSet());

      Node parentNode = new Node(fileContent, treeLocation.path, treeLocation.srcDst, parent.tree, subsNode, parent.type);
      parentNode.addDiff(parent.diff);

      return parentNode;
    }).forEach(this::addNode);
  }

  private Node addExtensionNode(Tree extensionTree, Node extendedNode) {
    TreeLocation treeLocation = localizeTree(extensionTree);
    Node node = new Node(getFileContent(treeLocation.srcDst, treeLocation.path), treeLocation.path,
            treeLocation.srcDst, extensionTree, null, NodeType.EXTENSION);
    node.addDiffs(extendedNode.getDiffs());
    return addNode(node);
  }

  private Node addNode(Node node) {
    if (nodeMap.containsKey(node.getId())) {
      Node existingNode = nodeMap.get(node.getId());

      existingNode.addDiffs(node.getDiffs());
      if (existingNode.getSubs() != null && node.getSubs() != null) {
        for (Node sub : node.getSubs()) {
          Optional<Node> foundSub = existingNode.getSubs().stream().filter(existingSub -> existingSub.getId().equals(sub.getId())).findFirst();
          if (foundSub.isPresent()) {
            foundSub.get().addDiffs(sub.getDiffs());
          } else {
            existingNode.getSubs().add(sub);
          }
        }
      }

      return existingNode;
    }

    graph.addVertex(node);
    nodeMap.put(node.getId(), node);

    addNodeContexts(node);

    return node;
  }

  private void addNodeContexts(Node node) {
    String path = node.getPath();
    SrcDst srcDst = node.getSrcDst();

    List<Pair<Tree, NodeType>> contexts = Context.get(node.getPath(), node.getTree());
    for (Pair<Tree, NodeType> context : contexts) {
      String potentialContextId = Node.formatId(path, srcDst, context.second, context.first);

      if (!nodeMap.containsKey(potentialContextId)) {
        Node contextNode = new Node(node.getFileContent(), path, srcDst, context.first, null, context.second);
        graph.addVertex(contextNode);
        nodeMap.put(contextNode.getId(), contextNode);
      }

      Node contextNode = nodeMap.get(potentialContextId);
      contextNode.addDiffs(node.getDiffs());
      injectContextNode(contextNode);
    }

    injectContextNode(node);
  }

  private void injectContextNode(Node contextNode) {
    List<Node> descendantNodes = graph.vertexSet().stream()
        .filter(node -> node.isDescendantOf(contextNode))
        .toList();
    List<Node> immediateDescendants = descendantNodes.stream().filter(
            subject -> descendantNodes.stream().noneMatch(subject::isDescendantOf))
        .toList();
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

    List<Node> predecessors = graph.vertexSet().stream().filter(contextNode::isDescendantOf)
        .toList();
    Optional<Node> immediatePredecessor = predecessors.stream().filter(
            subject -> predecessors.stream().noneMatch(object -> object.isDescendantOf(subject)))
        .findFirst();
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
    importTrees(aggregateTrees(srcTrees));
    importTrees(aggregateTrees(dstTrees));

    processDefUse();
    processClassLevelRelations();
    processExtensions(SrcDst.SRC);
    processExtensions(SrcDst.DST);
    processMapping();
    processSuccession();
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
        List<Tree> dstTrees = srcOrSub.getDiffs().stream()
                .map(diff -> diff.getAllMappings().getMonoMappingStore().getDstForSrc(srcOrSub.getTree())).toList();
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
    List<Node> nodes = graph.vertexSet().stream().toList();
    for (Node node : nodes) {
      if (node.getNodeType().equals(NodeType.LOCATION_CONTEXT)) {
        continue;
      }

      Tree tree = node.getTree();

      List<Tree> subTrees = new ArrayList<>();
      subTrees.add(tree);
      if (!node.isContext()) {
        subTrees.addAll(tree.getDescendants());
      }

      Constants constants = new Constants(node.getPath());

      List<Tree> methodDeclarations = new ArrayList<>();
      List<Tree> fieldDeclarations = new ArrayList<>();
      List<Tree> parameterDeclarations = new ArrayList<>();
      List<Tree> variableDeclarations = new ArrayList<>();
      for (Tree subTree : subTrees) {
        String subTreeType = subTree.getType().name;
        if (subTreeType.equals(constants.METHOD_DECLARATION)) {
          methodDeclarations.add(subTree);
        }
        if (subTreeType.equals(constants.FIELD_DECLARATION)) {
          fieldDeclarations.add(subTree);
        }
        // TODO: investigation: it is only for "Enhanced For" and other parameters will be in variable declarations array
        if (subTreeType.equals(constants.RECORD_COMPONENT)) {
          parameterDeclarations.add(subTree);
        }
        if (subTreeType.equals(constants.VARIABLE_DECLARATION_STATEMENT)) {
          variableDeclarations.add(subTree);
        }
      }
      if (methodDeclarations.isEmpty() && fieldDeclarations.isEmpty()
          && parameterDeclarations.isEmpty() && variableDeclarations.isEmpty()) {
        continue;
      }

      // prevent having one edge for method and some other for parameters
      for (Tree methodDeclaration : methodDeclarations) {
        List<Tree> methodDescendants = methodDeclaration.getDescendants();
        for (Tree methodDescendant : methodDescendants) {
          parameterDeclarations.remove(methodDescendant);
          variableDeclarations.remove(methodDescendant);
        }
      }

      for (Tree methodDeclaration : methodDeclarations) {
        UMLClass umlClass = getUMLClass(node.getPath(), methodDeclaration, node.getSrcDst());
        if (umlClass == null) {
          continue;
        }
        UMLOperation operation = findUMLOperation(umlClass, methodDeclaration);
        if (operation == null) {
          continue;
        }

        node.addIdentifier(operation.getName());

        Set<Node> invocationNodes = getInvocationNodes(operation, node.getSrcDst());
        for (Node invocationNode : invocationNodes) {
          addEdge(node, invocationNode, EdgeType.DEF_USE);
        }
      }

      for (Tree fieldDeclaration : fieldDeclarations) {
        UMLAttribute umlAttribute = findUMLAttribute(node.getPath(), fieldDeclaration,
            node.getSrcDst());
        if (umlAttribute == null) {
          continue;
        }

        node.addIdentifier(umlAttribute.getVariableDeclaration().getVariableName());

        Set<Node> useNodes = findAccessNodes(umlAttribute.getName(),
            node.isSrc() ? modelDiff.findFieldAccessesInParentModel(umlAttribute)
                : modelDiff.findFieldAccessesInChildModel(umlAttribute), node.getSrcDst());
        for (Node useNode : useNodes) {
          addEdge(node, useNode, EdgeType.DEF_USE);
        }
      }

      for (Tree parameterDeclaration : parameterDeclarations) {
        addVariableDeclarationEdges(node, parameterDeclaration);
      }
      List<Tree> methodParameterDeclarations = parameterDeclarations.stream().filter(
          parameterDeclaration -> parameterDeclaration.getParent().getType().name.equals(
              constants.METHOD_DECLARATION)).toList();
      for (Tree methodParameterDeclaration : methodParameterDeclarations) {
        addParameterArgumentEdges(node, methodParameterDeclaration);
      }

      for (Tree variableDeclaration : variableDeclarations) {
        Tree variableDeclarationFragment = TreeUtilFunctions.findChildByType(
            variableDeclaration, constants.VARIABLE_DECLARATION_FRAGMENT);
        if (variableDeclarationFragment == null) {
          continue;
        }

        addVariableDeclarationEdges(node, variableDeclarationFragment);
      }
    }
  }

  private void processExtensions(SrcDst srcDst) {
    HashMap<Node, Set<Node>> nodesExtensions = new HashMap<>();

    UMLModel umlModel =
        srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    for (UMLClass umlClass : umlModel.getClassList()) {
      for (UMLAttribute fieldDeclaration : umlClass.getAttributes()) {
        LocationInfo declarationLocation = fieldDeclaration.getLocationInfo();
        Set<AbstractCodeFragment> fieldAccesses =
            srcDst.equals(SrcDst.SRC) ? modelDiff.findFieldAccessesInParentModel(fieldDeclaration)
                : modelDiff.findFieldAccessesInChildModel(fieldDeclaration);
        Set<Node> accessNodes = findAccessNodes(fieldDeclaration.getName(), fieldAccesses, srcDst);
        pushNodesExtensions(nodesExtensions, accessNodes, declarationLocation, Tree::getParent,
            srcDst);
      }

      for (UMLOperation operation : umlClass.getOperations()) {
        LocationInfo operationLocation = operation.getLocationInfo();
        Set<Node> invocationNodes = getInvocationNodes(operation, srcDst);
        pushNodesExtensions(nodesExtensions, invocationNodes, operationLocation, null, srcDst);

        for (VariableDeclaration variableDeclaration : operation.getAllVariableDeclarations()) {
          LocationInfo declarationLocation = variableDeclaration.getLocationInfo();
          Set<Node> accessNodes = findAccessNodes(variableDeclaration.getVariableName(),
              variableDeclaration.getScope().getStatementsInScopeUsingVariable(), srcDst);
          pushNodesExtensions(nodesExtensions, accessNodes, declarationLocation, null, srcDst);
        }
      }
    }

    for (Entry<Node, Set<Node>> nodeExtensions : nodesExtensions.entrySet()) {
      for (Node extension : nodeExtensions.getValue()) {
        addEdge(extension, nodeExtensions.getKey(), EdgeType.DEF_USE);
      }
    }
  }

  private void pushNodesExtensions(HashMap<Node, Set<Node>> nodesExtensions,
      Set<Node> useNodes, LocationInfo usedLocation, Function<Tree, Tree> treeTransformer,
      SrcDst srcDst) {
    List<Node> usedNodes = findOverlappingNodes(usedLocation.getFilePath(), srcDst,
        usedLocation.getStartOffset(), usedLocation.getEndOffset(),
        (n) -> !n.isContext() && !n.isExtension());
    if (!usedNodes.isEmpty()) {
      return;
    }

    // TODO: only isolated nodes can take advantage of extensions?
    List<Node> isolatedUseNodes = useNodes.stream().filter(n -> graph.incomingEdgesOf(n).stream()
            .filter(edge -> edge.getType().equals(EdgeType.DEF_USE)).toList().isEmpty())
        .toList();
    if (isolatedUseNodes.isEmpty()) {
      return;
    }

    Tree usedRootTree = (srcDst.equals(SrcDst.SRC) ? srcContexts : dstContexts).get(
        usedLocation.getFilePath()).getRoot();
    Constants constants = new Constants(usedLocation.getFilePath());
    Tree usedTree = TreeUtilFunctions.findByLocationInfo(usedRootTree, usedLocation, constants);
    if (treeTransformer != null) {
      usedTree = treeTransformer.apply(usedTree);
    }

    for (Node isolatedUseNode : isolatedUseNodes) {
      Node usedExtensionNode = addExtensionNode(usedTree, isolatedUseNode);

      nodesExtensions.putIfAbsent(isolatedUseNode, new HashSet<>());
      nodesExtensions.get(isolatedUseNode).add(usedExtensionNode);
    }
  }

  private void addParameterArgumentEdges(Node node, Tree parameterDeclarationTree) {
    Constants constants = new Constants(node.getPath());

    int parameterIndex = parameterDeclarationTree.getParent().getChildren().stream()
        .filter(child -> child.getType().name.equals(constants.RECORD_COMPONENT)).toList()
        .indexOf(parameterDeclarationTree);

    Tree methodRoot = TreeUtilFunctions.getParentUntilType(parameterDeclarationTree,
        constants.METHOD_DECLARATION);
    // https://github.com/elastic/elasticsearch/commit/e0a458441cff9a4242cd93f4c02f06d72f2d63c4#diff-9b7bef16de393901cd8c75e73d2fb03afb90a10f1de191b7174275bbd8e71bd8L38
    if (methodRoot == null) {
      return;
    }

    UMLClass umlClass = getUMLClass(node.getPath(), parameterDeclarationTree, node.getSrcDst());
    if (umlClass == null) {
      return;
    }

    UMLOperation umlOperation = findUMLOperation(umlClass, methodRoot);
    if (umlOperation == null) {
      return;
    }

    List<AbstractCall> invocations =
        node.isSrc() ? modelDiff.findInvocationsInParentModel(umlOperation)
            : modelDiff.findInvocationsInChildModel(umlOperation);

    for (AbstractCall invocation : invocations) {
      Tree invocationRootTree = (node.isSrc() ? srcContexts : dstContexts).get(
          invocation.getLocationInfo().getFilePath()).getRoot();
      Tree invocationTree = TreeUtilFunctions.findByLocationInfo(invocationRootTree,
          invocation.getLocationInfo(),
          constants);
      Tree argumentsTree = TreeUtilFunctions.findChildByType(invocationTree,
          constants.METHOD_INVOCATION_ARGUMENTS);
      if (argumentsTree == null) {
        continue;
      }

      Tree argumentTree = argumentsTree.getChildren().get(parameterIndex);
      List<Node> argumentNodes = findOverlappingNodes(invocation.getLocationInfo().getFilePath(),
          node.getSrcDst(),
          argumentTree.getPos(), argumentTree.getEndPos(),
          (n) -> !n.isContext() && !n.isExtension());

      for (Node argumentNode : argumentNodes) {
        addEdge(node, argumentNode, EdgeType.DEF_USE);
      }
    }
  }

  private void processClassLevelRelations() {
    // Class Instance Creation
    List<Node> nodes = graph.vertexSet().stream()
        .filter(node -> !node.isContext() && !node.isExtension()).toList();
    for (Node node : nodes) {
      Tree tree = node.getTree();
      List<Tree> subTrees = new ArrayList<>(tree.getDescendants());
      subTrees.add(tree);

      Constants constants = new Constants(node.getPath());

      List<Tree> classInstanceCreations =
          subTrees.stream().filter(subTree -> subTree.getType().name.equals(
              constants.CLASS_INSTANCE_CREATION)).toList();
      for (Tree classInstanceCreation : classInstanceCreations) {
        Tree instantiatedClassTypeTree = TreeUtilFunctions.findChildByType(classInstanceCreation,
            constants.SIMPLE_TYPE);
        if (instantiatedClassTypeTree == null) {
          continue;
        }
        Tree instantiatedClassNameTree = TreeUtilFunctions.findChildByType(
            instantiatedClassTypeTree,
            constants.SIMPLE_NAME);
        if (instantiatedClassNameTree == null) {
          continue;
        }

        UMLClass instantiatedClass = getUMLClass(instantiatedClassNameTree.getLabel(),
            node.isSrc() ? modelDiff.getParentModel() : modelDiff.getChildModel());
        if (instantiatedClass == null) {
          continue;
        }

        Node classNode = getClassNode(instantiatedClass, node.getSrcDst());
        if (classNode == null || classNode.equals(node)) {
          continue;
        }

        addEdge(classNode, node, EdgeType.DEF_USE);
      }
    }

    // extends and implements
    processGeneralizations(SrcDst.SRC);
    processRealizations(SrcDst.SRC);
    processGeneralizations(SrcDst.DST);
    processRealizations(SrcDst.DST);
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

      UMLClass parentClass = getUMLClass(generalization.getParent(), umlModel);
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

      UMLClass parentClass = getUMLClass(realization.getSupplier(), umlModel);
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
  private UMLClass getUMLClass(String className, UMLModel model) {
//    List<UMLImport> contextImportedTypes = contextClass.getImportedTypes().stream()
//        .filter(importedType -> importedType.getName().endsWith(targetClassName))
//        .toList();
//    String targetQualifiedName = contextImportedTypes.isEmpty() ?
//        contextClass.getPackageName() + "." + targetClassName : contextImportedTypes.get(0).getName();
//    Optional<UMLClass> targetWithQualifiedName = model.getClassList().stream()
//        .filter(c -> c.getName().equals(targetQualifiedName)).findFirst();
//    if (targetWithQualifiedName.isPresent()) {
//      return targetWithQualifiedName.get();
//    }
    return model.getClassList().stream().filter(c -> c.getName().endsWith(className)).findFirst()
        .orElse(null);
  }

  @Nullable
  private Node getClassNode(UMLClass umlClass, SrcDst srcDst) {
    LocationInfo classLocation = umlClass.getLocationInfo();
    List<Node> classNodes = findOverlappingNodes(classLocation.getFilePath(), srcDst,
        classLocation.getStartOffset(), classLocation.getEndOffset(),
        (n) -> !n.isExtension() && n.getTree().getPos() == classLocation.getStartOffset()
            && n.getTree().getEndPos() == classLocation.getEndOffset());
    if (classNodes.isEmpty()) {
      return null;
    }

    Node classNode = classNodes.get(0);
    classNode.addIdentifier(umlClass.getNonQualifiedName());

    return classNode;
  }

  private UMLClass getUMLClass(String path, Tree tree, SrcDst srcDst) {
    Constants constants = new Constants(path);

    List<Tree> parents = new ArrayList<>();
    parents.add(tree);
    parents.addAll(tree.getParents());
    Tree parentClass = parents.stream().filter(parent -> {
      String parentType = parent.getType().name;
      return parentType.equals(constants.TYPE_DECLARATION) || parentType.equals(
          constants.ENUM_DECLARATION);
    }).findFirst().orElse(null);
    if (parentClass == null) { // there is no need to process def-use when it is out of type
      return null;
    }
    Tree parentTypeName = TreeUtilFunctions.findChildByType(parentClass,
        constants.SIMPLE_NAME);
    if (parentTypeName == null) {
      return null;
    }

    return getUMLClass(localizeTree(tree).path, parentTypeName.getLabel(),
        srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel());
  }

  private UMLClass getUMLClass(String path, String className, UMLModel umlModel) {
    if (path == null) {
      return null;
    }

    UMLClass UmlClass = null;
    for (UMLClass uc : umlModel.getClassList()) {
      if (uc.getSourceFile().equals(path) && uc.getNonQualifiedName().equals(className)) {
        UmlClass = uc;
        break;
      }
    }

    return UmlClass;
  }

  private void addVariableDeclarationEdges(Node node, Tree variableDeclarationTree) {
    Constants constants = new Constants(node.getPath());
    Tree methodRoot = TreeUtilFunctions.getParentUntilType(variableDeclarationTree,
        constants.METHOD_DECLARATION);
    // https://github.com/elastic/elasticsearch/commit/e0a458441cff9a4242cd93f4c02f06d72f2d63c4#diff-9b7bef16de393901cd8c75e73d2fb03afb90a10f1de191b7174275bbd8e71bd8L38
    if (methodRoot == null) {
      return;
    }

    UMLClass umlClass = getUMLClass(node.getPath(), variableDeclarationTree, node.getSrcDst());
    if (umlClass == null) {
      return;
    }

    UMLOperation umlOperation = findUMLOperation(umlClass, methodRoot);
    if (umlOperation == null) {
      return;
    }

    Optional<VariableDeclaration> foundOperationVariable = umlOperation.getAllVariableDeclarations()
        .stream()
        .filter(operationVariable -> {
          LocationInfo operationVariableLoc = operationVariable.getLocationInfo();
          return operationVariableLoc.getStartOffset() == variableDeclarationTree.getPos()
              && variableDeclarationTree.getEndPos()
              == operationVariableLoc.getEndOffset();
        }).findFirst();
    if (foundOperationVariable.isEmpty()) {
      return;
    }

    VariableDeclaration variableDeclaration = foundOperationVariable.get();

    node.addIdentifier(variableDeclaration.getVariableName());

    Set<Node> useNodes = findAccessNodes(variableDeclaration.getVariableName(),
        variableDeclaration.getScope().getStatementsInScopeUsingVariable(), node.getSrcDst());
    for (Node useNode : useNodes) {
      addEdge(node, useNode, EdgeType.DEF_USE);
    }
  }

  private Set<Node> getInvocationNodes(UMLOperation operation, SrcDst srcDst) {
    Set<Node> result = new HashSet<>();

    List<AbstractCall> invocations =
        srcDst.equals(SrcDst.SRC) ? modelDiff.findInvocationsInParentModel(operation)
            : modelDiff.findInvocationsInChildModel(operation);
    for (AbstractCall invocation : invocations) {
      LocationInfo invocationLocationInfo = invocation.getLocationInfo();
      List<Node> overlappingNodes = findOverlappingNodes(invocationLocationInfo.getFilePath(),
          srcDst, invocationLocationInfo.getStartOffset(),
          invocationLocationInfo.getEndOffset(), n -> !n.isContext() && !n.isExtension());
      result.addAll(overlappingNodes);
    }

    return result;
  }

  private Set<Node> findAccessNodes(String name, Set<AbstractCodeFragment> accessFragments,
      SrcDst srcDst) {
    Set<Node> result = new HashSet<>();

    for (AbstractCodeFragment accessFragment : accessFragments) {
      List<LeafExpression> useVariables =
          accessFragment.getVariables().stream().filter(variable -> variable.getString()
              .contains(name)).toList();

      for (LeafExpression useVariable : useVariables) {
        LocationInfo useVariableLocation = useVariable.getLocationInfo();
        List<Node> overlappingNodes = findOverlappingNodes(useVariableLocation.getFilePath(),
            srcDst, useVariableLocation.getStartOffset(), useVariableLocation.getEndOffset(),
            n -> !n.isContext() && !n.isExtension());
        result.addAll(overlappingNodes);
      }
    }

    return result;
  }

  private LocationInfo findClosestLocationInfo(LocationInfo subject,
      List<LocationInfo> candidates) {
    int pos = subject.getStartOffset();
    int endPos = subject.getEndOffset();

    List<LocationInfo> sameFileCandidates =
        candidates.stream()
            .filter(candidate -> subject.getFilePath().equals(candidate.getFilePath()))
            .toList();
    if (!sameFileCandidates.isEmpty()) {
      LocationInfo closestLocationInfo = null;

      int shortestDistance = Integer.MAX_VALUE;
      for (LocationInfo candidate : sameFileCandidates) {
        if (endPos >= candidate.getStartOffset() && candidate.getEndOffset() >= pos) {
          return candidate;
        }

        int distance =
            endPos < candidate.getStartOffset() ? candidate.getStartOffset() - endPos :
                pos - candidate.getEndOffset();
        if (distance < shortestDistance) {
          shortestDistance = distance;
          closestLocationInfo = candidate;
        }
      }

      return closestLocationInfo;
    }

    return candidates.get(0);
  }

  private List<Node> findOverlappingNodes(String path, SrcDst srcDst, int pos, int endPos,
      Predicate<Node> filter) {
    List<Node> result = new ArrayList<>();

    for (Node node : graph.vertexSet()) {
      Tree tree = node.getTree();
      if (node.getPath().equals(path) && node.getSrcDst().equals(srcDst)
          && tree.getPos() <= endPos && pos <= tree.getEndPos() && (filter == null
          || filter.test(node))) {
        result.add(node);
      }
    }

    return result;
  }

  private UMLOperation findUMLOperation(UMLClass umlClass, Tree methodDeclaration) {
    List<UMLOperation> operations = new ArrayList<>(umlClass.getOperations());
    for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
      operations.addAll(anonymousClass.getOperations());
    }

    for (UMLOperation operation : operations) {
      LocationInfo operationLoc = operation.getLocationInfo();
      if (operationLoc.getStartOffset() == methodDeclaration.getPos()
          && methodDeclaration.getEndPos() == operationLoc.getEndOffset()) {
        return operation;
      }
    }

    return null;
  }

  private UMLAttribute findUMLAttribute(String path, Tree fieldDeclaration, SrcDst srcDst) {
    UMLClass umlClass = getUMLClass(path, fieldDeclaration, srcDst);
    if (umlClass == null) {
      return null;
    }

    List<UMLAttribute> attributes = new ArrayList<>(umlClass.getAttributes());
    for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
      attributes.addAll(anonymousClass.getAttributes());
    }

    UMLAttribute result = null;
    for (UMLAttribute attribute : attributes) {
      LocationInfo attrLoc = attribute.getFieldDeclarationLocationInfo();
      if (fieldDeclaration.getPos() == attrLoc.getStartOffset()
          && fieldDeclaration.getEndPos() == attrLoc.getEndOffset()) {
        result = attribute;
        break;
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

  private record ImportTree(Tree tree, NodeType type, ASTDiff diff, String path) {}

  private record TreeLocation(SrcDst srcDst, String path) {}

  private record NodeTrees(Node node, Set<Tree> trees) {}
}