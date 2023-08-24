package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class MoveCodeRefactoring implements Refactoring {
	private VariableDeclarationContainer sourceContainer;
	private VariableDeclarationContainer targetContainer;
	private UMLOperationBodyMapper bodyMapper;
	private Set<AbstractCodeFragment> movedCodeFragmentsFromSourceOperation;
	private Set<AbstractCodeFragment> movedCodeFragmentsToTargetOperation;

	public MoveCodeRefactoring(VariableDeclarationContainer sourceContainer,
			VariableDeclarationContainer targetContainer, UMLOperationBodyMapper mapper) {
		this.sourceContainer = sourceContainer;
		this.targetContainer = targetContainer;
		this.bodyMapper = mapper;
		this.movedCodeFragmentsFromSourceOperation = new LinkedHashSet<AbstractCodeFragment>();
		this.movedCodeFragmentsToTargetOperation = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeMapping mapping : mapper.getMappings()) {
			this.movedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.movedCodeFragmentsToTargetOperation.add(mapping.getFragment2());
		}
	}

	public void updateMapperInfo() {
		this.movedCodeFragmentsFromSourceOperation.clear();
		this.movedCodeFragmentsToTargetOperation.clear();
		for(AbstractCodeMapping mapping : bodyMapper.getMappings()) {
			this.movedCodeFragmentsFromSourceOperation.add(mapping.getFragment1());
			this.movedCodeFragmentsToTargetOperation.add(mapping.getFragment2());
		}
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	public VariableDeclarationContainer getSourceContainer() {
		return sourceContainer;
	}

	public VariableDeclarationContainer getTargetContainer() {
		return targetContainer;
	}

	public Set<AbstractCodeMapping> getMappings() {
		return bodyMapper.getMappings();
	}

	public Set<AbstractCodeFragment> getMovedCodeFragmentsFromSourceOperation() {
		return movedCodeFragmentsFromSourceOperation;
	}

	public Set<AbstractCodeFragment> getMovedCodeFragmentsToTargetOperation() {
		return movedCodeFragmentsToTargetOperation;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getSourceContainer().codeRange()
				.setDescription("source method declaration")
				.setCodeElement(getSourceContainer().toString()));
		for(AbstractCodeFragment extractedCodeFragment : movedCodeFragmentsFromSourceOperation) {
			ranges.add(extractedCodeFragment.codeRange().setDescription("moved code from source method declaration"));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(getTargetContainer().codeRange()
				.setDescription("target method declaration")
				.setCodeElement(getTargetContainer().toString()));
		for(AbstractCodeFragment extractedCodeFragment : movedCodeFragmentsToTargetOperation) {
			ranges.add(extractedCodeFragment.codeRange().setDescription("moved code to target method declaration"));
		}
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_CODE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getSourceContainer().getLocationInfo().getFilePath(), getSourceContainer().getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getTargetContainer().getLocationInfo().getFilePath(), getTargetContainer().getClassName()));
		return pairs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(movedCodeFragmentsFromSourceOperation, movedCodeFragmentsToTargetOperation, sourceContainer,
				targetContainer);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveCodeRefactoring other = (MoveCodeRefactoring) obj;
		return Objects.equals(movedCodeFragmentsFromSourceOperation, other.movedCodeFragmentsFromSourceOperation)
				&& Objects.equals(movedCodeFragmentsToTargetOperation, other.movedCodeFragmentsToTargetOperation)
				&& Objects.equals(sourceContainer, other.sourceContainer)
				&& Objects.equals(targetContainer, other.targetContainer);
	}

	private String getClassName() {
		String sourceClassName = getSourceContainer().getClassName();
		String targetClassName = getTargetContainer().getClassName();
		return sourceClassName.equals(targetClassName) ? sourceClassName : targetClassName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append("from ");
		sb.append(sourceContainer);
		sb.append(" to ");
		sb.append(targetContainer);
		sb.append(" in class ");
		sb.append(getClassName());
		return sb.toString();
	}
}
