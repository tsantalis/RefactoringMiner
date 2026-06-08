package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.RefactoringType;

public class PushDownAttributeRefactoring extends MoveAttributeRefactoring {

	public PushDownAttributeRefactoring(UMLAttributeDiff diff) {
		super(diff);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.PUSH_DOWN_ATTRIBUTE;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(movedAttribute.codeRange()
				.setDescription("pushed down attribute declaration")
				.setCodeElement(movedAttribute.toString()));
		return ranges;
	}
}
