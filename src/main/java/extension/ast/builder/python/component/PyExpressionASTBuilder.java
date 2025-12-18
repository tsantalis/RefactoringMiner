package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.builder.python.PyASTBuilderUtil;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.OperatorEnum;
import extension.ast.node.expression.*;
import extension.ast.node.literal.LangDictionaryLiteral;
import extension.ast.node.literal.LangStringLiteral;
import extension.ast.node.literal.LangTupleLiteral;
import extension.base.lang.python.Python3Parser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class PyExpressionASTBuilder extends PyBaseASTBuilder {

    public PyExpressionASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitAtom(Python3Parser.AtomContext ctx) {
        // Handle numbers
        if (ctx.NUMBER() != null) {
            return LangASTNodeFactory.createNumberLiteral(ctx, ctx.NUMBER().getText());
        }

        // Handle None
        if (ctx.getText() != null && "None".equals(ctx.getText())) {
            return LangASTNodeFactory.createNullLiteral(ctx);
        }

        // Handle string literals
        if (ctx.getText() != null && PyASTBuilderUtil.isStringLiteral(ctx)) {
            return LangASTNodeFactory.createStringLiteral(ctx, ctx.getText());
        }

        if (ctx.ELLIPSIS() != null) {
            return LangASTNodeFactory.createEllipsisLiteral(ctx);
        }

        // Handle boolean literals
        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return LangASTNodeFactory.createBooleanLiteral(ctx, Boolean.parseBoolean(ctx.getText()));
        }

        // Handle dictionary literals and dictionary comprehensions
        if (ctx.OPEN_BRACE() != null && ctx.CLOSE_BRACE() != null) {
            if (ctx.dictorsetmaker() == null) {
                // Empty dictionary
                return LangASTNodeFactory.createDictionaryLiteral(ctx);
            } else {
                return mainBuilder.visit(ctx.dictorsetmaker());
            }
        }

        // Handle list literals and list comprehensions
        if (ctx.getText() != null && PyASTBuilderUtil.isListLiteral(ctx)) {
            if (ctx.testlist_comp() != null) {
                return mainBuilder.visit(ctx.testlist_comp());
            } else {
                // Empty list
                return LangASTNodeFactory.createListLiteral(ctx, new ArrayList<>());
            }
        }

        // Handle parenthesized expressions, tuples, and generator expressions
        if (ctx.OPEN_PAREN() != null && ctx.CLOSE_PAREN() != null) {
            if (ctx.testlist_comp() != null) {

                LangASTNode result = mainBuilder.visit(ctx.testlist_comp());

                // If the result is a comprehension, then it is a generator expression
                if (result instanceof LangComprehensionExpression comprehension) {
                    comprehension.setKind(LangComprehensionExpression.LangComprehensionKind.GENERATOR);
                    return comprehension;
                }

                // Otherwise handle as regular parenthesized expression or tuple
                if (result instanceof LangTupleLiteral tuple) {
                    if (tuple.getElements().size() == 1) {
                        // Single element in parentheses, create parenthesized expression
                        return LangASTNodeFactory.createParenthesizedExpression(ctx, tuple.getElements().get(0));
                    } else {
                        // Multiple elements, return as tuple
                        return result;
                    }
                } else {
                    // Single expression, create parenthesized expression
                    return LangASTNodeFactory.createParenthesizedExpression(ctx, result);
                }
            }
            // Empty parenthesis
            return LangASTNodeFactory.createParenthesizedExpression(ctx, null);
        }

        // Handle regular identifiers/names
        if (ctx.name() != null) {
            return LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx);
        }

        // Fallback
        String atomText = ctx.getText();
        if (atomText != null && !atomText.isEmpty()) {
            System.err.println("Warning: Unhandled atom type, creating SimpleName fallback: " + atomText);
            System.err.println("Context: " + ctx.getText());
            return LangASTNodeFactory.createSimpleName(atomText, ctx);
        }

        return null;
    }

    public LangASTNode visitAtom_expr(Python3Parser.Atom_exprContext ctx) {
        // Handle the base atom (which could be a function name like "print")
        LangASTNode baseExpr = mainBuilder.visit(ctx.atom());

        // If there are no trailers, just return the atom
        if (ctx.trailer().isEmpty()) {
            return baseExpr;
        }

        if (ctx.AWAIT() != null) {
            LangASTNode innerExpr = mainBuilder.visit(ctx.atom());
            return LangASTNodeFactory.createAwaitExpression(ctx, innerExpr);
        }

        // Process trailers (method calls, attribute access, etc.)
        LangASTNode result = baseExpr;
        for (Python3Parser.TrailerContext trailerCtx : ctx.trailer()) {
            if (trailerCtx.OPEN_PAREN() != null) {
                // This is a function call like print(i) or a method call like obj.method()
                LangMethodInvocation methodInvocation = LangASTNodeFactory.createMethodInvocation(ctx.getParent());

                // Set the expression - this is the object on which the method is called
                methodInvocation.setExpression(result);

                // Process arguments if they exist
                if (trailerCtx.arglist() != null) {
                    for (Python3Parser.ArgumentContext argCtx : trailerCtx.arglist().argument()) {
                        // Visit each argument and add it to the method invocation
                        LangASTNode argNode = mainBuilder.visit(argCtx);
                        if (argNode != null) {
                            methodInvocation.addArgument(argNode);
                        }
                    }
                }

                result = methodInvocation;
            } else if (trailerCtx.DOT() != null && trailerCtx.name() != null) {
                // This is attribute access: obj.attr
                String attrName = trailerCtx.name().getText();

                // Create a field access node that combines the object and the field name
                result = LangASTNodeFactory.createFieldAccess(result, attrName, trailerCtx);
            } else if (trailerCtx.OPEN_BRACK() != null && trailerCtx.CLOSE_BRACK() != null) {
                // This is index access: obj[index] or dict["key"]
                // Use the existing visitTrailer logic but update the target
                LangASTNode trailerResult = mainBuilder.visit(trailerCtx);

                // The visitTrailer method returns LangIndexAccess with null target
                if (trailerResult instanceof LangIndexAccess indexAccess) {
                    // set the correct target
                    indexAccess.setTarget(result);
                    result = indexAccess;
                }
            } else {
                LangASTNode trailerResult = mainBuilder.visit(trailerCtx);
                if (trailerResult instanceof LangFieldAccess fieldAccess && fieldAccess.getExpression() == null) {
                    fieldAccess.setExpression(result);
                    result = fieldAccess;
                } else {
                    result = trailerResult;
                }
            }
        }
        return result;
    }

    public LangASTNode visitStar_expr(Python3Parser.Star_exprContext ctx) {
        LangASTNode expr = mainBuilder.visit(ctx.expr());
        return LangASTNodeFactory.createPrefixExpression(expr, OperatorEnum.fromSymbol("*").getSymbol(), ctx);
    }


    public LangASTNode visitTestlist_star_expr(Python3Parser.Testlist_star_exprContext ctx) {
        // Handle single element case
        if (ctx.test().size() == 1 && ctx.star_expr().isEmpty()) {
            return mainBuilder.visit(ctx.test(0));
        }

        // Handle multiple elements or star expressions - create a list/tuple
        List<LangASTNode> elements = new ArrayList<>();

        // Add regular test expressions
        for (Python3Parser.TestContext testCtx : ctx.test()) {
            elements.add(mainBuilder.visit(testCtx));
        }

        // Add star expressions
        for (Python3Parser.Star_exprContext starCtx : ctx.star_expr()) {
            elements.add(mainBuilder.visit(starCtx));
        }

        if (elements.size() > 1) {
            return LangASTNodeFactory.createTupleLiteral(ctx, elements);
        }

        // Single element
        return elements.get(0);
    }

    public LangASTNode visitExpr_stmt(Python3Parser.Expr_stmtContext ctx) {
        if (ctx.ASSIGN().isEmpty() && ctx.augassign() == null) {
            LangASTNode expr = mainBuilder.visit(ctx.testlist_star_expr(0));

            if (expr instanceof LangStringLiteral && isModuleLevelDocstring(ctx)) {
                return LangASTNodeFactory.createComment(ctx, ((LangStringLiteral) expr).getValue(), false, true);
            }

            return LangASTNodeFactory.createExpressionStatement(expr, ctx);
        }

        // Augmented assignment (+=, -=, etc.)
        if (ctx.augassign() != null) {
            LangASTNode left = mainBuilder.visit(ctx.testlist_star_expr(0));

            // Grammar: augassign (yield_expr | testlist)
            LangASTNode right = null;
            if (ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
                right = mainBuilder.visit(ctx.yield_expr(0));
            } else if (ctx.testlist() != null) {
                right = mainBuilder.visit(ctx.testlist());
            }

            if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
            if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);

            String operator = ctx.augassign().getText();
            LangAssignment assignment = LangASTNodeFactory.createAssignment(operator, left, right, ctx);
            return LangASTNodeFactory.createExpressionStatement(assignment, ctx);
        }


        List<Python3Parser.Testlist_star_exprContext> parts = ctx.testlist_star_expr();
        int count = parts != null ? parts.size() : 0;

        if (count == 1 && ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
            LangASTNode left = mainBuilder.visit(parts.get(0));
            LangASTNode right = mainBuilder.visit(ctx.yield_expr(0));

            if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
            if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);

            LangAssignment assignment = LangASTNodeFactory.createAssignment("=", left, right, ctx);
            return LangASTNodeFactory.createExpressionStatement(assignment, ctx);
        }

        if (count >= 2 && (ctx.yield_expr() == null || ctx.yield_expr().isEmpty())) {
            LangASTNode right = mainBuilder.visit(parts.get(count - 1));
            for (int i = count - 2; i >= 0; i--) {
                LangASTNode left = mainBuilder.visit(parts.get(i));
                if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
                if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);
                right = LangASTNodeFactory.createAssignment("=", left, right, ctx);
            }
            return LangASTNodeFactory.createExpressionStatement(right, ctx);
        }

        if (count >= 1 && ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
            LangASTNode right = mainBuilder.visit(ctx.yield_expr(0));
            if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);

            for (int i = count - 1; i >= 0; i--) {
                LangASTNode left = mainBuilder.visit(parts.get(i));
                if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
                right = LangASTNodeFactory.createAssignment("=", left, right, ctx);
            }
            return LangASTNodeFactory.createExpressionStatement(right, ctx);
        }

        LangASTNode fallbackExpr = null;
        if (count >= 1) {
            fallbackExpr = mainBuilder.visit(parts.get(0));
            if (fallbackExpr == null) fallbackExpr = LangASTNodeFactory.createSimpleName("UNKNOWN_EXPR", ctx);
        } else if (ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
            fallbackExpr = mainBuilder.visit(ctx.yield_expr(0));
            if (fallbackExpr == null) fallbackExpr = LangASTNodeFactory.createSimpleName("UNKNOWN_YIELD", ctx);
        }
        if (fallbackExpr != null) {
            return LangASTNodeFactory.createExpressionStatement(fallbackExpr, ctx);
        }

        System.err.println("Warning: Unhandled expr_stmt context, creating placeholder");
        LangASTNode placeholder = LangASTNodeFactory.createSimpleName("UNSUPPORTED_EXPR", ctx);
        return LangASTNodeFactory.createExpressionStatement(placeholder, ctx);
    }


    private boolean isModuleLevelDocstring(Python3Parser.Expr_stmtContext ctx) {
        ParserRuleContext parent = ctx.getParent();
        while (parent != null) {
            if (parent instanceof Python3Parser.FuncdefContext ||
                    parent instanceof Python3Parser.ClassdefContext) {
                return false; // Inside a function or class
            }
            if (parent instanceof Python3Parser.File_inputContext) {
                return true; // At module level
            }
            parent = parent.getParent();
        }
        return false;
    }

    public LangASTNode visitExpr(Python3Parser.ExprContext ctx) {
        if (ctx.atom_expr() != null) {
            return mainBuilder.visitAtom_expr(ctx.atom_expr());
        }

        // Unary prefix
        if (ctx.expr().size() == 1 && ctx.getChildCount() > 1) {
            LangASTNode node = mainBuilder.visit(ctx.expr(0)); // operand is the last child
            for (int i = ctx.getChildCount() - 2; i >= 0; i--) { // wrap from right to left
                String symbol = ctx.getChild(i).getText();          // "+", "-", or "~"
                String op = OperatorEnum.fromSymbol(symbol).getSymbol();
                node = LangASTNodeFactory.createPrefixExpression(node, op, ctx);
            }
            return node;
        }

        if (ctx.expr().size() == 2) {
            LangASTNode leftNode = mainBuilder.visit(ctx.expr(0));
            LangASTNode rightNode = mainBuilder.visit(ctx.expr(1));
            String operator = PyASTBuilderUtil.extractOperator(ctx);
            return LangASTNodeFactory.createInfixExpression(leftNode, rightNode, operator, ctx);
        }

        return mainBuilder.visitChildren(ctx);
    }

    public LangASTNode visitComparison(Python3Parser.ComparisonContext ctx) {
        // If there's only one expr, just visit it
        if (ctx.expr().size() == 1) {
            return mainBuilder.visit(ctx.expr(0));
        }

        // If there are multiple expressions connected by comparison operators
        if (ctx.expr().size() >= 2) {
            LangASTNode leftNode = mainBuilder.visit(ctx.expr(0));

            // Check if the first expression failed to parse
            if (leftNode == null) {
                System.err.println("Warning: Left operand is null in comparison: " + ctx.getText());
                return null;
            }

            // For each comparison operator and right operand
            for (int i = 0; i < ctx.comp_op().size(); i++) {
                LangASTNode rightNode = mainBuilder.visit(ctx.expr(i + 1));

                // Check if the right operand is null
                if (rightNode == null) {
                    System.err.println("Warning: Right operand is null in comparison: " + ctx.getText());
                    return leftNode;
                }

                String operator = ctx.comp_op(i).getText();

                // Validate operator as well
                if (operator == null || operator.isEmpty()) {
                    System.err.println("Warning: Operator is null/empty in comparison: " + ctx.getText());
                    return leftNode;
                }

                // Create an infix expression for this comparison
                leftNode = LangASTNodeFactory.createInfixExpression(leftNode, rightNode, operator, ctx);
            }

            return leftNode;
        }

        return mainBuilder.visitChildren(ctx);
    }

    public LangASTNode visitPattern(Python3Parser.PatternContext ctx) {
        // If we have an as_pattern, visit it
        if (ctx.as_pattern() != null) {
            return mainBuilder.visit(ctx.as_pattern());
        }

        //TODO
//        if (ctx.or_pattern() != null) {
//            return mainBuilder.visit(ctx.or_pattern());
//        }

        // Should not occur, but as fallback
        return super.mainBuilder.visitPattern(ctx);
    }

    public LangASTNode visitTrailer(Python3Parser.TrailerContext ctx) {
        // Handle index access: [...]
        if (ctx.OPEN_BRACK() != null && ctx.CLOSE_BRACK() != null && ctx.subscriptlist() != null) {
                LangASTNode index = mainBuilder.visit(ctx.subscriptlist());
                return LangASTNodeFactory.createIndexAccess(ctx, null, index);
        }

        // Handle method calls: (...)
        if (ctx.OPEN_PAREN() != null && ctx.CLOSE_PAREN() != null) {
            LangMethodInvocation methodInvocation = LangASTNodeFactory.createMethodInvocation(ctx);

            // Process arguments if they exist
            if (ctx.arglist() != null) {
                for (Python3Parser.ArgumentContext argCtx : ctx.arglist().argument()) {
                    LangASTNode argNode = mainBuilder.visit(argCtx);
                    if (argNode != null) {
                        methodInvocation.addArgument(argNode);
                    }
                }
            }

            return methodInvocation;
        }

        if (ctx.DOT() != null && ctx.name() != null) {
            String attrName = ctx.name().getText();
            return LangASTNodeFactory.createFieldAccess(null, attrName, ctx);
        }

        return LangASTNodeFactory.createSimpleName(ctx.getText(), ctx);
    }

    public LangASTNode visitSubscriptlist(Python3Parser.SubscriptlistContext ctx) {
        if (ctx.subscript_().size() == 1) {
            return mainBuilder.visit(ctx.subscript_(0));
        }

        // If there are multiple subscripts, create a tuple literal
        List<LangASTNode> subscripts = new ArrayList<>();
        for (Python3Parser.Subscript_Context subscriptCtx : ctx.subscript_()) {
            LangASTNode subscript = mainBuilder.visit(subscriptCtx);
            if (subscript != null) {
                subscripts.add(subscript);
            }
        }

        return LangASTNodeFactory.createTupleLiteral(ctx, subscripts);
    }

    public LangASTNode visitSubscript_(Python3Parser.Subscript_Context ctx) {
        if (ctx.test().size() == 1 && ctx.COLON() == null) {
            return mainBuilder.visit(ctx.test(0));
        }

        LangASTNode lower = null;
        LangASTNode upper = null;
        LangASTNode step = null;

        List<Python3Parser.TestContext> tests = ctx.test();

        // Check if the first child is a test expression, then use it as lower bound
        // First child might be a colon, in which case we use null as lower bound
        if (!tests.isEmpty() && ctx.getChild(0) == tests.get(0)) {
            lower = mainBuilder.visit(tests.get(0));
        }

        if (tests.size() > 1) {
            upper = mainBuilder.visit(tests.get(1));
        } else if (tests.size() == 1 && ctx.getChild(0).getText().equals(":")) {
            upper = mainBuilder.visit(tests.get(0));
            lower = null;
        }

        if (ctx.sliceop() != null) {
            Python3Parser.SliceopContext sliceopCtx = ctx.sliceop();
            if (sliceopCtx.test() != null) {
                step = mainBuilder.visit(sliceopCtx.test());
            }
        }

        return LangASTNodeFactory.createSliceExpression(ctx, lower, upper, step);
    }


    public LangASTNode visitTestlist_comp(Python3Parser.Testlist_compContext ctx) {

        if (ctx.comp_for() != null && !ctx.comp_for().isEmpty()) {
            // visit the first expression and first comp_for
            LangASTNode expr = mainBuilder.visit(ctx.test(0));

            // Process all comp_for clauses
            List<LangComprehensionExpression.LangComprehensionClause> clauses = new ArrayList<>();
            processComprehensionClauses(ctx.comp_for(), clauses);

            // Determine if this is list or generator based on parent context
            if (ctx.getParent() instanceof Python3Parser.AtomContext parent) {
                if (parent.OPEN_BRACK() != null) {
                    return LangASTNodeFactory.createListComprehension(ctx, expr, clauses);
                } else {
                    return LangASTNodeFactory.createGeneratorExpression(ctx, expr, clauses);
                }
            }

            // Default
            return LangASTNodeFactory.createGeneratorExpression(ctx, expr, clauses);
        }

        // Regular testlist
        List<LangASTNode> elements = new ArrayList<>();
        for (Python3Parser.TestContext testCtx : ctx.test()) {
            elements.add(mainBuilder.visit(testCtx));
        }

        if (elements.size() == 1) {
            return elements.get(0);
        } else {
            return LangASTNodeFactory.createTupleLiteral(ctx, elements);
        }
    }

    public LangASTNode visitDictorsetmaker(Python3Parser.DictorsetmakerContext ctx) {

        if (ctx.comp_for() != null && !ctx.comp_for().isEmpty()) {
            // Process all comp_for clauses
            List<LangComprehensionExpression.LangComprehensionClause> clauses = new ArrayList<>();
            processComprehensionClauses(ctx.comp_for(), clauses);

            if (ctx.COLON() != null && !ctx.COLON().isEmpty()) {
                // Dictionary comprehension: {key: value for ...}
                LangASTNode keyExpr = mainBuilder.visit(ctx.test(0));
                LangASTNode valueExpr = mainBuilder.visit(ctx.test(1));
                return LangASTNodeFactory.createDictComprehension(ctx, keyExpr, valueExpr, clauses);
            } else {
                // Set comprehension: {expr for ...}
                LangASTNode expr = mainBuilder.visit(ctx.test(0));
                return LangASTNodeFactory.createSetComprehension(ctx, expr, clauses);
            }
        }

        // Regular dictionary or set literal
        if (ctx.COLON() != null && !ctx.COLON().isEmpty()) {
            LangDictionaryLiteral dict = LangASTNodeFactory.createDictionaryLiteral(ctx);
            if (ctx.test().size() % 2 == 0) {
                for (int i = 0; i < ctx.test().size(); i += 2) {
                    LangASTNode key = mainBuilder.visit(ctx.test(i));
                    LangASTNode value = mainBuilder.visit(ctx.test(i + 1));
                    dict.addEntry(key, value);
                }
            }
            return dict;
        } else {
            List<LangASTNode> elements = new ArrayList<>();
            for (Python3Parser.TestContext testCtx : ctx.test()) {
                elements.add(mainBuilder.visit(testCtx));
            }
            return LangASTNodeFactory.createListLiteral(ctx, elements);
        }
    }


    private void processComprehensionClauses(Python3Parser.Comp_forContext compFor,
                                             List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        if (compFor == null) return;

        boolean isAsync = compFor.ASYNC() != null;

        List<LangASTNode> targets = new ArrayList<>();
        if (compFor.exprlist() != null) {
            LangASTNode targetNode = mainBuilder.visit(compFor.exprlist());
            if (targetNode instanceof LangTupleLiteral tupleLiteral) {
                targets.addAll(tupleLiteral.getElements());
            } else {
                targets.add(targetNode);
            }
        }

        LangExpression iterable = null;
        if (compFor.or_test() != null) {
            LangASTNode iterableNode = mainBuilder.visit(compFor.or_test());
            if (iterableNode instanceof LangExpression) {
                iterable = (LangExpression) iterableNode;
            }
        }

        List<LangASTNode> filters = new ArrayList<>();

        // Create clause for current comp_for before processing comp_iter
        LangComprehensionExpression.LangComprehensionClause clause =
                LangASTNodeFactory.createComprehensionClause(compFor, isAsync, targets, iterable, filters);
        clauses.add(clause);

        if (compFor.comp_iter() != null) {
            // Process comp_iter, handle both filters and nested comp_for
            processCompIter(compFor.comp_iter(), filters, clauses);
        }
    }

    private void processCompIter(Python3Parser.Comp_iterContext compIter,
                                 List<LangASTNode> currentFilters,
                                 List<LangComprehensionExpression.LangComprehensionClause> allClauses) {
        if (compIter == null) return;

        if (compIter.comp_if() != null) {
            // filter condition
            LangASTNode condition = mainBuilder.visit(compIter.comp_if().test_nocond());
            if (condition != null) {
                currentFilters.add(condition);
            }
            // Continue processing nested comp_iter
            if (compIter.comp_if().comp_iter() != null) {
                processCompIter(compIter.comp_if().comp_iter(), currentFilters, allClauses);
            }
        } else if (compIter.comp_for() != null) {
            // This is a nested comp_for, process it as a new clause
            processComprehensionClauses(compIter.comp_for(), allClauses);
        }
    }
}
