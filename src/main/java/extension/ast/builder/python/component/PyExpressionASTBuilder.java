package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.builder.python.PyASTBuilderUtil;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.expression.*;
import extension.ast.node.literal.LangDictionaryLiteral;
import extension.ast.node.literal.LangStringLiteral;
import extension.ast.node.literal.LangTupleLiteral;
import extension.base.lang.python.PythonParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class PyExpressionASTBuilder extends PyBaseASTBuilder {

    public PyExpressionASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitAtom(PythonParser.AtomContext ctx) {
        if (ctx.NUMBER() != null) {
            return LangASTNodeFactory.createNumberLiteral(ctx, ctx.NUMBER().getText());
        }

        if (ctx.getText() != null && "None".equals(ctx.getText())) {
            return LangASTNodeFactory.createNullLiteral(ctx);
        }

        if (ctx.getText() != null && PyASTBuilderUtil.isStringLiteral(ctx)) {
            return LangASTNodeFactory.createStringLiteral(ctx, ctx.getText());
        }

        if (ctx.ELLIPSIS() != null) {
            return LangASTNodeFactory.createEllipsisLiteral(ctx);
        }

        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return LangASTNodeFactory.createBooleanLiteral(ctx, Boolean.parseBoolean(ctx.getText()));
        }

        if (ctx.dict() != null) {
            return mainBuilder.visit(ctx.dict());
        }

        if (ctx.list() != null) {
             return mainBuilder.visit(ctx.list());
        }

        if (ctx.group() != null) {
            return mainBuilder.visit(ctx.group());
        }

        if (ctx.tuple() != null) {
            return mainBuilder.visit(ctx.tuple());
        }

        if (ctx.strings() != null) {
            return mainBuilder.visit(ctx.strings());
        }

        if (ctx.name() != null) {
            return LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx);
        }

        if (ctx.listcomp() != null) return mainBuilder.visit(ctx.listcomp());
        if (ctx.dictcomp() != null) return mainBuilder.visit(ctx.dictcomp());
        if (ctx.setcomp() != null) return mainBuilder.visit(ctx.setcomp());
        if (ctx.set() != null) return mainBuilder.visit(ctx.set());
        if (ctx.genexp() != null) return mainBuilder.visit(ctx.genexp());

        throw new IllegalStateException("Unhandled atom: " + ctx.getText());
    }

    public LangASTNode visitPrimary(PythonParser.PrimaryContext ctx) {
        if (ctx.atom() != null) {
            return mainBuilder.visit(ctx.atom());
        }

        LangASTNode primary = mainBuilder.visit(ctx.primary());

        if (ctx.DOT() != null) {
            return LangASTNodeFactory.createFieldAccess(primary, ctx.name().getText(), ctx);
        }

        if (ctx.LPAR() != null) {
            LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(ctx);
            invocation.setExpression(primary);
            if (primary instanceof LangSimpleName simpleName) {
                invocation.setName(simpleName.getIdentifier());
            } else if (primary instanceof LangFieldAccess fieldAccess) {
                invocation.setName(fieldAccess.getName().getIdentifier());
            }
            if (ctx.arguments() != null) {
                processArguments(ctx.arguments(), invocation);
            }
            return invocation;
        }

        if (ctx.LSQB() != null) {
            return LangASTNodeFactory.createIndexAccess(ctx, primary, mainBuilder.visit(ctx.slices()));
        }
        
        if (ctx.genexp() != null) {
             LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(ctx);
             invocation.setExpression(primary);
             if (primary instanceof LangSimpleName simpleName) {
                 invocation.setName(simpleName.getIdentifier());
             } else if (primary instanceof LangFieldAccess fieldAccess) {
                 invocation.setName(fieldAccess.getName().getIdentifier());
             }
             invocation.addArgument(mainBuilder.visit(ctx.genexp()));
             return invocation;
        }

        return primary;
    }

    private void processArguments(PythonParser.ArgumentsContext ctx, LangMethodInvocation invocation) {
        if (ctx.args() != null) {
            PythonParser.ArgsContext args = ctx.args();

            for (int i = 0; i < args.getChildCount(); i++) {
                ParseTree child = args.getChild(i);
                if (child instanceof PythonParser.ExpressionContext exprCtx) {
                    invocation.addArgument(mainBuilder.visit(exprCtx));
                } else if (child instanceof PythonParser.Starred_expressionContext starredCtx) {
                    // Old builder strips '*' prefix
                    invocation.addArgument(mainBuilder.visit(starredCtx.expression()));
                } else if (child instanceof PythonParser.Assignment_expressionContext assignCtx) {
                    invocation.addArgument(mainBuilder.visit(assignCtx));
                }
            }
            
            if (args.kwargs() != null) {
                processKwargs(args.kwargs(), invocation);
            }
        } else if (ctx.getChildCount() > 0 && ctx.getChild(0) instanceof PythonParser.KwargsContext) {
            processKwargs((PythonParser.KwargsContext) ctx.getChild(0), invocation);
        }
    }

    private void processKwargs(PythonParser.KwargsContext ctx, LangMethodInvocation invocation) {
        for (PythonParser.Kwarg_or_starredContext child : ctx.kwarg_or_starred()) {
            if (child.name() != null) {
                invocation.addArgument(mainBuilder.visit(child.expression()));
            } else {
                invocation.addArgument(mainBuilder.visit(child.starred_expression().expression()));
            }
        }
        for (PythonParser.Kwarg_or_double_starredContext child : ctx.kwarg_or_double_starred()) {
            if (child.name() != null) {
                invocation.addArgument(mainBuilder.visit(child.expression()));
            } else {
                invocation.addArgument(mainBuilder.visit(child.expression()));
            }
        }
    }

    public LangASTNode visitKwarg_or_starred(PythonParser.Kwarg_or_starredContext ctx) {
        if (ctx.name() != null) {
            LangASTNode left = LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
            LangASTNode right = mainBuilder.visit(ctx.expression());
            return LangASTNodeFactory.createAssignment("=", left, right, ctx);
        }
        return mainBuilder.visit(ctx.starred_expression());
    }

    public LangASTNode visitKwarg_or_double_starred(PythonParser.Kwarg_or_double_starredContext ctx) {
        if (ctx.name() != null) {
            LangASTNode left = LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
            LangASTNode right = mainBuilder.visit(ctx.expression());
            return LangASTNodeFactory.createAssignment("=", left, right, ctx);
        }
        LangASTNode expr = mainBuilder.visit(ctx.expression());
        return LangASTNodeFactory.createPrefixExpression(expr, "**", ctx);
    }

    public LangASTNode visitStarred_expression(PythonParser.Starred_expressionContext ctx) {
        LangASTNode expr = mainBuilder.visit(ctx.expression());
        return LangASTNodeFactory.createPrefixExpression(expr, "*", ctx);
    }

    public LangASTNode visitAssignment(PythonParser.AssignmentContext ctx) {

        if (ctx.COLON() != null) {
            if (ctx.name() != null) {
                return LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
            } else {
                if (ctx.single_target() != null) {
                    return mainBuilder.visit(ctx.single_target());
                } else {
                    return mainBuilder.visit(ctx.single_subscript_attribute_target());
                }
            }
        }

        if (ctx.EQUAL() != null && !ctx.EQUAL().isEmpty()) {

            LangASTNode rhs;
            PythonParser.Annotated_rhsContext rhsCtx = ctx.annotated_rhs();

            if (rhsCtx == null) {
                System.err.println("Warning: Null annotated_rhs in assignment: " + ctx.getText());
                rhs = LangASTNodeFactory.createSimpleName("UNKNOWN_RHS", ctx);
            } else if (rhsCtx.yield_expr() != null) {
                rhs = mainBuilder.visit(rhsCtx.yield_expr());
            } else if (rhsCtx.star_expressions() != null) {
                rhs = mainBuilder.visit(rhsCtx.star_expressions());
            } else {
                // Should never happen according to grammar
                System.err.println("Warning: Empty annotated_rhs in assignment: " + ctx.getText());
                rhs = LangASTNodeFactory.createSimpleName("UNKNOWN_RHS", ctx);
            }

            LangASTNode currentRight = rhs;

            for (int i = ctx.star_targets().size() - 1; i >= 0; i--) {
                LangASTNode left = mainBuilder.visit(ctx.star_targets(i));
                currentRight = LangASTNodeFactory.createAssignment(
                        "=",
                        left,
                        currentRight,
                        ctx
                );
            }

            return currentRight;
        }

        if (ctx.augassign() != null) {

            String operator = ctx.augassign().getText();

            LangASTNode leftSide = mainBuilder.visit(ctx.single_target());

            LangASTNode right;
            PythonParser.Annotated_rhsContext rhsCtx = ctx.annotated_rhs();

            if (rhsCtx == null) {
                System.err.println("Warning: Null annotated_rhs in augmented assignment: " + ctx.getText());
                right = LangASTNodeFactory.createSimpleName("UNKNOWN_RHS", ctx);
            } else if (rhsCtx.yield_expr() != null) {
                right = mainBuilder.visit(rhsCtx.yield_expr());
            } else if (rhsCtx.star_expressions() != null) {
                right = mainBuilder.visit(rhsCtx.star_expressions());
            } else {
                System.err.println("Warning: Empty annotated_rhs in augmented assignment: " + ctx.getText());
                right = LangASTNodeFactory.createSimpleName("UNKNOWN_RHS", ctx);
            }

            return LangASTNodeFactory.createAssignment(
                    operator,
                    leftSide,
                    right,
                    ctx
            );
        }

        System.err.println("Warning: Unknown assignment form: " + ctx.getText());
        return null;
    }

    public LangASTNode visitExpression(PythonParser.ExpressionContext ctx) {
        if (ctx.disjunction() != null && !ctx.disjunction().isEmpty()) {
            if (ctx.disjunction().size() > 1 && ctx.expression() != null) {
                LangASTNode thenExpr = mainBuilder.visit(ctx.disjunction(0));
                LangASTNode condition = mainBuilder.visit(ctx.disjunction(1));
                LangASTNode elseExpr = mainBuilder.visit(ctx.expression());
                return LangASTNodeFactory.createTernaryExpression(ctx, condition, thenExpr, elseExpr);
            }
            return mainBuilder.visit(ctx.disjunction(0));
        }
        if (ctx.lambdef() != null) return mainBuilder.visit(ctx.lambdef());
        return null;
    }

    public LangASTNode visitDisjunction(PythonParser.DisjunctionContext ctx) {
        if (ctx.conjunction().size() == 1) return mainBuilder.visit(ctx.conjunction(0));

        LangASTNode left = mainBuilder.visit(ctx.conjunction(0));
        for (int i = 1; i < ctx.conjunction().size(); i++) {
            LangASTNode right = mainBuilder.visit(ctx.conjunction(i));
            left = LangASTNodeFactory.createInfixExpression(left, right, "or", ctx);
        }
        return left;
    }

    public LangASTNode visitConjunction(PythonParser.ConjunctionContext ctx) {
        if (ctx.inversion().size() == 1) return mainBuilder.visit(ctx.inversion(0));

        LangASTNode left = mainBuilder.visit(ctx.inversion(0));
        for (int i = 1; i < ctx.inversion().size(); i++) {
            LangASTNode right = mainBuilder.visit(ctx.inversion(i));
            left = LangASTNodeFactory.createInfixExpression(left, right, "and", ctx);
        }
        return left;
    }

    public LangASTNode visitInversion(PythonParser.InversionContext ctx) {
        if (ctx.NOT() != null) {
            LangASTNode operand = mainBuilder.visit(ctx.inversion());
            return LangASTNodeFactory.createPrefixExpression(operand, "not", ctx);
        }
        return mainBuilder.visit(ctx.comparison());
    }

    public LangASTNode visitComparison(PythonParser.ComparisonContext ctx) {
        if (ctx.compare_op_bitwise_or_pair() == null || ctx.compare_op_bitwise_or_pair().isEmpty()) return mainBuilder.visit(ctx.bitwise_or());
        
        LangASTNode left = mainBuilder.visit(ctx.bitwise_or());
        for (PythonParser.Compare_op_bitwise_or_pairContext pair : ctx.compare_op_bitwise_or_pair()) {
            String op = PyASTBuilderUtil.extractOperator(pair);
            LangASTNode right = null;
            if (pair.eq_bitwise_or() != null) right = mainBuilder.visit(pair.eq_bitwise_or().bitwise_or());
            else if (pair.noteq_bitwise_or() != null) right = mainBuilder.visit(pair.noteq_bitwise_or().bitwise_or());
            else if (pair.lte_bitwise_or() != null) right = mainBuilder.visit(pair.lte_bitwise_or().bitwise_or());
            else if (pair.lt_bitwise_or() != null) right = mainBuilder.visit(pair.lt_bitwise_or().bitwise_or());
            else if (pair.gte_bitwise_or() != null) right = mainBuilder.visit(pair.gte_bitwise_or().bitwise_or());
            else if (pair.gt_bitwise_or() != null) right = mainBuilder.visit(pair.gt_bitwise_or().bitwise_or());
            else if (pair.notin_bitwise_or() != null) right = mainBuilder.visit(pair.notin_bitwise_or().bitwise_or());
            else if (pair.in_bitwise_or() != null) right = mainBuilder.visit(pair.in_bitwise_or().bitwise_or());
            else if (pair.isnot_bitwise_or() != null) right = mainBuilder.visit(pair.isnot_bitwise_or().bitwise_or());
            else if (pair.is_bitwise_or() != null) right = mainBuilder.visit(pair.is_bitwise_or().bitwise_or());
            
            left = LangASTNodeFactory.createInfixExpression(left, right, op, ctx);
        }
        return left;
    }

    public LangASTNode visitBitwise_or(PythonParser.Bitwise_orContext ctx) {
        if (ctx.bitwise_or() == null) return mainBuilder.visit(ctx.bitwise_xor());
        
        LangASTNode left = mainBuilder.visit(ctx.bitwise_or());
        LangASTNode right = mainBuilder.visit(ctx.bitwise_xor());
        return LangASTNodeFactory.createInfixExpression(left, right, "|", ctx);
    }

    public LangASTNode visitBitwise_xor(PythonParser.Bitwise_xorContext ctx) {
        if (ctx.bitwise_xor() == null) return mainBuilder.visit(ctx.bitwise_and());
        
        LangASTNode left = mainBuilder.visit(ctx.bitwise_xor());
        LangASTNode right = mainBuilder.visit(ctx.bitwise_and());
        return LangASTNodeFactory.createInfixExpression(left, right, "^", ctx);
    }

    public LangASTNode visitBitwise_and(PythonParser.Bitwise_andContext ctx) {
        if (ctx.bitwise_and() == null) return mainBuilder.visit(ctx.shift_expr());
        
        LangASTNode left = mainBuilder.visit(ctx.bitwise_and());
        LangASTNode right = mainBuilder.visit(ctx.shift_expr());
        return LangASTNodeFactory.createInfixExpression(left, right, "&", ctx);
    }

    public LangASTNode visitShift_expr(PythonParser.Shift_exprContext ctx) {
        if (ctx.shift_expr() == null) return mainBuilder.visit(ctx.sum());
        
        LangASTNode left = mainBuilder.visit(ctx.shift_expr());
        LangASTNode right = mainBuilder.visit(ctx.sum());
        String op = ctx.getChild(1).getText();
        return LangASTNodeFactory.createInfixExpression(left, right, op, ctx);
    }

    public LangASTNode visitSum(PythonParser.SumContext ctx) {
        if (ctx.sum() == null) return mainBuilder.visit(ctx.term());
        
        LangASTNode left = mainBuilder.visit(ctx.sum());
        LangASTNode right = mainBuilder.visit(ctx.term());
        String op = ctx.getChild(1).getText();
        return LangASTNodeFactory.createInfixExpression(left, right, op, ctx);
    }

    public LangASTNode visitTerm(PythonParser.TermContext ctx) {
        if (ctx.term() == null) return mainBuilder.visit(ctx.factor());
        
        LangASTNode left = mainBuilder.visit(ctx.term());
        LangASTNode right = mainBuilder.visit(ctx.factor());
        String op = ctx.getChild(1).getText();
        return LangASTNodeFactory.createInfixExpression(left, right, op, ctx);
    }

    public LangASTNode visitFactor(PythonParser.FactorContext ctx) {
        if (ctx.power() != null) return mainBuilder.visit(ctx.power());
        
        String op = ctx.getChild(0).getText();
        LangASTNode operand = mainBuilder.visit(ctx.factor());
        return LangASTNodeFactory.createPrefixExpression(operand, op, ctx);
    }

    public LangASTNode visitPower(PythonParser.PowerContext ctx) {
        LangASTNode left = mainBuilder.visit(ctx.await_primary());
        if (ctx.factor() != null) {
            LangASTNode right = mainBuilder.visit(ctx.factor());
            return LangASTNodeFactory.createInfixExpression(left, right, "**", ctx);
        }
        return left;
    }

    public LangASTNode visitAwait_primary(PythonParser.Await_primaryContext ctx) {
        if (ctx.AWAIT() != null) {
            PythonParser.PrimaryContext primary = ctx.primary();
            while (primary.atom() == null && primary.primary() != null) {
                primary = primary.primary();
            }
            LangASTNode operand;
            if (primary.atom() != null) {
                operand = mainBuilder.visit(primary.atom());
            } else {
                operand = mainBuilder.visit(ctx.primary());
            }
            return LangASTNodeFactory.createAwaitExpression(ctx, operand);
        }
        return mainBuilder.visit(ctx.primary());
    }

    public LangASTNode visitList(PythonParser.ListContext ctx) {
        List<LangASTNode> elements = new ArrayList<>();
        if (ctx.star_named_expressions() != null) {
            for (PythonParser.Star_named_expressionContext exprCtx : ctx.star_named_expressions().star_named_expression()) {
                LangASTNode expr = mainBuilder.visit(exprCtx);
                if (expr != null) elements.add(expr);
            }
        }
        if (elements.size() == 1) {
            return elements.get(0);
        } else if (elements.size() > 1) {
            return LangASTNodeFactory.createTupleLiteral(ctx, elements);
        }
        return LangASTNodeFactory.createListLiteral(ctx, elements);
    }

    public LangASTNode visitTuple(PythonParser.TupleContext ctx) {
        List<LangASTNode> elements = new ArrayList<>();
        
        if (ctx.star_named_expression() != null) {
            LangASTNode firstExpr = mainBuilder.visit(ctx.star_named_expression());
            if (firstExpr != null) elements.add(firstExpr);
        }
        
        if (ctx.star_named_expressions() != null) {
            for (PythonParser.Star_named_expressionContext exprCtx : ctx.star_named_expressions().star_named_expression()) {
                LangASTNode expr = mainBuilder.visit(exprCtx);
                if (expr != null) elements.add(expr);
            }
        }
        return LangASTNodeFactory.createTupleLiteral(ctx, elements);
    }

    public LangASTNode visitSet(PythonParser.SetContext ctx) {
        List<LangASTNode> elements = new ArrayList<>();
        if (ctx.star_named_expressions() != null) {
            for (PythonParser.Star_named_expressionContext exprCtx : ctx.star_named_expressions().star_named_expression()) {
                LangASTNode expr = mainBuilder.visit(exprCtx);
                if (expr != null) elements.add(expr);
            }
        }
        return LangASTNodeFactory.createListLiteral(ctx, elements);
    }


    public LangASTNode visitDict(PythonParser.DictContext ctx) {
        LangDictionaryLiteral dict = LangASTNodeFactory.createDictionaryLiteral(ctx);
        if (ctx.double_starred_kvpairs() != null) {
            for (PythonParser.Double_starred_kvpairContext kvCtx : ctx.double_starred_kvpairs().double_starred_kvpair()) {
                if (kvCtx.kvpair() != null) {
                    LangASTNode key = mainBuilder.visit(kvCtx.kvpair().expression(0));
                    LangASTNode value = mainBuilder.visit(kvCtx.kvpair().expression(1));
                    dict.addEntry(key, value);
                } else if (kvCtx.bitwise_or() != null) {
                    // Handle **expression
                    LangASTNode value = mainBuilder.visit(kvCtx.bitwise_or());
                    dict.addEntry(null, LangASTNodeFactory.createPrefixExpression(value, "**", kvCtx));
                }
            }
        }
        return dict;
    }

    public LangASTNode visitStar_named_expression(PythonParser.Star_named_expressionContext ctx) {
        if (ctx.STAR() != null) {
            LangASTNode expr = mainBuilder.visit(ctx.named_expression());
            return LangASTNodeFactory.createPrefixExpression(expr, "*", ctx);
        }
        return mainBuilder.visit(ctx.named_expression());
    }

    public LangASTNode visitStar_expressions(PythonParser.Star_expressionsContext ctx) {
        if (ctx.star_expression().size() == 1) {
            return mainBuilder.visit(ctx.star_expression(0));
        }
        List<LangASTNode> elements = new ArrayList<>();
        for (PythonParser.Star_expressionContext exprCtx : ctx.star_expression()) {
            LangASTNode expr = mainBuilder.visit(exprCtx);
            if (expr != null) elements.add(expr);
        }
        return LangASTNodeFactory.createTupleLiteral(ctx, elements);
    }

    public LangASTNode visitStar_expression(PythonParser.Star_expressionContext ctx) {
        if (ctx.STAR() != null) {
            LangASTNode expr = mainBuilder.visit(ctx.expression());
            return LangASTNodeFactory.createPrefixExpression(expr, "*", ctx);
        }
        return mainBuilder.visit(ctx.expression());
    }

    public LangASTNode visitListcomp(PythonParser.ListcompContext ctx) {
        LangASTNode expression = mainBuilder.visit(ctx.named_expression());
        List<LangComprehensionExpression.LangComprehensionClause> clauses = new ArrayList<>();
        processComprehensionClauses(ctx.for_if_clauses(), clauses);
        return LangASTNodeFactory.createListComprehension(ctx, expression, clauses);
    }

    public LangASTNode visitSetcomp(PythonParser.SetcompContext ctx) {
        LangASTNode expression = mainBuilder.visit(ctx.named_expression());
        List<LangComprehensionExpression.LangComprehensionClause> clauses = new ArrayList<>();
        processComprehensionClauses(ctx.for_if_clauses(), clauses);
        return LangASTNodeFactory.createSetComprehension(ctx, expression, clauses);
    }

    public LangASTNode visitGenexp(PythonParser.GenexpContext ctx) {
        LangASTNode expression = ctx.assignment_expression() != null ? mainBuilder.visit(ctx.assignment_expression()) : mainBuilder.visit(ctx.expression());
        List<LangComprehensionExpression.LangComprehensionClause> clauses = new ArrayList<>();
        processComprehensionClauses(ctx.for_if_clauses(), clauses);
        return LangASTNodeFactory.createGeneratorExpression(ctx, expression, clauses);
    }

    public LangASTNode visitDictcomp(PythonParser.DictcompContext ctx) {
        LangASTNode key = mainBuilder.visit(ctx.kvpair().expression(0));
        LangASTNode value = mainBuilder.visit(ctx.kvpair().expression(1));
        List<LangComprehensionExpression.LangComprehensionClause> clauses = new ArrayList<>();
        processComprehensionClauses(ctx.for_if_clauses(), clauses);
        return LangASTNodeFactory.createDictComprehension(ctx, key, value, clauses);
    }

    private void processComprehensionClauses(PythonParser.For_if_clausesContext ctx, List<LangComprehensionExpression.LangComprehensionClause> clauses) {
        for (PythonParser.For_if_clauseContext clauseCtx : ctx.for_if_clause()) {
            boolean isAsync = clauseCtx.ASYNC() != null;
            List<LangASTNode> targets = new ArrayList<>();
            LangASTNode targetNode = mainBuilder.visit(clauseCtx.star_targets());
            if (targetNode != null) {
                targets.add(targetNode);
            }
            LangExpression iterable = null;
            if (clauseCtx.disjunction() != null && !clauseCtx.disjunction().isEmpty()) {
                LangASTNode iterableNode = mainBuilder.visit(clauseCtx.disjunction(0));
                if (iterableNode instanceof LangExpression) {
                    iterable = (LangExpression) iterableNode;
                }
            }
            List<LangASTNode> filters = new ArrayList<>();
            if (clauseCtx.disjunction() != null && clauseCtx.disjunction().size() > 1) {
                for (int i = 1; i < clauseCtx.disjunction().size(); i++) {
                    LangASTNode filterNode = mainBuilder.visit(clauseCtx.disjunction(i));
                    if (filterNode != null) {
                        filters.add(filterNode);
                    }
                }
            }
            clauses.add(LangASTNodeFactory.createComprehensionClause(clauseCtx, isAsync, targets, iterable, filters));
        }
    }

    public LangASTNode visitLambdef(PythonParser.LambdefContext ctx) {
        List<LangASTNode> parameters = new ArrayList<>();
        if (ctx.lambda_params() != null && ctx.lambda_params().lambda_parameters() != null) {
            processLambdaParameters(ctx.lambda_params().lambda_parameters(), parameters);
        }
        LangASTNode body = mainBuilder.visit(ctx.expression());
        return LangASTNodeFactory.createLambdaExpression(ctx, body, parameters);
    }

    private void processLambdaParameters(PythonParser.Lambda_parametersContext ctx, List<LangASTNode> parameters) {
        if (ctx.lambda_slash_no_default() != null) {
            for (PythonParser.Lambda_param_no_defaultContext p : ctx.lambda_slash_no_default().lambda_param_no_default()) {
                parameters.add(createLambdaParam(p.lambda_param(), null, false, false));
            }
        }
        if (ctx.lambda_slash_with_default() != null) {
            for (PythonParser.Lambda_param_no_defaultContext p : ctx.lambda_slash_with_default().lambda_param_no_default()) {
                parameters.add(createLambdaParam(p.lambda_param(), null, false, false));
            }
            for (PythonParser.Lambda_param_with_defaultContext p : ctx.lambda_slash_with_default().lambda_param_with_default()) {
                parameters.add(createLambdaParam(p.lambda_param(), p.default_assignment().expression(), false, false));
            }
        }
        if (ctx.lambda_param_no_default() != null) {
            for (PythonParser.Lambda_param_no_defaultContext p : ctx.lambda_param_no_default()) {
                parameters.add(createLambdaParam(p.lambda_param(), null, false, false));
            }
        }
        if (ctx.lambda_param_with_default() != null) {
            for (PythonParser.Lambda_param_with_defaultContext p : ctx.lambda_param_with_default()) {
                parameters.add(createLambdaParam(p.lambda_param(), p.default_assignment().expression(), false, false));
            }
        }
        if (ctx.lambda_star_etc() != null) {
            PythonParser.Lambda_star_etcContext s = ctx.lambda_star_etc();
            if (s.lambda_param_no_default() != null) {
                parameters.add(createLambdaParam(s.lambda_param_no_default().lambda_param(), null, true, false));
            }
            if (s.lambda_param_maybe_default() != null) {
                for (PythonParser.Lambda_param_maybe_defaultContext p : s.lambda_param_maybe_default()) {
                    parameters.add(createLambdaParam(p.lambda_param(), p.default_assignment() != null ? p.default_assignment().expression() : null, false, false));
                }
            }
            if (s.lambda_kwds() != null) {
                parameters.add(createLambdaParam(s.lambda_kwds().lambda_param_no_default().lambda_param(), null, false, true));
            }
        }
    }

    private LangASTNode createLambdaParam(PythonParser.Lambda_paramContext p, PythonParser.ExpressionContext defaultVal, boolean isStar, boolean isDoubleStar) {
        LangASTNode defaultValueExpression = defaultVal != null ? mainBuilder.visit(defaultVal) : null;
        extension.ast.node.declaration.LangSingleVariableDeclaration decl = LangASTNodeFactory.createSingleVariableDeclaration(p.name().getText(), defaultValueExpression, p);
        decl.setParameter(true);
        decl.setVarArgs(isStar);
        decl.setKwArgs(isDoubleStar);
        return decl;
    }

    public LangASTNode visitStrings(PythonParser.StringsContext ctx) {

        boolean hasFString = !ctx.fstring().isEmpty();
        boolean hasTString = !ctx.tstring().isEmpty();
        boolean hasPlainString = !ctx.string().isEmpty();

        if (!hasFString && !hasTString && hasPlainString) {
            StringBuilder value = new StringBuilder();

            for (PythonParser.StringContext s : ctx.string()) {
                LangASTNode node = mainBuilder.visit(s);
                if (node instanceof LangStringLiteral lit) {
                    value.append(lit.getValue());
                } else if (node != null) {
                    value.append(s.getText());
                }
            }

            return LangASTNodeFactory.createStringLiteral(ctx, value.toString());
        }

        List<LangTemplateExpressionPart> parts = new ArrayList<>();

        for (ParseTree child : ctx.children) {
            LangASTNode node = mainBuilder.visit(child);
            if (node == null) continue;

            if (node instanceof LangTemplateStringExpression tmpl) {
                parts.addAll(tmpl.getParts());
            } else if (node instanceof LangTemplateExpressionPart part) {
                parts.add(part);
            } else if (node instanceof LangStringLiteral lit) {
                LangTemplateExpressionPart part = LangASTNodeFactory.createTemplateExpressionPart(
                        (ParserRuleContext) child, lit, null
                );
                part.addChild(lit);
                parts.add(part);
            }
        }

        LangTemplateStringExpression.TemplateStringKind kind =
                hasTString ? LangTemplateStringExpression.TemplateStringKind.TSTRING :
                        LangTemplateStringExpression.TemplateStringKind.FSTRING;

        return LangASTNodeFactory.createTemplateStringExpression(
                ctx,
                kind,
                parts
        );
    }

    public LangASTNode visitString(PythonParser.StringContext ctx) {
        System.out.println("String: " + ctx.getText());
        return LangASTNodeFactory.createStringLiteral(ctx, ctx.getText());
    }

    public LangASTNode visitFstring(PythonParser.FstringContext ctx) {
        List<LangTemplateExpressionPart> parts = new ArrayList<>();

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                int type = ((TerminalNode) child).getSymbol().getType();
                if (type == PythonParser.FSTRING_START || type == PythonParser.FSTRING_END) {
                    continue;
                }
                
                String tokenText = child.getText();
                if (!tokenText.isEmpty()) {
                    LangStringLiteral stringPart = LangASTNodeFactory.createStringLiteral((ParserRuleContext) child, tokenText);
                    LangTemplateExpressionPart part = LangASTNodeFactory.createTemplateExpressionPart(null, stringPart, null);
                    part.addChild(stringPart);
                    parts.add(part);
                }
            } else {
                LangASTNode node = mainBuilder.visit(child);
                if (node instanceof LangTemplateExpressionPart part) {
                    parts.add(part);
                } else if (node != null) {
                    LangTemplateExpressionPart part = LangASTNodeFactory.createTemplateExpressionPart((ParserRuleContext) child, node, null);
                    part.addChild(node);
                    parts.add(part);
                }
            }
        }

        return LangASTNodeFactory.createTemplateStringExpression(ctx,
                LangTemplateStringExpression.TemplateStringKind.FSTRING, parts);
    }

    public LangASTNode visitFstring_middle(PythonParser.Fstring_middleContext ctx) {
        if (ctx.fstring_replacement_field() != null) {
            return mainBuilder.visit(ctx.fstring_replacement_field());
        } else if (ctx.FSTRING_MIDDLE() != null) {
            String text = ctx.FSTRING_MIDDLE().getText();
            return LangASTNodeFactory.createStringLiteral(ctx, text);
        }
        return null;
    }

    public LangASTNode visitFstring_replacement_field(
            PythonParser.Fstring_replacement_fieldContext ctx) {

        LangASTNode expr = null;
        if (ctx.annotated_rhs() != null) {
            expr = mainBuilder.visit(ctx.annotated_rhs());
        }

        LangASTNode format = null;
        if (ctx.fstring_full_format_spec() != null) {
            format = mainBuilder.visit(ctx.fstring_full_format_spec());
        }

        LangTemplateExpressionPart part = LangASTNodeFactory.createTemplateExpressionPart(ctx, expr, format);
        if (expr != null) part.addChild(expr);
        if (format != null) part.addChild(format);
        
        if (ctx.EQUAL() != null) {
            part.setDebug(true);
        }
        
        if (ctx.fstring_conversion() != null) {
            part.setConversion(ctx.fstring_conversion().name().getText());
        }

        return part;
    }

    public LangASTNode visitFstring_full_format_spec(PythonParser.Fstring_full_format_specContext ctx) {
        List<LangASTNode> parts = new ArrayList<>();
        for (PythonParser.Fstring_format_specContext spec : ctx.fstring_format_spec()) {
            parts.add(mainBuilder.visit(spec));
        }

        if (parts.size() == 1) return parts.get(0);

        return parts.isEmpty() ? null : parts.get(0);
    }

    public LangASTNode visitFstring_format_spec(PythonParser.Fstring_format_specContext ctx) {
        if (ctx.FSTRING_MIDDLE() != null) {
            return LangASTNodeFactory.createStringLiteral(ctx, ctx.FSTRING_MIDDLE().getText());
        }
        if (ctx.fstring_replacement_field() != null) {
            return mainBuilder.visit(ctx.fstring_replacement_field());
        }
        return null;
    }

    public LangASTNode visitTstring(PythonParser.TstringContext ctx) {
        List<LangTemplateExpressionPart> parts = new ArrayList<>();

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                int type = ((TerminalNode) child).getSymbol().getType();
                if (type == PythonParser.TSTRING_START || type == PythonParser.TSTRING_END) {
                    continue;
                }
                
                String tokenText = child.getText();
                if (!tokenText.isEmpty()) {
                    LangStringLiteral stringPart = LangASTNodeFactory.createStringLiteral((ParserRuleContext) child, tokenText);
                    LangTemplateExpressionPart part = LangASTNodeFactory.createTemplateExpressionPart((ParserRuleContext) child, stringPart, null);
                    parts.add(part);
                }
            } else {
                LangASTNode node = mainBuilder.visit(child);
                if (node instanceof LangTemplateExpressionPart part) {
                    parts.add(part);
                } else if (node != null) {
                    parts.add(LangASTNodeFactory.createTemplateExpressionPart((ParserRuleContext) child, node, null));
                }
            }
        }

        return LangASTNodeFactory.createTemplateStringExpression(ctx, LangTemplateStringExpression.TemplateStringKind.TSTRING, parts);
    }

    public LangASTNode visitTstring_middle(PythonParser.Tstring_middleContext ctx) {
        if (ctx.tstring_replacement_field() != null) {
            return mainBuilder.visit(ctx.tstring_replacement_field());
        } else if (ctx.TSTRING_MIDDLE() != null) {
            String text = ctx.TSTRING_MIDDLE().getText();
            return LangASTNodeFactory.createStringLiteral(ctx, text);
        }
        return null;
    }

    public LangASTNode visitTstring_replacement_field(PythonParser.Tstring_replacement_fieldContext ctx) {
        LangASTNode expr = null;
        if (ctx.annotated_rhs() != null) {
            expr = mainBuilder.visit(ctx.annotated_rhs());
        }

        LangASTNode format = null;
        if (ctx.tstring_full_format_spec() != null) {
            format = mainBuilder.visit(ctx.tstring_full_format_spec());
        }

        LangTemplateExpressionPart part = LangASTNodeFactory.createTemplateExpressionPart(ctx, expr, format);
        
        if (ctx.EQUAL() != null) {
            part.setDebug(true);
        }
        
        if (ctx.fstring_conversion() != null) {
            part.setConversion(ctx.fstring_conversion().name().getText());
        }

        return part;
    }

    public LangASTNode visitTstring_full_format_spec(PythonParser.Tstring_full_format_specContext ctx) {
        List<LangASTNode> parts = new ArrayList<>();
        for (PythonParser.Tstring_format_specContext spec : ctx.tstring_format_spec()) {
            parts.add(mainBuilder.visit(spec));
        }
        return parts.isEmpty() ? null : parts.get(0);
    }

    public LangASTNode visitTstring_format_spec(PythonParser.Tstring_format_specContext ctx) {
        if (ctx.TSTRING_MIDDLE() != null) {
            return LangASTNodeFactory.createStringLiteral(ctx, ctx.TSTRING_MIDDLE().getText());
        }
        if (ctx.tstring_replacement_field() != null) {
            return mainBuilder.visit(ctx.tstring_replacement_field());
        }
        return null;
    }

    public LangASTNode visitYield_expr(PythonParser.Yield_exprContext ctx) {
        LangASTNode expression = ctx.star_expressions() != null ? mainBuilder.visit(ctx.star_expressions()) : null;
        return LangASTNodeFactory.createYieldStatement(ctx, expression);
    }

    public LangASTNode visitStar_targets(PythonParser.Star_targetsContext ctx) {
        if (ctx.star_target().size() == 1) {
            return mainBuilder.visit(ctx.star_target(0));
        }
        List<LangASTNode> elements = new ArrayList<>();
        for (PythonParser.Star_targetContext exprCtx : ctx.star_target()) {
            LangASTNode expr = mainBuilder.visit(exprCtx);
            if (expr != null) elements.add(expr);
        }
        return LangASTNodeFactory.createTupleLiteral(ctx, elements);
    }

    public LangASTNode visitStar_target(PythonParser.Star_targetContext ctx) {
        if (ctx.STAR() != null) {
            LangASTNode expr = mainBuilder.visit(ctx.star_target());
            return LangASTNodeFactory.createPrefixExpression(expr, "*", ctx);
        }
        return mainBuilder.visit(ctx.target_with_star_atom());
    }

    public LangASTNode visitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx) {
        if (ctx.t_primary() != null) {
            LangASTNode primary = mainBuilder.visit(ctx.t_primary());
            if (ctx.DOT() != null) {
                return LangASTNodeFactory.createFieldAccess(primary, ctx.name().getText(), ctx);
            }
            if (ctx.LSQB() != null) {
                return LangASTNodeFactory.createIndexAccess(ctx, primary, mainBuilder.visit(ctx.slices()));
            }
            return primary;
        }
        return mainBuilder.visit(ctx.star_atom());
    }

    public LangASTNode visitStar_atom(PythonParser.Star_atomContext ctx) {
        if (ctx.name() != null) return LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx);
        if (ctx.target_with_star_atom() != null) return mainBuilder.visit(ctx.target_with_star_atom());
        if (ctx.star_targets_tuple_seq() != null) {
            List<LangASTNode> elements = new ArrayList<>();
            for (PythonParser.Star_targetContext exprCtx : ctx.star_targets_tuple_seq().star_target()) {
                LangASTNode expr = mainBuilder.visit(exprCtx);
                if (expr != null) elements.add(expr);
            }
            return LangASTNodeFactory.createTupleLiteral(ctx, elements);
        }
        if (ctx.star_targets_list_seq() != null) {
            List<LangASTNode> elements = new ArrayList<>();
            for (PythonParser.Star_targetContext exprCtx : ctx.star_targets_list_seq().star_target()) {
                LangASTNode expr = mainBuilder.visit(exprCtx);
                if (expr != null) elements.add(expr);
            }
            return LangASTNodeFactory.createListLiteral(ctx, elements);
        }
        return null;
    }

    public LangASTNode visitT_primary(PythonParser.T_primaryContext ctx) {
        if (ctx.atom() != null) return mainBuilder.visit(ctx.atom());
        LangASTNode primary = mainBuilder.visit(ctx.t_primary());
        if (ctx.DOT() != null) return LangASTNodeFactory.createFieldAccess(primary, ctx.name().getText(), ctx);
        if (ctx.LSQB() != null) return LangASTNodeFactory.createIndexAccess(ctx, primary, mainBuilder.visit(ctx.slices()));
        if (ctx.genexp() != null) return mainBuilder.visit(ctx.genexp());
        if (ctx.LPAR() != null) {
            LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(ctx);
            invocation.setExpression(primary);
            if (primary instanceof LangSimpleName simpleName) {
                invocation.setName(simpleName.getIdentifier());
            } else if (primary instanceof LangFieldAccess fieldAccess) {
                invocation.setName(fieldAccess.getName().getIdentifier());
            }
            if (ctx.arguments() != null) processArguments(ctx.arguments(), invocation);
            return invocation;
        }
        return primary;
    }

    public LangASTNode visitNamed_expression(PythonParser.Named_expressionContext ctx) {
        if (ctx.assignment_expression() != null) {
            return mainBuilder.visit(ctx.assignment_expression());
        }
        return mainBuilder.visit(ctx.expression());
    }

    public LangASTNode visitSingle_target(PythonParser.Single_targetContext ctx) {

        if (ctx.single_subscript_attribute_target() != null) {
            return mainBuilder.visit(ctx.single_subscript_attribute_target());
        }

        if (ctx.name() != null) {
            return LangASTNodeFactory.createSimpleName(
                    ctx.name().getText(),
                    ctx
            );
        }

        if (ctx.single_target() != null) {
            return mainBuilder.visit(ctx.single_target());
        }

        return null;
    }

    public LangASTNode visitSingle_subscript_attribute_target(
            PythonParser.Single_subscript_attribute_targetContext ctx) {

        LangASTNode base = mainBuilder.visit(ctx.t_primary());

        if (ctx.name() != null) {
            return LangASTNodeFactory.createFieldAccess(
                    base,
                    ctx.name().getText(),
                    ctx
            );
        }

        if (ctx.slices() != null) {
            LangASTNode slices = mainBuilder.visit(ctx.slices());
            return LangASTNodeFactory.createIndexAccess(ctx, base, slices);
        }

        return base;
    }

    public LangASTNode visitGroup(PythonParser.GroupContext ctx) {
        LangASTNode innerExpression;

        if (ctx.yield_expr() != null) {
            innerExpression = mainBuilder.visit(ctx.yield_expr());
        } else if (ctx.named_expression() != null) {
            innerExpression = mainBuilder.visit(ctx.named_expression());
        } else {
            // Should never happen according to grammar
            throw new IllegalStateException("Empty group: " + ctx.getText());
        }

        return LangASTNodeFactory.createParenthesizedExpression(ctx, innerExpression);
    }

    public LangASTNode visitAssignment_expression(PythonParser.Assignment_expressionContext ctx) {
        LangASTNode left = LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
        LangASTNode right = mainBuilder.visit(ctx.expression());
        return LangASTNodeFactory.createAssignment(":=", left, right, ctx);
    }

    public LangASTNode visitSlices(PythonParser.SlicesContext ctx) {
        if (ctx.slice().size() == 1) {
            return mainBuilder.visit(ctx.slice(0));
        }
        List<LangASTNode> elements = new ArrayList<>();
        for (PythonParser.SliceContext sliceCtx : ctx.slice()) {
            LangASTNode slice = mainBuilder.visit(sliceCtx);
            if (slice != null) {
                elements.add(slice);
            }
        }
        LangTupleLiteral tuple = LangASTNodeFactory.createTupleLiteral(ctx, elements);
        tuple.setParenthesized(false);
        return tuple;
    }

    public LangASTNode visitSlice(PythonParser.SliceContext ctx) {
        if (ctx.named_expression() != null) {
            return mainBuilder.visit(ctx.named_expression());
        }

        LangASTNode lower = null;
        LangASTNode upper = null;
        LangASTNode step = null;

        int colonCount = ctx.COLON().size();

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof PythonParser.ExpressionContext) {
                LangASTNode expr = mainBuilder.visit(child);
                if (lower == null && colonCount > 0 && i < ctx.children.indexOf(ctx.COLON(0))) {
                    lower = expr;
                } else if (upper == null && colonCount > 0 && i > ctx.children.indexOf(ctx.COLON(0))
                        && (colonCount == 1 || i < ctx.children.indexOf(ctx.COLON(1)))) {
                    upper = expr;
                } else if (step == null && colonCount == 2 && i > ctx.children.indexOf(ctx.COLON(1))) {
                    step = expr;
                }
            }
        }

        return LangASTNodeFactory.createSliceExpression(ctx, lower, upper, step);
    }

    public LangASTNode visitName(PythonParser.NameContext ctx) {
        return LangASTNodeFactory.createSimpleName(ctx.getText(), ctx);
    }

    public LangASTNode visitAttr(PythonParser.AttrContext ctx) {
        List<PythonParser.NameContext> names = ctx.name();
        LangASTNode result = LangASTNodeFactory.createSimpleName(names.get(0).getText(), names.get(0));
        for (int i = 1; i < names.size(); i++) {
            result = LangASTNodeFactory.createFieldAccess(result, names.get(i).getText(), ctx);
        }
        return result;
    }

    public LangASTNode visitName_or_attr(PythonParser.Name_or_attrContext ctx) {
        List<PythonParser.NameContext> names = ctx.name();
        LangASTNode result = LangASTNodeFactory.createSimpleName(names.get(0).getText(), names.get(0));
        for (int i = 1; i < names.size(); i++) {
            result = LangASTNodeFactory.createFieldAccess(result, names.get(i).getText(), ctx);
        }
        return result;
    }

    public LangASTNode visitLiteral_expr(PythonParser.Literal_exprContext ctx) {
        if (ctx.strings() != null) return mainBuilder.visit(ctx.strings());
        if (ctx.signed_number() != null) return LangASTNodeFactory.createNumberLiteral(ctx, ctx.signed_number().getText());
        if (ctx.complex_number() != null) return LangASTNodeFactory.createNumberLiteral(ctx, ctx.complex_number().getText());
        return null;
    }
}
