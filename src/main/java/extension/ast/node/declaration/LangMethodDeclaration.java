package extension.ast.node.declaration;

import extension.ast.node.NodeTypeEnum;
import extension.ast.node.PositionInfo;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangBlock;
import extension.ast.visitor.LangASTVisitor;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

// Class representing a method within a type
public class LangMethodDeclaration extends LangDeclaration {
    private String name;
    private String cleanName;
    private List<LangSingleVariableDeclaration> parameters = new ArrayList<>();
    private LangBlock body;
    private Visibility visibility;
    private boolean isStatic; //  Top Level Methods should be static
    private boolean isConstructor;
    private boolean isAbstract;
    private boolean isFinal;
    private boolean isNative;
    private boolean isSynchronized;
    private boolean isAsync;
    private String returnTypeAnnotation;
    private List<LangAnnotation> langAnnotations = new ArrayList<>();
    private List<LangComment> comments = new ArrayList<>();


    public LangMethodDeclaration() {super(NodeTypeEnum.METHOD_DECLARATION);}

    public LangMethodDeclaration(PositionInfo positionInfo) {
        super(NodeTypeEnum.METHOD_DECLARATION, positionInfo);
    }

    public LangMethodDeclaration(int startLine, int startChar, int endLine, int endChar, int startColumn, int endColumn) {
        super(NodeTypeEnum.METHOD_DECLARATION, startLine, startChar, endLine, endChar, startColumn, endColumn);
    }

    public void addParameter(LangSingleVariableDeclaration langSingleVariableDeclaration) {
        parameters.add(langSingleVariableDeclaration);
        addChild(langSingleVariableDeclaration);
    }

    public void addComment(LangComment comment) {
        comments.add(comment);
        addChild(comment);
    }

    @Override
    public void accept(LangASTVisitor visitor) {
        visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LangBlock getBody() {
        return body;
    }

    public void setBody(LangBlock body) {
        this.body = body;
        addChild(body);
    }

    public List<LangSingleVariableDeclaration> getParameters() {
        return parameters;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public void setConstructor(boolean constructor) {
        isConstructor = constructor;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setNative(boolean aNative) {
        isNative = aNative;
    }

    public boolean isSynchronized() {
        return isSynchronized;
    }

    public void setSynchronized(boolean aSynchronized) {
        isSynchronized = aSynchronized;
    }

    public String getReturnTypeAnnotation() {
        return returnTypeAnnotation;
    }

    public void setReturnTypeAnnotation(String returnTypeAnnotation) {
        this.returnTypeAnnotation = returnTypeAnnotation;
    }

    public String getCleanName() {
        return cleanName;
    }

    public void setCleanName(String cleanName) {
        this.cleanName = cleanName;
    }

    public List<LangAnnotation> getLangAnnotations() {
        return langAnnotations;
    }

    public void setLangAnnotations(List<LangAnnotation> annotations) {
        this.langAnnotations = annotations;
        for (LangAnnotation annotation : annotations) {
            addChild(annotation);
        }
    }

    public List<LangComment> getComments() {
        return comments;
    }

    public void setComments(List<LangComment> comments) {
        this.comments = comments;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean async) {
        isAsync = async;
    }

    public String toString() {
        return "LangMethodDeclaration{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters +
                ", body=" + body +
                ", visibility=" + visibility +
                ", cleanName=" + cleanName +
                ", returnType=" + returnTypeAnnotation +
                '}';
    }
}
