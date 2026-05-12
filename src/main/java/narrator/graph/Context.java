package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;

public class Context {

  private static List<String> getSemanticContext(String path, String treeType) {
    Constants constants = new Constants(path);
    if (treeType.equals(constants.CLASS_INSTANCE_CREATION)) {
      return List.of(constants.CLASS_INSTANCE_CREATION, constants.METHOD_INVOCATION,
          constants.METHOD_INVOCATION_RECEIVER, constants.EXPRESSION_STATEMENT);
    }
    if (treeType.equals(constants.SIMPLE_NAME)) {
      return List.of(constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.EXPRESSION_STATEMENT, constants.IF_STATEMENT,
          constants.RETURN_STATEMENT, constants.SUPER_CONSTRUCTOR_INVOCATION,
          constants.FIELD_DECLARATION, constants.TAG_ELEMENT, constants.ENHANCED_FOR_STATEMENT,
          constants.RECORD_COMPONENT, constants.METHOD_DECLARATION, constants.METHOD_DECLARATION);
    }
    if (treeType.equals(constants.RECORD_COMPONENT)) {
      return List.of(constants.METHOD_DECLARATION);
    }
    if (treeType.equals(constants.PREFIX_EXPRESSION)) {
      return List.of(constants.METHOD_INVOCATION, constants.IF_STATEMENT);
    }
    if (treeType.equals(constants.INFIX_EXPRESSION)) {
      return List.of(constants.METHOD_INVOCATION, constants.EXPRESSION_STATEMENT,
          constants.CLASS_INSTANCE_CREATION, constants.IF_STATEMENT,
          constants.VARIABLE_DECLARATION_STATEMENT);
    }
    if (treeType.equals("FieldAccess")) {
      return List.of(constants.METHOD_INVOCATION, constants.CLASS_INSTANCE_CREATION,
          constants.ENHANCED_FOR_STATEMENT);
    }
    if (treeType.equals("NullLiteral")) {
      return List.of(constants.CONSTRUCTOR_INVOCATION,
          constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION,
          constants.IF_STATEMENT, constants.RETURN_STATEMENT);
    }
    if (treeType.equals(constants.BOOLEAN_LITERAL)) {
      return List.of(constants.CONSTRUCTOR_INVOCATION,
          constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION,
          constants.IF_STATEMENT, constants.RETURN_STATEMENT);
    }
    if (treeType.equals(constants.NUMBER_LITERAL)) {
      return List.of(constants.CONSTRUCTOR_INVOCATION,
          constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION,
          constants.IF_STATEMENT, constants.RETURN_STATEMENT);
    }
    if (treeType.equals(constants.STRING_LITERAL)) {
      return List.of(constants.CONSTRUCTOR_INVOCATION,
          constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION,
          constants.IF_STATEMENT, constants.RETURN_STATEMENT);
    }
    if (treeType.equals(constants.METHOD_INVOCATION)) {
      return List.of(constants.IF_STATEMENT, constants.METHOD_INVOCATION,
          constants.FIELD_DECLARATION, constants.CLASS_INSTANCE_CREATION,
          constants.FOR_STATEMENT, constants.RETURN_STATEMENT, constants.BLOCK);
    }
    if (treeType.equals("LambdaExpression")) {
      return List.of(constants.METHOD_INVOCATION, constants.VARIABLE_DECLARATION_STATEMENT);
    }
    if (treeType.equals(constants.MODIFIER)) {
      return List.of(constants.FIELD_DECLARATION);
    }
    if (treeType.equals(constants.BREAK_STATEMENT)) {
      return List.of(constants.SWITCH_STATEMENT);
    }
    if (treeType.equals(constants.SWITCH_CASE)) {
      return List.of(constants.SWITCH_STATEMENT);
    }
    if (treeType.equals(constants.MARKER_ANNOTATION)) {
      return List.of(constants.FIELD_DECLARATION, constants.VARIABLE_DECLARATION_STATEMENT,
          constants.METHOD_DECLARATION);
    }
    if (treeType.equals(constants.EXPRESSION_STATEMENT)) {
      return List.of(constants.BLOCK);
    }
    if (treeType.equals(constants.METHOD_INVOCATION_RECEIVER)) {
      return List.of(constants.METHOD_INVOCATION);
    }
    if (treeType.equals(constants.METHOD_INVOCATION_ARGUMENTS)) {
      return List.of(constants.METHOD_INVOCATION);
    }
    if (treeType.equals(constants.TAG_ELEMENT)) {
      return List.of(constants.JAVA_DOC);
    }
    if (treeType.equals(constants.TEXT_ELEMENT)) {
      return List.of(constants.JAVA_DOC, constants.TAG_ELEMENT);
    }
    if (treeType.equals("CastExpression")) {
      return List.of(constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION);
    }
    if (treeType.equals("ArrayCreation")) {
      return List.of(constants.EXPRESSION_STATEMENT);
    }
    if (treeType.equals("ParameterizedType")) {
      return List.of(constants.VARIABLE_DECLARATION_STATEMENT, constants.RECORD_COMPONENT,
          constants.ENHANCED_FOR_STATEMENT);
    }
    if (treeType.equals(constants.CATCH_CLAUSE)) {
      return List.of(constants.TRY_STATEMENT);
    }
    if (treeType.equals("ParenthesizedExpression")) {
      return List.of(constants.VARIABLE_DECLARATION_STATEMENT, constants.IF_STATEMENT);
    }
    if (treeType.equals(constants.THROW_STATEMENT)) {
      return List.of(constants.TRY_STATEMENT, constants.IF_STATEMENT);
    }
    if (treeType.equals(constants.QUALIFIED_NAME)) {
      return List.of(constants.FIELD_DECLARATION, "NormalAnnotation", constants.IF_STATEMENT,
          constants.METHOD_INVOCATION);
    }
    if (treeType.equals(constants.CONDITIONAL_EXPRESSION)) {
      return List.of(constants.VARIABLE_DECLARATION_STATEMENT, constants.RETURN_STATEMENT,
          constants.METHOD_INVOCATION);
    }
    if (treeType.equals("MethodRefParameter")) {
      return List.of(constants.TAG_ELEMENT);
    }
    if (treeType.equals("ThisExpression")) {
      return List.of(constants.METHOD_INVOCATION, constants.CLASS_INSTANCE_CREATION);
    }
    if (treeType.equals("ArrayAccess")) {
      return List.of(constants.INFIX_EXPRESSION);
    }
    if (treeType.equals(constants.IF_STATEMENT)) {
      return List.of(constants.BLOCK);
    }
    if (treeType.equals(constants.BLOCK)) {
      return List.of(constants.METHOD_INVOCATION);
    }
    return null;
  }

