package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EnforcePreconditionRefactoring implements Refactoring {
    private AbstractCall assumption;
    private LocationInfoProvider replacedElement;
    private VariableDeclarationContainer operationBefore;
    private VariableDeclarationContainer operationAfter;

    public EnforcePreconditionRefactoring(AbstractCall assumption, LocationInfoProvider replacedElement, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
        this.assumption = assumption;
        this.replacedElement = replacedElement;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public VariableDeclarationContainer getOperationBefore() {
        return operationBefore;
    }

    public void setOperationBefore(VariableDeclarationContainer operationBefore) {
        this.operationBefore = operationBefore;
    }

    public VariableDeclarationContainer getOperationAfter() {
        return operationAfter;
    }

    public void setOperationAfter(VariableDeclarationContainer operationAfter) {
        this.operationAfter = operationAfter;
    }

    public AbstractCall getAssumption() {
        return assumption;
    }

    public LocationInfoProvider getReplacedElement() {
        return replacedElement;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return switch (replacedElement) {
            case UMLAnnotation ignored -> RefactoringType.REPLACE_IGNORE_WITH_ASSUMPTION;
            case CompositeStatementObject ignored -> RefactoringType.REPLACE_CONDITIONAL_WITH_ASSUMPTION;
            case AbstractCall ignored -> RefactoringType.REPLACE_ASSERTION_WITH_ASSUMPTION;
            default -> throw new IllegalStateException("Unexpected replaced element type: " + replacedElement.getClass().getName());
        };
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
        pairs.add(new ImmutablePair<>(getReplacedElement().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
        return pairs;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
        pairs.add(new ImmutablePair<>(getAssumption().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<CodeRange>();
        ranges.add(replacedElement.codeRange()
                .setDescription("original " + switch (replacedElement) {
                    case UMLAnnotation ignored -> "annotation";
                    case CompositeStatementObject ignored -> "condition";
                    case AbstractCall ignored -> "assertion";
                    default -> throw new IllegalStateException("Unexpected replaced element type: " + replacedElement.getClass().getName());
                })
                .setCodeElement(replacedElement.toString()));
        String elementType = operationBefore.getElementType();
        ranges.add(operationBefore.codeRange()
                .setDescription("original " + elementType + " declaration")
                .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<CodeRange>();
        ranges.add(assumption.codeRange()
                .setDescription("assumption")
                .setCodeElement(assumption.toString()));
        String elementType = operationAfter.getElementType();
        ranges.add(operationAfter.codeRange()
                .setDescription(elementType + " declaration with ternary conditional")
                .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(switch (replacedElement) {
            case UMLAnnotation ann -> ann.toString();
            case CompositeStatementObject cond -> cond.getString().contains("\n") ? cond.getString().substring(0, cond.getString().indexOf("\n")) : cond.getString();
            case AbstractCall ass -> ass.getString();
            default -> throw new IllegalStateException("Unexpected replaced element type: " + replacedElement.getClass().getName());
        });
        sb.append(" to ");
        sb.append(assumption.getString());
        String elementType = operationAfter.getElementType();
        sb.append(" in ").append(elementType).append(" ");
        sb.append(operationAfter.toQualifiedString());
        sb.append(" from class ").append(operationAfter.getClassName());
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EnforcePreconditionRefactoring other = (EnforcePreconditionRefactoring) obj;
        return Objects.equals(assumption, other.assumption) && Objects.equals(replacedElement, other.replacedElement) && Objects.equals(operationBefore, other.operationBefore) && Objects.equals(operationAfter, other.operationAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assumption, operationAfter, operationBefore, replacedElement);
    }
}
