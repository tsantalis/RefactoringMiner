package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AssumeRefactoring extends AssertionRefactoring implements MethodLevelRefactoring {
    private Set<AbstractCodeMapping> assumeMappings;
    private AbstractCall assumption;
    private VariableDeclarationContainer operationBefore;
    private VariableDeclarationContainer operationAfter;

    public AssumeRefactoring(Set<AbstractCodeMapping> mappings, AbstractCall assumption, VariableDeclarationContainer operationBefore, VariableDeclarationContainer operationAfter) {
        this.assumeMappings = mappings;
        this.assumption = assumption;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    @Override
    public AnnotationProvider getProviderBefore() {
        return operationBefore;
    }

    @Override
    public AnnotationProvider getProviderAfter() {
        return operationAfter;
    }

    @Override
    public Optional<String> getTemplateParameterBefore() {
        AbstractCodeMapping mapping = assumeMappings.iterator().next();
        return Optional.of(mapping.getFragment1().getString());
    }

    public AbstractCall getCall() {
        return assumption;
    }

    public Set<AbstractCodeMapping> getMappings() {
        return assumeMappings;
    }

    public VariableDeclarationContainer getOperationBefore() {
        return operationBefore;
    }

    public VariableDeclarationContainer getOperationAfter() {
        return operationAfter;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_CONDITIONAL_WITH_ASSUMPTION;
    }

    @Override
    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
        return pairs;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<CodeRange>();
        for(AbstractCodeMapping mapping : assumeMappings) {
            AbstractCodeFragment fragment = mapping.getFragment1();
            ranges.add(fragment.codeRange()
                    .setDescription("original code")
                    .setCodeElement(fragment.getString()));
        }
        String elementType = operationBefore.getElementType();
        ranges.add(operationBefore.codeRange()
                .setDescription("original " + elementType + " declaration")
                .setCodeElement(operationBefore.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<CodeRange>();
        for(AbstractCodeMapping mapping : assumeMappings) {
            AbstractCodeFragment fragment = mapping.getFragment2();
            ranges.add(fragment.codeRange()
                    .setDescription("assume code")
                    .setCodeElement(fragment.getString()));
        }
        String elementType = operationAfter.getElementType();
        ranges.add(operationAfter.codeRange()
                .setDescription(elementType + " declaration with assumption")
                .setCodeElement(operationAfter.toString()));
        return ranges;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AssumeRefactoring other = (AssumeRefactoring) obj;
        return Objects.equals(assumption, other.assumption) && Objects.equals(assumeMappings, other.assumeMappings) && Objects.equals(operationBefore, other.operationBefore) && Objects.equals(operationAfter, other.operationAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assumption, assumeMappings, operationBefore, operationAfter);
    }
}
