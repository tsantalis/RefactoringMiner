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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final HashMap<String, Node> nodeMap;
    private final UMLModelDiff modelDiff;
    private final APTED editDistance = new APTED(new StringUnitCostModel());
    private final float distanceThreshold = 0;
    private final Map<String, String> srcContents;
    private final Map<String, String> dstContents;
    private final Set<String> invalidTypes;
    Map<String, TreeContext> srcContexts;
    Map<String, TreeContext> dstContexts;

    public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents,
            Map<String, String> dstContents,
            Map<String, TreeContext> srcContexts, Map<String, TreeContext> dstContexts) {
        graph = GraphTypeBuilder.<Node, Edge>directed().allowingMultipleEdges(true)
                .allowingSelfLoops(true).edgeClass(Edge.class).weighted(true).buildGraph();
        nodeMap = new HashMap<>();
        this.modelDiff = modelDiff;
        this.srcContents = srcContents;
        this.dstContents = dstContents;
        this.srcContexts = srcContexts;
        this.dstContexts = dstContexts;

        this.invalidTypes = new HashSet<>() {{
            add(Constants.get().EMPTY_STATEMENT);
            add(Constants.get().IMPORT_DECLARATION);
            add(Constants.get().PACKAGE_DECLARATION);
        }};
    }

    private Pair<String, String> localizeTree(Tree tree) {
        Tree root = TreeUtilFunctions.getParentUntilType(
                tree, Constants.get().COMPILATION_UNIT);
        return findContext(srcContexts, root, "src")
                .or(() -> findContext(dstContexts, root, "dst"))
                .orElseGet(() -> new Pair<>("unreachable", "unreachable"));
    }

    private Optional<Pair<String, String>> findContext(
            Map<String, TreeContext> contexts,
            Tree root,
            String label) {
        return contexts.entrySet().stream()
                .filter(e -> e.getValue().getRoot().equals(root))
                .map(e -> new Pair<>(label, e.getKey()))
                .findFirst();
    }

    private String getContent(String srcDst, String path) {
        return (srcDst.equals("src") ? srcContents : dstContents).get(path);
    }

    private void importTrees(HashMap<Tree, Set<Tree>> trees, Set<Tree> allMoves,
            NodeType nodeType, MappingStore mappingStore) {
        HashMap<Tree, Set<Tree>> treesMoves = new HashMap<>();
        for (Tree tree : trees.keySet()) {
            Set<Tree> treeMoves = new HashSet<>();
            allMoves.stream().filter(move -> tree.getPos() <= move.getPos()
                    && move.getEndPos() <= tree.getEndPos()).forEach(treeMoves::add);

            treesMoves.put(tree, treeMoves);
        }
        trees.entrySet().stream().map(entry -> {
            Tree tree = entry.getKey();
            Set<Tree> subTrees = entry.getValue();

            HashMap<Tree, Tree> moveTrees = new HashMap<>();
            for (Tree treeMove : treesMoves.get(tree)) {
                moveTrees.put(treeMove,
                        nodeType.equals(NodeType.DELETION) ? mappingStore.getDstForSrc(treeMove)
                                : mappingStore.getSrcForDst(treeMove));
            }

            Pair<String, String> treeLocation = localizeTree(tree);
            return new Node(getContent(treeLocation.first, treeLocation.second),
                    treeLocation.second, tree, subTrees, moveTrees, nodeType);
        }).forEach(this::addNode);
    }

    public void importDiff(ASTDiff diff) {
        TreeClassifier classifier = diff.createRootNodesClassifier();

        importTrees(aggregateTrees(
                        getValidTrees(classifier.getDeletedSrcs())), classifier.getMovedSrcs(),
                NodeType.DELETION, diff.getAllMappings().getMonoMappingStore());
        importTrees(aggregateTrees(
                        getValidTrees(classifier.getInsertedDsts())), classifier.getMovedDsts(),
                NodeType.ADDITION, diff.getAllMappings().getMonoMappingStore());

        System.out.println("hello");

        // TODO: added and deleted files
//        List<String> diffDestinationPaths = diffSet.stream().map(ASTDiff::getDstPath).toList();
//        List<String> addedPaths =
//                dstContexts.keySet().stream().filter(path -> !diffDestinationPaths.contains(path))
//                        .toList();
//        addedPaths.forEach(path -> {
//            Tree addedTree = dstContexts.get(path).getRoot();
//            network.importHunks(new HashSet<>(addedTree.getChildren()), null);
//        });

        // TODO: pure moves
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

    private Node addTreeNode(Tree tree, NodeType nodeType) {
        Pair<String, String> treeLocation = localizeTree(tree);
        String path = treeLocation.second;

        String potentialNodeId = Node.formatId(path, tree);
        if (nodeMap.containsKey(potentialNodeId)) {
            return nodeMap.get(potentialNodeId);
        }

        Node node = new Node(getContent(treeLocation.first, treeLocation.second), path, tree,
                null, null, nodeType);
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

        Node lastNode = node;
        List<Pair<Tree, NodeType>> contexts = Context.get(node.getTree());
        for (Pair<Tree, NodeType> context : contexts) {
            String potentialContextId = Node.formatId(path, context.first);
            if (!nodeMap.containsKey(potentialContextId)) {
                Node contextNode = new Node(node.getFileContent(), path, context.first, null, null,
                        context.second);
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
        processMoves();
        processDefUse();
        // TODO: how does it contribute to usage pattern if it is connected to a context class?
        processClassInstanceCreations();
        processSimilarity();
        processSuccession();
    }

    private void processMoves() {
        List<Node> nodes = new ArrayList<>(graph.vertexSet());
        List<Node> moveIncludingNodes = nodes.stream().filter(node -> node.getMoveTrees() != null)
                .toList();
        for (Node subject : moveIncludingNodes) {
            for (Node object : moveIncludingNodes) {
                if (!(subject.getNodeType().equals(NodeType.DELETION) && object.getNodeType()
                        .equals(NodeType.ADDITION))) {
                    continue;
                }

                Collection<Tree> subjectDstMoves = subject.getMoveTrees().values();
                Set<Tree> objectSrcMoves = object.getMoveTrees().keySet();
                if (subjectDstMoves.stream().anyMatch(objectSrcMoves::contains)) {
                    addEdge(subject, object, EdgeType.Move);
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
                        node.getNodeType().equals(NodeType.DELETION) ? null
                                : modelDiff.findFieldAccessesInChildModel(umlAttribute));
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

                Optional<UMLClass> instantiatedClass = (node.getNodeType().equals(NodeType.DELETION)
                        ? modelDiff.getParentModel() : modelDiff.getChildModel()).getClassList()
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
                        classDeclarationLocation.getEndOffset());
                for (Node classNode : classDeclarationNodes) {
                    if (classNode.equals(node) || !classNode.getNodeType()
                            .equals(NodeType.ADDITION)) {
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
                node.getNodeType().equals(NodeType.DELETION) ? modelDiff.getParentModel()
                        : modelDiff.getChildModel());
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
                        variableDeclaration.getScope().getStatementsInScopeUsingVariable());
                for (Node useNode : useNodes) {
                    addEdge(node, useNode, EdgeType.DEF_USE);
                }

                AbstractExpression variableInitStatement = operationVariable.getInitializer();
                if (variableInitStatement == null) {
                    continue;
                }

                List<AbstractCall> variableInitInvocations = variableInitStatement.getMethodInvocations();
                for (AbstractCall methodInvocation : variableInitInvocations) {
                    UMLOperation declaration = node.getNodeType().equals(NodeType.DELETION) ? null
                            : modelDiff.findDeclarationsInChildModel(umlOperation,
                                    methodInvocation);
                    if (declaration == null) {
                        continue;
                    }

                    LocationInfo locationInfo = declaration.getLocationInfo();
                    String path = locationInfo.getFilePath();
                    Tree tree = TreeUtilFunctions.findByLocationInfo(
                            dstContexts.get(path).getRoot(), locationInfo);
                    if (nodeMap.containsKey(Node.formatId(path, tree))) {
                        continue;
                    }

                    Node declarationNode = addTreeNode(tree, NodeType.EXTENSION);
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
            Set<AbstractCodeFragment> accessFragments) {
        ArrayList<Node> result = new ArrayList<>();

        for (AbstractCodeFragment accessFragment : accessFragments) {
            // TODO: only the variable itself or fragment entirely?
            // List<LeafExpression> useVariables =
            //         accessFragment.getVariables().stream().filter(variable -> variable.getString()
            //         .equals(variableName)).toList();
            LocationInfo useLoc = accessFragment.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(useLoc.getFilePath(),
                    useLoc.getStartOffset(),
                    useLoc.getEndOffset());
            List<Node> actualOverlappingNodes = overlappingNodes.stream()
                    .filter(node -> !node.isContext()).toList();
            result.addAll(actualOverlappingNodes);
        }

        // declaration change without any usage change
        if (result.isEmpty()) {
            List<LocationInfo> changedFilesUseStatementsLocation =
                    accessFragments.stream().map(LocationInfoProvider::getLocationInfo)
                            .filter(locationInfo -> dstContexts.containsKey(
                                    locationInfo.getFilePath())).toList();
            if (changedFilesUseStatementsLocation.isEmpty()) {
                return result;
            }

            LocationInfo closestLocationInfo = findClosestLocationInfo(declarationLocation,
                    changedFilesUseStatementsLocation);
            String path = closestLocationInfo.getFilePath();
            Tree tree = TreeUtilFunctions.findByLocationInfo(dstContexts.get(path).getRoot(),
                    closestLocationInfo);
            Node node = addTreeNode(tree, NodeType.EXTENSION);

            result.add(node);
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
        List<AbstractCall> invocations = node.getNodeType().equals(NodeType.DELETION) ? null
                : modelDiff.findInvocationsInChildModel(operation);

        List<Node> result = new ArrayList<>();
        for (AbstractCall invocation : invocations) {
            LocationInfo invocationLocationInfo = invocation.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(invocationLocationInfo.getFilePath(),
                    invocationLocationInfo.getStartOffset(), invocationLocationInfo.getEndOffset());
            List<Node> actualOverlappingNodes = overlappingNodes.stream()
                    .filter(n -> !n.isContext()).toList();
            result.addAll(actualOverlappingNodes);
        }

        return result;
    }

    private List<Node> findOverlappingNodes(String path, int pos, int endPos) {
        List<Node> result = new ArrayList<>();

        for (Node node : graph.vertexSet()) {
            Tree hunk = node.getTree();

            if (node.getPath().equals(path) && hunk.getPos() <= endPos && pos <= hunk.getEndPos()) {
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

        UMLOperation result = null;
        for (UMLOperation operation : operations) {
            LocationInfo operationLoc = operation.getLocationInfo();
            if (operationLoc.getStartOffset() == methodDeclaration.getPos()
                    && methodDeclaration.getEndPos() == operationLoc.getEndOffset()) {
                result = operation;
                break;
            }
        }

        return result;
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
            Node rightSibling = subject.getSiblings().second;
            if (rightSibling == null) {
                continue;
            }

            Tree rightSiblingTree = rightSibling.getTree();
            for (Node object : graph.vertexSet()) {
                if (object.getTree().equals(rightSiblingTree)) {
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
                if (object.isContext()) {
                    continue;
                }

                if (subject.equals(object)) {
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
}