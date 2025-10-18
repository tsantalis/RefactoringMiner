package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Context {
    private static List<String> locationContext = new ArrayList<>() {{
        // location context
        add(Constants.COMPILATION_UNIT);
        add(Constants.TYPE_DECLARATION);
        add(Constants.ENUM_DECLARATION);
        add(Constants.RECORD_DECLARATION);
        add(Constants.METHOD_DECLARATION);

        // intel context
        //        add(Constants.VARIABLE_DECLARATION_STATEMENT);
        //        add(Constants.RETURN_STATEMENT);
        //        add(Constants.JAVA_DOC);
        //        add(Constants.METHOD_INVOCATION);
    }};

    public static final HashMap<String, List<String>> semanticContext = new HashMap<>() {{
        put(Constants.CLASS_INSTANCE_CREATION, List.of(Constants.CLASS_INSTANCE_CREATION, Constants.METHOD_INVOCATION
                , Constants.METHOD_INVOCATION_RECEIVER, Constants.EXPRESSION_STATEMENT));
        put(Constants.SIMPLE_NAME, List.of(Constants.VARIABLE_DECLARATION_STATEMENT, Constants.METHOD_INVOCATION,
                Constants.CLASS_INSTANCE_CREATION, Constants.EXPRESSION_STATEMENT, Constants.IF_STATEMENT,
                Constants.RETURN_STATEMENT, Constants.SUPER_CONSTRUCTOR_INVOCATION, Constants.FIELD_DECLARATION,
                Constants.TAG_ELEMENT, Constants.ENHANCED_FOR_STATEMENT));
        put(Constants.PREFIX_EXPRESSION, List.of(Constants.METHOD_INVOCATION, Constants.IF_STATEMENT));
        put(Constants.INFIX_EXPRESSION, List.of(Constants.METHOD_INVOCATION, Constants.EXPRESSION_STATEMENT,
                Constants.CLASS_INSTANCE_CREATION, Constants.IF_STATEMENT, Constants.VARIABLE_DECLARATION_STATEMENT));
        put("FieldAccess", List.of(Constants.METHOD_INVOCATION, Constants.CLASS_INSTANCE_CREATION,
                Constants.ENHANCED_FOR_STATEMENT));
        put("NullLiteral", List.of(Constants.CONSTRUCTOR_INVOCATION, Constants.VARIABLE_DECLARATION_STATEMENT,
                Constants.METHOD_INVOCATION, Constants.CLASS_INSTANCE_CREATION, Constants.ENUM_CONSTANT_DECLARATION,
                Constants.IF_STATEMENT, Constants.RETURN_STATEMENT));
        put(Constants.BOOLEAN_LITERAL, List.of(Constants.CONSTRUCTOR_INVOCATION,
                Constants.VARIABLE_DECLARATION_STATEMENT, Constants.METHOD_INVOCATION,
                Constants.CLASS_INSTANCE_CREATION, Constants.ENUM_CONSTANT_DECLARATION, Constants.IF_STATEMENT,
                Constants.RETURN_STATEMENT));
        put(Constants.NUMBER_LITERAL, List.of(Constants.CONSTRUCTOR_INVOCATION,
                Constants.VARIABLE_DECLARATION_STATEMENT, Constants.METHOD_INVOCATION,
                Constants.CLASS_INSTANCE_CREATION, Constants.ENUM_CONSTANT_DECLARATION, Constants.IF_STATEMENT,
                Constants.RETURN_STATEMENT));
        put(Constants.STRING_LITERAL, List.of(Constants.CONSTRUCTOR_INVOCATION,
                Constants.VARIABLE_DECLARATION_STATEMENT, Constants.METHOD_INVOCATION,
                Constants.CLASS_INSTANCE_CREATION, Constants.ENUM_CONSTANT_DECLARATION, Constants.IF_STATEMENT,
                Constants.RETURN_STATEMENT));
        put(Constants.METHOD_INVOCATION, List.of(Constants.IF_STATEMENT, Constants.METHOD_INVOCATION,
                Constants.CLASS_INSTANCE_CREATION, Constants.FOR_STATEMENT, Constants.RETURN_STATEMENT,
                Constants.BLOCK));
        put("LambdaExpression", List.of(Constants.METHOD_INVOCATION, Constants.VARIABLE_DECLARATION_STATEMENT));
        put(Constants.MODIFIER, List.of(Constants.FIELD_DECLARATION));
        put(Constants.BREAK_STATEMENT, List.of(Constants.SWITCH_STATEMENT));
        put(Constants.SWITCH_CASE, List.of(Constants.SWITCH_STATEMENT));
        put(Constants.MARKER_ANNOTATION, List.of(Constants.FIELD_DECLARATION,
                Constants.VARIABLE_DECLARATION_STATEMENT));
        put(Constants.EXPRESSION_STATEMENT, List.of(Constants.BLOCK));
        put(Constants.METHOD_INVOCATION_RECEIVER, List.of(Constants.METHOD_INVOCATION));
        put(Constants.METHOD_INVOCATION_ARGUMENTS, List.of(Constants.METHOD_INVOCATION));
        put(Constants.TAG_ELEMENT, List.of(Constants.JAVA_DOC));
        put(Constants.TEXT_ELEMENT, List.of(Constants.JAVA_DOC, Constants.TAG_ELEMENT));
        put("CastExpression", List.of(Constants.CLASS_INSTANCE_CREATION, Constants.ENUM_CONSTANT_DECLARATION));
        put("ArrayCreation", List.of(Constants.EXPRESSION_STATEMENT));
        put("ParameterizedType", List.of(Constants.VARIABLE_DECLARATION_STATEMENT, Constants.RECORD_COMPONENT,
                Constants.ENHANCED_FOR_STATEMENT));
        put(Constants.CATCH_CLAUSE, List.of(Constants.TRY_STATEMENT));
        put("ParenthesizedExpression", List.of(Constants.VARIABLE_DECLARATION_STATEMENT, Constants.IF_STATEMENT));
        put(Constants.THROW_STATEMENT, List.of(Constants.TRY_STATEMENT, Constants.IF_STATEMENT));
        put(Constants.QUALIFIED_NAME, List.of(Constants.FIELD_DECLARATION, "NormalAnnotation", Constants.IF_STATEMENT
                , Constants.METHOD_INVOCATION));
        put(Constants.CONDITIONAL_EXPRESSION, List.of(Constants.VARIABLE_DECLARATION_STATEMENT,
                Constants.RETURN_STATEMENT, Constants.METHOD_INVOCATION));
        put("MethodRefParameter", List.of(Constants.TAG_ELEMENT));
        put("ThisExpression", List.of(Constants.METHOD_INVOCATION, Constants.CLASS_INSTANCE_CREATION));
        put("ArrayAccess", List.of(Constants.INFIX_EXPRESSION));
    }};

    public static List<Pair<Tree, NodeType>> get(Tree tree) {
        List<Pair<Tree, NodeType>> contexts = new ArrayList<>();

        String treeType = tree.getType().name;

        List<String> treeSemanticContexts = semanticContext.get(treeType);
        Tree parent = tree;
        while (true) {
            parent = parent.getParent();

            if (parent == null) {
                return contexts;
            }

            String parentType = parent.getType().name;

            if (locationContext.contains(parentType)) {
                contexts.add(new Pair<>(parent, NodeType.LOCATION_CONTEXT));
            }

            if (treeSemanticContexts != null && treeSemanticContexts.contains(parentType)) {
                contexts.add(new Pair<>(parent, NodeType.SEMANTIC_CONTEXT));
            }
        }
    }
}


