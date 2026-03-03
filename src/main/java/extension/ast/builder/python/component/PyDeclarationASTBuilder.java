package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangFieldAccess;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.literal.LangStringLiteral;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangBlock;
import extension.ast.node.statement.LangExpressionStatement;
import extension.base.lang.python.PythonParser;
import gr.uom.java.xmi.Visibility;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PyDeclarationASTBuilder extends PyBaseASTBuilder {

    public PyDeclarationASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitClass_def(PythonParser.Class_defContext ctx) {
        List<LangAnnotation> annotations = new ArrayList<>();
        if (ctx.decorators() != null) {
            LangASTNode decorators = mainBuilder.visit(ctx.decorators());
            if (decorators instanceof LangBlock) {
                for (LangASTNode node : ((LangBlock) decorators).getStatements()) {
                    if (node instanceof LangAnnotation) {
                        annotations.add((LangAnnotation) node);
                    }
                }
            }
        }
        return visitClass_def_raw(ctx.class_def_raw(), annotations);
    }

    public LangASTNode visitClass_def_raw(PythonParser.Class_def_rawContext ctx, List<LangAnnotation> annotations) {
        String className = ctx.name().getText();
        LangTypeDeclaration typeDeclaration = LangASTNodeFactory.createTypeDeclaration(className, ctx);
        typeDeclaration.setLangAnnotations(annotations);
        typeDeclaration.setActualSignature("class " + ctx.name().getText());
        typeDeclaration.setVisibility(Visibility.PUBLIC);
        typeDeclaration.setTopLevel(true);
        typeDeclaration.setAbstract(isClassAbstract(ctx));
        typeDeclaration.setEnum(isClassEnum(ctx));

        // TODO
//        if (ctx.type_params() != null) {
//            typeDeclaration.setTypeParameters(ctx.type_params().getText());
//        }

        if (ctx.arguments() != null) {
            setSuperClasses(ctx.arguments(), typeDeclaration);
        }



        if (ctx.type_params() != null) {
            LangASTNode typeParams = mainBuilder.visit(ctx.type_params());
            // Store type params
        }

        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());
        if (body != null) {
            if (!body.getStatements().isEmpty() &&
                    body.getStatements().get(0) instanceof LangExpressionStatement stmt &&
                    stmt.getExpression() instanceof LangStringLiteral str) {
                LangComment docstring = LangASTNodeFactory.createComment(ctx, str.getValue(), false, true);
                typeDeclaration.addComment(docstring);
                body.getStatements().remove(0);
            }

            for (LangASTNode statement : body.getStatements()) {
                if (statement instanceof LangTypeDeclaration) {
                    typeDeclaration.addStatement(statement);
                } else if (statement instanceof LangMethodDeclaration) {
                    typeDeclaration.addMethod((LangMethodDeclaration) statement);
                } else if (statement instanceof LangComment) {
                    typeDeclaration.addComment((LangComment) statement);
                } else if (statement instanceof LangExpressionStatement exprStmt && exprStmt.getExpression() instanceof LangAssignment assignment) {
                    typeDeclaration.addAssignment(assignment);
                } else {
                    typeDeclaration.addStatement(statement);
                }
            }
        }

        return typeDeclaration;
    }

    private void setSuperClasses(PythonParser.ArgumentsContext ctx,
                                 LangTypeDeclaration typeDeclaration) {
        if (ctx.args() == null) return;
        List<LangSimpleName> superClasses = new ArrayList<>();

        for (PythonParser.ExpressionContext expr : ctx.args().expression()) {
            String argText = expr.getText();
            if (argText.contains("metaclass=")) {
                continue;
            }
            superClasses.add(
                    LangASTNodeFactory.createSimpleName(argText, expr)
            );
        }
        typeDeclaration.setSuperClassNames(superClasses);
    }

    private boolean isClassAbstract(PythonParser.Class_def_rawContext ctx) {
        if (ctx.arguments() != null) {
            String argsText = ctx.arguments().getText();
            return argsText.contains("metaclass=ABCMeta") ||
                    argsText.contains("metaclass=abc.ABCMeta") ||
                    argsText.contains("ABC") ||
                    argsText.contains("abc.ABC");
        }
        return false;
    }

    private boolean isClassEnum(PythonParser.Class_def_rawContext ctx) {
        if (ctx.arguments() != null && ctx.arguments().args() != null) {
            for (PythonParser.ExpressionContext expr : ctx.arguments().args().expression()) {
                String argText = expr.getText();
                if (argText.equals("Enum") || argText.equals("IntEnum") ||
                        argText.equals("Flag") || argText.equals("IntFlag") ||
                        argText.equals("AutoEnum") || argText.equals("StrEnum") ||
                        argText.endsWith(".Enum")) {
                    return true;
                }
            }
        }
        return false;
    }

    public LangASTNode visitFunction_def(PythonParser.Function_defContext ctx) {
        List<LangAnnotation> annotations = new ArrayList<>();
        if (ctx.decorators() != null) {
            LangASTNode decorators = mainBuilder.visit(ctx.decorators());
            if (decorators instanceof LangBlock) {
                for (LangASTNode node : ((LangBlock) decorators).getStatements()) {
                    if (node instanceof LangAnnotation) {
                        annotations.add((LangAnnotation) node);
                    }
                }
            }
        }
        return visitFunction_def_raw(ctx.function_def_raw(), annotations);
    }

    public LangASTNode visitFunction_def_raw(PythonParser.Function_def_rawContext ctx, List<LangAnnotation> annotations) {
        if (ctx.block() == null) {
            System.err.println("Warning: Function " + ctx.name().getText() + " has no body, skipping");
            return null;
        }

        List<LangSingleVariableDeclaration> parameters = new ArrayList<>();
        if (ctx.params() != null && ctx.params().parameters() != null) {
            processParameters(ctx.params().parameters(), parameters);
        }

        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());

        String docstring = null;
        if (body != null && !body.getStatements().isEmpty() &&
                body.getStatements().get(0) instanceof LangExpressionStatement stmt &&
                stmt.getExpression() instanceof LangStringLiteral str) {
            docstring = str.getValue();
            body.getStatements().remove(0);
        }

        LangMethodDeclaration methodDeclaration = LangASTNodeFactory.createMethodDeclaration(ctx.name().getText(), ctx, parameters, body);
        methodDeclaration.setLangAnnotations(annotations);
        methodDeclaration.setConstructor(isMethodAConstructor(methodDeclaration));
        methodDeclaration.setVisibility(getMethodVisibility(methodDeclaration));
        methodDeclaration.setCleanName(extractCleanName(ctx.name().getText()));
        methodDeclaration.setStatic(isMethodStatic(annotations));
        methodDeclaration.setAbstract(isMethodAbstract(annotations));
        methodDeclaration.setName(ctx.name().getText());

        if (ctx.type_params() != null) {
            LangASTNode typeParams = mainBuilder.visit(ctx.type_params());
            // Store type params
        }

        //TODO
