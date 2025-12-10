package extension.ast.node.unit;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.visitor.LangASTVisitor;
import extension.base.LangSupportedEnum;

import java.util.ArrayList;
import java.util.List;

// Class representing the entire source file (LangCompilationUnit)
public class LangCompilationUnit extends LangASTNode {
    private List<LangTypeDeclaration> types = new ArrayList<>();
    private List<LangMethodDeclaration> methods = new ArrayList<>();
    private List<LangAssignment> moduleLevelAssignments = new ArrayList<>();
    private List<LangASTNode> statements = new ArrayList<>();
    private List<LangImportStatement> imports = new ArrayList<>();
    private List<LangComment> comments = new ArrayList<>();
    private LangSupportedEnum language;

    public LangCompilationUnit() {super(NodeTypeEnum.COMPILATION_UNIT);}

    public LangCompilationUnit(PositionInfo positionInfo) {
        super(NodeTypeEnum.COMPILATION_UNIT, positionInfo);
    }

    public LangCompilationUnit(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.COMPILATION_UNIT, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public void addImport(LangImportStatement importStmt) {
        imports.add(importStmt);
        addChild(importStmt);
    }

    public void addType(LangTypeDeclaration type) {
        types.add(type);
        addChild(type);
    }

    public void addMethod(LangMethodDeclaration method) {
        method.setStatic(true);
        methods.add(method);
        addChild(method);
    }

    public void addAssignment(LangAssignment assignment) {
        moduleLevelAssignments.add(assignment);
        addChild(assignment);
    }

    public void addStatement(LangASTNode statement) {
        statements.add(statement);
        addChild(statement);
    }

    public void addComment(LangComment comment) {
        comments.add(comment);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public List<LangTypeDeclaration> getTypes() {
        return types;
    }

    public void setTypes(List<LangTypeDeclaration> types) {
        this.types = types;
    }

    public List<LangMethodDeclaration> getMethods() {
        return methods;
    }

    public void setMethods(List<LangMethodDeclaration> methods) {
        this.methods = methods;
    }

    public List<LangAssignment> getModuleLevelAssignments() {
        return moduleLevelAssignments;
    }

    public List<LangASTNode> getStatements() {
        return statements;
    }

    public void setStatements(List<LangASTNode> statements) {
        this.statements = statements;
    }

    public List<LangImportStatement> getImports() {
        return imports;
    }

    public void setImports(List<LangImportStatement> imports) {
        this.imports = imports;
    }

    public List<LangComment> getComments() {
        return comments;
    }

    public void setComments(List<LangComment> comments) {
        this.comments = comments;
    }

    public LangSupportedEnum getLanguage() {
        return language;
    }

    public void setLanguage(LangSupportedEnum language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "LangCompilationUnit{" +
                "types=" + types +
                ", topLevelMethods=" + methods +
                ", topLevelStatements=" + statements +
                ", imports=" + imports +
                ", comments=" + comments +
                '}';
    }
}