  private static boolean isLocationContext(String path, String treeType) {
    Constants constants = new Constants(path);

    if (treeType.equals(constants.COMPILATION_UNIT)) {
      return true;
    }
    if (treeType.equals(constants.TYPE_DECLARATION)) {
      return true;
    }
    if (treeType.equals(constants.ENUM_DECLARATION)) {
      return true;
    }
    if (treeType.equals(constants.RECORD_DECLARATION)) {
      return true;
    }
    return treeType.equals(constants.METHOD_DECLARATION);
  }

  public static List<Pair<Tree, NodeType>> get(String path, Tree tree) {
    List<Pair<Tree, NodeType>> contexts = new ArrayList<>();

    String treeType = tree.getType().name;

    List<String> treeSemanticContexts = getSemanticContext(path, treeType);
    Tree parent = tree;
    while (true) {
      parent = parent.getParent();

      if (parent == null) {
        return contexts;
      }

      String parentType = parent.getType().name;

      if (treeSemanticContexts != null && treeSemanticContexts.contains(parentType)) {
        contexts.add(new Pair<>(parent, NodeType.SEMANTIC_CONTEXT));
      }

      if (isLocationContext(path, parentType)) {
        contexts.add(new Pair<>(parent, NodeType.LOCATION_CONTEXT));
      }
    }
  }

  public static List<Node> get(Graph<Node, Edge> graph, Node node) {
    List<Node> contexts = new ArrayList<>();

    Node parent = node;
    while (true) {
      Optional<Node> nextParent = graph.outgoingEdgesOf(parent).stream()
          .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).map(
              graph::getEdgeTarget).findFirst();
      if (nextParent.isEmpty()) {
        return contexts;
      }

      parent = nextParent.get();
      contexts.add(parent);
    }
  }
}


