package narrator.graph;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;
import narrator.apted.costmodel.StringUnitCostModel;
import narrator.apted.distance.APTED;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public class HunkNetwork {
    private final Graph<Node, Edge> graph;
    private final HashMap<String, Node> nodeMap;
    private final UMLModelDiff modelDiff;
    private final UMLModel model;
    private final APTED editDistance = new APTED(new StringUnitCostModel());
    private final float distanceThreshold = 0;
    private Map<String, String> srcContents;
    private Map<String, String> dstContents;
    Map<String, TreeContext> childContexts;

    public HunkNetwork(UMLModelDiff modelDiff, Map<String, String> srcContents, Map<String, String> dstContents,
                       Map<String, TreeContext> childContexts) {
        graph = GraphTypeBuilder.<Node, Edge>directed().allowingMultipleEdges(true).allowingSelfLoops(true).edgeClass(Edge.class).weighted(true).buildGraph();
        nodeMap = new HashMap<>();
        this.modelDiff = modelDiff;
        this.model = modelDiff.getChildModel();
        this.srcContents = srcContents;
        this.dstContents = dstContents;
        this.childContexts = childContexts;
    }

    public void importHunks(String path, String srcPath, Set<Tree> additions, MappingStore mappings) {
        HashMap<Tree, Set<Tree>> additionsChildren = new HashMap<>();
        for (Tree subject : additions) {
            boolean isParent = true;

            for (Tree object : additions) {
                if (subject.equals(object)) {
                    continue;
                }

                if (object.getPos() <= subject.getPos() && subject.getEndPos() <= object.getEndPos()) {
                    isParent = false;
                    break;
                }
            }

            if (isParent) {
                additionsChildren.put(subject, new HashSet<>());
            }
        }
        for (Tree parent : additionsChildren.keySet()) {
            for (Tree addition : additions) {
                if (parent.equals(addition)) {
                    continue;
                }

                if (parent.getPos() <= addition.getPos() && addition.getEndPos() <= parent.getEndPos()) {
                    additionsChildren.get(parent).add(addition);
                }
            }
        }


        for (Map.Entry<Tree, Set<Tree>> additionChildren : additionsChildren.entrySet()) {
            Tree parent = additionChildren.getKey();

            Node node = new Node(dstContents.get(path), path, parent);

            if (mappings != null && srcPath != null) {
                List<Tree> trees = new ArrayList<>(parent.getDescendants());
                trees.add(parent);
                List<Tree> srcs = trees.stream().map(mappings::getSrcForDst).filter(Objects::nonNull).toList();

                List<Tree> uniqueSrcs = new ArrayList<>();
                for (Tree subject : srcs) {
                    boolean isUnique = true;
                    for (Tree object : srcs) {
                        if (object.getPos() <= subject.getPos() && subject.getEndPos() <= object.getEndPos() && !subject.equals(object)) {
                            isUnique = false;
                            break;
                        }
                    }

                    if (isUnique) {
                        uniqueSrcs.add(subject);
                    }
                }

                if (!uniqueSrcs.isEmpty()) {
                    uniqueSrcs.sort(Comparator.comparingInt(Tree::getPos));
                    node.setSrcs(uniqueSrcs.stream().map(src -> new Node(srcContents.get(srcPath), srcPath, src)).toList());
                    node.setDsts(uniqueSrcs.stream().map(mappings::getDstForSrc).filter(Objects::nonNull).toList());
                }
            }

            Set<Tree> children = additionChildren.getValue();
            if (!children.isEmpty()) {
                node.setDstExceptions(children);
            }

            addNode(node);
        }
    }

    private Node addTreeNode(String path, Tree tree, NodeType nodeType) {
        String potentialNodeId = Node.formatId(path, tree);
        if (nodeMap.containsKey(potentialNodeId)) {
            return nodeMap.get(potentialNodeId);
        }

        Node node = new Node(dstContents.get(path), path, tree, nodeType);
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
                Node contextNode = new Node(node.getFileContent(), path, context.first, context.second);
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
        processDefUse();
        // TODO: how does it contribute to usage pattern if it is connected to a context class?
        processClassInstanceCreations();
        processOutOfClasses();
        processSimilarity();
        processSuccession();
    }

    private void processDefUse() {
        List<Node> nodes = new ArrayList<>(graph.vertexSet());
        for (Node node : nodes) {
            if (node.isContext()) {
                continue;
            }

            Tree tree = node.getTree();

            if (tree.getType().name.equals(Constants.EMPTY_STATEMENT)) {
                node.setActive(false);
                continue;
            }

            List<Tree> subTrees = new ArrayList<>(tree.getDescendants());
            subTrees.add(tree);

            List<Tree> methodDeclarations = new ArrayList<>();
            List<Tree> fieldDeclarations = new ArrayList<>();
            List<Tree> parameterDeclarations = new ArrayList<>();
            List<Tree> variableDeclarations = new ArrayList<>();
            for (Tree subTree : subTrees) {
                String subTreeType = subTree.getType().name;
                switch (subTreeType) {
                    case Constants.METHOD_DECLARATION -> methodDeclarations.add(subTree);
                    case Constants.FIELD_DECLARATION -> fieldDeclarations.add(subTree);
                    // TODO: investigation: it is only for "Enhanced For" and other parameters will be in variable
                    //  declarations array
                    case Constants.RECORD_COMPONENT -> parameterDeclarations.add(subTree);
                    case Constants.VARIABLE_DECLARATION_STATEMENT -> variableDeclarations.add(subTree);
                }
            }
            if (methodDeclarations.isEmpty() && fieldDeclarations.isEmpty() && parameterDeclarations.isEmpty() && variableDeclarations.isEmpty()) {
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
                addMethodInvocationEdges(node, new Node(node.getFileContent(), node.getPath(), methodDeclaration));
            }

            for (Tree fieldDeclaration : fieldDeclarations) {
                UMLAttribute umlAttribute = findUMLAttribute(new Node(node.getFileContent(), node.getPath(),
                        fieldDeclaration));
                if (umlAttribute == null) {
                    continue;
                }

                node.addIdentifier(umlAttribute.getVariableDeclaration().getVariableName());

                List<Node> useNodes = findAccessNodes(umlAttribute.getLocationInfo(),
                        modelDiff.findFieldAccessesInChildModel(umlAttribute));
                for (Node useNode : useNodes) {
                    addEdge(node, useNode, EdgeType.DEF_USE);
                }
            }

            for (Tree parameterDeclaration : parameterDeclarations) {
                addVariableDeclarationEdges(node, new Node(node.getFileContent(), node.getPath(),
                        parameterDeclaration));
            }

            for (Tree variableDeclaration : variableDeclarations) {
                Tree variableDeclarationFragment = TreeUtilFunctions.findChildByType(variableDeclaration,
                        Constants.VARIABLE_DECLARATION_FRAGMENT);
                if (variableDeclarationFragment == null) {
                    continue;
                }

                addVariableDeclarationEdges(node, new Node(node.getFileContent(), node.getPath(),
                        variableDeclarationFragment));
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
                    subTrees.stream().filter(subTree -> subTree.getType().name.equals(Constants.CLASS_INSTANCE_CREATION)).toList();
            for (Tree classInstanceCreation : classInstanceCreations) {
                Tree classType = TreeUtilFunctions.findChildByType(classInstanceCreation, Constants.SIMPLE_TYPE);
                if (classType == null) {
                    continue;
                }
                Tree className = TreeUtilFunctions.findChildByType(classType, Constants.SIMPLE_NAME);
                if (className == null) {
                    continue;
                }

                Optional<UMLClass> instantiatedClass =
                        modelDiff.getChildModel().getClassList().stream().filter(c -> c.getName().endsWith(className.getLabel())).findFirst();
                if (instantiatedClass.isEmpty()) {
                    continue;
                }

                LocationInfo classDeclarationLocation = instantiatedClass.get().getLocationInfo();
                List<Node> classDeclarationNodes = findOverlappingNodes(classDeclarationLocation.getFilePath(),
                        classDeclarationLocation.getStartOffset(), classDeclarationLocation.getEndOffset());
                for (Node classNode : classDeclarationNodes) {
                    if (classNode.equals(node) || !classNode.getNodeType().equals(NodeType.BASE)) {
                        continue;
                    }

                    addEdge(classNode, node, EdgeType.DEF_USE);
                    classNode.addIdentifier(instantiatedClass.get().getNonQualifiedName());
                }
            }
        }
    }

    private UMLClass getUMLClass(Node node) {
        Tree hunkParentType = TreeUtilFunctions.getParentUntilType(node.getTree(), Constants.TYPE_DECLARATION);
        if (hunkParentType == null) { // there is no need to process def-use when it is out of type
            return null;
        }
        Tree parentTypeName = TreeUtilFunctions.findChildByType(hunkParentType, Constants.SIMPLE_NAME);
        if (parentTypeName == null) {
            return null;
        }

        return getUMLClass(node.getPath(), parentTypeName.getLabel());
    }

    private UMLClass getUMLClass(String path, String typeName) {
        if (path == null) {
            return null;
        }

        UMLClass UmlClass = null;
        for (UMLClass uc : model.getClassList()) {
            if (uc.getSourceFile().equals(path) && uc.getNonQualifiedName().equals(typeName)) {
                UmlClass = uc;
                break;
            }
        }

        return UmlClass;
    }

    private void addVariableDeclarationEdges(Node node, Node variableDeclarationNode) {
        Tree variableDeclarationTree = variableDeclarationNode.getTree();

        Tree methodRoot = TreeUtilFunctions.getParentUntilType(variableDeclarationTree, Constants.METHOD_DECLARATION);
        if (methodRoot == null) { // https://github.com/elastic/elasticsearch/commit/e0a458441cff9a4242cd93f4c02f06d72f2d63c4#diff-9b7bef16de393901cd8c75e73d2fb03afb90a10f1de191b7174275bbd8e71bd8L38
            return;
        }

        UMLClass umlClass = getUMLClass(variableDeclarationNode);
        if (umlClass == null) {
            return;
        }

        UMLOperation umlOperation = findUMLOperation(umlClass, methodRoot);
        if (umlOperation == null) {
            return;
        }

        for (VariableDeclaration operationVariable : umlOperation.getAllVariableDeclarations()) {
            LocationInfo operationVariableLoc = operationVariable.getLocationInfo();
            if (operationVariableLoc.getStartOffset() == variableDeclarationTree.getPos() && variableDeclarationTree.getEndPos() == operationVariableLoc.getEndOffset()) {
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
                    UMLOperation declaration = modelDiff.findDeclarationsInChildModel(umlOperation, methodInvocation);
                    if (declaration == null) {
                        continue;
                    }

                    LocationInfo locationInfo = declaration.getLocationInfo();
                    String path = locationInfo.getFilePath();
                    Tree tree = TreeUtilFunctions.findByLocationInfo(childContexts.get(path).getRoot(), locationInfo);
                    if (nodeMap.containsKey(Node.formatId(path, tree))) {
                        continue;
                    }

                    Node declarationNode = addTreeNode(path, tree, NodeType.EXTENSION);
                    addEdge(declarationNode, node, EdgeType.DEF_USE);
                }
            }
        }
    }

    private void addMethodInvocationEdges(Node node, Node methodDeclarationNode) {
        UMLClass umlClass = getUMLClass(methodDeclarationNode);
        if (umlClass == null) {
            return;
        }

        UMLOperation operation = findUMLOperation(umlClass, methodDeclarationNode.getTree());
        if (operation == null) {
            return;
        }

        node.addIdentifier(operation.getName());

        List<Node> invocationNodes = findInvocationNodes(operation);
        for (Node invocationNode : invocationNodes) {
            addEdge(node, invocationNode, EdgeType.DEF_USE);
        }
    }

    private List<Node> findAccessNodes(LocationInfo declarationLocation, Set<AbstractCodeFragment> accessFragments) {
        ArrayList<Node> result = new ArrayList<>();

        for (AbstractCodeFragment accessFragment : accessFragments) {
            // TODO: only the variable itself or fragment entirely?
            // List<LeafExpression> useVariables =
            //         accessFragment.getVariables().stream().filter(variable -> variable.getString()
            //         .equals(variableName)).toList();
            LocationInfo useLoc = accessFragment.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(useLoc.getFilePath(), useLoc.getStartOffset(),
                    useLoc.getEndOffset());
            List<Node> actualOverlappingNodes = overlappingNodes.stream().filter(node -> !node.isContext()).toList();
            result.addAll(actualOverlappingNodes);
        }

        // declaration change without any usage change
        if (result.isEmpty()) {
            List<LocationInfo> changedFilesUseStatementsLocation =
                    accessFragments.stream().map(LocationInfoProvider::getLocationInfo).filter(locationInfo -> childContexts.containsKey(locationInfo.getFilePath())).toList();
            if (changedFilesUseStatementsLocation.isEmpty()) {
                return result;
            }

            LocationInfo closestLocationInfo = findClosestLocationInfo(declarationLocation,
                    changedFilesUseStatementsLocation);
            String path = closestLocationInfo.getFilePath();
            Tree tree = TreeUtilFunctions.findByLocationInfo(childContexts.get(path).getRoot(), closestLocationInfo);
            Node node = addTreeNode(path, tree, NodeType.EXTENSION);

            result.add(node);
        }

        return result;
    }

    private LocationInfo findClosestLocationInfo(LocationInfo subject, List<LocationInfo> candidates) {
        int pos = subject.getStartOffset();
        int endPos = subject.getEndOffset();

        List<LocationInfo> sameFileCandidates =
                candidates.stream().filter(candidate -> subject.getFilePath().equals(candidate.getFilePath())).toList();
        if (!sameFileCandidates.isEmpty()) {
            LocationInfo closestLocationInfo = null;

            int shortestDistance = Integer.MAX_VALUE;
            for (LocationInfo candidate : sameFileCandidates) {
                if (endPos >= candidate.getStartOffset() && candidate.getEndOffset() >= pos) {
                    return candidate;
                }

                int distance = endPos < candidate.getStartOffset() ? candidate.getStartOffset() - endPos :
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

    private List<Node> findInvocationNodes(UMLOperation operation) {
        List<AbstractCall> invocations = modelDiff.findInvocationsInChildModel(operation);

        List<Node> result = new ArrayList<>();
        for (AbstractCall invocation : invocations) {
            LocationInfo invocationLocationInfo = invocation.getLocationInfo();
            List<Node> overlappingNodes = findOverlappingNodes(invocationLocationInfo.getFilePath(),
                    invocationLocationInfo.getStartOffset(), invocationLocationInfo.getEndOffset());
            List<Node> actualOverlappingNodes = overlappingNodes.stream().filter(node -> !node.isContext()).toList();
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
            if (operationLoc.getStartOffset() == methodDeclaration.getPos() && methodDeclaration.getEndPos() == operationLoc.getEndOffset()) {
                result = operation;
                break;
            }
        }

        return result;
    }

    private UMLAttribute findUMLAttribute(Node fieldDeclarationNode) {
        UMLClass umlClass = getUMLClass(fieldDeclarationNode);
        if (umlClass == null) {
            return null;
        }

        List<UMLAttribute> attributes = new ArrayList<>(umlClass.getAttributes());
        for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
            attributes.addAll(anonymousClass.getAttributes());
        }

        Tree fieldDeclarationTree = fieldDeclarationNode.getTree();

        UMLAttribute result = null;
        for (UMLAttribute attribute : attributes) {
            LocationInfo attrLoc = attribute.getFieldDeclarationLocationInfo();
            if (fieldDeclarationTree.getPos() == attrLoc.getStartOffset() && fieldDeclarationTree.getEndPos() == attrLoc.getEndOffset()) {
                result = attribute;
                break;
            }
        }

        return result;
    }

    private void processOutOfClasses() {
        Set<Node> nodes = graph.vertexSet();

        for (Node node : nodes) {
            String type = node.getTree().getType().name;

            //            UmlClass.getImportedTypes();
            if (type.equals(Constants.IMPORT_DECLARATION) || type.equals(Constants.PACKAGE_DECLARATION)) {
                node.setActive(false);
            }
        }

        //            UmlClass.getPackageDeclaration()
        //            UmlClass.getPackageDeclarationJavadoc()
        //            UmlClass.getPackageDeclarationComments()

        //            UmlClass.getJavadoc();
        //            UmlClass.getComments();
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

                float distance = editDistance.computeEditDistance(subject.getTree(), object.getTree());
                if (distance <= distanceThreshold) {
                    addEdge(subject, object, EdgeType.SIMILARITY);
                }
            }
        }
    }
}