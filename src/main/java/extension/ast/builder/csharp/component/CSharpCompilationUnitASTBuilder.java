package extension.ast.builder.csharp.component;

import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.csharp.CSharpParserBaseVisitor;

import java.util.List;

/**
 * Handles C# compilation unit level constructs.
 */
public class CSharpCompilationUnitASTBuilder extends CSharpParserBaseVisitor<LangASTNode> {

    protected final CSharpASTBuilder mainBuilder;

    public CSharpCompilationUnitASTBuilder(CSharpASTBuilder mainBuilder) {
        this.mainBuilder = mainBuilder;
    }

    @Override
    public LangASTNode visitCompilation_unit(CSharpParser.Compilation_unitContext ctx) {
        LangCompilationUnit unit = LangASTNodeFactory.createCompilationUnit(ctx);
        unit.setLanguage(LangSupportedEnum.CSHARP);
        // using directives
        CSharpParser.Using_directivesContext usingsCtx = ctx.using_directives();
        if (usingsCtx != null) {
            List<CSharpParser.Using_directiveContext> list = usingsCtx.using_directive();
            for (CSharpParser.Using_directiveContext u : list) {
                LangImportStatement imp = LangASTNodeFactory.createImportStatement(u);
                unit.addImport(imp);
            }
        }
        // namespace members (namespaces, types)
        CSharpParser.Namespace_member_declarationsContext members = ctx.namespace_member_declarations();
        if (members != null) {
            for (CSharpParser.Namespace_member_declarationContext decl : members.namespace_member_declaration()) {
                LangASTNode child = mainBuilder.visit(decl);
                if (child == null) continue;
                if (child instanceof LangTypeDeclaration) {
                    unit.addType((LangTypeDeclaration) child);
                } else if (child instanceof LangMethodDeclaration) {
                    unit.addMethod((LangMethodDeclaration) child);
                } else {
                    unit.addStatement(child);
                }
            }
        }
        return unit;
    }
}
