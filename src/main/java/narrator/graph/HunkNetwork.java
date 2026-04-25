package narrator.graph;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    private Pair<SrcDst, String> localizeTree(Tree tree) {
        List<Tree> parents = tree.getParents();
        Tree root = parents.get(parents.size() - 1);
        return findContext(srcContexts, root, SrcDst.SRC)
                .or(() -> findContext(dstContexts, root, SrcDst.DST))
                .orElse(null);
    }

    private Optional<Pair<SrcDst, String>> findContext(
            Map<String, TreeContext> contexts,
            Tree root,
            SrcDst srcDst) {
        return contexts.entrySet().stream()
                .filter(e -> e.getValue().getRoot().equals(root))
                .map(e -> new Pair<>(srcDst, e.getKey()))
                .findFirst();
    }

    private String getFileContent(SrcDst srcDst, String path) {
        return (srcDst.equals(SrcDst.SRC) ? srcContents : dstContents).get(path);
    }

    public void importFiles(List<Entry<String, TreeContext>> deletedFiles,
            List<Entry<String, TreeContext>> addedFiles) {
        if (deletedFiles.isEmpty() && addedFiles.isEmpty()) {
            return;
        }

        for (Entry<String, TreeContext> deletedFile : deletedFiles) {
            List<Pair<Tree, NodeType>> srcTrees = getValidTrees(deletedFile.getKey(),
                    deletedFile.getValue().getRoot().getChildren()).stream()
                    .map(tree -> new Pair<>(tree, NodeType.DELETION)).toList();
            importTrees(aggregateTrees(srcTrees), SrcDst.SRC, null);
        }
        for (Entry<String, TreeContext> addedFile : addedFiles) {
            List<Pair<Tree, NodeType>> dstTrees = getValidTrees(addedFile.getKey(),
                    addedFile.getValue().getRoot().getChildren()).stream()
                    .map(tree -> new Pair<>(tree, NodeType.ADDITION)).toList();
            importTrees(aggregateTrees(dstTrees), SrcDst.DST, null);
        }
    }

    public void importDiff(ASTDiff diff) {
        TreeClassifier classifier = diff.createRootNodesClassifier();

        Set<Pair<Tree, NodeType>> srcTrees = new HashSet<>();
        srcTrees.addAll(getValidTrees(diff.getSrcPath(), classifier.getDeletedSrcs()).stream()
                .map(tree -> new Pair<>(tree, NodeType.DELETION)).toList());
        srcTrees.addAll(getValidTrees(diff.getSrcPath(), classifier.getMovedSrcs()).stream()
                .map(tree -> new Pair<>(tree, NodeType.SRC_MOVE)).toList());
        srcTrees.addAll(getValidTrees(diff.getSrcPath(), classifier.getUpdatedSrcs()).stream()
                .map(tree -> new Pair<>(tree, NodeType.SRC_UPDATE)).toList());
        importTrees(aggregateTrees(srcTrees), SrcDst.SRC, diff);

        Set<Pair<Tree, NodeType>> dstTrees = new HashSet<>();
        dstTrees.addAll(getValidTrees(diff.getDstPath(), classifier.getInsertedDsts()).stream()
                .map(tree -> new Pair<>(tree, NodeType.ADDITION)).toList());
        dstTrees.addAll(getValidTrees(diff.getDstPath(), classifier.getMovedDsts()).stream()
                .map(tree -> new Pair<>(tree, NodeType.DST_MOVE)).toList());
        dstTrees.addAll(getValidTrees(diff.getDstPath(), classifier.getUpdatedDsts()).stream()
                .map(tree -> new Pair<>(tree, NodeType.DST_UPDATE)).toList());
        importTrees(aggregateTrees(dstTrees), SrcDst.DST, diff);
    }

    private Set<Tree> getValidTrees(String path, Collection<Tree> trees) {
        Constants constants = new Constants(path);
        Set<String> invalidTypes = new HashSet<>() {{
            add(constants.EMPTY_STATEMENT);
            add(constants.IMPORT_DECLARATION);
            add(constants.PACKAGE_DECLARATION);
        }};

        return trees.stream()
                .filter(addition -> !invalidTypes.contains(addition.getType().name)).collect(
                        Collectors.toSet());
    }

    private HashMap<Pair<Tree, NodeType>, Set<Tree>> aggregateTrees(
            Collection<Pair<Tree, NodeType>> trees) {
        HashMap<Pair<Tree, NodeType>, Set<Tree>> result = new HashMap<>();

        for (Pair<Tree, NodeType> subject : trees) {
            boolean isParent = true;

            for (Pair<Tree, NodeType> object : trees) {
                if (subject.equals(object)) {
                    continue;
                }

                if (object.first.getPos() <= subject.first.getPos()
                        && subject.first.getEndPos() <= object.first.getEndPos()) {
                    isParent = false;
                    break;
                }
            }

            if (isParent) {
                result.put(subject, new HashSet<>());
            }
        }
        for (Pair<Tree, NodeType> parent : result.keySet()) {
            for (Pair<Tree, NodeType> addition : trees) {
                if (parent.equals(addition)) {
                    continue;
                }

                if (parent.first.getPos() <= addition.first.getPos()
                        && addition.first.getEndPos() <= parent.first.getEndPos()) {
                    result.get(parent).add(addition.first);
                }
            }
        }

        return result;
    }

    private void importTrees(HashMap<Pair<Tree, NodeType>, Set<Tree>> trees, SrcDst srcDst,
            ASTDiff diff) {
        trees.entrySet().stream().map(entry -> {
            Tree tree = entry.getKey().first;
            Set<Tree> subTrees = entry.getValue();
            Pair<SrcDst, String> treeLocation = localizeTree(tree);
            return new Node(getFileContent(treeLocation.first, treeLocation.second),
                    treeLocation.second, srcDst, tree, subTrees.isEmpty() ? null : subTrees,
                    null, entry.getKey().second, diff);
        }).forEach(this::addNode);
    }

    private Node addExtensionNode(Tree extensionTree, Node extendedNode) {
        Pair<SrcDst, String> treeLocation = localizeTree(extensionTree);
        Node node = new Node(getFileContent(treeLocation.first, treeLocation.second),
                treeLocation.second, treeLocation.first, extensionTree, null, null,
                NodeType.EXTENSION, extendedNode.getDiff());
        return addNode(node);
    }

    private Node addNode(Node node) {
        if (nodeMap.containsKey(node.getId())) {
            return nodeMap.get(node.getId());
        }

        graph.addVertex(node);
        nodeMap.put(node.getId(), node);

        addNodeContext(node);

        return node;
    }

    private void addNodeContext(Node node) {
        String path = node.getPath();
        SrcDst srcDst = node.getSrcDst();

        Node lastNode = node;
        List<Pair<Tree, NodeType>> contexts = Context.get(node.getPath(), node.getTree());
        for (Pair<Tree, NodeType> context : contexts) {
            String potentialContextId = Node.formatId(path, srcDst, context.second, context.first);
            if (!nodeMap.containsKey(potentialContextId)) {
                Node contextNode = new Node(node.getFileContent(), path, srcDst, context.first,
                        null, null, context.second, node.getDiff());
                graph.addVertex(contextNode);
                nodeMap.put(contextNode.getId(), contextNode);
            }

            Node contextNode = nodeMap.get(potentialContextId);
            if (graph.getEdge(lastNode, contextNode) != null) {
                break;
            }
            graph.addEdge(lastNode, contextNode, new Edge(EdgeType.CONTEXT));

            lastNode = contextNode;
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
        processMapping();
        processDefUse();
        processClassInstanceCreations();
        processSuccession();

        System.out.println(graph.vertexSet().size() + "-" + graph.edgeSet().size());
    }

    private void processMapping() {
        List<Node> nodes = graph.vertexSet().stream().toList();
        List<Node> srcNodes = nodes.stream().filter(Node::isSrc).toList();
        List<Node> dstNodes = nodes.stream().filter(Node::isDst).toList();

        for (Node srcNode : srcNodes) {
            ASTDiff diff = srcNode.getDiff();
            if (diff == null) {
                continue;
            }
            MappingStore mappingStore = diff.getAllMappings().getMonoMappingStore();

            Set<Tree> srcTrees = new HashSet<>();
            srcTrees.add(srcNode.getTree());
            if (srcNode.getSubTrees() != null) {
                srcTrees.addAll(srcNode.getSubTrees());
            }
            List<Tree> dstTrees = srcTrees.stream().map(mappingStore::getDstForSrc)
                    .filter(Objects::nonNull).toList();

            List<Node> mappedDstNodes = dstNodes.stream().filter(dstNode -> {
                Set<Tree> dstNodeTrees = new HashSet<>();
                dstNodeTrees.add(dstNode.getTree());
                if (dstNode.getSubTrees() != null) {
                    dstNodeTrees.addAll(dstNode.getSubTrees());
                }

                return dstNodeTrees.stream().anyMatch(dstTrees::contains);
            }).toList();
            if (srcNode.getNodeType().equals(NodeType.SEMANTIC_CONTEXT) || srcNode.getNodeType()
                    .equals(NodeType.LOCATION_CONTEXT)) {
                mappedDstNodes = mappedDstNodes.stream().filter(dstNode -> {
                    if (!dstNode.getNodeType().equals(NodeType.SEMANTIC_CONTEXT)
                            && !dstNode.getNodeType().equals(NodeType.LOCATION_CONTEXT)) {
                        return true;
                    }
                    return srcNode.getNodeType().equals(dstNode.getNodeType());
                }).toList();
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
                addMethodInvocationEdges(node, methodDeclaration);
            }

            for (Tree fieldDeclaration : fieldDeclarations) {
                UMLAttribute umlAttribute = findUMLAttribute(node.getPath(), fieldDeclaration,
                        node.getSrcDst());
                if (umlAttribute == null) {
                    continue;
                }

                node.addIdentifier(umlAttribute.getVariableDeclaration().getVariableName());

                List<Node> useNodes = findAccessNodes(umlAttribute.getName(),
                        umlAttribute.getLocationInfo(),
                        node.isSrc()
                                ? modelDiff.findFieldAccessesInParentModel(umlAttribute)
                                : modelDiff.findFieldAccessesInChildModel(umlAttribute),
                        node.isSrc() ? srcContexts : dstContexts,
                        node);
                for (Node useNode : useNodes) {
                    addEdge(node, useNode, EdgeType.DEF_USE);
                }
            }

            for (Tree parameterDeclaration : parameterDeclarations) {
                addVariableDeclarationEdges(node, parameterDeclaration);
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

    private void processClassInstanceCreations() {
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
                Tree classType = TreeUtilFunctions.findChildByType(classInstanceCreation,
                        constants.SIMPLE_TYPE);
                if (classType == null) {
                    continue;
                }
                Tree className = TreeUtilFunctions.findChildByType(classType,
                        constants.SIMPLE_NAME);
                if (className == null) {
                    continue;
                }

                Optional<UMLClass> instantiatedClass = (node.isSrc() ? modelDiff.getParentModel()
                        : modelDiff.getChildModel()).getClassList()
                        .stream()
                        .filter(c -> c.getName().endsWith(className.getLabel()))
                        .findFirst();
                if (instantiatedClass.isEmpty()) {
                    continue;
                }

                LocationInfo classDeclarationLocation = instantiatedClass.get().getLocationInfo();
                List<Node> classDeclarationNodes = findOverlappingNodes(
                        classDeclarationLocation.getFilePath(), node.getSrcDst(),
                        classDeclarationLocation.getStartOffset(),
                        classDeclarationLocation.getEndOffset(),
                        (n) -> !n.isExtension()
                                && n.getTree().getPos() == classDeclarationLocation.getStartOffset()
                                && n.getTree().getEndPos()
                                == classDeclarationLocation.getEndOffset());
                for (Node classNode : classDeclarationNodes) {
                    if (classNode.equals(node)) {
                        continue;
                    }

                    addEdge(classNode, node, EdgeType.DEF_USE);
                    classNode.addIdentifier(instantiatedClass.get().getNonQualifiedName());
                }
            }
        }
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

        return getUMLClass(localizeTree(tree).second, parentTypeName.getLabel(),
                srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel());
    }

    private UMLClass getUMLClass(String path, String typeName, UMLModel umlModel) {
        if (path == null) {
            return null;
        }

        UMLClass UmlClass = null;
        for (UMLClass uc : umlModel.getClassList()) {
            if (uc.getSourceFile().equals(path) && uc.getNonQualifiedName().equals(typeName)) {
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

        List<Node> useNodes = findAccessNodes(variableDeclaration.getVariableName(),
                variableDeclaration.getLocationInfo(),
                variableDeclaration.getScope().getStatementsInScopeUsingVariable(),
                node.isSrc() ? srcContexts : dstContexts, node);
        for (Node useNode : useNodes) {
            addEdge(node, useNode, EdgeType.DEF_USE);
        }

        if (node.isContext()) {
            return;
        }

        AbstractExpression variableInitStatement = variableDeclaration.getInitializer();
        if (variableInitStatement == null) {
            return;
        }

        List<AbstractCall> variableInitInvocations = variableInitStatement.getMethodInvocations();
        for (AbstractCall methodInvocation : variableInitInvocations) {
            UMLOperation declaration =
                    node.isSrc() ? modelDiff.findDeclarationsInParentModel(umlOperation,
                            methodInvocation)
                            : modelDiff.findDeclarationsInChildModel(umlOperation,
                                    methodInvocation);
            if (declaration == null) {
                continue;
            }

            LocationInfo locationInfo = declaration.getLocationInfo();
            String path = locationInfo.getFilePath();

            List<Node> declarationNodes = findOverlappingNodes(path, node.getSrcDst(),
                    locationInfo.getStartOffset(),
                    locationInfo.getEndOffset(), (n) -> !n.isContext() && !n.isExtension());
            // those declaration nodes will establish this relation
            if (!declarationNodes.isEmpty()) {
                continue;
            }

            Tree tree = TreeUtilFunctions.findByLocationInfo(
                    (node.isSrc() ? srcContexts : dstContexts).get(path).getRoot(),
                    locationInfo, new Constants(node.getPath()));
            Node declarationNode = addExtensionNode(tree, node);
            // used extension
            addEdge(declarationNode, node, EdgeType.DEF_USE);
        }
    }

    private void addMethodInvocationEdges(Node node, Tree methodDeclaration) {
        UMLClass umlClass = getUMLClass(node.getPath(), methodDeclaration, node.getSrcDst());
        if (umlClass == null) {
            return;
        }

        UMLOperation operation = findUMLOperation(umlClass, methodDeclaration);
        if (operation == null) {
            return;
        }

        node.addIdentifier(operation.getName());

        List<AbstractCall> invocations =
                node.isSrc() ? modelDiff.findInvocationsInParentModel(operation)
                        : modelDiff.findInvocationsInChildModel(operation);
        List<Node> invocationNodes = new ArrayList<>();
        for (AbstractCall invocation : invocations) {
            LocationInfo invocationLocationInfo = invocation.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(invocationLocationInfo.getFilePath(),
                    node.getSrcDst(), invocationLocationInfo.getStartOffset(),
                    invocationLocationInfo.getEndOffset(), n -> !n.isContext() && !n.isExtension());
            invocationNodes.addAll(overlappingNodes);
        }

        for (Node invocationNode : invocationNodes) {
            addEdge(node, invocationNode, EdgeType.DEF_USE);
        }
    }

    private List<Node> findAccessNodes(String name, LocationInfo declarationLocation,
            Set<AbstractCodeFragment> accessFragments, Map<String, TreeContext> contexts,
            Node node) {
        ArrayList<Node> result = new ArrayList<>();

        for (AbstractCodeFragment accessFragment : accessFragments) {
            List<LeafExpression> useVariables =
                    accessFragment.getVariables().stream().filter(variable -> variable.getString()
                            .contains(name)).toList();

            // TODO: check for potential false negatives
            if (useVariables.isEmpty()) {
                System.out.println(name + "-" + accessFragment.getVariables().stream().map(
                        LeafExpression::getString).toList());
            }

            for (LeafExpression useVariable : useVariables) {
                LocationInfo useLoc = useVariable.getLocationInfo();
                List<Node> overlappingNodes = findOverlappingNodes(useLoc.getFilePath(),
                        node.getSrcDst(), useLoc.getStartOffset(), useLoc.getEndOffset(),
                        n -> !n.isContext() && !n.isExtension());
                result.addAll(overlappingNodes);
            }
        }

        // declaration change without any usage change
        if (result.isEmpty() && !node.isContext()) {
            List<LocationInfo> changedFilesUseStatementsLocation =
                    accessFragments.stream().map(LocationInfoProvider::getLocationInfo)
                            .filter(locationInfo -> contexts.containsKey(
                                    locationInfo.getFilePath())).toList();
            if (changedFilesUseStatementsLocation.isEmpty()) {
                return result;
            }

            LocationInfo closestLocationInfo = findClosestLocationInfo(declarationLocation,
                    changedFilesUseStatementsLocation);
            String path = closestLocationInfo.getFilePath();
            Tree tree = TreeUtilFunctions.findByLocationInfo(contexts.get(path).getRoot(),
                    closestLocationInfo, new Constants(node.getPath()));
            // use extension
            Node extensionNode = addExtensionNode(tree, node);
            result.add(extensionNode);
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
}