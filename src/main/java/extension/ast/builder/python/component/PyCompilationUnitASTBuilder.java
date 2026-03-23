package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.PositionInfo;
import extension.ast.node.PositionUtils;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.literal.LangStringLiteral;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangExpressionStatement;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import extension.base.lang.python.PythonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PyCompilationUnitASTBuilder extends PyBaseASTBuilder {

    public PyCompilationUnitASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitFile_input(PythonParser.File_inputContext ctx) {
        LangCompilationUnit compilationUnit = LangASTNodeFactory.createCompilationUnit(ctx);
        compilationUnit.setLanguage(LangSupportedEnum.PYTHON);

        if (ctx.statements() != null) {
            for (PythonParser.StatementContext stmtCtx : ctx.statements().statement()) {
                LangASTNode stmt = mainBuilder.visit(stmtCtx);

                if (stmt == null) continue;

                if (stmt instanceof LangTypeDeclaration) {
                    compilationUnit.addType((LangTypeDeclaration) stmt);
                } else if (stmt instanceof LangMethodDeclaration) {
                    compilationUnit.addMethod((LangMethodDeclaration) stmt);
                } else if (stmt instanceof LangComment) {
                    compilationUnit.addComment((LangComment) stmt);
                } else if (stmt instanceof LangImportStatement) {
                    compilationUnit.addImport((LangImportStatement) stmt);
                } else if (stmt instanceof LangExpressionStatement exprStatement) {
                    if (exprStatement.getExpression() instanceof LangAssignment assignment)
                        compilationUnit.addAssignment(assignment);
                    else if (exprStatement.getExpression() instanceof LangStringLiteral str) {
                        LangComment comment = LangASTNodeFactory.createComment(ctx, str.getValue(), false, true);
                        compilationUnit.addComment(comment);
                    } else {
                        compilationUnit.addStatement(stmt);
                    }
                } else {
                    compilationUnit.addStatement(stmt);
                }
            }
        }

        return compilationUnit;
    }


    public LangASTNode visitImport_stmt(PythonParser.Import_stmtContext ctx) {
        PositionInfo positionInfo = PositionUtils.getPositionInfo(ctx);

        List<LangImportStatement> importStatements = new ArrayList<>();

        if (ctx.import_name() != null) {
            PythonParser.Import_nameContext importNameCtx = ctx.import_name();
            PythonParser.Dotted_as_namesContext dottedAsNames = importNameCtx.dotted_as_names();
            List<LangImportStatement.LangImportItem> importItems = new ArrayList<>();
            for (PythonParser.Dotted_as_nameContext dottedAsName : dottedAsNames.dotted_as_name()) {
                String modulePath = dottedAsName.dotted_name().getText();
                PositionInfo importItemPositionInfo = PositionUtils.getPositionInfo(dottedAsName);
                // Get the alias if present
                String alias = null;
                if (dottedAsName.AS() != null && dottedAsName.name() != null) {
                    alias = dottedAsName.name().getText();
                }
                LangImportStatement.LangImportItem importItem = new LangImportStatement.LangImportItem(modulePath, alias, importItemPositionInfo);
                importItems.add(importItem);
            }
            // Create import statement
            LangImportStatement importStmt = new LangImportStatement(positionInfo, importItems);
            // If we're in a compilation unit, add it there
            if (importStmt.getRootCompilationUnit() != null) {
                importStmt.getRootCompilationUnit().addImport(importStmt);
            }
            // Track all created import statements
            importStatements.add(importStmt);
        } else if (ctx.import_from() != null) {
            PythonParser.Import_fromContext importFromCtx = ctx.import_from();

            // Get the module from which to import (could be null for relative imports like "from . import name")
            String fromModule = null;
            if (importFromCtx.dotted_name() != null) {
                fromModule = importFromCtx.dotted_name().getText();
            }

            // Handle relative imports (dots before module name)
            int relativeLevel = 0;
            if (importFromCtx.DOT() != null && !importFromCtx.DOT().isEmpty()) {
                relativeLevel = importFromCtx.DOT().size();
            } else if (importFromCtx.ELLIPSIS() != null && !importFromCtx.ELLIPSIS().isEmpty()) {
                relativeLevel = 3 * importFromCtx.ELLIPSIS().size(); // Each ... is 3 dots
            }

            if (importFromCtx.import_from_targets() != null) {
                PythonParser.Import_from_targetsContext targets = importFromCtx.import_from_targets();
                if (targets.STAR() != null) {
                    LangImportStatement importStmt = new LangImportStatement(fromModule, relativeLevel, positionInfo, Collections.emptyList(), true);
                    if (importStmt.getRootCompilationUnit() != null) {
                        importStmt.getRootCompilationUnit().addImport(importStmt);
                    }
                    importStatements.add(importStmt);
                } else if (targets.import_from_as_names() != null) {
                    List<LangImportStatement.LangImportItem> importItems = new ArrayList<>();
                    for (PythonParser.Import_from_as_nameContext importAsName : targets.import_from_as_names().import_from_as_name()) {
                        PositionInfo importItemPositionInfo = PositionUtils.getPositionInfo(importAsName);
                        String importedName = importAsName.name(0).getText();
                        String alias = null;
                        if (importAsName.AS() != null && importAsName.name().size() > 1) {
                            alias = importAsName.name(1).getText();
                        }
                        LangImportStatement.LangImportItem importItem = new LangImportStatement.LangImportItem(importedName, alias, importItemPositionInfo);
                        importItems.add(importItem);
                    }
                    LangImportStatement importStmt = new LangImportStatement(fromModule, relativeLevel, positionInfo, importItems, false);
                    if (importStmt.getRootCompilationUnit() != null) {
                        importStmt.getRootCompilationUnit().addImport(importStmt);
                    }
                    importStatements.add(importStmt);
                }
            }
        }

        // Return the last import statement created, or null if no imports were processed
        if (!importStatements.isEmpty()) {
            return importStatements.get(importStatements.size() - 1);
        }

        return null;
    }

}
