package extension.ast.builder.go;

import extension.ast.builder.LangASTBuilder;
import extension.ast.builder.go.component.GoCompilationUnitASTBuilder;
import extension.ast.builder.go.component.GoDeclarationASTBuilder;
import extension.ast.builder.go.component.GoStatementASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.go.GoParser;
import extension.base.lang.go.GoParserBaseVisitor;

public class GoASTBuilder extends GoParserBaseVisitor<LangASTNode> implements LangASTBuilder<GoParser.SourceFileContext> {

    private final GoCompilationUnitASTBuilder compilationUnitBuilder;
    private final GoDeclarationASTBuilder declarationBuilder;
    private final GoStatementASTBuilder statementBuilder;

    public GoASTBuilder() {
        this.compilationUnitBuilder = new GoCompilationUnitASTBuilder(this);
        this.declarationBuilder = new GoDeclarationASTBuilder(this);
        this.statementBuilder = new GoStatementASTBuilder(this);
    }

    public LangASTNode build(GoParser.SourceFileContext ctx) { return visitSourceFile(ctx); }

    // Exposed so GoCompilationUnitASTBuilder can reach the grouped-typeDecl helper method
    // (buildTypeDeclarations returns a List<LangTypeDeclaration>, which doesn't fit the
    // single-node ANTLR visitor dispatch used for everything else).
    public GoDeclarationASTBuilder getDeclarationBuilder() { return declarationBuilder; }

    /** CompilationUnit related methods **/
    @Override public LangASTNode visitSourceFile(GoParser.SourceFileContext ctx) { return compilationUnitBuilder.visitSourceFile(ctx); }

    /** Declaration related methods **/
    @Override public LangASTNode visitFunctionDecl(GoParser.FunctionDeclContext ctx) { return declarationBuilder.visitFunctionDecl(ctx); }
    @Override public LangASTNode visitMethodDecl(GoParser.MethodDeclContext ctx) { return declarationBuilder.visitMethodDecl(ctx); }

    /** Statement-related methods **/
    @Override public LangASTNode visitBlock(GoParser.BlockContext ctx) { return statementBuilder.visitBlock(ctx); }
}
