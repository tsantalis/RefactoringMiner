package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

public class Context {

  private static List<String> getSemanticContext(String path, String treeType) {
    Constants constants = new Constants(path);
    if (treeType.equals(constants.CLASS_INSTANCE_CREATION)) {
      return List.of(constants.CLASS_INSTANCE_CREATION, constants.METHOD_INVOCATION,
          constants.METHOD_INVOCATION_RECEIVER, constants.EXPRESSION_STATEMENT);
    }
    if (treeType.equals(constants.RETURN_STATEMENT)) {
      return List.of(constants.METHOD_DECLARATION);
    }
    if (treeType.equals(constants.SIMPLE_NAME)) {
      return List.of(constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.EXPRESSION_STATEMENT, constants.IF_STATEMENT,
          constants.RETURN_STATEMENT, constants.SUPER_CONSTRUCTOR_INVOCATION,
          constants.FIELD_DECLARATION, constants.TAG_ELEMENT, constants.ENHANCED_FOR_STATEMENT,
          constants.RECORD_COMPONENT, constants.METHOD_DECLARATION, constants.METHOD_DECLARATION,
          constants.SUBSCRIPT, "binary_operator", constants.ARGUMENT_LIST, constants.PARAMETERS);
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
          constants.IF_STATEMENT, constants.RETURN_STATEMENT, constants.FIELD_DECLARATION);
    }
    if (treeType.equals(constants.NUMBER_LITERAL)) {
      return List.of(constants.CONSTRUCTOR_INVOCATION,
          constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION,
          constants.IF_STATEMENT, constants.RETURN_STATEMENT, constants.FIELD_DECLARATION);
    }
    if (treeType.equals(constants.STRING_LITERAL)) {
      return List.of(constants.CONSTRUCTOR_INVOCATION,
          constants.VARIABLE_DECLARATION_STATEMENT, constants.METHOD_INVOCATION,
          constants.CLASS_INSTANCE_CREATION, constants.ENUM_CONSTANT_DECLARATION,
          constants.IF_STATEMENT, constants.RETURN_STATEMENT, constants.FIELD_DECLARATION);
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
      return List.of(constants.FIELD_DECLARATION, constants.METHOD_DECLARATION);
    }
    if (treeType.equals(constants.BREAK_STATEMENT)) {
      return List.of(constants.SWITCH_STATEMENT);
    }
    if (treeType.equals(constants.SWITCH_CASE)) {
      return List.of(constants.SWITCH_STATEMENT);
    }
    if (treeType.equals(constants.MARKER_ANNOTATION) || treeType.equals("SingleMemberAnnotation")
        || treeType.equals("NormalAnnotation")) {
      return List.of(constants.FIELD_DECLARATION, constants.VARIABLE_DECLARATION_STATEMENT,
          constants.METHOD_DECLARATION);
    }
    if (treeType.equals(constants.EXPRESSION_STATEMENT) || treeType.equals(constants.VARIABLE_DECLARATION_STATEMENT)
        || treeType.equals(constants.IF_STATEMENT) || treeType.equals(constants.FOR_STATEMENT)
        || treeType.equals(constants.ENHANCED_FOR_STATEMENT) || treeType.equals(constants.WHILE_STATEMENT)
        || treeType.equals(constants.DO_STATEMENT) || treeType.equals(constants.TRY_STATEMENT)
        || treeType.equals(constants.SWITCH_STATEMENT)) {
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
      return List.of(constants.TRY_STATEMENT, constants.IF_STATEMENT, constants.METHOD_DECLARATION);
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
    if (treeType.equals(constants.BLOCK)) {
      return List.of(constants.METHOD_INVOCATION);
    }
    if (treeType.equals(constants.ASSERT_STATEMENT)) {
      return List.of(constants.BLOCK, constants.METHOD_DECLARATION);
    }
    if (treeType.equals("ellipsis")) {
      return List.of(constants.SUBSCRIPT);
    }
    if (treeType.equals("slice")) {
      return List.of(constants.SUBSCRIPT);
    }
    if (treeType.equals("integer")) {
      return List.of(constants.ARGUMENT_LIST, constants.METHOD_INVOCATION, "binary_operator",
              constants.EXPRESSION_STATEMENT, constants.METHOD_DECLARATION, constants.SUBSCRIPT);
    }
    if (treeType.equals("keyword_argument")) {
      return List.of(constants.CALL, constants.EXPRESSION_STATEMENT);
    }
    return null;
  }

