package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.*;
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
  private final Map<String, Node> idNodeMap = new HashMap<>();
  private final Map<String, Node> promptIdNodeMap = new HashMap<>();
  private final UMLModelDiff modelDiff;
  private final UMLsGenerator umlsGenerator;
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
    this.umlsGenerator = new UMLsGenerator(modelDiff, srcContents, dstContents);
    this.srcContents = srcContents;
    this.dstContents = dstContents;
    this.srcContexts = srcContexts;
    this.dstContexts = dstContexts;
  }

  private String getFileContent(SrcDst srcDst, String path) {
    return (srcDst.equals(SrcDst.SRC) ? srcContents : dstContents).get(path);
  }

  public void importFiles(List<Entry<String, TreeContext>> deletedFiles,
      List<Entry<String, TreeContext>> addedFiles) {
    for (Entry<String, TreeContext> deletedFile : deletedFiles) {
      String path = deletedFile.getKey();
      srcTrees.addAll(getValidTrees(path, deletedFile.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.DELETION, null, path, SrcDst.SRC)).toList());
    }
    for (Entry<String, TreeContext> addedFile : addedFiles) {
      String path = addedFile.getKey();
      dstTrees.addAll(getValidTrees(path, addedFile.getValue().getRoot().getChildren()).stream()
              .map(tree -> new ImportTree(tree, NodeType.ADDITION, null, path, SrcDst.DST)).toList());
    }
  }

  public void importDiff(ASTDiff diff) {
    TreeClassifier classifier = diff.createRootNodesClassifier();
    String srcPath = diff.getSrcPath();
    String dstPath = diff.getDstPath();

    srcTrees.addAll(getValidTrees(srcPath, classifier.getMovedSrcs()).stream()
            .map(tree -> new ImportTree(tree, NodeType.SRC_MOVE, diff, srcPath, SrcDst.SRC)).toList());
    if (srcPath.equals(dstPath)) {
      srcTrees.addAll(getValidTrees(srcPath, classifier.getDeletedSrcs()).stream()
          .map(tree -> new ImportTree(tree, NodeType.DELETION, diff, srcPath, SrcDst.SRC)).toList());
      srcTrees.addAll(getValidTrees(srcPath, classifier.getUpdatedSrcs()).stream()
          .map(tree -> new ImportTree(tree, NodeType.SRC_UPDATE, diff, srcPath, SrcDst.SRC)).toList());
    }

    dstTrees.addAll(getValidTrees(dstPath, classifier.getMovedDsts()).stream()
            .map(tree -> new ImportTree(tree, NodeType.DST_MOVE, diff, dstPath, SrcDst.DST)).toList());
    if (srcPath.equals(dstPath)) {
      dstTrees.addAll(getValidTrees(dstPath, classifier.getInsertedDsts()).stream()
          .map(tree -> new ImportTree(tree, NodeType.ADDITION, diff, dstPath, SrcDst.DST)).toList());
      dstTrees.addAll(getValidTrees(dstPath, classifier.getUpdatedDsts()).stream()
          .map(tree -> new ImportTree(tree, NodeType.DST_UPDATE, diff, dstPath, SrcDst.DST)).toList());
    }
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

  private void importTrees(HashMap<ImportTree, Set<ImportTree>> trees) {
    trees.entrySet().stream().map(entry -> {
      ImportTree parent = entry.getKey();
      Set<ImportTree> subs = entry.getValue();
      String fileContent = getFileContent(parent.srcDst, parent.path);
      Set<Node> subsNode = subs.stream().map(sub -> {
        Node subNode = new Node(fileContent, sub.path, parent.srcDst, sub.tree, null, sub.type, null);
        subNode.addDiff(sub.diff);
        return subNode;
      }).collect(Collectors.toSet());

      Node parentNode = new Node(fileContent, parent.path, parent.srcDst, parent.tree, subsNode, parent.type, promptIdNodeMap);
      parentNode.addDiff(parent.diff);

      return parentNode;
    }).forEach(this::addNode);
  }

  private Node addExtensionNode(Tree extensionTree, Node extendedNode) {
    List<Tree> extensionParents = extensionTree.getParents();
    Tree extensionRoot = extensionParents.get(extensionParents.size() - 1);
    String path = (extendedNode.isSrc() ? srcContexts : dstContexts).entrySet().stream()
            .filter(e -> e.getValue().getRoot().equals(extensionRoot)).findFirst().get().getKey();

    Node node = new Node(getFileContent(extendedNode.getSrcDst(), path), path, extendedNode.getSrcDst(),
            extensionTree, null, NodeType.EXTENSION, promptIdNodeMap);
    node.addDiffs(extendedNode.getDiffs());
    return addNode(node);
  }

  private Node addNode(Node node) {
    if (idNodeMap.containsKey(node.getId())) {
      Node existingNode = idNodeMap.get(node.getId());

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
    idNodeMap.put(node.getId(), node);
    promptIdNodeMap.put(node.getPromptId(), node);
    node.setUMLs(umlsGenerator.getUMLs(node.getTree(), node.getSrcDst(), node.getPath(), false));

    addNodeContexts(node);

    return node;
  }

  private void addNodeContexts(Node node) {
    String path = node.getPath();
    SrcDst srcDst = node.getSrcDst();

    List<Pair<Tree, NodeType>> contexts = Context.get(node.getPath(), node.getTree());
    for (Pair<Tree, NodeType> context : contexts) {
      String potentialContextId = Node.formatId(path, srcDst, context.second, context.first);

      if (!idNodeMap.containsKey(potentialContextId)) {
        Node contextNode = new Node(node.getFileContent(), path, srcDst, context.first, null, context.second, promptIdNodeMap);
        graph.addVertex(contextNode);
        idNodeMap.put(contextNode.getId(), contextNode);
        promptIdNodeMap.put(contextNode.getPromptId(), contextNode);
      }

      Node contextNode = idNodeMap.get(potentialContextId);
      contextNode.addDiffs(node.getDiffs());
      contextNode.setUMLs(umlsGenerator.getUMLs(contextNode.getTree(), contextNode.getSrcDst(), contextNode.getPath(), true));

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
        defUseTargets.addAll(getInvocationNodes(operation, node.getSrcDst()));
      }

      for (UMLAttribute attribute : umls.umlAttributes) {
        node.addIdentifier(attribute.getVariableDeclaration().getVariableName());
        defUseTargets.addAll(findAccessNodes(attribute.getName(),
                node.isSrc() ? modelDiff.findFieldAccessesInParentModel(attribute)
                : modelDiff.findFieldAccessesInChildModel(attribute), node.getSrcDst()));
      }

      for (VariableDeclaration variableDeclaration : umls.variableDeclarations) {
        node.addIdentifier(variableDeclaration.getVariableName());
        defUseTargets.addAll(findAccessNodes(variableDeclaration.getVariableName(),
                variableDeclaration.getScope().getStatementsInScopeUsingVariable(), node.getSrcDst()));
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
    Tree usedTree = TreeUtilFunctions.findByLocationInfo(usedRootTree, usedLocation, new Constants(usedLocation.getFilePath()));
    if (treeTransformer != null) {
      usedTree = treeTransformer.apply(usedTree);
    }

    for (Node isolatedUseNode : isolatedUseNodes) {
      Node usedExtensionNode = addExtensionNode(usedTree, isolatedUseNode);

      nodesExtensions.putIfAbsent(isolatedUseNode, new HashSet<>());
      nodesExtensions.get(isolatedUseNode).add(usedExtensionNode);
    }
  }

  private Set<Node> getArgumentNodes(UMLOperation umlOperation, VariableDeclaration parameterDeclaration, SrcDst srcDst) {
    Set<Node> result = new HashSet<>();

    int parameterIndex = umlOperation.getParameterDeclarationList().indexOf(parameterDeclaration);

    List<AbstractCall> invocations = srcDst.equals(SrcDst.SRC) ? modelDiff.findInvocationsInParentModel(umlOperation)
            : modelDiff.findInvocationsInChildModel(umlOperation);
    for (AbstractCall invocation : invocations) {
      // Passing callback through method reference does not have arguments
      if (parameterIndex >= invocation.arguments().size()) {
        continue;
      }

      LocationInfo invocationLocation = invocation.getLocationInfo();

      String invocationFileContent = srcDst.equals(SrcDst.SRC) ? srcContents.get(invocationLocation.getFilePath()) : dstContents.get(invocationLocation.getFilePath());
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
      result.addAll(findOverlappingNodes(invocationLocation.getFilePath(), srcDst, startOffset, endOffset,
              (n) -> !n.isContext() && !n.isExtension()));
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

  private record ImportTree(Tree tree, NodeType type, ASTDiff diff, String path, SrcDst srcDst) {}

  private record NodeTrees(Node node, Set<Tree> trees) {}
}