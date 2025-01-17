package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.UMLModelDiff;
import narrator.apted.costmodel.StringUnitCostModel;
import narrator.apted.distance.APTED;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public class GraphProcessor {
    private final Graph<Node, Edge> graph;
    private final HashMap<String, Node> nodeMap;
    private final UMLModelDiff modelDiff;
    private final UMLModel model;
    private final APTED editDistance = new APTED(new StringUnitCostModel());
    private final float distanceThreshold = 0;

    public GraphProcessor(UMLModelDiff modelDiff) {
        graph = GraphTypeBuilder.<Node, Edge>directed().allowingMultipleEdges(true).allowingSelfLoops(true).edgeClass(Edge.class).weighted(true).buildGraph();
        nodeMap = new HashMap<>();
        this.modelDiff = modelDiff;
        this.model = modelDiff.getChildModel();
    }

    public void addHunkNode(String fileContent, String path, Tree tree) {
        Node node = new Node(fileContent, path, tree, false);

        if (!nodeMap.containsKey(node.getId())) {
            graph.addVertex(node);
            nodeMap.put(node.getId(), node);
        }

        Node lastNode = node;
        List<Tree> contexts = Context.get(tree);
        for (Tree context : contexts) {
            Node contextNode = new Node(fileContent, path, context, true);
            if (!nodeMap.containsKey(contextNode.getId())) {
                graph.addVertex(contextNode);
                nodeMap.put(contextNode.getId(), contextNode);
            }

            contextNode = nodeMap.get(contextNode.getId());
            if (graph.getEdge(lastNode, contextNode) == null) {
                graph.addEdge(lastNode, contextNode, new Edge(EdgeType.CONTEXT, 1));
            }

            lastNode = contextNode;
        }

    }

    private void addEdge(Node node1, Node node2, EdgeType edgeType, float weight) {
        if (node1.equals(node2)) {
            return;
        }

        graph.addEdge(node1, node2, new Edge(edgeType, weight));
    }

    public Graph<Node, Edge> getGraph() {
        return graph;
    }

    public void process() {
        processDefUse();
        processOutOfClasses();
        processSimilarity();
        processSuccession();
        processClassInstanceCreations();
        processContextMethodInvocations();
    }

    private void processDefUse() {
        for (Node node : graph.vertexSet()) {
            if (node.isContext()) {
                continue;
            }

            Tree tree = node.getTree();

            if (tree.getType().name.equals(Constants.EMPTY_STATEMENT)) {
                node.setActive(false);
                continue;
            }

            List<Tree> subTrees = new ArrayList<>();
            subTrees.add(tree);
            subTrees.addAll(tree.getDescendants());

            List<Tree> methodDeclarations = new ArrayList<>();
            List<Tree> fieldDeclarations = new ArrayList<>();
            List<Tree> parameterDeclarations = new ArrayList<>();
            List<Tree> variableDeclarations = new ArrayList<>();
            for (Tree subTree : subTrees) {
                String subTreeType = subTree.getType().name;
                switch (subTreeType) {
                    case Constants.METHOD_DECLARATION -> methodDeclarations.add(subTree);
                    case Constants.FIELD_DECLARATION -> fieldDeclarations.add(subTree);
                    case Constants.RECORD_COMPONENT -> parameterDeclarations.add(subTree);
                    case Constants.VARIABLE_DECLARATION_STATEMENT -> variableDeclarations.add(subTree);
                }
            }
            if (methodDeclarations.isEmpty() && fieldDeclarations.isEmpty() && parameterDeclarations.isEmpty() && variableDeclarations.isEmpty()) {
                continue;
            }

            for (Tree methodDeclaration : methodDeclarations) {
                List<Tree> methodDescendants = methodDeclaration.getDescendants();
                for (Tree methodDescendant : methodDescendants) {
                    if (methodDescendant.getType().name.equals(Constants.RECORD_COMPONENT)) {
                        parameterDeclarations.remove(methodDescendant);
                    }
                }
            }

            UMLClass umlClass = getUMLClass(node);
            if (umlClass == null) {
                continue;
            }

            for (Tree methodDeclaration : methodDeclarations) {
                addMethodInvocationEdges(umlClass, node, methodDeclaration);
            }

            for (Tree fieldDeclaration : fieldDeclarations) {
                UMLAttribute umlAttribute = findUMLAttribute(umlClass, fieldDeclaration);
                if (umlAttribute == null) {
                    continue;
                }

                List<Node> useNodes = findUseNodes(umlAttribute.getVariableDeclaration());
                for (Node useNode : useNodes) {
                    addEdge(node, useNode, EdgeType.DEF_USE, 1);
                }
            }

            for (Tree parameterDeclaration : parameterDeclarations) {
                addVariableDeclarationUseEdges(umlClass, node, parameterDeclaration);
            }

            for (Tree variableDeclaration : variableDeclarations) {
                Tree variableDeclarationFragment = TreeUtilFunctions.findChildByType(variableDeclaration,
                        Constants.VARIABLE_DECLARATION_FRAGMENT);
                if (variableDeclarationFragment == null) {
                    continue;
                }

                addVariableDeclarationUseEdges(umlClass, node, variableDeclarationFragment);
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

    private void addVariableDeclarationUseEdges(UMLClass umlClass, Node node, Tree variableDeclaration) {
        Tree methodRoot = TreeUtilFunctions.getParentUntilType(variableDeclaration, Constants.METHOD_DECLARATION);
        if (methodRoot == null) { // https://github.com/elastic/elasticsearch/commit/e0a458441cff9a4242cd93f4c02f06d72f2d63c4#diff-9b7bef16de393901cd8c75e73d2fb03afb90a10f1de191b7174275bbd8e71bd8L38
            return;
        }

        UMLOperation umlOperation = findUMLOperation(umlClass, methodRoot);
        if (umlOperation == null) {
            return;
        }

        for (VariableDeclaration operationVariable : umlOperation.getAllVariableDeclarations()) {
            LocationInfo operationVariableLoc = operationVariable.getLocationInfo();
            if (operationVariableLoc.getStartOffset() == variableDeclaration.getPos() && variableDeclaration.getEndPos() == operationVariableLoc.getEndOffset()) {
                List<Node> useNodes = findUseNodes(operationVariable.getVariableDeclaration());
                for (Node useNode : useNodes) {
                    addEdge(node, useNode, EdgeType.DEF_USE, 1);
                }
            }
        }
    }

    private void addMethodInvocationEdges(UMLClass umlClass, Node node, Tree methodDeclaration) {
        UMLOperation operation = findUMLOperation(umlClass, methodDeclaration);
        if (operation == null) {
            return;
        }

        List<Node> invocationNodes = findInvocationNodes(operation);
        for (Node invocationNode : invocationNodes) {
            addEdge(node, invocationNode, EdgeType.DEF_USE, 1);
        }
    }

    private List<Node> findUseNodes(VariableDeclaration declaration) {
        Set<AbstractCodeFragment> useStatements = declaration.getScope().getStatementsInScopeUsingVariable();

        ArrayList<Node> result = new ArrayList<>();
        for (AbstractCodeFragment useStatement : useStatements) {
            List<LeafExpression> useVariables = useStatement.getVariables().stream().filter(variable -> variable.getString().equals(declaration.getVariableName())).toList();
            for (LeafExpression useVariable : useVariables) {
                LocationInfo useLoc = useVariable.getLocationInfo();
                List<Node> overlappingNodes = findOverlappingNodes(useLoc.getFilePath(), useLoc.getStartOffset(),
                        useLoc.getEndOffset());
                List<Node> actualOverlappingNodes = overlappingNodes.stream().filter(node -> !node.isContext()).toList();
                result.addAll(actualOverlappingNodes);
            }
        }

        return result;
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

    private UMLAttribute findUMLAttribute(UMLClass umlClass, Tree fieldDeclaration) {
        List<UMLAttribute> attributes = new ArrayList<>(umlClass.getAttributes());
        for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
            attributes.addAll(anonymousClass.getAttributes());
        }

        UMLAttribute result = null;
        for (UMLAttribute attribute : attributes) {
            LocationInfo attrLoc = attribute.getFieldDeclarationLocationInfo();
            if (fieldDeclaration.getPos() == attrLoc.getStartOffset() && fieldDeclaration.getEndPos() == attrLoc.getEndOffset()) {
                result = attribute;
                break;
            }
        }

        return result;
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

    private void processOutOfClasses() {
        Set<Node> nodes = graph.vertexSet();

        for (Node node : nodes) {
            Tree hunk = node.getTree();

            //            UmlClass.getImportedTypes();
            if (hunk.getType().name.equals(Constants.IMPORT_DECLARATION)) {
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
            Node rightSibling = subject.getSiblings().getRight();
            if (rightSibling == null) {
                continue;
            }

            Tree rightSiblingTree = rightSibling.getTree();
            for (Node object : graph.vertexSet()) {
                if (object.getTree().equals(rightSiblingTree)) {
                    addEdge(subject, object, EdgeType.SUCCESSION, 1);
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
                    addEdge(subject, object, EdgeType.SIMILARITY, 1 / (distance + 1));
                }
            }
        }
    }

    private void processClassInstanceCreations() {
        for (Node node : graph.vertexSet()) {
            if (node.isContext()) {
                continue;
            }

            List<Tree> subTrees = new ArrayList<>();
            Tree tree = node.getTree();
            subTrees.add(tree);
            subTrees.addAll(tree.getDescendants());

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

                LocationInfo createdClassLocation = instantiatedClass.get().getLocationInfo();
                List<Node> classNodes = findOverlappingNodes(createdClassLocation.getFilePath(),
                        createdClassLocation.getStartOffset(), createdClassLocation.getEndOffset());
                for (Node classNode : classNodes) {
                    if (classNode.equals(node) || !classNode.getTree().getType().name.equals(Constants.TYPE_DECLARATION)) {
                        continue;
                    }

                    addEdge(node, classNode, EdgeType.DEF_USE, 1);
                }

            }
        }
    }

    private void processContextMethodInvocations() {
        for (Node node : graph.vertexSet()) {
            if (!node.isContext()) {
                continue;
            }

            Tree tree = node.getTree();
            if (!tree.getType().name.equals(Constants.METHOD_DECLARATION)) {
                continue;
            }

            UMLClass umlClass = getUMLClass(node);
            if (umlClass == null) {
                continue;
            }

            addMethodInvocationEdges(umlClass, node, tree);
        }
    }
}