package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.expression.LangExpression;
import extension.ast.node.expression.LangMethodInvocation;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.literal.LangDictionaryLiteral;
import extension.ast.node.literal.LangTupleLiteral;
import extension.ast.node.statement.LangBlock;
import extension.base.lang.python.PythonParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class PyStatementASTBuilder extends PyBaseASTBuilder {

    public PyStatementASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitBlock(PythonParser.BlockContext ctx) {
        List<LangASTNode> statements = new ArrayList<>();
        if (ctx.statements() != null) {
            for (PythonParser.StatementContext stmtCtx : ctx.statements().statement()) {
                LangASTNode stmt = mainBuilder.visit(stmtCtx);
                if (stmt != null) {
                    statements.add(stmt);
                }
            }
        }
        return LangASTNodeFactory.createBlock(ctx, statements);
    }

    public LangASTNode visitStatement(PythonParser.StatementContext ctx) {
        if (ctx.compound_stmt() != null) {
            return mainBuilder.visit(ctx.compound_stmt());
        }
        if (ctx.simple_stmts() != null) {
            return mainBuilder.visit(ctx.simple_stmts());
        }
        return null;
    }

    public LangASTNode visitSimple_stmts(PythonParser.Simple_stmtsContext ctx) {
        if (ctx.simple_stmt().size() == 1) {
            return mainBuilder.visit(ctx.simple_stmt(0));
        }
        List<LangASTNode> statements = new ArrayList<>();
        for (PythonParser.Simple_stmtContext stmtCtx : ctx.simple_stmt()) {
            LangASTNode stmt = mainBuilder.visit(stmtCtx);
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        return LangASTNodeFactory.createBlock(ctx, statements);
    }

    public LangASTNode visitSimple_stmt(PythonParser.Simple_stmtContext ctx) {
        if (ctx.assignment() != null) {
            LangASTNode assignment = mainBuilder.visit(ctx.assignment());
            return LangASTNodeFactory.createExpressionStatement(assignment, ctx);
        }
        if (ctx.type_alias() != null) {
            LangASTNode typeAlias = mainBuilder.visit(ctx.type_alias());
            return LangASTNodeFactory.createExpressionStatement(typeAlias, ctx);
        }
        if (ctx.star_expressions() != null) {
            LangASTNode expr = mainBuilder.visit(ctx.star_expressions());
            return LangASTNodeFactory.createExpressionStatement(expr, ctx);
        }
        if (ctx.return_stmt() != null) return mainBuilder.visit(ctx.return_stmt());
        if (ctx.import_stmt() != null) return mainBuilder.visit(ctx.import_stmt());
        if (ctx.raise_stmt() != null) return mainBuilder.visit(ctx.raise_stmt());
        if (ctx.getText().equals("pass")) return LangASTNodeFactory.createPassStatement(ctx);
        if (ctx.del_stmt() != null) return mainBuilder.visit(ctx.del_stmt());
        if (ctx.yield_stmt() != null) return mainBuilder.visit(ctx.yield_stmt());
        if (ctx.assert_stmt() != null) return mainBuilder.visit(ctx.assert_stmt());
        if (ctx.getText().equals("break")) return LangASTNodeFactory.createBreakStatement(ctx);
        if (ctx.getText().equals("continue")) return LangASTNodeFactory.createContinueStatement(ctx);
        if (ctx.global_stmt() != null) return mainBuilder.visit(ctx.global_stmt());
        if (ctx.nonlocal_stmt() != null) return mainBuilder.visit(ctx.nonlocal_stmt());
        return null;
    }

    // TODO
    public LangASTNode visitType_alias(PythonParser.Type_aliasContext ctx) {
        String name = ctx.name().getText();
        LangASTNode value = mainBuilder.visit(ctx.expression());
        LangASTNode left = LangASTNodeFactory.createSimpleName(name, ctx.name());
        return LangASTNodeFactory.createAssignment("type", left, value, ctx);
    }

    public LangASTNode visitIf_stmt(PythonParser.If_stmtContext ctx) {
        LangASTNode condition = mainBuilder.visit(ctx.named_expression());
        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());
        
        LangASTNode elseBody = null;
        if (ctx.elif_stmt() != null) {
            elseBody = mainBuilder.visit(ctx.elif_stmt());
        } else if (ctx.else_block() != null) {
            elseBody = mainBuilder.visit(ctx.else_block());
        }
        
        return LangASTNodeFactory.createIfStatement(condition, body, elseBody, ctx);
    }

    public LangASTNode visitElif_stmt(PythonParser.Elif_stmtContext ctx) {
        LangASTNode condition = mainBuilder.visit(ctx.named_expression());
        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());
        
        LangASTNode elseBody = null;
        if (ctx.elif_stmt() != null) {
            elseBody = mainBuilder.visit(ctx.elif_stmt());
        } else if (ctx.else_block() != null) {
            elseBody = mainBuilder.visit(ctx.else_block());
        }
        
        return LangASTNodeFactory.createIfStatement(condition, body, elseBody, ctx);
    }

    public LangASTNode visitElse_block(PythonParser.Else_blockContext ctx) {
        return mainBuilder.visit(ctx.block());
    }

    public LangASTNode visitReturn_stmt(PythonParser.Return_stmtContext ctx) {
        LangASTNode expression = null;
        if (ctx.star_expressions() != null) {
            List<PythonParser.Star_expressionContext> exprs = ctx.star_expressions().star_expression();
            if (exprs != null && !exprs.isEmpty()) {
                expression = mainBuilder.visit(exprs.get(exprs.size() - 1));
            }
        }
        return LangASTNodeFactory.createReturnStatement(expression, ctx);
    }

    public LangASTNode visitFor_stmt(PythonParser.For_stmtContext ctx) {
        LangASTNode iterable = mainBuilder.visit(ctx.star_expressions());
        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());
        LangBlock elseBody = ctx.else_block() != null ? (LangBlock) mainBuilder.visit(ctx.else_block()) : null;

        List<LangSingleVariableDeclaration> initializers = new ArrayList<>();
        if (ctx.star_targets() != null && ctx.star_targets().star_target() != null) {
            for (PythonParser.Star_targetContext targetCtx : ctx.star_targets().star_target()) {
                LangASTNode targetNode = mainBuilder.visit(targetCtx);
                if (targetNode instanceof LangSimpleName simpleName) {
                    extension.ast.node.declaration.LangSingleVariableDeclaration decl =
                            LangASTNodeFactory.createSingleVariableDeclaration(simpleName.getIdentifier(), null, targetCtx);
                    initializers.add(decl);
                } else if (targetNode instanceof LangTupleLiteral tuple) {
                    for (LangASTNode element : tuple.getElements()) {
                        if (element instanceof LangSimpleName name) {
                            LangSingleVariableDeclaration decl =
                                    LangASTNodeFactory.createSingleVariableDeclaration(name.getIdentifier(), null, name.getPositionInfo());
                            initializers.add(decl);
                        }
                    }
                }
            }
        }

        LangASTNode forNode = LangASTNodeFactory.createForStatement(initializers, iterable, new ArrayList<>(), body, elseBody, ctx);

        if (ctx.ASYNC() != null) {
            return LangASTNodeFactory.createAsyncStatement(ctx, forNode);
        }

        return forNode;
    }


    public LangASTNode visitWhile_stmt(PythonParser.While_stmtContext ctx) {
        LangASTNode condition = ctx.named_expression() != null ? mainBuilder.visit(ctx.named_expression()) : null;
        LangBlock body = ctx.block() != null ? (LangBlock) mainBuilder.visit(ctx.block()) : LangASTNodeFactory.createBlock(ctx, new ArrayList<>());
        LangBlock elseBody = ctx.else_block() != null ? (LangBlock) mainBuilder.visit(ctx.else_block()) : null;
        return LangASTNodeFactory.createWhileStatement(condition, body, elseBody, ctx);
    }

    public LangASTNode visitTry_stmt(PythonParser.Try_stmtContext ctx) {
        LangBlock tryBlock = ctx.block() != null ? (LangBlock) mainBuilder.visit(ctx.block()) : LangASTNodeFactory.createBlock(ctx, new ArrayList<>());
        List<extension.ast.node.statement.LangCatchClause> catchClauses = new ArrayList<>();
        
        if (ctx.except_block() != null) {
            for (PythonParser.Except_blockContext exceptCtx : ctx.except_block()) {
                LangASTNode catchClause = mainBuilder.visit(exceptCtx);
                if (catchClause instanceof extension.ast.node.statement.LangCatchClause) {
                    catchClauses.add((extension.ast.node.statement.LangCatchClause) catchClause);
                }
            }
        }
        
        LangBlock elseBlock = ctx.else_block() != null ? (LangBlock) mainBuilder.visit(ctx.else_block()) : null;
        LangBlock finallyBlock = ctx.finally_block() != null ? (LangBlock) mainBuilder.visit(ctx.finally_block()) : null;
        
        return LangASTNodeFactory.createTryStatement(ctx, tryBlock, catchClauses, elseBlock, finallyBlock);
    }

    public LangASTNode visitExcept_block(PythonParser.Except_blockContext ctx) {
        List<LangASTNode> exceptionTypes = new ArrayList<>();
        extension.ast.node.expression.LangSimpleName exceptionVariable = null;
        
        if (ctx.expression() != null) {
            LangASTNode typeNode = mainBuilder.visit(ctx.expression());
            if (typeNode != null) exceptionTypes.add(typeNode);
        }
        
        if (ctx.name() != null) {
            exceptionVariable = LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
        }
        
        LangASTNode body = ctx.block() != null ? mainBuilder.visit(ctx.block()) : LangASTNodeFactory.createBlock(ctx, new ArrayList<>());
        return LangASTNodeFactory.createCatchClause(ctx, exceptionTypes, exceptionVariable, body);
    }

    public LangASTNode visitFinally_block(PythonParser.Finally_blockContext ctx) {
        return ctx.block() != null ? mainBuilder.visit(ctx.block()) : LangASTNodeFactory.createBlock(ctx, new ArrayList<>());
    }

    public LangASTNode visitWith_stmt(PythonParser.With_stmtContext ctx) {
        List<LangASTNode> items = new ArrayList<>();
        for (PythonParser.With_itemContext itemCtx : ctx.with_item()) {
            LangASTNode item = mainBuilder.visit(itemCtx);
            if (item != null) items.add(item);
        }
        LangBlock body = ctx.block() != null ? (LangBlock) mainBuilder.visit(ctx.block()) : LangASTNodeFactory.createBlock(ctx, new ArrayList<>());
        LangASTNode withNode = LangASTNodeFactory.createWithStatement(ctx, items, body);
        
        if (ctx.ASYNC() != null) {
            return LangASTNodeFactory.createAsyncStatement(ctx, withNode);
        }
        
        return withNode;
    }

    public LangASTNode visitWith_item(PythonParser.With_itemContext ctx) {
        LangASTNode expression = mainBuilder.visit(ctx.expression());
        if (ctx.star_target() != null) {
            LangASTNode target = mainBuilder.visit(ctx.star_target());
            return LangASTNodeFactory.createWithContextItem(ctx, expression, target);
        }
        return LangASTNodeFactory.createWithContextItem(ctx, expression);
    }

    public LangASTNode visitRaise_stmt(PythonParser.Raise_stmtContext ctx) {
        LangASTNode exc = !ctx.expression().isEmpty() ? mainBuilder.visit(ctx.expression(0)) : null;
        LangASTNode cause = ctx.expression().size() > 1 ? mainBuilder.visit(ctx.expression(1)) : null;
        return LangASTNodeFactory.createThrowStatement(ctx, exc, cause);
    }

    public LangASTNode visitAssert_stmt(PythonParser.Assert_stmtContext ctx) {
        LangASTNode condition = mainBuilder.visit(ctx.expression(0));
        LangASTNode message = ctx.expression().size() > 1 ? mainBuilder.visit(ctx.expression(1)) : null;
        return LangASTNodeFactory.createAssertStatement(ctx, condition, message);
    }

    public LangASTNode visitGlobal_stmt(PythonParser.Global_stmtContext ctx) {
        List<String> names = new ArrayList<>();
        for (PythonParser.NameContext nameCtx : ctx.name()) {
            names.add(nameCtx.getText());
        }
        return LangASTNodeFactory.createGlobalStatement(ctx, names);
    }

    public LangASTNode visitNonlocal_stmt(PythonParser.Nonlocal_stmtContext ctx) {
        List<String> names = new ArrayList<>();
        for (PythonParser.NameContext nameCtx : ctx.name()) {
            names.add(nameCtx.getText());
        }
        return LangASTNodeFactory.createNonlocalStatement(ctx, names);
    }

    public LangASTNode visitDel_stmt(PythonParser.Del_stmtContext ctx) {
        List<LangASTNode> targets = new ArrayList<>();

        for (PythonParser.Del_targetContext t : ctx.del_targets().del_target()) {
            LangASTNode node = mainBuilder.visit(t);
            if (node instanceof LangExpression expr) {
                targets.add(expr);
            }
        }

        return LangASTNodeFactory.createDelStatement(ctx, targets);
    }

    public LangASTNode visitYield_stmt(PythonParser.Yield_stmtContext ctx) {
        LangASTNode expression = null;
        PythonParser.Yield_exprContext yieldExpr = ctx.yield_expr();
        if (yieldExpr.star_expressions() != null) {
            List<PythonParser.Star_expressionContext> exprs = yieldExpr.star_expressions().star_expression();
            if (exprs != null && !exprs.isEmpty()) {
                expression = mainBuilder.visit(exprs.get(exprs.size() - 1));
            }
        } else if (yieldExpr.expression() != null) {
            // yield from expression
            expression = mainBuilder.visit(yieldExpr.expression());
        }
        return LangASTNodeFactory.createYieldStatement(ctx, expression);
    }

    public LangASTNode visitExcept_star_block(PythonParser.Except_star_blockContext ctx) {
        List<LangASTNode> exceptionTypes = new ArrayList<>();
        extension.ast.node.expression.LangSimpleName exceptionVariable = null;
        
        if (ctx.expression() != null) {
            LangASTNode typeNode = mainBuilder.visit(ctx.expression());
            if (typeNode != null) exceptionTypes.add(typeNode);
        }
        
        if (ctx.name() != null) {
            exceptionVariable = LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
        }
        
        LangASTNode body = mainBuilder.visit(ctx.block());
        return LangASTNodeFactory.createCatchClause(ctx, exceptionTypes, exceptionVariable, body);
    }

    public LangASTNode visitMatch_stmt(PythonParser.Match_stmtContext ctx) {
        LangASTNode expression = mainBuilder.visit(ctx.subject_expr());
        List<extension.ast.node.statement.LangCaseStatement> caseStatements = new ArrayList<>();
        for (PythonParser.Case_blockContext caseCtx : ctx.case_block()) {
            caseStatements.add((extension.ast.node.statement.LangCaseStatement) mainBuilder.visit(caseCtx));
        }
        return LangASTNodeFactory.createSwitchStatement(ctx, expression, caseStatements);
    }

    public LangASTNode visitCase_block(PythonParser.Case_blockContext ctx) {
        LangASTNode pattern = mainBuilder.visit(ctx.patterns());
        // Handle guard if present
        LangASTNode guard = null;
        if (ctx.guard() != null) {
            guard = mainBuilder.visit(ctx.guard().named_expression());
        }
        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());
        
        return LangASTNodeFactory.createCaseStatement(ctx, pattern, guard, body);
    }

    public LangASTNode visitPatterns(PythonParser.PatternsContext ctx) {
        if (ctx.open_sequence_pattern() != null) return mainBuilder.visit(ctx.open_sequence_pattern());
        return mainBuilder.visit(ctx.pattern());
    }

    public LangASTNode visitPattern(PythonParser.PatternContext ctx) {
        if (ctx.as_pattern() != null) return mainBuilder.visit(ctx.as_pattern());
        return mainBuilder.visit(ctx.or_pattern());
    }

    public LangASTNode visitAs_pattern(PythonParser.As_patternContext ctx) {
        // pattern 'as' name
        LangASTNode pattern = mainBuilder.visit(ctx.or_pattern());
        if (ctx.pattern_capture_target() != null) {
            LangASTNode target = mainBuilder.visit(ctx.pattern_capture_target());
            return LangASTNodeFactory.createAsPattern(ctx, pattern, target);
        }
        return pattern;
    }

    public LangASTNode visitOr_pattern(PythonParser.Or_patternContext ctx) {
        if (ctx.closed_pattern().size() == 1) return mainBuilder.visit(ctx.closed_pattern(0));
        
        List<LangASTNode> patterns = new ArrayList<>();
        for (PythonParser.Closed_patternContext cp : ctx.closed_pattern()) {
            patterns.add(mainBuilder.visit(cp));
        }
        LangASTNode result = patterns.get(0);
        for (int i = 1; i < patterns.size(); i++) {
            result = LangASTNodeFactory.createInfixExpression(result, patterns.get(i), "|", ctx);
        }
        return result;
    }

    public LangASTNode visitClosed_pattern(PythonParser.Closed_patternContext ctx) {
        if (ctx.literal_pattern() != null) return mainBuilder.visit(ctx.literal_pattern());
        if (ctx.capture_pattern() != null) return mainBuilder.visit(ctx.capture_pattern());
        if (ctx.wildcard_pattern() != null) return mainBuilder.visit(ctx.wildcard_pattern());
        if (ctx.value_pattern() != null) return mainBuilder.visit(ctx.value_pattern());
        if (ctx.group_pattern() != null) return mainBuilder.visit(ctx.group_pattern().pattern());
        if (ctx.sequence_pattern() != null) return mainBuilder.visit(ctx.sequence_pattern());
        if (ctx.mapping_pattern() != null) return mainBuilder.visit(ctx.mapping_pattern());
        if (ctx.class_pattern() != null) return mainBuilder.visit(ctx.class_pattern());
        return null;
    }

    public LangASTNode visitSequence_pattern(PythonParser.Sequence_patternContext ctx) {
        List<LangASTNode> elements = new ArrayList<>();
        if (ctx.maybe_sequence_pattern() != null) {
            for (PythonParser.Maybe_star_patternContext p : ctx.maybe_sequence_pattern().maybe_star_pattern()) {
                elements.add(mainBuilder.visit(p));
            }
        }
        return LangASTNodeFactory.createListLiteral(ctx, elements);
    }

    public LangASTNode visitMaybe_star_pattern(PythonParser.Maybe_star_patternContext ctx) {
        if (ctx.star_pattern() != null) return mainBuilder.visit(ctx.star_pattern());
        return mainBuilder.visit(ctx.pattern());
    }

    public LangASTNode visitStar_pattern(PythonParser.Star_patternContext ctx) {
        LangASTNode target = mainBuilder.visit(ctx.name());
        return LangASTNodeFactory.createPrefixExpression(target, "*", ctx);
    }

    public LangASTNode visitMapping_pattern(PythonParser.Mapping_patternContext ctx) {
        LangDictionaryLiteral dict = LangASTNodeFactory.createDictionaryLiteral(ctx);
        if (ctx.items_pattern() != null) {
            for (PythonParser.Key_value_patternContext kv : ctx.items_pattern().key_value_pattern()) {
                LangASTNode key = mainBuilder.visit(kv.literal_expr() != null ? kv.literal_expr() : kv.attr());
                LangASTNode value = mainBuilder.visit(kv.pattern());
                dict.addEntry(key, value);
            }
        }
        if (ctx.double_star_pattern() != null) {
            LangASTNode target = mainBuilder.visit(ctx.double_star_pattern().pattern_capture_target());
            dict.addEntry(null, LangASTNodeFactory.createPrefixExpression(target, "**", ctx.double_star_pattern()));
        }
        return dict;
    }

    public LangASTNode visitClass_pattern(PythonParser.Class_patternContext ctx) {
        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(ctx);
        invocation.setExpression(mainBuilder.visit(ctx.name_or_attr()));
        if (ctx.positional_patterns() != null) {
            for (PythonParser.PatternContext p : ctx.positional_patterns().pattern()) {
                invocation.addArgument(mainBuilder.visit(p));
            }
        }
        if (ctx.keyword_patterns() != null) {
            for (PythonParser.Keyword_patternContext kp : ctx.keyword_patterns().keyword_pattern()) {
                LangASTNode left = LangASTNodeFactory.createSimpleName(kp.name().getText(), kp.name());
                LangASTNode right = mainBuilder.visit(kp.pattern());
                invocation.addArgument(LangASTNodeFactory.createAssignment("=", left, right, kp));
            }
        }
        return invocation;
    }

    public LangASTNode visitLiteral_pattern(PythonParser.Literal_patternContext ctx) {return LangASTNodeFactory.createLiteralPattern(ctx, ctx.getText());}

    public LangASTNode visitCapture_pattern(PythonParser.Capture_patternContext ctx) {return LangASTNodeFactory.createVariablePattern(ctx, ctx.getText());}

    public LangASTNode visitWildcard_pattern(PythonParser.Wildcard_patternContext ctx) {return LangASTNodeFactory.createVariablePattern(ctx, "_");}

    public LangASTNode visitPass_stmt(ParserRuleContext ctx) {
        return LangASTNodeFactory.createPassStatement(ctx);
    }

    public LangASTNode visitBreak_stmt(ParserRuleContext ctx) {
        return LangASTNodeFactory.createBreakStatement(ctx);
    }

    public LangASTNode visitContinue_stmt(ParserRuleContext ctx) {return LangASTNodeFactory.createContinueStatement(ctx);}

    public LangASTNode visitCompound_stmt(PythonParser.Compound_stmtContext ctx) {
        if (ctx.function_def() != null) return mainBuilder.visit(ctx.function_def());
        if (ctx.if_stmt() != null) return mainBuilder.visit(ctx.if_stmt());
        if (ctx.class_def() != null) return mainBuilder.visit(ctx.class_def());
        if (ctx.with_stmt() != null) return mainBuilder.visit(ctx.with_stmt());
        if (ctx.for_stmt() != null) return mainBuilder.visit(ctx.for_stmt());
        if (ctx.try_stmt() != null) return mainBuilder.visit(ctx.try_stmt());
        if (ctx.while_stmt() != null) return mainBuilder.visit(ctx.while_stmt());
        if (ctx.match_stmt() != null) return mainBuilder.visit(ctx.match_stmt());
        return null;
    }

    public LangASTNode visitSubject_expr(PythonParser.Subject_exprContext ctx) {
        if (ctx.named_expression() != null) return mainBuilder.visit(ctx.named_expression());
        if (ctx.star_named_expression() != null) return mainBuilder.visit(ctx.star_named_expression());
        return null;
    }

    public LangASTNode visitOpen_sequence_pattern(PythonParser.Open_sequence_patternContext ctx) {
        List<LangASTNode> elements = new ArrayList<>();
        if (ctx.maybe_star_pattern() != null) {
            elements.add(mainBuilder.visit(ctx.maybe_star_pattern()));
        }
        if (ctx.maybe_sequence_pattern() != null) {
            for (PythonParser.Maybe_star_patternContext p : ctx.maybe_sequence_pattern().maybe_star_pattern()) {
                elements.add(mainBuilder.visit(p));
            }
        }
        return LangASTNodeFactory.createListLiteral(ctx, elements);
    }

    public LangASTNode visitPattern_capture_target(PythonParser.Pattern_capture_targetContext ctx) {
        return LangASTNodeFactory.createSimpleName(ctx.getText(), ctx);
    }

    public LangASTNode visitValue_pattern(PythonParser.Value_patternContext ctx) {
        return mainBuilder.visit(ctx.attr());
    }

    public LangASTNode visitDel_target(PythonParser.Del_targetContext ctx) {
        if (ctx.t_primary() != null) {
            LangASTNode primary = mainBuilder.visit(ctx.t_primary());
            if (ctx.name() != null) {
                return LangASTNodeFactory.createFieldAccess(primary, ctx.name().getText(), ctx);
            }
            if (ctx.slices() != null) {
                LangASTNode slices = mainBuilder.visit(ctx.slices());
                return LangASTNodeFactory.createIndexAccess(ctx, primary, slices);
            }
            return primary;
        }
        if (ctx.del_t_atom() != null) {
            return mainBuilder.visit(ctx.del_t_atom());
        }
        return null;
    }

    public LangASTNode visitDel_t_atom(PythonParser.Del_t_atomContext ctx) {
        if (ctx.name() != null) {
            return LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx.name());
        }
        if (ctx.del_target() != null) {
            return mainBuilder.visit(ctx.del_target());
        }
        if (ctx.del_targets() != null) {
            List<LangASTNode> targets = new ArrayList<>();
            for (PythonParser.Del_targetContext t : ctx.del_targets().del_target()) {
                LangASTNode node = mainBuilder.visit(t);
                if (node != null) targets.add(node);
            }
            return LangASTNodeFactory.createBlock(ctx, targets);
        }
        return null;
    }


}