  public static boolean isLocationContext(String path, String treeType) {
    Constants constants = new Constants(path);
    if (constants.isRoot(treeType) || constants.isNamedBlock(treeType)) {
      return true;
    }
    return false;
  }

  public static List<Pair<Tree, NodeType>> get(String path, Tree tree) {
    return get(path, tree, false);
  }

  public static List<Pair<Tree, NodeType>> get(String path, Tree tree, boolean change) {
    return get(path, tree, change, null);
  }

  // A change none of whose semantic contexts holds it reads within the statement, the block, the field declaration or
  // the annotation holding it, as the other changes do within theirs. A context printed on the lines of the change alone
  // shows nothing around it, so the change reads within the nearest one printing more, within the lambda or the
  // anonymous class it is in and but the body of a member, which a change at the expression level does not need whole
  public static List<Pair<Tree, NodeType>> get(String path, Tree tree, boolean change, @Nullable String content) {
    List<Pair<Tree, NodeType>> contexts = new ArrayList<>();

    String treeType = tree.getType().name;

    List<String> treeSemanticContexts = getSemanticContext(path, treeType);
    boolean held = !change;
    Constants constants = new Constants(path);
    List<Pair<Tree, NodeType>> unchanged = change && content != null ? get(path, tree, true, null) : null;
    TreeUtilFunctions.LineRange changeLines = unchanged == null ? null : TreeUtilFunctions.getLineRange(tree, content);
    boolean widened = false;
    Tree parent = tree;
    while (true) {
      parent = parent.getParent();

      if (parent == null) {
        return widened || unchanged == null ? contexts : unchanged;
      }

      String parentType = parent.getType().name;

      boolean semantic = treeSemanticContexts != null && treeSemanticContexts.contains(parentType);
      // The code around a lambda or an anonymous class holding the change is no context to widen to
      if (changeLines != null && !held && isCallback(parentType, constants)) {
        changeLines = null;
      }
      if (changeLines != null && !held && (semantic || isHolder(parent, constants)) && !isLocationContext(path, parentType)) {
        // Until a context prints more than the change, only one doing so holds it
        if (printsBeyond(parent, changeLines, content, constants) && !isMemberBody(parent, constants)) {
          contexts.add(new Pair<>(parent, NodeType.SEMANTIC_CONTEXT));
          held = true;
          widened = true;
        }
      } else if (semantic) {
        contexts.add(new Pair<>(parent, NodeType.SEMANTIC_CONTEXT));
        held = true;
      } else if (!held && isHolder(parent, constants)) {
        contexts.add(new Pair<>(parent, NodeType.SEMANTIC_CONTEXT));
        held = true;
      }

      if (isLocationContext(path, parentType)) {
        contexts.add(new Pair<>(parent, NodeType.LOCATION_CONTEXT));
        held = true;
      }
    }
  }

  // A block is printed from the construct it is the body of
  private static boolean printsBeyond(Tree context, TreeUtilFunctions.LineRange changeLines, String content, Constants constants) {
    TreeUtilFunctions.LineRange range = TreeUtilFunctions.getLineRange(context, content);
    int start = range.startLine();
    if (context.getType().name.equals(constants.BLOCK) && context.getParent() != null
        && !context.getParent().getType().name.equals(constants.BLOCK)) {
      start = Math.min(start, TreeUtilFunctions.getLineRange(context.getParent(), content).startLine());
    }

    return start < changeLines.startLine() || changeLines.endLine() < range.endLine();
  }

  private static boolean isMemberBody(Tree tree, Constants constants) {
    Tree owner = tree.getParent();
    return tree.getType().name.equals(constants.BLOCK) && owner != null && (constants.isMethod(owner.getType().name)
        || owner.getType().name.equals("Initializer") || isCallback(owner.getType().name, constants));
  }

  private static boolean isCallback(String treeType, Constants constants) {
    return treeType.equals(constants.LAMBDA_EXPRESSION) || treeType.equals(constants.ANONYMOUS_CLASS_DECLARATION);
  }

  private static boolean isHolder(Tree tree, Constants constants) {
    String type = tree.getType().name;
    return type.equals(constants.FIELD_DECLARATION) || type.equals(constants.BLOCK)
        || type.equals(constants.MARKER_ANNOTATION) || type.equals("SingleMemberAnnotation") || type.equals("NormalAnnotation")
        || (tree.getParent() != null && tree.getParent().getType().name.equals(constants.BLOCK));
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


