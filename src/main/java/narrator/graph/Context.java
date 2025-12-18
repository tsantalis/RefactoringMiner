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
        add(Constants.get().COMPILATION_UNIT);
        add(Constants.get().TYPE_DECLARATION);
        add(Constants.get().ENUM_DECLARATION);
        add(Constants.get().RECORD_DECLARATION);
        add(Constants.get().METHOD_DECLARATION);

        // intel context
        //        add(Constants.get().VARIABLE_DECLARATION_STATEMENT);
        //        add(Constants.get().RETURN_STATEMENT);
        //        add(Constants.get().JAVA_DOC);
        //        add(Constants.get().METHOD_INVOCATION);
    }};

    public static final HashMap<String, List<String>> semanticContext = new HashMap<>() {{
        put(Constants.get().CLASS_INSTANCE_CREATION, List.of(Constants.get().CLASS_INSTANCE_CREATION, Constants.get().METHOD_INVOCATION
                , Constants.get().METHOD_INVOCATION_RECEIVER, Constants.get().EXPRESSION_STATEMENT));
        put(Constants.get().SIMPLE_NAME, List.of(Constants.get().VARIABLE_DECLARATION_STATEMENT, Constants.get().METHOD_INVOCATION,
                Constants.get().CLASS_INSTANCE_CREATION, Constants.get().EXPRESSION_STATEMENT, Constants.get().IF_STATEMENT,
                Constants.get().RETURN_STATEMENT, Constants.get().SUPER_CONSTRUCTOR_INVOCATION, Constants.get().FIELD_DECLARATION,
                Constants.get().TAG_ELEMENT, Constants.get().ENHANCED_FOR_STATEMENT));
        put(Constants.get().PREFIX_EXPRESSION, List.of(Constants.get().METHOD_INVOCATION, Constants.get().IF_STATEMENT));
        put(Constants.get().INFIX_EXPRESSION, List.of(Constants.get().METHOD_INVOCATION, Constants.get().EXPRESSION_STATEMENT,
                Constants.get().CLASS_INSTANCE_CREATION, Constants.get().IF_STATEMENT, Constants.get().VARIABLE_DECLARATION_STATEMENT));
        put("FieldAccess", List.of(Constants.get().METHOD_INVOCATION, Constants.get().CLASS_INSTANCE_CREATION,
                Constants.get().ENHANCED_FOR_STATEMENT));
        put("NullLiteral", List.of(Constants.get().CONSTRUCTOR_INVOCATION, Constants.get().VARIABLE_DECLARATION_STATEMENT,
                Constants.get().METHOD_INVOCATION, Constants.get().CLASS_INSTANCE_CREATION, Constants.get().ENUM_CONSTANT_DECLARATION,
                Constants.get().IF_STATEMENT, Constants.get().RETURN_STATEMENT));
        put(Constants.get().BOOLEAN_LITERAL, List.of(Constants.get().CONSTRUCTOR_INVOCATION,
                Constants.get().VARIABLE_DECLARATION_STATEMENT, Constants.get().METHOD_INVOCATION,
                Constants.get().CLASS_INSTANCE_CREATION, Constants.get().ENUM_CONSTANT_DECLARATION, Constants.get().IF_STATEMENT,
                Constants.get().RETURN_STATEMENT));
        put(Constants.get().NUMBER_LITERAL, List.of(Constants.get().CONSTRUCTOR_INVOCATION,
                Constants.get().VARIABLE_DECLARATION_STATEMENT, Constants.get().METHOD_INVOCATION,
                Constants.get().CLASS_INSTANCE_CREATION, Constants.get().ENUM_CONSTANT_DECLARATION, Constants.get().IF_STATEMENT,
                Constants.get().RETURN_STATEMENT));
        put(Constants.get().STRING_LITERAL, List.of(Constants.get().CONSTRUCTOR_INVOCATION,
                Constants.get().VARIABLE_DECLARATION_STATEMENT, Constants.get().METHOD_INVOCATION,
                Constants.get().CLASS_INSTANCE_CREATION, Constants.get().ENUM_CONSTANT_DECLARATION, Constants.get().IF_STATEMENT,
                Constants.get().RETURN_STATEMENT));
        put(Constants.get().METHOD_INVOCATION, List.of(Constants.get().IF_STATEMENT, Constants.get().METHOD_INVOCATION,
                Constants.get().CLASS_INSTANCE_CREATION, Constants.get().FOR_STATEMENT, Constants.get().RETURN_STATEMENT,
                Constants.get().BLOCK));
        put("LambdaExpression", List.of(Constants.get().METHOD_INVOCATION, Constants.get().VARIABLE_DECLARATION_STATEMENT));
        put(Constants.get().MODIFIER, List.of(Constants.get().FIELD_DECLARATION));
        put(Constants.get().BREAK_STATEMENT, List.of(Constants.get().SWITCH_STATEMENT));
        put(Constants.get().SWITCH_CASE, List.of(Constants.get().SWITCH_STATEMENT));
        put(Constants.get().MARKER_ANNOTATION, List.of(Constants.get().FIELD_DECLARATION,
                Constants.get().VARIABLE_DECLARATION_STATEMENT));
        put(Constants.get().EXPRESSION_STATEMENT, List.of(Constants.get().BLOCK));
        put(Constants.get().METHOD_INVOCATION_RECEIVER, List.of(Constants.get().METHOD_INVOCATION));
        put(Constants.get().METHOD_INVOCATION_ARGUMENTS, List.of(Constants.get().METHOD_INVOCATION));
        put(Constants.get().TAG_ELEMENT, List.of(Constants.get().JAVA_DOC));
        put(Constants.get().TEXT_ELEMENT, List.of(Constants.get().JAVA_DOC, Constants.get().TAG_ELEMENT));
        put("CastExpression", List.of(Constants.get().CLASS_INSTANCE_CREATION, Constants.get().ENUM_CONSTANT_DECLARATION));
        put("ArrayCreation", List.of(Constants.get().EXPRESSION_STATEMENT));
        put("ParameterizedType", List.of(Constants.get().VARIABLE_DECLARATION_STATEMENT, Constants.get().RECORD_COMPONENT,
                Constants.get().ENHANCED_FOR_STATEMENT));
        put(Constants.get().CATCH_CLAUSE, List.of(Constants.get().TRY_STATEMENT));
        put("ParenthesizedExpression", List.of(Constants.get().VARIABLE_DECLARATION_STATEMENT, Constants.get().IF_STATEMENT));
        put(Constants.get().THROW_STATEMENT, List.of(Constants.get().TRY_STATEMENT, Constants.get().IF_STATEMENT));
        put(Constants.get().QUALIFIED_NAME, List.of(Constants.get().FIELD_DECLARATION, "NormalAnnotation", Constants.get().IF_STATEMENT
                , Constants.get().METHOD_INVOCATION));
        put(Constants.get().CONDITIONAL_EXPRESSION, List.of(Constants.get().VARIABLE_DECLARATION_STATEMENT,
                Constants.get().RETURN_STATEMENT, Constants.get().METHOD_INVOCATION));
        put("MethodRefParameter", List.of(Constants.get().TAG_ELEMENT));
        put("ThisExpression", List.of(Constants.get().METHOD_INVOCATION, Constants.get().CLASS_INSTANCE_CREATION));
        put("ArrayAccess", List.of(Constants.get().INFIX_EXPRESSION));
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


