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
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import narrator.apted.costmodel.StringUnitCostModel;
import narrator.apted.distance.APTED;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class HunkNetwork {

    private final Graph<Node, Edge> graph;
    private final HashMap<String, Node> nodeMap = new HashMap<>();
    private final UMLModelDiff modelDiff;
    private final APTED editDistance = new APTED(new StringUnitCostModel());
    private final float distanceThreshold = 0;
    private final Map<String, String> srcContents;
    private final Map<String, String> dstContents;
    private final Set<String> invalidTypes = new HashSet<>() {{
        add(Constants.get().EMPTY_STATEMENT);
        add(Constants.get().IMPORT_DECLARATION);
        add(Constants.get().PACKAGE_DECLARATION);
    }};
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
        Tree root = TreeUtilFunctions.getParentUntilType(
                tree, Constants.get().COMPILATION_UNIT);
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

    private String getContent(SrcDst srcDst, String path) {
        return (srcDst.equals(SrcDst.SRC) ? srcContents : dstContents).get(path);
    }

    public void importDiff(ASTDiff diff) {
        TreeClassifier classifier = diff.createRootNodesClassifier();
        importTrees(aggregateTrees(getValidTrees(classifier.getDeletedSrcs())),
                classifier.getMovedSrcs(), SrcDst.SRC, NodeType.DELETION, NodeType.SRC_MOVE, diff);
        importTrees(aggregateTrees(getValidTrees(classifier.getInsertedDsts())),
                classifier.getMovedDsts(), SrcDst.DST, NodeType.ADDITION, NodeType.DST_MOVE, diff);
    }

    private Set<Tree> getValidTrees(Set<Tree> trees) {
        return trees.stream()
                .filter(addition -> !this.invalidTypes.contains(addition.getType().name)).collect(
                        Collectors.toSet());
    }

    private HashMap<Tree, Set<Tree>> aggregateTrees(Set<Tree> trees) {
        HashMap<Tree, Set<Tree>> result = new HashMap<>();

        for (Tree subject : trees) {
            boolean isParent = true;

            for (Tree object : trees) {
                if (subject.equals(object)) {
                    continue;
                }

                if (object.getPos() <= subject.getPos()
                        && subject.getEndPos() <= object.getEndPos()) {
                    isParent = false;
                    break;
                }
            }

            if (isParent) {
                result.put(subject, new HashSet<>());
            }
        }
        for (Tree parent : result.keySet()) {
            for (Tree addition : trees) {
                if (parent.equals(addition)) {
                    continue;
                }

                if (parent.getPos() <= addition.getPos()
                        && addition.getEndPos() <= parent.getEndPos()) {
                    result.get(parent).add(addition);
                }
            }
        }

        return result;
    }

    private void importTrees(HashMap<Tree, Set<Tree>> trees, Set<Tree> allMoves, SrcDst srcDst,
            NodeType baseType, NodeType moveType, ASTDiff diff) {
        Set<Tree> parentTrees = trees.keySet();

        HashMap<Tree, Set<Tree>> treesMoves = new HashMap<>();
        for (Tree parentTree : parentTrees) {
            Set<Tree> treeMoves = new HashSet<>();
            allMoves.stream().filter(move -> parentTree.getPos() <= move.getPos()
                    && move.getEndPos() <= parentTree.getEndPos()).forEach(treeMoves::add);

            treesMoves.put(parentTree, treeMoves);
        }
        trees.entrySet().stream().map(entry -> {
            Tree tree = entry.getKey();
            Set<Tree> subTrees = entry.getValue();
            Set<Tree> moveTrees = treesMoves.get(tree);
            Pair<SrcDst, String> treeLocation = localizeTree(tree);
            return new Node(getContent(treeLocation.first, treeLocation.second),
                    treeLocation.second, srcDst, tree, subTrees.isEmpty() ? null : subTrees,
                    moveTrees.isEmpty() ? null : moveTrees, baseType, diff);
        }).forEach(this::addNode);

        List<Tree> pureMoves = allMoves.stream().filter(move -> parentTrees.stream()
                .noneMatch(parentTree -> parentTree.getPos() <= move.getPos()
                        && move.getEndPos() <= parentTree.getEndPos())).toList();
        pureMoves.stream().map(tree -> {
            Pair<SrcDst, String> treeLocation = localizeTree(tree);
            return new Node(getContent(treeLocation.first, treeLocation.second),
                    treeLocation.second, srcDst, tree, null, null, moveType, diff);
        }).forEach(this::addNode);
    }

    private Node addExtensionNode(Tree extensionTree, Node extendedNode) {
        Pair<SrcDst, String> treeLocation = localizeTree(extensionTree);
        String path = treeLocation.second;

        String potentialNodeId = Node.formatId(path, treeLocation.first, NodeType.EXTENSION,
                extensionTree);
        if (nodeMap.containsKey(potentialNodeId)) {
            return nodeMap.get(potentialNodeId);
        }

        Node node = new Node(getContent(treeLocation.first, treeLocation.second), path,
                treeLocation.first, extensionTree, null, null, NodeType.EXTENSION,
                extendedNode.getDiff());
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
        List<Pair<Tree, NodeType>> contexts = Context.get(node.getTree());
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
        processAddedDeletedFiles();

        processMoves();
        processDefUse();
        // TODO: how does it contribute to usage pattern if it is connected to a context class?
        processClassInstanceCreations();
        // DEPRECATED
//        processSimilarity();
        processSuccession();
        processMapping();
    }

    // TODO: find a diff-based import of added and deleted files
    private void processAddedDeletedFiles() {
        List<ASTDiff> importedDiffs = graph.vertexSet().stream().map(Node::getDiff).toList();
        Set<String> srcFiles = importedDiffs.stream().map(ASTDiff::getSrcPath)
                .collect(Collectors.toSet());
        List<String> deletedFiles = srcContexts.keySet().stream()
                .filter(path -> !srcFiles.contains(path))
//                .map(path -> srcContexts.get(path).getRoot())
                .toList();
        Set<String> dstFiles = importedDiffs.stream().map(ASTDiff::getDstPath)
                .collect(Collectors.toSet());
        List<String> addedFiles = dstContexts.keySet().stream()
                .filter(path -> !dstFiles.contains(path))
//                .map(path -> dstContexts.get(path).getRoot())
                .toList();
        System.out.println(addedFiles);
        System.out.println(deletedFiles);
    }

    private void processMoves() {
        List<Node> nodes = new ArrayList<>(graph.vertexSet());
        List<Node> moveIncludingNodes = nodes.stream().filter(node -> node.getMoveTrees() != null)
                .toList();
        List<Node> additionNodes = moveIncludingNodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.ADDITION)).toList();
        List<Node> dstMoveNodes = nodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.DST_MOVE)).toList();
        List<Node> deletionNodes = moveIncludingNodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.DELETION)).toList();
        List<Node> srcMoveNodes = nodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.SRC_MOVE)).toList();

        for (Node deletionNode : deletionNodes) {
            MappingStore mappingStore = deletionNode.getDiff().getAllMappings()
                    .getMonoMappingStore();
            List<Tree> deletionMovesDsts = deletionNode.getMoveTrees().stream()
                    .map(mappingStore::getDstForSrc).toList();

            for (Node additionNode : additionNodes) {
                if (additionNode.getMoveTrees().stream().anyMatch(deletionMovesDsts::contains)) {
                    addEdge(deletionNode, additionNode, EdgeType.MAPPING);
                }
            }

            for (Node dstMoveNode : dstMoveNodes) {
                if (deletionMovesDsts.contains(dstMoveNode.getTree())) {
                    addEdge(deletionNode, dstMoveNode, EdgeType.MAPPING);
                }
            }
        }

        for (Node srcMoveNode : srcMoveNodes) {
            MappingStore mappingStore = srcMoveNode.getDiff().getAllMappings()
                    .getMonoMappingStore();
            Tree dst = mappingStore.getDstForSrc(srcMoveNode.getTree());

            for (Node additionNode : additionNodes) {
                if (additionNode.getMoveTrees().contains(dst)) {
                    addEdge(srcMoveNode, additionNode, EdgeType.MAPPING);
                }
            }

            for (Node dstMoveNode : dstMoveNodes) {
                if (dst.equals(dstMoveNode.getTree())) {
                    addEdge(srcMoveNode, dstMoveNode, EdgeType.MAPPING);
                }
            }
        }

    }

    private void processDefUse() {
        List<Node> nodes = new ArrayList<>(graph.vertexSet());
        for (Node node : nodes) {
            if (node.isContext()) {
                continue;
            }

            Tree tree = node.getTree();

            List<Tree> subTrees = new ArrayList<>(tree.getDescendants());
            subTrees.add(tree);

            List<Tree> methodDeclarations = new ArrayList<>();
            List<Tree> fieldDeclarations = new ArrayList<>();
            List<Tree> parameterDeclarations = new ArrayList<>();
            List<Tree> variableDeclarations = new ArrayList<>();
            for (Tree subTree : subTrees) {
                String subTreeType = subTree.getType().name;
                if (subTreeType.equals(Constants.get().METHOD_DECLARATION)) {
                    methodDeclarations.add(subTree);
                }
                if (subTreeType.equals(Constants.get().FIELD_DECLARATION)) {
                    fieldDeclarations.add(subTree);
                }
                // TODO: investigation: it is only for "Enhanced For" and other parameters will be in variable declarations array
                if (subTreeType.equals(Constants.get().RECORD_COMPONENT)) {
                    parameterDeclarations.add(subTree);
                }
                if (subTreeType.equals(Constants.get().VARIABLE_DECLARATION_STATEMENT)) {
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
                UMLAttribute umlAttribute = findUMLAttribute(node, fieldDeclaration);
                if (umlAttribute == null) {
                    continue;
                }

                node.addIdentifier(umlAttribute.getVariableDeclaration().getVariableName());

                List<Node> useNodes = findAccessNodes(umlAttribute.getLocationInfo(),
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
                        variableDeclaration,
                        Constants.get().VARIABLE_DECLARATION_FRAGMENT);
                if (variableDeclarationFragment == null) {
                    continue;
                }

                addVariableDeclarationEdges(node, variableDeclarationFragment);
            }
        }
    }

    private void processClassInstanceCreations() {
        for (Node node : graph.vertexSet()) {
            if (node.isContext()) {
                continue;
            }

            Tree tree = node.getTree();
            List<Tree> subTrees = new ArrayList<>(tree.getDescendants());
            subTrees.add(tree);

            List<Tree> classInstanceCreations =
                    subTrees.stream().filter(subTree -> subTree.getType().name.equals(
                            Constants.get().CLASS_INSTANCE_CREATION)).toList();
            for (Tree classInstanceCreation : classInstanceCreations) {
                Tree classType = TreeUtilFunctions.findChildByType(classInstanceCreation,
                        Constants.get().SIMPLE_TYPE);
                if (classType == null) {
                    continue;
                }
                Tree className = TreeUtilFunctions.findChildByType(classType,
                        Constants.get().SIMPLE_NAME);
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
                        classDeclarationLocation.getFilePath(),
                        classDeclarationLocation.getStartOffset(),
                        classDeclarationLocation.getEndOffset(),
                        n -> localizeTree(n.getTree()).first.equals(localizeTree(
                                node.getTree()).first));
                for (Node classNode : classDeclarationNodes) {
                    if (classNode.equals(node) || classNode.getNodeType()
                            .equals(NodeType.SEMANTIC_CONTEXT) || classNode.getNodeType()
                            .equals(NodeType.LOCATION_CONTEXT)) {
                        continue;
                    }

                    addEdge(classNode, node, EdgeType.DEF_USE);
                    classNode.addIdentifier(instantiatedClass.get().getNonQualifiedName());
                }
            }
        }
    }

    private UMLClass getUMLClass(Node node, Tree tree) {
        Tree hunkParentType = TreeUtilFunctions.getParentUntilType(tree,
                Constants.get().TYPE_DECLARATION);
        if (hunkParentType == null) { // there is no need to process def-use when it is out of type
            return null;
        }
        Tree parentTypeName = TreeUtilFunctions.findChildByType(hunkParentType,
                Constants.get().SIMPLE_NAME);
        if (parentTypeName == null) {
            return null;
        }

        return getUMLClass(localizeTree(tree).second, parentTypeName.getLabel(),
                node.isSrc() ? modelDiff.getParentModel() : modelDiff.getChildModel());
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
        Tree methodRoot = TreeUtilFunctions.getParentUntilType(variableDeclarationTree,
                Constants.get().METHOD_DECLARATION);
        // https://github.com/elastic/elasticsearch/commit/e0a458441cff9a4242cd93f4c02f06d72f2d63c4#diff-9b7bef16de393901cd8c75e73d2fb03afb90a10f1de191b7174275bbd8e71bd8L38
        if (methodRoot == null) {
            return;
        }

        UMLClass umlClass = getUMLClass(node, variableDeclarationTree);
        if (umlClass == null) {
            return;
        }

        UMLOperation umlOperation = findUMLOperation(umlClass, methodRoot);
        if (umlOperation == null) {
            return;
        }

        for (VariableDeclaration operationVariable : umlOperation.getAllVariableDeclarations()) {
            LocationInfo operationVariableLoc = operationVariable.getLocationInfo();
            if (operationVariableLoc.getStartOffset() == variableDeclarationTree.getPos()
                    && variableDeclarationTree.getEndPos() == operationVariableLoc.getEndOffset()) {
                VariableDeclaration variableDeclaration = operationVariable.getVariableDeclaration();

                node.addIdentifier(variableDeclaration.getVariableName());

                List<Node> useNodes = findAccessNodes(variableDeclaration.getLocationInfo(),
                        variableDeclaration.getScope().getStatementsInScopeUsingVariable(),
                        node.isSrc() ? srcContexts : dstContexts, node);
                for (Node useNode : useNodes) {
                    addEdge(node, useNode, EdgeType.DEF_USE);
                }

                AbstractExpression variableInitStatement = operationVariable.getInitializer();
                if (variableInitStatement == null) {
                    continue;
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
                    Tree tree = TreeUtilFunctions.findByLocationInfo(
                            (node.isSrc() ? srcContexts : dstContexts).get(path).getRoot(),
                            locationInfo);
                    if (nodeMap.containsKey(
                            Node.formatId(path, node.getSrcDst(), NodeType.EXTENSION, tree))) {
                        continue;
                    }

                    Node declarationNode = addExtensionNode(tree, node);
                    addEdge(declarationNode, node, EdgeType.DEF_USE);
                }
            }
        }
    }

    private void addMethodInvocationEdges(Node node, Tree methodDeclaration) {
        UMLClass umlClass = getUMLClass(node, methodDeclaration);
        if (umlClass == null) {
            return;
        }

        UMLOperation operation = findUMLOperation(umlClass, methodDeclaration);
        if (operation == null) {
            return;
        }

        node.addIdentifier(operation.getName());

        List<Node> invocationNodes = findInvocationNodes(node, operation);
        for (Node invocationNode : invocationNodes) {
            addEdge(node, invocationNode, EdgeType.DEF_USE);
        }
    }

    private List<Node> findAccessNodes(LocationInfo declarationLocation,
            Set<AbstractCodeFragment> accessFragments, Map<String, TreeContext> contexts,
            Node node) {
        ArrayList<Node> result = new ArrayList<>();

        for (AbstractCodeFragment accessFragment : accessFragments) {
            // TODO: only the variable itself or fragment entirely?
            // List<LeafExpression> useVariables =
            //         accessFragment.getVariables().stream().filter(variable -> variable.getString()
            //         .equals(variableName)).toList();
            LocationInfo useLoc = accessFragment.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(useLoc.getFilePath(),
                    useLoc.getStartOffset(), useLoc.getEndOffset(),
                    n -> !n.isContext() && localizeTree(n.getTree()).first.equals(localizeTree(
                            node.getTree()).first));
            result.addAll(overlappingNodes);
        }

        // declaration change without any usage change
        if (result.isEmpty()) {
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
                    closestLocationInfo);
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

    private List<Node> findInvocationNodes(Node node, UMLOperation operation) {
        List<AbstractCall> invocations =
                node.isSrc() ? modelDiff.findInvocationsInParentModel(operation)
                        : modelDiff.findInvocationsInChildModel(operation);

        List<Node> result = new ArrayList<>();
        for (AbstractCall invocation : invocations) {
            LocationInfo invocationLocationInfo = invocation.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(invocationLocationInfo.getFilePath(),
                    invocationLocationInfo.getStartOffset(), invocationLocationInfo.getEndOffset(),
                    n -> !n.isContext() & localizeTree(n.getTree()).first.equals(localizeTree(
                            node.getTree()).first));
            result.addAll(overlappingNodes);
        }

        return result;
    }

    private List<Node> findOverlappingNodes(String path, int pos, int endPos,
            Predicate<Node> filter) {
        List<Node> result = new ArrayList<>();

        for (Node node : graph.vertexSet()) {
            Tree tree = node.getTree();
            if (!(node.getPath().equals(path) && tree.getPos() <= endPos && pos <= tree.getEndPos())
                    || (filter != null && !filter.test(node))) {
                continue;
            }

            result.add(node);
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

    private UMLAttribute findUMLAttribute(Node node, Tree fieldDeclaration) {
        UMLClass umlClass = getUMLClass(node, fieldDeclaration);
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
        for (Node subject : graph.vertexSet()) {
            Tree rightSibling = subject.getRight();
            if (rightSibling == null) {
                continue;
            }

            for (Node object : graph.vertexSet()) {
                if (object.getTree().equals(rightSibling)) {
                    addEdge(subject, object, EdgeType.SUCCESSION);
                }
            }
        }
    }

    // TODO: if only exact matches are considered, much more simpler algorithm can be used
    private void processSimilarity() {
        Set<Node> nodes = graph.vertexSet();
        for (Node subject : nodes) {
            if (subject.isContext()) {
                continue;
            }

            for (Node object : nodes) {
                if (object.isContext() || subject.equals(object)) {
                    continue;
                }

                float distance = editDistance.computeEditDistance(subject.getTree(),
                        object.getTree());
                if (distance <= distanceThreshold) {
                    addEdge(subject, object, EdgeType.SIMILARITY);
                }
            }
        }
    }

    // TODO: pure moves can be handled here
    public void processMapping() {
        Set<Node> nodes = graph.vertexSet();
        for (Node subject : nodes) {
            Tree tree = subject.getTree();
            MappingStore mappingStore = subject.getDiff().getAllMappings().getMonoMappingStore();

            Tree dst = mappingStore.getDstForSrc(tree);
            if (dst == null) {
                continue;
            }

            // TODO: should be equal to the root tree or any descendant trees?
            List<Node> dstNodes = nodes.stream().filter(node -> node.getTree().equals(dst))
                    .toList();
            if (subject.isContext()) {
                dstNodes = dstNodes.stream()
                        .filter(node -> node.getNodeType().equals(subject.getNodeType())).toList();
            }
            for (Node dstNode : dstNodes) {
                addEdge(subject, dstNode, EdgeType.MAPPING);
            }
        }
    }
}