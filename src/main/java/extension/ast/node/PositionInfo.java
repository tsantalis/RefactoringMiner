package extension.ast.node;

public class PositionInfo {

    private int startLine;
    private int endLine;
    private int startChar;
    private int endChar;
    private int startColumn;
    private int endColumn;

    public PositionInfo() {}

    public PositionInfo(int startLine, int endLine, int startChar, int endChar, int startColumn, int endColumn) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.startChar = startChar;
        this.endChar = endChar;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
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

    @Override
    public String toString() {
        return "PositionInfo{" +
                "startLine=" + startLine +
                ", endLine=" + endLine +
                ", startChar=" + startChar +
                ", endChar=" + endChar +
                ", startColumn=" + startColumn +
                ", endColumn=" + endColumn +
                '}';
    }
}
