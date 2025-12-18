package extension.ast.node.metadata;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.visitor.LangASTVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an annotation or decorator in a language-agnostic way.
 * This class handles metadata elements like Java annotations (@Override)
 * or Python decorators (@staticmethod).
 */
public class LangAnnotation extends LangASTNode {

    private LangSimpleName name;
    private LangASTNode value;  // For single-member annotations
    private Map<String, LangASTNode> memberValuePairs = new LinkedHashMap<>();
    private List<LangASTNode> arguments = new ArrayList<>();

    /**
     * Creates a new annotation with the given name.
     *
     * @param name The name of the annotation
     */
    public LangAnnotation(LangSimpleName name) {
        super(NodeTypeEnum.ANNOTATION);
        this.name = name;
    }

    /**
     * Creates a new annotation with position information.
     *
     * @param name The name of the annotation
     * @param positionInfo Position information for the annotation
     */
    public LangAnnotation(LangSimpleName name, PositionInfo positionInfo) {
        super(NodeTypeEnum.ANNOTATION, positionInfo);
        this.name = name;
    }

    /**
     * Creates a new annotation with position information and arguments.
     *
     * @param name The name of the annotation
     * @param arguments Arguments for the annotation
     * @param positionInfo Position information for the annotation
     */
    public LangAnnotation(LangSimpleName name, List<LangASTNode> arguments, PositionInfo positionInfo) {
        super(NodeTypeEnum.ANNOTATION, positionInfo);
        this.name = name;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }

    /**
     * Creates a new annotation with detailed position information.
     *
     * @param startLine The starting line of the annotation
     * @param startChar The starting character position of the annotation
     * @param endLine The ending line of the annotation
     * @param endChar The ending character position of the annotation
     * @param startColumn The starting column of the annotation
     * @param endColumn The ending column of the annotation
     */
    public LangAnnotation(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.ANNOTATION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    /**
     * Gets the name of this annotation.
     *
     * @return The annotation name
     */
    public LangSimpleName getName() {
        return name;
    }

    /**
     * Sets the name of this annotation.
     *
     * @param name The annotation name
     */
    public void setName(LangSimpleName name) {
        this.name = name;
    }

    /**
     * Gets the single value of this annotation (for single-member annotations).
     *
     * @return The annotation value
     */
    public LangASTNode getValue() {
        return value;
    }

    /**
     * Sets the single value of this annotation (for single-member annotations).
     *
     * @param value The annotation value
     */
    public void setValue(LangASTNode value) {
        this.value = value;
        addChild(value);
    }

    /**
     * Gets the member-value pairs for this annotation (for normal annotations).
     *
     * @return Map of member names to their values
     */
    public Map<String, LangASTNode> getMemberValuePairs() {
        return memberValuePairs;
    }

    /**
     * Adds a member-value pair to this annotation.
     *
     * @param name The member name
     * @param value The member value
     */
    public void addMemberValuePair(String name, LangASTNode value) {
        this.memberValuePairs.put(name, value);
        addChild(value);
    }

    /**
     * Gets the positional arguments for this annotation.
     *
     * @return List of annotation arguments
     */
    public List<LangASTNode> getArguments() {
        return arguments;
    }

    /**
     * Sets the positional arguments for this annotation.
     *
     * @param arguments List of annotation arguments
     */
    public void setArguments(List<LangASTNode> arguments) {
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        for (LangASTNode arg : this.arguments) {
            addChild(arg);
        }
    }

    /**
     * Adds a positional argument to this annotation.
     *
     * @param argument The argument to add
     */
    public void addArgument(LangASTNode argument) {
        this.arguments.add(argument);
        addChild(argument);
    }

    /**
     * Determines if this is a marker annotation (no values).
     *
     * @return true if this is a marker annotation
     */
    public boolean isMarkerAnnotation() {
        return value == null && memberValuePairs.isEmpty() && arguments.isEmpty();
    }

    /**
     * Determines if this is a single-member annotation.
     *
     * @return true if this is a single-member annotation
     */
    public boolean isSingleMemberAnnotation() {
        return value != null;
    }

    /**
     * Determines if this is a normal annotation (with named parameters).
     *
     * @return true if this is a normal annotation
     */
    public boolean isNormalAnnotation() {
        return !memberValuePairs.isEmpty();
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "LangAnnotation{" +
                "name=" + name +
                ", value=" + value +
                ", memberValuePairs=" + memberValuePairs +
                ", arguments=" + arguments +
                '}';
    }
}