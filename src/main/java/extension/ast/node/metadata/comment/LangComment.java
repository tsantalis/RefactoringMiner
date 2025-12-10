package extension.ast.node.metadata.comment;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.visitor.LangASTVisitor;

/**
 * Represents a comment node in the Language AST.
 * Handles different types of comments including line comments,
 * block comments, and documentation comments.
 */
public class LangComment extends LangASTNode {
    private String content;
    private boolean isBlockComment; // true for /* */, false for // comments
    private boolean isDocComment;   // true for /** */ in Java, triple-quoted in Python
    private LangASTNode attachedNode; // The node this comment is attached to (if any)

    /**
     * Creates a new comment node
     *
     * @param content The text content of the comment
     * @param isBlockComment Whether this is a block comment
     * @param isDocComment Whether this is a documentation comment
     * @param positionInfo Position information for the comment
     */
    public LangComment(String content, boolean isBlockComment, boolean isDocComment, PositionInfo positionInfo) {
        super(NodeTypeEnum.COMMENT, positionInfo);
        this.content = content;
        this.isBlockComment = isBlockComment;
        this.isDocComment = isDocComment;
    }


    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Gets the raw content of the comment including comment delimiters
     *
     * @return The raw comment text
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the cleaned content of the comment with comment delimiters removed
     *
     * @return The cleaned comment text
     */
    public String getCleanedContent() {
        if (content == null) {
            return "";
        }

        String text = content.trim();

        // Handle Python comments
        if (text.startsWith("#")) {
            return text.substring(1).trim();
        }

        // Handle Python docstrings
        if ((text.startsWith("\"\"\"") && text.endsWith("\"\"\"")) ||
                (text.startsWith("'''") && text.endsWith("'''"))) {
            return text.substring(3, text.length() - 3).trim();
        }

        // Handle Java line comments
        if (text.startsWith("//")) {
            return text.substring(2).trim();
        }

        // Handle Java block comments
        if (text.startsWith("/*") && text.endsWith("*/")) {
            String inner = text.substring(2, text.length() - 2).trim();

            // Further process multi-line block comments
            if (inner.contains("\n")) {
                StringBuilder sb = new StringBuilder();
                for (String line : inner.split("\n")) {
                    line = line.trim();
                    // Remove leading asterisks common in JavaDoc style
                    if (line.startsWith("*")) {
                        line = line.substring(1).trim();
                    }
                    sb.append(line).append("\n");
                }
                return sb.toString().trim();
            }

            return inner;
        }

        // If no specific format is detected, return as is
        return text;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isBlockComment() {
        return isBlockComment;
    }

    public void setBlockComment(boolean blockComment) {
        isBlockComment = blockComment;
    }

    public boolean isDocComment() {
        return isDocComment;
    }

    public void setDocComment(boolean docComment) {
        isDocComment = docComment;
    }

    /**
     * Checks if this is a line comment
     *
     * @return true if this is a single-line comment, false otherwise
     */
    public boolean isLineComment() {
        return !isBlockComment && !isDocComment;
    }

    /**
     * Gets the node this comment is attached to, if any
     *
     * @return The attached node or null if this comment isn't attached
     */
    public LangASTNode getAttachedNode() {
        return attachedNode;
    }

    /**
     * Sets the node this comment is attached to
     *
     * @param node The node to attach this comment to
     */
    public void setAttachedNode(LangASTNode node) {
        this.attachedNode = node;
    }

    /**
     * Determines if this comment is attached to a node
     *
     * @return true if attached, false otherwise
     */
    public boolean isAttached() {
        return attachedNode != null;
    }

    /**
     * Generates a string representation of this comment
     *
     * @return A string representation
     */
    @Override
    public String toString() {
        return "LangComment{" +
                "content='" + content + '\'' +
                ", isBlockComment=" + isBlockComment +
                ", isDocComment=" + isDocComment +
                ", attachedNode=" + attachedNode +
                '}';
    }
}