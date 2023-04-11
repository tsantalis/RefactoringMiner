package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

public class PullUpOperationRefactoring extends MoveOperationRefactoring {

	public PullUpOperationRefactoring(UMLOperationBodyMapper bodyMapper) {
		super(bodyMapper);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PULL_UP_OPERATION;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(movedOperation.codeRange()
				.setDescription("pulled up method declaration")
				.setCodeElement(movedOperation.toString()));
		return ranges;
	}
}
