package extension.ast.node;


import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.unit.LangCompilationUnit;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class LangASTNode {

    private NodeTypeEnum nodeType;
    private int startLine;
    private int endLine;
    private int startChar;
    private int endChar;
    private int startColumn;
    private int endColumn;
    private int length;
    private PositionInfo positionInfo;
    private LangASTNode parent;
    private List<LangASTNode> children;

    public LangASTNode(NodeTypeEnum nodeType) {
        this.nodeType = nodeType;
        this.children = new ArrayList<>();
    }

    public LangASTNode(NodeTypeEnum nodeType, PositionInfo positionInfo) {
        this.nodeType = nodeType;
        this.startLine = positionInfo.getStartLine();
        this.endLine = positionInfo.getEndLine();
        this.startChar = positionInfo.getStartChar();
        this.endChar = positionInfo.getEndChar() + 1;
        this.startColumn = positionInfo.getStartColumn();
        this.endColumn = positionInfo.getEndColumn();
        this.positionInfo = positionInfo;
        this.length = this.endChar - this.startChar;
        if (this.endChar <= this.startChar) {
            System.err.println("Warning: Invalid source range for " + nodeType +
                    " - start: " + this.startChar + ", end: " + this.endChar);
        }
        this.children = new ArrayList<>();
    }

    public LangASTNode(NodeTypeEnum nodeType, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        this.nodeType = nodeType;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startChar = startChar;
        this.endChar = endChar + 1;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.length = this.endChar - this.startChar;
        if (this.endChar <= this.startChar) {
            System.err.println("Warning: Invalid source range for " + nodeType +
                    " - start: " + this.startChar + ", end: " + this.endChar);
        }
        this.children = new ArrayList<>();
    }

    public void addChild(LangASTNode child) {
        if (child == null) { return; }
        child.setParent(this);
        this.children.add(child);
    }

    public LangCompilationUnit getRootCompilationUnit() {
        LangASTNode current = this;
        while (current != null && !(current instanceof LangCompilationUnit)) {
            current = current.getParent();
        }
        return (LangCompilationUnit) current;
    }

    public List<LangAnnotation> getAnnotations() {
        List<LangAnnotation> annotations = new ArrayList<>();
        for (LangASTNode child : getChildren()) {
            if (child instanceof LangAnnotation) {
                annotations.add((LangAnnotation) child);
            }
        }
        return annotations;
    }

    // Accept method for a visitor pattern
    public abstract void accept(LangASTVisitor visitor);

    public NodeTypeEnum getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeTypeEnum nodeType) {
        this.nodeType = nodeType;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getStartChar() {
        return startChar;
    }

    public void setStartChar(int startChar) {
        this.startChar = startChar;
    }

    public int getEndChar() {
        return endChar;
    }

    public void setEndChar(int endChar) {
        this.endChar = endChar;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public void setStartColumn(int startColumn) {
        this.startColumn = startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public LangASTNode getParent() {
        return parent;
    }

    public void setParent(LangASTNode parent) {
        this.parent = parent;
    }

    public List<LangASTNode> getChildren() {
        return children;
    }

    public void setChildren(List<LangASTNode> children) {
        this.children = children;
    }

    public PositionInfo getPositionInfo() { return positionInfo; }

    public void setPositionInfo(PositionInfo positionInfo) { this.positionInfo = positionInfo; }

    @Override
    public abstract String toString();

}