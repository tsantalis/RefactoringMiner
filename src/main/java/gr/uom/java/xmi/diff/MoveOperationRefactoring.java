package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.AnnotationProvider;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;

public class MoveOperationRefactoring extends AbstractMoveRefactoring {
	protected VariableDeclarationContainer originalOperation;
	protected VariableDeclarationContainer movedOperation;
	private Set<Replacement> replacements;
	private UMLOperationBodyMapper bodyMapper;
	private List<UMLOperationBodyMapper> nestedMappers;

	public MoveOperationRefactoring(UMLOperationBodyMapper bodyMapper) throws RefactoringMinerTimedOutException {
		this.bodyMapper = bodyMapper;
		this.originalOperation = bodyMapper.getContainer1();
		this.movedOperation = bodyMapper.getContainer2();
		this.replacements = bodyMapper.getReplacements();
		this.nestedMappers = new ArrayList<>();
		if(bodyMapper.getOperation1() != null && bodyMapper.getOperation2() != null &&
				(bodyMapper.getOperation1().getNestedOperations().size() > 0 || bodyMapper.getOperation2().getNestedOperations().size() > 0)) {
			for(UMLOperation operation : bodyMapper.getOperation1().getNestedOperations()) {
				UMLOperation operationWithTheSameSignature = bodyMapper.getOperation2().nestedOperationWithTheSameSignatureIgnoringChangedTypes(operation);
				if(operationWithTheSameSignature != null) {
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(operation, operationWithTheSameSignature, bodyMapper.getClassDiff());
					addNestedMapper(mapper);
				}
			}
		}
	}

	@Override
	public AnnotationProvider getProviderBefore() {
		return originalOperation;
	}

	@Override
	public AnnotationProvider getProviderAfter() {
		return movedOperation;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		if(!originalOperation.getName().equals(movedOperation.getName())) {
			return RefactoringType.MOVE_AND_RENAME_OPERATION;
		}
		return RefactoringType.MOVE_OPERATION;
	}

	public void addNestedMapper(UMLOperationBodyMapper mapper) {
		nestedMappers.add(mapper);
	}

	public List<UMLOperationBodyMapper> getNestedMappers() {
		return nestedMappers;
	}

	public UMLOperationBodyMapper getBodyMapper() {
		return bodyMapper;
	}

	public VariableDeclarationContainer getOriginalOperation() {
		return originalOperation;
	}

	public VariableDeclarationContainer getMovedOperation() {
		return movedOperation;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	/**
	 * @return the code range of the source method in the <b>parent</b> commit
	 */
	public CodeRange getSourceOperationCodeRangeBeforeMove() {
		return originalOperation.codeRange();
	}

	/**
	 * @return the code range of the target method in the <b>child</b> commit
	 */
	public CodeRange getTargetOperationCodeRangeAfterMove() {
		return movedOperation.codeRange();
	}

	public boolean compatibleWith(MoveAttributeRefactoring ref) {
		if(ref.getMovedAttribute().getClassName().equals(this.movedOperation.getClassName()) &&
				ref.getOriginalAttribute().getClassName().equals(this.originalOperation.getClassName())) {
			List<String> originalOperationVariables = this.originalOperation.getAllVariables();
			List<String> movedOperationVariables = this.movedOperation.getAllVariables();
			return originalOperationVariables.contains(ref.getOriginalAttribute().getName()) &&
					movedOperationVariables.contains(ref.getMovedAttribute().getName());
		}
		return false;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalOperation().getLocationInfo().getFilePath(), getOriginalOperation().getClassName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getMovedOperation().getLocationInfo().getFilePath(), getMovedOperation().getClassName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalOperation.codeRange()
				.setDescription("original method declaration")
				.setCodeElement(originalOperation.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(movedOperation.codeRange()
				.setDescription("moved method declaration")
				.setCodeElement(movedOperation.toString()));
		return ranges;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((movedOperation == null) ? 0 : movedOperation.hashCode());
		result = prime * result + ((movedOperation == null) ? 0 : movedOperation.getLocationInfo().hashCode());
		result = prime * result + ((originalOperation == null) ? 0 : originalOperation.hashCode());
		result = prime * result + ((originalOperation == null) ? 0 : originalOperation.getLocationInfo().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveOperationRefactoring other = (MoveOperationRefactoring) obj;
		if (movedOperation == null) {
			if (other.movedOperation != null)
				return false;
		} else if (!movedOperation.equals(other.movedOperation)) {
			return false;
		} else if(!movedOperation.getLocationInfo().equals(other.movedOperation.getLocationInfo())) {
			return false;
		}
		if (originalOperation == null) {
			if (other.originalOperation != null)
				return false;
		} else if (!originalOperation.equals(other.originalOperation)) {
			return false;
		} else if (!originalOperation.getLocationInfo().equals(other.originalOperation.getLocationInfo())) {
			return false;
		}
		return true;
	}
}