//        if (ctx.type_params() != null) {
//            methodDeclaration.setTypeParameters(ctx.type_params().getText());
//        }

        if (ctx.expression() != null) {
            methodDeclaration.setReturnTypeAnnotation(ctx.expression().getText());
        } else {
            boolean hasReturn = false;
            if (body != null && body.getStatements() != null) {
                for (LangASTNode statement : body.getStatements()) {
                    if (NodeTypeEnum.RETURN_STATEMENT.equals(statement.getNodeType())) {
                        hasReturn = true;
                        break;
                    }
                }
            }
            if (hasReturn) {
                methodDeclaration.setReturnTypeAnnotation(TypeObjectEnum.OBJECT.getName());
            } else {
                methodDeclaration.setReturnTypeAnnotation("None");
            }
        }

        if (docstring != null) {
            LangComment comment = LangASTNodeFactory.createComment(ctx, docstring, false, true);
            methodDeclaration.addComment(comment);
        }

        if (ctx.getChild(0).getText().equals("async")) {
             LangAnnotation asyncAnnotation = LangASTNodeFactory.createAnnotation(
                    ctx,
                    LangASTNodeFactory.createSimpleName("async", ctx),
                    new ArrayList<>()
            );
             methodDeclaration.getLangAnnotations().add(asyncAnnotation);
             methodDeclaration.setAsync(true);
        }

        return methodDeclaration;
    }

    private void processParameters(PythonParser.ParametersContext ctx, List<LangSingleVariableDeclaration> parameters) {
        if (ctx.slash_no_default() != null) {
            for (PythonParser.Param_no_defaultContext p : ctx.slash_no_default().param_no_default()) {
                parameters.add(createParam(p.param(), null, false, false));
            }
        }
        if (ctx.slash_with_default() != null) {
            for (PythonParser.Param_no_defaultContext p : ctx.slash_with_default().param_no_default()) {
                parameters.add(createParam(p.param(), null, false, false));
            }
            for (PythonParser.Param_with_defaultContext p : ctx.slash_with_default().param_with_default()) {
                parameters.add(createParam(p.param(), p.default_assignment().expression(), false, false));
            }
        }
        if (ctx.param_no_default() != null) {
            for (PythonParser.Param_no_defaultContext p : ctx.param_no_default()) {
                parameters.add(createParam(p.param(), null, false, false));
            }
        }
        if (ctx.param_with_default() != null) {
            for (PythonParser.Param_with_defaultContext p : ctx.param_with_default()) {
                parameters.add(createParam(p.param(), p.default_assignment().expression(), false, false));
            }
        }
        if (ctx.star_etc() != null) {
            PythonParser.Star_etcContext s = ctx.star_etc();
            if (s.param_no_default() != null) {
                parameters.add(createParam(s.param_no_default().param(), null, true, false));
            }
            if (s.param_no_default_star_annotation() != null) {
                parameters.add(createParam(s.param_no_default_star_annotation().param_star_annotation().name(), null, true, false));
            }
            if (s.param_maybe_default() != null) {
                for (PythonParser.Param_maybe_defaultContext p : s.param_maybe_default()) {
                    parameters.add(createParam(p.param(), p.default_assignment() != null ? p.default_assignment().expression() : null, false, false));
                }
            }
            if (s.kwds() != null) {
                parameters.add(createParam(s.kwds().param_no_default().param(), null, false, true));
            }
        }
    }

    private LangSingleVariableDeclaration createParam(PythonParser.ParamContext paramContext, PythonParser.ExpressionContext defaultVal, boolean isStar, boolean isDoubleStar) {
        LangASTNode defaultValueExpression = defaultVal != null ? mainBuilder.visit(defaultVal) : null;
        LangSingleVariableDeclaration decl = LangASTNodeFactory.createSingleVariableDeclaration(paramContext.name().getText(), defaultValueExpression, paramContext);
        decl.setTypeAnnotation(TypeObjectEnum.OBJECT);
        decl.setParameter(true);
        decl.setVarArgs(isStar);
        decl.setKwArgs(isDoubleStar);
        if (paramContext.annotation() != null) {
            decl.setTypeAnnotation(TypeObjectEnum.fromType(paramContext.annotation().expression().getText()));
        }
        return decl;
    }

    private LangSingleVariableDeclaration createParam(PythonParser.NameContext name, PythonParser.ExpressionContext defaultVal, boolean isStar, boolean isDoubleStar) {
        LangASTNode defaultValueExpression = defaultVal != null ? mainBuilder.visit(defaultVal) : null;
        LangSingleVariableDeclaration decl = LangASTNodeFactory.createSingleVariableDeclaration(name.getText(), defaultValueExpression, name);
        decl.setTypeAnnotation(TypeObjectEnum.OBJECT);
        decl.setParameter(true);
        decl.setVarArgs(isStar);
        decl.setKwArgs(isDoubleStar);
        return decl;
    }

    private Visibility getMethodVisibility(LangMethodDeclaration methodDecl) {
        String name = methodDecl.getName();
        if (name.startsWith("__") && !name.endsWith("__")) {
            return Visibility.PRIVATE;
        }
        else if (name.startsWith("_") && !name.startsWith("__")) {
            return Visibility.PROTECTED;
        }
        return Visibility.PUBLIC;
    }

    private String extractCleanName(String name) {
        if (name.startsWith("__") && name.endsWith("__")) {
            return name.substring(2, name.length() - 2);
        }

        if (name.startsWith("_") && name.endsWith("__")) {
            return name.substring(1, name.length() - 2);
        }

        // Handle private methods (double underscore prefix)
        if (name.startsWith("__")) {
            return name.substring(2);
        }
        // Handle protected methods (single underscore prefix)
        else if (name.startsWith("_")) {
            return name.substring(1);
        }

        return name;
    }

    private boolean isMethodAConstructor(LangMethodDeclaration methodDecl) {
        return methodDecl.getName().equals("__init__");
    }

    private boolean isMethodStatic(List<LangAnnotation> annotations) {
        for (LangAnnotation annotation : annotations) {
            if ("staticmethod".equals(annotation.getName().getIdentifier())){
                return true;
            }
        }
        return false;
    }

    private boolean isMethodAbstract(List<LangAnnotation> annotations) {
        for (LangAnnotation annotation : annotations) {
            if ("abstractmethod".equals(annotation.getName().getIdentifier())){
                return true;
            }
        }
        return false;
    }

    public LangASTNode visitDecorators(PythonParser.DecoratorsContext ctx) {
        List<LangAnnotation> annotations = new ArrayList<>();
        for (PythonParser.Named_expressionContext namedExpr : ctx.named_expression()) {
            PythonParser.PrimaryContext callPrimary = findDecoratorCallPrimary(namedExpr);
            
            if (callPrimary != null && (callPrimary.LPAR() != null || callPrimary.genexp() != null)) {
                LangASTNode calleeExpr = mainBuilder.visit(callPrimary.primary());
                String decoratorNameStr = flattenExpressionToName(calleeExpr);
                LangSimpleName name = LangASTNodeFactory.createSimpleName(decoratorNameStr, namedExpr);
                
                List<LangASTNode> positionalArgs = new ArrayList<>();
                Map<String, LangASTNode> memberValuePairs = new LinkedHashMap<>();
                
                if (callPrimary.arguments() != null && callPrimary.arguments().args() != null) {
                    PythonParser.ArgsContext args = callPrimary.arguments().args();
                    for (int i = 0; i < args.getChildCount(); i++) {
                        ParseTree child = args.getChild(i);
                        if (child instanceof PythonParser.ExpressionContext exprCtx) {
                            positionalArgs.add(mainBuilder.visit(exprCtx));
                        } else if (child instanceof PythonParser.Starred_expressionContext starredCtx) {
                            positionalArgs.add(mainBuilder.visit(starredCtx));
                        } else if (child instanceof PythonParser.Assignment_expressionContext assignCtx) {
                            positionalArgs.add(mainBuilder.visit(assignCtx));
                        }
                    }
                    if (args.kwargs() != null) {
                        for (PythonParser.Kwarg_or_starredContext kws : args.kwargs().kwarg_or_starred()) {
                            if (kws.name() != null) {
                                LangASTNode value = mainBuilder.visit(kws.expression());
                                memberValuePairs.put(kws.name().getText(), value);
                            } else {
                                positionalArgs.add(mainBuilder.visit(kws));
                            }
                        }
                        for (PythonParser.Kwarg_or_double_starredContext kwds : args.kwargs().kwarg_or_double_starred()) {
                            if (kwds.name() != null) {
                                LangASTNode value = mainBuilder.visit(kwds.expression());
                                memberValuePairs.put(kwds.name().getText(), value);
                            } else {
                                positionalArgs.add(mainBuilder.visit(kwds));
                            }
                        }
                    }
                } else if (callPrimary.arguments() != null) {
                    ParseTree firstChild = callPrimary.arguments().getChild(0);
                    if (firstChild instanceof PythonParser.KwargsContext kwargsCtx) {
                        for (PythonParser.Kwarg_or_starredContext kws : kwargsCtx.kwarg_or_starred()) {
                            if (kws.name() != null) {
                                LangASTNode value = mainBuilder.visit(kws.expression());
                                memberValuePairs.put(kws.name().getText(), value);
                            } else {
                                positionalArgs.add(mainBuilder.visit(kws));
                            }
                        }
                        for (PythonParser.Kwarg_or_double_starredContext kwds : kwargsCtx.kwarg_or_double_starred()) {
                            if (kwds.name() != null) {
                                LangASTNode value = mainBuilder.visit(kwds.expression());
                                memberValuePairs.put(kwds.name().getText(), value);
                            } else {
                                positionalArgs.add(mainBuilder.visit(kwds));
                            }
                        }
                    }
                }
                
                LangAnnotation annotation = LangASTNodeFactory.createAnnotation(namedExpr, name, memberValuePairs, positionalArgs);
                annotations.add(annotation);
            } else {
                LangASTNode expr = mainBuilder.visit(namedExpr);
                LangAnnotation annotation;
                if (expr instanceof LangSimpleName sn) {
                    annotation = LangASTNodeFactory.createAnnotation(namedExpr, sn);
                } else if (expr instanceof LangFieldAccess) {
                    String decoratorNameStr = flattenExpressionToName(expr);
                    LangSimpleName name = LangASTNodeFactory.createSimpleName(decoratorNameStr, namedExpr);
                    annotation = LangASTNodeFactory.createAnnotation(namedExpr, name);
                } else {
                    String decoratorName = namedExpr.getText();
                    annotation = LangASTNodeFactory.createAnnotation(namedExpr, LangASTNodeFactory.createSimpleName(decoratorName, namedExpr));
                }
                annotations.add(annotation);
            }
        }
        return LangASTNodeFactory.createBlock(ctx, new ArrayList<>(annotations));
    }


    private PythonParser.PrimaryContext findDecoratorCallPrimary(PythonParser.Named_expressionContext namedExpr) {
        return findPrimaryWithCall(namedExpr);
    }
    
    private PythonParser.PrimaryContext findPrimaryWithCall(ParseTree tree) {
        if (tree instanceof PythonParser.PrimaryContext primary) {
            if (primary.LPAR() != null || primary.genexp() != null) {
                return primary;
            }
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            PythonParser.PrimaryContext result = findPrimaryWithCall(tree.getChild(i));
            if (result != null) return result;
        }
        return null;
    }
    
    /**
     * Recursively flatten a LangFieldAccess chain or LangSimpleName to a dotted string.
     * For example: LangFieldAccess{pytest.mark.parametrize} -> "pytest.mark.parametrize"
     */
    private String flattenExpressionToName(LangASTNode expr) {
        if (expr instanceof LangSimpleName sn) {
            return sn.getIdentifier();
        } else if (expr instanceof LangFieldAccess fa) {
            String baseName = flattenExpressionToName(fa.getExpression());
            return baseName + "." + fa.getName().getIdentifier();
        } else {
            // Fallback
            return expr.toString();
        }
    }

    public LangASTNode visitType_params(PythonParser.Type_paramsContext ctx) {
        List<LangASTNode> params = new ArrayList<>();
        for (PythonParser.Type_paramContext tp : ctx.type_param_seq().type_param()) {
            params.add(visitType_param(tp));
        }
        return LangASTNodeFactory.createBlock(ctx, params);
    }

    public LangASTNode visitType_param(PythonParser.Type_paramContext ctx) {
        String name = ctx.name().getText();
        LangASTNode bound = null;
        if (ctx.type_param_bound() != null) {
            bound = mainBuilder.visit(ctx.type_param_bound().expression());
        }
        LangASTNode defaultValue = null;
        if (ctx.type_param_default() != null) {
            defaultValue = mainBuilder.visit(ctx.type_param_default().expression());
        }
        // Using SingleVariableDeclaration as a proxy for TypeParam if no specific node exists
        return LangASTNodeFactory.createSingleVariableDeclaration(name, defaultValue, ctx);
    }
}
