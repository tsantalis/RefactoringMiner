package extension.ast.builder.go.component;

import extension.ast.builder.go.GoASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.PositionInfo;
import extension.ast.node.PositionUtils;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import extension.base.lang.go.GoParser;

import java.util.ArrayList;
import java.util.List;

public class GoCompilationUnitASTBuilder extends GoBaseASTBuilder {

    public GoCompilationUnitASTBuilder(GoASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitSourceFile(GoParser.SourceFileContext ctx) {
        LangCompilationUnit compilationUnit = LangASTNodeFactory.createCompilationUnit(ctx);
        compilationUnit.setLanguage(LangSupportedEnum.GO);

        for (GoParser.ImportDeclContext importDecl : ctx.importDecl()) {
            for (LangImportStatement importStatement : buildImportStatements(importDecl)) {
                compilationUnit.addImport(importStatement);
            }
        }

        for (GoParser.FunctionDeclContext functionDecl : ctx.functionDecl()) {
            if (mainBuilder.visit(functionDecl) instanceof LangMethodDeclaration method) {
                compilationUnit.addMethod(method);
            }
        }

        for (GoParser.MethodDeclContext methodDecl : ctx.methodDecl()) {
            if (mainBuilder.visit(methodDecl) instanceof LangMethodDeclaration method) {
                compilationUnit.addMethod(method);
            }
        }

        for (GoParser.DeclarationContext declaration : ctx.declaration()) {
            if (declaration.typeDecl() != null) {
                for (LangTypeDeclaration type : mainBuilder.getDeclarationBuilder().buildTypeDeclarations(declaration.typeDecl())) {
                    compilationUnit.addType(type);
                }
            }
        }

        return compilationUnit;
    }

    private List<LangImportStatement> buildImportStatements(GoParser.ImportDeclContext ctx) {
        List<LangImportStatement> result = new ArrayList<>();
        for (GoParser.ImportSpecContext spec : ctx.importSpec()) {
            String path = stripQuotes(spec.importPath().getText());
            String alias = null;
            if (spec.DOT() != null) {
                alias = ".";
            } else if (spec.packageName() != null) {
                alias = spec.packageName().getText();
            }
            PositionInfo positionInfo = PositionUtils.getPositionInfo(spec);
            LangImportStatement.LangImportItem item = new LangImportStatement.LangImportItem(path, alias, positionInfo);
            LangImportStatement importStatement = new LangImportStatement(positionInfo, List.of(item));
            importStatement.setModuleName(path);
            result.add(importStatement);
        }
        return result;
    }

    private static String stripQuotes(String text) {
        if (text != null && text.length() >= 2 && (text.charAt(0) == '"' || text.charAt(0) == '`')) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

}
