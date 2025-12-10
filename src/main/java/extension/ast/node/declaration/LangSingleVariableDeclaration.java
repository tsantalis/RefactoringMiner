package extension.ast.node.declaration;

import extension.ast.node.LangASTNode;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.visitor.LangASTVisitor;

/**
 * Represents a method/function parameter declaration in a JDT-like style
 */
public class LangSingleVariableDeclaration extends LangDeclaration {
    private LangSimpleName langSimpleName;
    private TypeObjectEnum typeAnnotation;
    private LangASTNode defaultValue;
    private boolean hasTypeAnnotation;
    private boolean isVarArgs;
    private boolean isKwArgs;
    private boolean isAttribute;
    private boolean isParameter;
    private boolean isEnumConstant;
    private boolean isFinal;

    public LangSingleVariableDeclaration() {super(NodeTypeEnum.SINGLE_VARIABLE_DECLARATION);}

    public LangSingleVariableDeclaration(LangSimpleName langSimpleName, LangASTNode defaultValue, PositionInfo positionInfo) {
        super(NodeTypeEnum.SINGLE_VARIABLE_DECLARATION, positionInfo);
        this.langSimpleName = langSimpleName;
        this.defaultValue = defaultValue;
    }

    public LangSingleVariableDeclaration(LangSimpleName langSimpleName, int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.SINGLE_VARIABLE_DECLARATION, startLine, startChar, endLine, endChar, startColumn, endColumn);
        this.langSimpleName = langSimpleName;
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public LangASTNode getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(LangASTNode defaultValue) {
        this.defaultValue = defaultValue;
    }

    public LangSimpleName getLangSimpleName() {
        return langSimpleName;
    }

    public void setLangSimpleName(LangSimpleName langSimpleName) {
        this.langSimpleName = langSimpleName;
    }

    public TypeObjectEnum getTypeAnnotation() {
        return typeAnnotation;
    }

    public void setTypeAnnotation(TypeObjectEnum typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    public boolean hasTypeAnnotation() {
        return hasTypeAnnotation;
    }

    public void setHasTypeAnnotation(boolean hasTypeAnnotation) {
        this.hasTypeAnnotation = hasTypeAnnotation;
    }

    public boolean isVarArgs() {
        return isVarArgs;
    }

    public void setVarArgs(boolean varArgs) {
        isVarArgs = varArgs;
    }

    public boolean isAttribute() {
        return isAttribute;
    }

    public void setAttribute(boolean attribute) {
        isAttribute = attribute;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setParameter(boolean parameter) {
        isParameter = parameter;
    }

    public boolean isEnumConstant() {
        return isEnumConstant;
    }

    public void setEnumConstant(boolean enumConstant) {
        isEnumConstant = enumConstant;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean isKwArgs() {
        return isKwArgs;
    }

    public void setKwArgs(boolean kwArgs) {
        isKwArgs = kwArgs;
    }

    public String toString() {
        return "LangSingleVariableDeclaration{" +
                "langSimpleName='" + langSimpleName + '\'' +
                "isParameter='" + isParameter + '\'' +
                "isVarArgs='" + isVarArgs + '\'' +
                "isKwArgs='" + isKwArgs + '\'' +
                '}';
    }
}
