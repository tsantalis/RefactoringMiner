package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Context {
    private static List<String> validParentTypes = new ArrayList<>() {{
        add(Constants.COMPILATION_UNIT);
        add(Constants.TYPE_DECLARATION);
        add(Constants.METHOD_DECLARATION);
        add(Constants.VARIABLE_DECLARATION_STATEMENT);
        add(Constants.RETURN_STATEMENT);
    }};

    public static List<Tree> get(Tree tree) {
        List<Tree> contexts = new ArrayList<>();

//        String type = tree.getType().name;
//        if (typeToContextType.containsKey(type)) {
//            validParentTypes.addAll(typeToContextType.get(type));
//        }

        Tree parent = tree;
        String parentType;
        while (true) {
            parent = parent.getParent();

            if (parent == null) {
                return contexts;
            }

            parentType = parent.getType().name;

            if (!validParentTypes.contains(parentType)) {
                continue;
            }

            contexts.add(parent);
        }
    }

    public static final HashMap<String, List<String>> typeToContextType = new HashMap<>() {{
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
        put(Constants.RECORD_COMPONENT, List.of(Constants.METHOD_DECLARATION, Constants.RECORD_DECLARATION));
        put(Constants.RETURN_STATEMENT, List.of(Constants.METHOD_DECLARATION));
        put(Constants.SIMPLE_TYPE, List.of(Constants.METHOD_DECLARATION, Constants.TYPE_DECLARATION,
                Constants.ENUM_DECLARATION));
        put(Constants.METHOD_INVOCATION, List.of(Constants.IF_STATEMENT, Constants.METHOD_INVOCATION,
                Constants.CLASS_INSTANCE_CREATION, Constants.FOR_STATEMENT, Constants.RETURN_STATEMENT,
                Constants.BLOCK));
        put("LambdaExpression", List.of(Constants.METHOD_INVOCATION, Constants.VARIABLE_DECLARATION_STATEMENT));
        put(Constants.MODIFIER, List.of(Constants.FIELD_DECLARATION, Constants.METHOD_DECLARATION,
                Constants.TYPE_DECLARATION));
        put(Constants.BREAK_STATEMENT, List.of(Constants.SWITCH_STATEMENT));
        put(Constants.SWITCH_CASE, List.of(Constants.SWITCH_STATEMENT));
        put("NormalAnnotation", List.of(Constants.TYPE_DECLARATION, Constants.METHOD_DECLARATION));
        put(Constants.MARKER_ANNOTATION, List.of(Constants.TYPE_DECLARATION, Constants.METHOD_DECLARATION,
                Constants.FIELD_DECLARATION, Constants.VARIABLE_DECLARATION_STATEMENT));
        put(Constants.SINGLE_MEMBER_ANNOTATION, List.of(Constants.TYPE_DECLARATION, Constants.METHOD_DECLARATION));
        put(Constants.EXPRESSION_STATEMENT, List.of(Constants.BLOCK));
        put(Constants.METHOD_INVOCATION_RECEIVER, List.of(Constants.METHOD_INVOCATION));
        put(Constants.METHOD_INVOCATION_ARGUMENTS, List.of(Constants.METHOD_INVOCATION));
        put(Constants.TAG_ELEMENT, List.of(Constants.JAVA_DOC));
        put(Constants.TEXT_ELEMENT, List.of(Constants.JAVA_DOC, Constants.TAG_ELEMENT));
        put("CastExpression", List.of(Constants.CLASS_INSTANCE_CREATION, Constants.ENUM_CONSTANT_DECLARATION));
        put("ArrayCreation", List.of(Constants.EXPRESSION_STATEMENT));
        put("ParameterizedType", List.of(Constants.METHOD_DECLARATION, Constants.VARIABLE_DECLARATION_STATEMENT));
        put(Constants.CATCH_CLAUSE, List.of(Constants.TRY_STATEMENT));
        put("ParenthesizedExpression", List.of(Constants.VARIABLE_DECLARATION_STATEMENT, Constants.IF_STATEMENT));
        put(Constants.SUPER_CONSTRUCTOR_INVOCATION, List.of(Constants.METHOD_DECLARATION));
        put(Constants.CONSTRUCTOR_INVOCATION, List.of(Constants.METHOD_DECLARATION));
        put(Constants.PRIMITIVE_TYPE, List.of(Constants.METHOD_DECLARATION));
        put(Constants.THROW_STATEMENT, List.of(Constants.TRY_STATEMENT, Constants.IF_STATEMENT));
        put(Constants.QUALIFIED_NAME, List.of(Constants.FIELD_DECLARATION, "NormalAnnotation", Constants.IF_STATEMENT
                , Constants.METHOD_INVOCATION));
        put(Constants.CONDITIONAL_EXPRESSION, List.of(Constants.VARIABLE_DECLARATION_STATEMENT,
                Constants.RETURN_STATEMENT, Constants.METHOD_INVOCATION));
        put(Constants.ENUM_CONSTANT_DECLARATION, List.of(Constants.ENUM_DECLARATION));
        put("MethodRefParameter", List.of(Constants.TAG_ELEMENT));
        put("ThisExpression", List.of(Constants.METHOD_INVOCATION, Constants.CLASS_INSTANCE_CREATION));
        put("TypeParameter", List.of(Constants.TYPE_DECLARATION));
    }};
}
