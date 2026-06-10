package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;

import java.util.ArrayList;
import java.util.List;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

public class MoveAndRenameClassRefactoring extends RenameClassRefactoring implements PackageLevelRefactoring {
	
	public MoveAndRenameClassRefactoring(UMLClass originalClass,  UMLClass renamedClass) {
		super(originalClass, renamedClass);
	}

	public RenamePattern getRenamePattern() {
		int separatorPos = PrefixSuffixUtils.separatorPosOfCommonSuffix('.', originalClass.getPackageName(), renamedClass.getPackageName());
		if (separatorPos == -1) {
			return new RenamePattern(originalClass.getPackageName(), renamedClass.getPackageName());
		}
		String originalPath = originalClass.getName().substring(0, originalClass.getName().length() - separatorPos);
		String movedPath = renamedClass.getName().substring(0, renamedClass.getName().length() - separatorPos);
		return new RenamePattern(originalPath, movedPath);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_RENAME_CLASS;
	}

	public String getMovedClassName() {
		return getRenamedClassName();
	}

	public UMLClass getMovedClass() {
		return getRenamedClass();
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalClass.codeRange()
				.setDescription("original type declaration")
				.setCodeElement(originalClass.getName()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(renamedClass.codeRange()
				.setDescription("moved and renamed type declaration")
				.setCodeElement(renamedClass.getName()));
		return ranges;
	}
}
